package org.example.music.controller;

import org.example.music.dto.Result;
import org.example.music.entity.Song;
import org.example.music.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/music/history")
public class HistoryController {

    @Autowired
    private HistoryService historyService;

    /** GET /music/history/list?userId=1 */
    @GetMapping("/list")
    public Result<List<Song>> list(@RequestParam(defaultValue = "1") Long userId) {
        return Result.ok(historyService.getPlayHistory(userId));
    }

    /** POST /music/history {userId, songId} */
    @PostMapping
    public Result<Void> record(@RequestBody Map<String, Long> body) {
        Long userId = body.getOrDefault("userId", 1L);
        Long songId = body.get("songId");
        if (songId == null) {
            return Result.fail(400, "songId 不能为空");
        }
        historyService.recordPlay(userId, songId);
        return Result.ok();
    }
}
