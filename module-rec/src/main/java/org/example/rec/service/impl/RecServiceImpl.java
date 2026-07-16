package org.example.rec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.rec.client.MusicFeignClient;
import org.example.rec.dto.ResultDTO;
import org.example.rec.dto.SongDTO;
import org.example.rec.entity.DailyRecommend;
import org.example.rec.entity.UserBehavior;
import org.example.rec.mapper.DailyRecommendMapper;
import org.example.rec.mapper.UserBehaviorMapper;
import org.example.rec.service.RecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecServiceImpl extends ServiceImpl<DailyRecommendMapper, DailyRecommend> implements RecService {

    private static final String REDIS_HOT_KEY = "song:hot:rank";
    private static final int MAX_HISTORY = 50;

    @Autowired
    private UserBehaviorMapper behaviorMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired(required = false)
    private MusicFeignClient musicFeignClient;

    // ============ 热门榜单 ============
    @Override
    public List<Map<String, Object>> getHotRanking(int topN) {
        Set<ZSetOperations.TypedTuple<String>> set =
                redisTemplate.opsForZSet().reverseRangeWithScores(REDIS_HOT_KEY, 0, topN - 1);

        if (set == null || set.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : set) {
            Integer songId = Integer.valueOf(tuple.getValue());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("songId", songId);
            item.put("score", tuple.getScore() != null ? tuple.getScore().longValue() : 0);
            // 通过 Feign 补全歌曲名称和歌手
            enrichSongInfo(item, songId);
            result.add(item);
        }
        return result;
    }

    // ============ 今日推荐 ============
    @Override
    public List<Map<String, Object>> getDailyRecommend(Integer userId) {
        LambdaQueryWrapper<DailyRecommend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyRecommend::getUserId, userId)
               .eq(DailyRecommend::getPushDate, LocalDate.now())
               .orderByAsc(DailyRecommend::getId)
               .last("LIMIT 10");

        List<DailyRecommend> list = baseMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DailyRecommend dr : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("songId", dr.getSongId());
            item.put("reason", dr.getReason());
            enrichSongInfo(item, dr.getSongId());
            result.add(item);
        }
        return result;
    }

    /**
     * 通过 Feign 补全歌曲的标题和歌手信息
     * 若音乐服务不可用，至少保留 songId，前端可降级展示
     */
    private void enrichSongInfo(Map<String, Object> item, Integer songId) {
        if (musicFeignClient == null) {
            item.putIfAbsent("title", "歌曲#" + songId);
            item.putIfAbsent("artist", "未知歌手");
            return;
        }
        try {
            ResultDTO<SongDTO> r = musicFeignClient.getSongById(songId);
            if (r != null && r.isSuccess() && r.getData() != null) {
                item.put("title", r.getData().getTitle());
                item.put("artist", r.getData().getArtist());
                return;
            }
        } catch (Exception ignored) { }
        // 回退
        item.putIfAbsent("title", "歌曲#" + songId);
        item.putIfAbsent("artist", "未知歌手");
    }

    // ============ 用户偏好 ============
    @Override
    public List<String> getUserPreferences(Integer userId) {
        // 1. 获取用户播放/收藏最多的歌曲
        List<Map<String, Object>> topSongs = getUserTopSongs(userId, 20);
        if (topSongs.isEmpty()) {
            return List.of("摇滚", "电子", "流行"); // 新用户，给默认标签
        }

        // 2. 通过 Feign 统计各流派出现次数
        Map<String, Integer> genreCount = new LinkedHashMap<>();
        for (Map<String, Object> row : topSongs) {
            Integer songId = (Integer) row.get("song_id");
            String genre = getSongGenre(songId);
            if (genre != null && !genre.isBlank()) {
                genreCount.merge(genre, 1, Integer::sum);
            }
        }

        // 3. 按频次排序，取前 5 个
        if (genreCount.isEmpty()) {
            return List.of("摇滚", "电子", "流行");
        }
        return genreCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** 获取用户播放最多的歌曲（供偏好分析和定时任务复用） */
    private List<Map<String, Object>> getUserTopSongs(Integer userId, int limit) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserBehavior> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.select("song_id, COUNT(*) AS cnt")
               .eq("user_id", userId)
               .in("action", "play", "like")
               .groupBy("song_id")
               .orderByDesc("cnt")
               .last("LIMIT " + limit);
        return behaviorMapper.selectMaps(wrapper);
    }

    /** 通过 Feign 获取歌曲流派 */
    private String getSongGenre(Integer songId) {
        if (musicFeignClient == null) return null;
        try {
            ResultDTO<SongDTO> r = musicFeignClient.getSongById(songId);
            if (r != null && r.isSuccess() && r.getData() != null) {
                return r.getData().getGenre();
            }
        } catch (Exception ignored) { }
        return null;
    }

    // ============ 相似歌曲 ============
    @Override
    public List<Map<String, Object>> getSimilarSongs(Integer songId) {

        // 第一步: 尝试通过 Feign 从音乐服务获取歌曲流派和歌手信息
        String genre = null;
        String artist = null;
        if (musicFeignClient != null) {
            try {
                ResultDTO<SongDTO> r = musicFeignClient.getSongById(songId);
                if (r != null && r.isSuccess() && r.getData() != null) {
                    genre = r.getData().getGenre();
                    artist = r.getData().getArtist();
                }
            } catch (Exception ignored) {
                // 音乐服务不可用，后面走回退
            }
        }

        // 第二步: 优先按流派搜索同风格歌曲
        if (genre != null && !genre.isBlank()) {
            List<Map<String, Object>> songs = searchByGenre(songId, genre);
            if (!songs.isEmpty()) return songs;
        }

        // 第三步: 流派不够，按歌手搜索
        if (artist != null && !artist.isBlank()) {
            List<Map<String, Object>> songs = searchByArtist(songId, artist);
            if (!songs.isEmpty()) return songs;
        }

        // 第四步: 回退 — 基于用户行为的协同过滤
        return getBehaviorBasedSimilar(songId);
    }

    /** 按流派搜索同风格歌曲 */
    private List<Map<String, Object>> searchByGenre(Integer songId, String genre) {
        try {
            ResultDTO<List<SongDTO>> r = musicFeignClient.searchSongs(genre);
            if (r != null && r.isSuccess() && r.getData() != null) {
                return r.getData().stream()
                        .filter(s -> !s.getId().equals(songId))   // 排除自己
                        .limit(10)
                        .map(s -> {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("songId", s.getId());
                            item.put("title", s.getTitle());
                            item.put("artist", s.getArtist());
                            item.put("reason", "同流派 · " + genre);
                            return item;
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) { }
        return Collections.emptyList();
    }

    /** 按歌手搜索同风格歌曲 */
    private List<Map<String, Object>> searchByArtist(Integer songId, String artist) {
        try {
            ResultDTO<List<SongDTO>> r = musicFeignClient.searchSongs(artist);
            if (r != null && r.isSuccess() && r.getData() != null) {
                return r.getData().stream()
                        .filter(s -> !s.getId().equals(songId))
                        .limit(10)
                        .map(s -> {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("songId", s.getId());
                            item.put("title", s.getTitle());
                            item.put("artist", s.getArtist());
                            item.put("reason", "同歌手 · " + artist);
                            return item;
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) { }
        return Collections.emptyList();
    }

    /** 回退方案: 基于用户行为的协同过滤 — 和你有相似口味的人还喜欢… */
    private List<Map<String, Object>> getBehaviorBasedSimilar(Integer songId) {
        List<Map<String, Object>> rows = behaviorMapper.findSimilarByBehavior(songId, 10);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("songId", row.get("song_id"));
            item.put("reason", "和你口味相似的人也喜欢这首歌");
            result.add(item);
        }
        return result;
    }

    // ============ 用户行为上报 ============
    @Override
    public void reportBehavior(UserBehavior behavior) {
        // 1. 写数据库
        behavior.setCreateTime(LocalDateTime.now());
        behaviorMapper.insert(behavior);

        // 2. 更新 Redis 热门榜单：播放 +1 / 收藏 +3 / 跳过 +0 / 分享 +2
        int scoreIncr = switch (behavior.getAction()) {
            case "like" -> 3;
            case "share" -> 2;
            case "play" -> 1;
            default -> 0;
        };
        if (scoreIncr > 0) {
            String songIdStr = String.valueOf(behavior.getSongId());
            redisTemplate.opsForZSet().incrementScore(REDIS_HOT_KEY, songIdStr, scoreIncr);
        }
    }
}
