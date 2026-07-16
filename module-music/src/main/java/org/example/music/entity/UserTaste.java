package org.example.music.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 用户品味画像实体
 */
@TableName("user_taste")
public class UserTaste {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String emotionPrefs;
    private String topEmotions;
    private String tasteDesc;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ── Getters ──
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getEmotionPrefs() { return emotionPrefs; }
    public String getTopEmotions() { return topEmotions; }
    public String getTasteDesc() { return tasteDesc; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Setters ──
    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setEmotionPrefs(String emotionPrefs) { this.emotionPrefs = emotionPrefs; }
    public void setTopEmotions(String topEmotions) { this.topEmotions = topEmotions; }
    public void setTasteDesc(String tasteDesc) { this.tasteDesc = tasteDesc; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
