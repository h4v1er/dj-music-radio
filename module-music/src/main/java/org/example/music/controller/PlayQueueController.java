package org.example.music.controller;

import org.example.music.dto.PlayQueueStateDTO;
import org.example.music.dto.Result;
import org.example.music.service.PlayQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/music/queue")
public class PlayQueueController {

    @Autowired
    private PlayQueueService playQueueService;

    /** GET /music/queue/state?userId=1 */
    @GetMapping("/state")
    public Result<PlayQueueStateDTO> state(@RequestParam(defaultValue = "1") Long userId) {
        return Result.ok(playQueueService.getState(userId));
    }

    /** PUT /music/queue/state */
    @PutMapping("/state")
    public Result<PlayQueueStateDTO> save(@RequestBody PlayQueueStateDTO state) {
        return Result.ok(playQueueService.saveState(state));
    }

    /** DELETE /music/queue/state?userId=1 */
    @DeleteMapping("/state")
    public Result<Void> clear(@RequestParam(defaultValue = "1") Long userId) {
        playQueueService.clearState(userId);
        return Result.ok();
    }
}
