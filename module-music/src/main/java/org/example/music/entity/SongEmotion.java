package org.example.music.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 歌曲情绪画像实体
 */
@TableName("song_emotion")
public class SongEmotion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long songId;
    private String primaryEmotion;
    private String secondaryEmotion;
    private Integer valence;
    private Integer arousal;
    private Integer emotionIntensity;
    private String moodTags;
    private String lyricTheme;
    private String lyricKeywords;
    private String suitableScenes;
    private Integer analyzed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ── Getters ──
    public Long getId() { return id; }
    public Long getSongId() { return songId; }
    public String getPrimaryEmotion() { return primaryEmotion; }
    public String getSecondaryEmotion() { return secondaryEmotion; }
    public Integer getValence() { return valence; }
    public Integer getArousal() { return arousal; }
    public Integer getEmotionIntensity() { return emotionIntensity; }
    public String getMoodTags() { return moodTags; }
    public String getLyricTheme() { return lyricTheme; }
    public String getLyricKeywords() { return lyricKeywords; }
    public String getSuitableScenes() { return suitableScenes; }
    public Integer getAnalyzed() { return analyzed; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters ──
    public void setId(Long id) { this.id = id; }
    public void setSongId(Long songId) { this.songId = songId; }
    public void setPrimaryEmotion(String primaryEmotion) { this.primaryEmotion = primaryEmotion; }
    public void setSecondaryEmotion(String secondaryEmotion) { this.secondaryEmotion = secondaryEmotion; }
    public void setValence(Integer valence) { this.valence = valence; }
    public void setArousal(Integer arousal) { this.arousal = arousal; }
    public void setEmotionIntensity(Integer emotionIntensity) { this.emotionIntensity = emotionIntensity; }
    public void setMoodTags(String moodTags) { this.moodTags = moodTags; }
    public void setLyricTheme(String lyricTheme) { this.lyricTheme = lyricTheme; }
    public void setLyricKeywords(String lyricKeywords) { this.lyricKeywords = lyricKeywords; }
    public void setSuitableScenes(String suitableScenes) { this.suitableScenes = suitableScenes; }
    public void setAnalyzed(Integer analyzed) { this.analyzed = analyzed; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
