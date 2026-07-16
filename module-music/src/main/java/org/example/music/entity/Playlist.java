package org.example.music.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("playlist")
public class Playlist {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Long userId;
    private String coverUrl;
    private Integer songCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ── Getters ──
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Long getUserId() { return userId; }
    public String getCoverUrl() { return coverUrl; }
    public Integer getSongCount() { return songCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Setters ──
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setSongCount(Integer songCount) { this.songCount = songCount; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
