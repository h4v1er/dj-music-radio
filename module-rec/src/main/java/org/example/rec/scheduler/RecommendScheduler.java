package org.example.rec.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.rec.client.MusicFeignClient;
import org.example.rec.dto.ResultDTO;
import org.example.rec.dto.SongDTO;
import org.example.rec.entity.DailyRecommend;
import org.example.rec.entity.UserBehavior;
import org.example.rec.mapper.DailyRecommendMapper;
import org.example.rec.mapper.UserBehaviorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * 每日推荐定时任务
 * 每天凌晨 2:00 自动执行，分析用户行为并生成个性化推荐
 */
@Component
public class RecommendScheduler {

    @Autowired
    private UserBehaviorMapper behaviorMapper;
    @Autowired
    private DailyRecommendMapper dailyRecommendMapper;
    @Autowired(required = false)
    private MusicFeignClient musicFeignClient;

    /**
     * 每天凌晨 2:00 执行
     * 格式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyRecommendations() {
        System.out.println("[每日推荐] 定时任务开始执行 — " + LocalDate.now());

        // 1. 获取所有有行为记录的用户
        List<Integer> userIds = getDistinctUserIds();
        if (userIds.isEmpty()) {
            System.out.println("[每日推荐] 暂无用户行为数据，跳过");
            return;
        }

        System.out.println("[每日推荐] 找到 " + userIds.size() + " 个活跃用户");

        // 2. 为每个用户生成推荐
        int totalGenerated = 0;
        for (Integer userId : userIds) {
            List<DailyRecommend> recs = generateForUser(userId);
            if (!recs.isEmpty()) {
                // 先删旧、再插新
                deleteTodayRecommendations(userId);
                for (DailyRecommend rec : recs) {
                    dailyRecommendMapper.insert(rec);
                }
                totalGenerated += recs.size();
            }
        }

        System.out.println("[每日推荐] 完成！共为 " + userIds.size() + " 个用户生成 " + totalGenerated + " 条推荐");
    }

    /** 获取所有有行为记录的用户ID */
    private List<Integer> getDistinctUserIds() {
        QueryWrapper<UserBehavior> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT user_id").lambda().in(UserBehavior::getAction, "play", "like");
        return behaviorMapper.selectList(wrapper).stream()
                .map(UserBehavior::getUserId)
                .distinct()
                .toList();
    }

    /** 为单个用户生成推荐 */
    private List<DailyRecommend> generateForUser(Integer userId) {
        // 获取用户最近常听的歌曲（按播放次数排序，取前 5 首）
        List<Map<String, Object>> topSongs = getUserTopSongs(userId, 5);

        if (topSongs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> recommendedIds = new HashSet<>();
        List<DailyRecommend> result = new ArrayList<>();

        // 对用户听的每首 top 歌曲，找相似推荐
        for (Map<String, Object> row : topSongs) {
            Integer songId = (Integer) row.get("song_id");

            // 尝试通过 Feign 获取流派，推荐同流派歌曲
            String genre = getSongGenre(songId);
            if (genre != null) {
                try {
                    ResultDTO<List<SongDTO>> r = musicFeignClient.searchSongs(genre);
                    if (r != null && r.isSuccess() && r.getData() != null) {
                        for (SongDTO s : r.getData()) {
                            if (!s.getId().equals(songId) && recommendedIds.add(s.getId())) {
                                DailyRecommend dr = new DailyRecommend();
                                dr.setUserId(userId);
                                dr.setSongId(s.getId());
                                dr.setReason("同流派 · " + genre);
                                dr.setPushDate(LocalDate.now());
                                result.add(dr);
                                if (result.size() >= 10) return result;
                            }
                        }
                    }
                } catch (Exception ignored) { }
            }

            // 行为协同过滤：喜欢这首歌的人也喜欢…
            if (result.size() < 10) {
                List<Map<String, Object>> similar = behaviorMapper.findSimilarByBehavior(songId, 5);
                for (Map<String, Object> sim : similar) {
                    Integer simId = (Integer) sim.get("song_id");
                    if (!simId.equals(songId) && recommendedIds.add(simId)) {
                        DailyRecommend dr = new DailyRecommend();
                        dr.setUserId(userId);
                        dr.setSongId(simId);
                        dr.setReason("和你口味相似的人也喜欢这首歌");
                        dr.setPushDate(LocalDate.now());
                        result.add(dr);
                        if (result.size() >= 10) return result;
                    }
                }
            }
        }

        return result;
    }

    /** 获取用户播放最多的歌曲 */
    private List<Map<String, Object>> getUserTopSongs(Integer userId, int limit) {
        QueryWrapper<UserBehavior> wrapper = new QueryWrapper<>();
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

    /** 删除用户今天的旧推荐（防止重复） */
    private void deleteTodayRecommendations(Integer userId) {
        LambdaQueryWrapper<DailyRecommend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyRecommend::getUserId, userId)
               .eq(DailyRecommend::getPushDate, LocalDate.now());
        dailyRecommendMapper.delete(wrapper);
    }
}
