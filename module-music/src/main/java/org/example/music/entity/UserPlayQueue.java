package org.example.music.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("user_play_queue")
public class UserPlayQueue {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long songId;
    private String source;
    private String sourceId;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private String coverUrl;
    private String filePath;
    private Integer duration;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getSongId() { return songId; }
    public String getSource() { return source; }
    public String getSourceId() { return sourceId; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getGenre() { return genre; }
    public String getCoverUrl() { return coverUrl; }
    public String getFilePath() { return filePath; }
    public Integer getDuration() { return duration; }
    public Integer getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setSongId(Long songId) { this.songId = songId; }
    public void setSource(String source) { this.source = source; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setAlbum(String album) { this.album = album; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
