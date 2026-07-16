package org.example.music.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("play_history")
public class PlayHistory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long songId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime playedAt;

    // ── Getters ──
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getSongId() { return songId; }
    public LocalDateTime getPlayedAt() { return playedAt; }

    // ── Setters ──
    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setSongId(Long songId) { this.songId = songId; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }
}
