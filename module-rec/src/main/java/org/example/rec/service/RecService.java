package org.example.rec.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.rec.entity.DailyRecommend;
import org.example.rec.entity.UserBehavior;

import java.util.List;
import java.util.Map;

public interface RecService extends IService<DailyRecommend> {

    /** 热门榜单 TOP N — 从 Redis ZSET 读取 */
    List<Map<String, Object>> getHotRanking(int topN);

    /** 今日推荐 — 根据用户偏好 */
    List<Map<String, Object>> getDailyRecommend(Integer userId);

    /** 相似歌曲推荐 — 同流派/同歌手 */
    List<Map<String, Object>> getSimilarSongs(Integer songId);

    /** 上报用户行为 — 写库 + 更新 Redis 热度 */
    void reportBehavior(UserBehavior behavior);

    /** 获取用户偏好标签 — 从行为数据推导 */
    List<String> getUserPreferences(Integer userId);
}
