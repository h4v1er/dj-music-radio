package org.example.music.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_player_state")
public class UserPlayerState {

    @TableId
    private Long userId;
    private String playMode;
    private String currentKey;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getUserId() { return userId; }
    public String getPlayMode() { return playMode; }
    public String getCurrentKey() { return currentKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setPlayMode(String playMode) { this.playMode = playMode; }
    public void setCurrentKey(String currentKey) { this.currentKey = currentKey; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
