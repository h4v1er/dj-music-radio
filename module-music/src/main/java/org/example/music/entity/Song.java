package org.example.music.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("song")
public class Song {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private Integer duration;
    private String coverUrl;
    private String source;
    private String sourceId;
    private String filePath;
    private String lyric;
    private Integer playCount;
    private String emotionTags;
    private Integer emotionAnalyzed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ── Getters ──
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getGenre() { return genre; }
    public Integer getDuration() { return duration; }
    public String getCoverUrl() { return coverUrl; }
    public String getSource() { return source; }
    public String getSourceId() { return sourceId; }
    public String getFilePath() { return filePath; }
    public String getLyric() { return lyric; }
    public Integer getPlayCount() { return playCount; }
    public String getEmotionTags() { return emotionTags; }
    public Integer getEmotionAnalyzed() { return emotionAnalyzed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Setters ──
    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setAlbum(String album) { this.album = album; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setSource(String source) { this.source = source; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setLyric(String lyric) { this.lyric = lyric; }
    public void setPlayCount(Integer playCount) { this.playCount = playCount; }
    public void setEmotionTags(String emotionTags) { this.emotionTags = emotionTags; }
    public void setEmotionAnalyzed(Integer emotionAnalyzed) { this.emotionAnalyzed = emotionAnalyzed; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
