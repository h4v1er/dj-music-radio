package org.example.music.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("user_favorite")
public class UserFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long songId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ── Getters ──
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getSongId() { return songId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters ──
    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setSongId(Long songId) { this.songId = songId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
