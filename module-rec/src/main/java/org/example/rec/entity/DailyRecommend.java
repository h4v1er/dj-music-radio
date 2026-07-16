package org.example.rec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

/**
 * 每日推荐记录表
 * 由定时任务每天凌晨生成，存储每个用户的个性化推荐结果
 */
@TableName("daily_recommend")
public class DailyRecommend {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer songId;
    /** 推荐理由，展示给用户看 */
    private String reason;
    private LocalDate pushDate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getSongId() {
        return songId;
    }

    public void setSongId(Integer songId) {
        this.songId = songId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getPushDate() {
        return pushDate;
    }

    public void setPushDate(LocalDate pushDate) {
        this.pushDate = pushDate;
    }
}
