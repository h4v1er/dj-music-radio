package org.example.music.service;

import org.example.music.entity.UserTaste;
import java.util.Map;

/**
 * 用户品味画像服务接口
 */
public interface UserTasteService {

    /**
     * 获取用户品味画像
     */
    UserTaste getUserTaste(Long userId);

    /**
     * 从歌单导入更新品味
     */
    void updateFromPlaylist(Long userId, Long playlistId);

    /**
     * 收藏/取消收藏时更新
     */
    void onFavorite(Long userId, Long songId, boolean isAdd);

    /**
     * 播放时更新
     */
    void onPlay(Long userId, Long songId);

    /**
     * 获取情绪偏好分布
     */
    Map<String, Double> getEmotionDistribution(Long userId);

    /**
     * 强制刷新用户品味
     */
    UserTaste refreshTaste(Long userId);
}
