package org.example.music.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;

/**
 * 情绪词典实体
 */
@TableName("emotion_keyword")
public class EmotionKeyword {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String keyword;
    private String emotionTag;
    private BigDecimal weight;

    // ── Getters ──
    public Long getId() { return id; }
    public String getKeyword() { return keyword; }
    public String getEmotionTag() { return emotionTag; }
    public BigDecimal getWeight() { return weight; }

    // ── Setters ──
    public void setId(Long id) { this.id = id; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public void setEmotionTag(String emotionTag) { this.emotionTag = emotionTag; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
}
