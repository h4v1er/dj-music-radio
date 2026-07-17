package org.example.rec.controller;

import org.example.rec.entity.UserBehavior;
import org.example.rec.service.RecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rec")
public class RecController {

    @Autowired
    private RecService recService;

    /** 服务健康检查 */
    @GetMapping("/hello")
    public String hello() {
        return "📊 推荐服务已就绪！";
    }

    /** 热门榜单 — 默认返回 TOP 10 */
    @GetMapping("/hot")
    public List<Map<String, Object>> hot() {
        return recService.getHotRanking(10);
    }

    /** 今日推荐 */
    @GetMapping("/daily")
    public List<Map<String, Object>> daily(@RequestParam Integer userId) {
        return recService.getDailyRecommend(userId);
    }

    /** 手动刷新今日推荐 */
    @PostMapping("/daily/refresh")
    public List<Map<String, Object>> refreshDaily(@RequestParam Integer userId) {
        return recService.refreshDailyRecommend(userId);
    }

    /** 相似歌曲推荐 */
    @GetMapping("/similar")
    public List<Map<String, Object>> similar(@RequestParam Integer songId) {
        return recService.getSimilarSongs(songId);
    }

    /** 上报用户行为 */
    @PostMapping("/behavior")
    public String reportBehavior(@RequestBody UserBehavior behavior) {
        recService.reportBehavior(behavior);
        return "ok";
    }

    /** 用户偏好标签 */
    @GetMapping("/preferences")
    public List<String> preferences(@RequestParam Integer userId) {
        return recService.getUserPreferences(userId);
    }
}
