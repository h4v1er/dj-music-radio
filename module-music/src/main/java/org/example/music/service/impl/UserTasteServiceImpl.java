package org.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.music.analysis.EmotionCalculator;
import org.example.music.entity.*;
import org.example.music.mapper.*;
import org.example.music.service.UserTasteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserTasteServiceImpl implements UserTasteService {

    private static final Logger log = LoggerFactory.getLogger(UserTasteServiceImpl.class);

    @Autowired
    private UserTasteMapper userTasteMapper;
    @Autowired
    private SongEmotionMapper songEmotionMapper;
    @Autowired
    private PlaylistSongMapper playlistSongMapper;
    @Autowired
    private SongMapper songMapper;
    @Autowired
    private UserFavoriteMapper favoriteMapper;
    @Autowired
    private PlayHistoryMapper historyMapper;

    // 加权系数
    private static final double WEIGHT_FAVORITE = 3.0;
    private static final double WEIGHT_PLAY = 1.0;
    private static final double WEIGHT_IMPORT = 0.6;

    @Override
    public UserTaste getUserTaste(Long userId) {
        UserTaste taste = userTasteMapper.selectOne(
                new LambdaQueryWrapper<UserTaste>()
                        .eq(UserTaste::getUserId, userId));
        if (taste == null) {
            taste = new UserTaste();
            taste.setUserId(userId);
            taste.setEmotionPrefs("{}");
            taste.setTopEmotions("");
            taste.setTasteDesc("品味待探索");
        }
        return taste;
    }

    @Override
    public void updateFromPlaylist(Long userId, Long playlistId) {
        try {
            // 收集歌单中所有歌曲的情绪画像
            List<PlaylistSong> relations = playlistSongMapper.selectList(
                    new LambdaQueryWrapper<PlaylistSong>()
                            .eq(PlaylistSong::getPlaylistId, playlistId));
            if (relations.isEmpty()) return;

            List<Long> songIds = relations.stream()
                    .map(PlaylistSong::getSongId).collect(Collectors.toList());
            List<SongEmotion> emotions = songEmotionMapper.selectList(
                    new LambdaQueryWrapper<SongEmotion>()
                            .in(SongEmotion::getSongId, songIds));

            Map<String, Double> dist = new HashMap<>();
            for (SongEmotion se : emotions) {
                if (se.getPrimaryEmotion() != null) {
                    dist.merge(se.getPrimaryEmotion(), WEIGHT_IMPORT, Double::sum);
                }
            }
            updateTaste(userId, dist);
            log.info("品味更新(导入): userId={}, playlistId={}, emotionCount={}", userId, playlistId, emotions.size());
        } catch (Exception e) {
            log.error("品味更新(导入)失败: userId={}, playlistId={}", userId, playlistId, e);
        }
    }

    @Override
    public void onFavorite(Long userId, Long songId, boolean isAdd) {
        try {
            SongEmotion se = songEmotionMapper.selectOne(
                    new LambdaQueryWrapper<SongEmotion>()
                            .eq(SongEmotion::getSongId, songId));
            if (se == null) return;

            Map<String, Double> dist = new HashMap<>();
            double weight = isAdd ? WEIGHT_FAVORITE : -WEIGHT_FAVORITE;
            if (se.getPrimaryEmotion() != null) {
                dist.put(se.getPrimaryEmotion(), weight);
            }
            if (se.getSecondaryEmotion() != null && !se.getSecondaryEmotion().isEmpty()) {
                dist.put(se.getSecondaryEmotion(), weight * 0.5);
            }
            updateTaste(userId, dist);
            log.info("品味更新(收藏): userId={}, songId={}, add={}", userId, songId, isAdd);
        } catch (Exception e) {
            log.error("品味更新(收藏)失败: userId={}, songId={}", userId, songId, e);
        }
    }

    @Override
    public void onPlay(Long userId, Long songId) {
        try {
            SongEmotion se = songEmotionMapper.selectOne(
                    new LambdaQueryWrapper<SongEmotion>()
                            .eq(SongEmotion::getSongId, songId));
            if (se == null) return;

            Map<String, Double> dist = new HashMap<>();
            if (se.getPrimaryEmotion() != null) {
                dist.put(se.getPrimaryEmotion(), WEIGHT_PLAY);
            }
            updateTaste(userId, dist);
        } catch (Exception e) {
            log.error("品味更新(播放)失败: userId={}, songId={}", userId, songId, e);
        }
    }

    @Override
    public Map<String, Double> getEmotionDistribution(Long userId) {
        UserTaste taste = getUserTaste(userId);
        return parseDistribution(taste.getEmotionPrefs());
    }

    @Override
    public UserTaste refreshTaste(Long userId) {
        // 从所有历史数据重新计算品味（收藏 + 播放历史）
        Map<String, Double> dist = new HashMap<>();

        // 收藏的歌曲
        List<UserFavorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId));
        for (UserFavorite fav : favorites) {
            SongEmotion se = songEmotionMapper.selectOne(
                    new LambdaQueryWrapper<SongEmotion>()
                            .eq(SongEmotion::getSongId, fav.getSongId()));
            if (se != null && se.getPrimaryEmotion() != null) {
                dist.merge(se.getPrimaryEmotion(), WEIGHT_FAVORITE, Double::sum);
            }
        }

        // 播放历史
        List<PlayHistory> histories = historyMapper.selectList(
                new LambdaQueryWrapper<PlayHistory>()
                        .eq(PlayHistory::getUserId, userId));
        for (PlayHistory h : histories) {
            SongEmotion se = songEmotionMapper.selectOne(
                    new LambdaQueryWrapper<SongEmotion>()
                            .eq(SongEmotion::getSongId, h.getSongId()));
            if (se != null && se.getPrimaryEmotion() != null) {
                dist.merge(se.getPrimaryEmotion(), WEIGHT_PLAY, Double::sum);
            }
        }

        updateTaste(userId, dist);
        return getUserTaste(userId);
    }

    // ── 私有方法 ──

    private void updateTaste(Long userId, Map<String, Double> newWeights) {
        UserTaste taste = getUserTaste(userId);
        Map<String, Double> existing = parseDistribution(taste.getEmotionPrefs());

        // 指数移动平均融合
        for (Map.Entry<String, Double> e : newWeights.entrySet()) {
            existing.merge(e.getKey(), e.getValue(), (old, add) -> old * 0.9 + add * 0.1);
        }

        // 清理极低权重
        existing.entrySet().removeIf(e -> Math.abs(e.getValue()) < 0.01);

        // 更新
        taste.setEmotionPrefs(toJson(existing));
        taste.setTopEmotions(String.join(",", EmotionCalculator.topEmotions(existing, 3)));
        taste.setTasteDesc(EmotionCalculator.describeTaste(existing));

        if (taste.getId() == null) {
            userTasteMapper.insert(taste);
        } else {
            userTasteMapper.updateById(taste);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> parseDistribution(String json) {
        Map<String, Double> map = new LinkedHashMap<>();
        if (json == null || json.isEmpty() || "{}".equals(json)) return map;
        try {
            // 简单 JSON 解析: {"key": value, ...}
            String content = json.replaceAll("[{}\"]", "").trim();
            if (content.isEmpty()) return map;
            for (String pair : content.split(",")) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    map.put(kv[0].trim(), Double.parseDouble(kv[1].trim()));
                }
            }
        } catch (Exception e) {
            log.warn("解析品味分布JSON失败: {}", json, e);
        }
        return map;
    }

    private String toJson(Map<String, Double> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Double> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":")
                    .append(String.format("%.4f", e.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
