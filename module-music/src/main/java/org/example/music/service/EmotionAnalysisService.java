package org.example.music.service;

import org.example.music.entity.SongEmotion;
import java.util.List;

/**
 * 情绪分析服务接口
 */
public interface EmotionAnalysisService {

    /**
     * 分析单首歌曲（已有歌词时使用）
     */
    SongEmotion analyzeSong(Long songId);

    /**
     * 批量分析歌单中的所有歌曲（异步）
     */
    void analyzePlaylistAsync(Long playlistId);

    /**
     * 获取歌曲情绪画像
     */
    SongEmotion getSongEmotion(Long songId);

    /**
     * 按情绪标签搜索歌曲ID列表
     */
    List<Long> searchByEmotion(String emotionTag);

    /**
     * 批量获取情绪画像
     */
    List<SongEmotion> getBatchEmotions(List<Long> songIds);
}
