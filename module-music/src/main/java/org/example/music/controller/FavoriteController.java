package org.example.music.controller;

import org.example.music.dto.Result;
import org.example.music.entity.Song;
import org.example.music.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/music/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /** GET /music/favorite/list?userId=1 */
    @GetMapping("/list")
    public Result<List<Song>> list(@RequestParam(defaultValue = "1") Long userId) {
        return Result.ok(favoriteService.getFavoriteSongs(userId));
    }

    /** POST /music/favorite/{songId}?userId=1 */
    @PostMapping("/{songId}")
    public Result<Void> add(@PathVariable Long songId, @RequestParam(defaultValue = "1") Long userId) {
        favoriteService.addFavorite(userId, songId);
        return Result.ok();
    }

    /** DELETE /music/favorite/{songId}?userId=1 */
    @DeleteMapping("/{songId}")
    public Result<Void> remove(@PathVariable Long songId, @RequestParam(defaultValue = "1") Long userId) {
        favoriteService.removeFavorite(userId, songId);
        return Result.ok();
    }

    /** GET /music/favorite/check/{songId}?userId=1 */
    @GetMapping("/check/{songId}")
    public Result<Map<String, Boolean>> check(@PathVariable Long songId,
                                              @RequestParam(defaultValue = "1") Long userId) {
        boolean favorited = favoriteService.isFavorited(userId, songId);
        return Result.ok(Map.of("favorited", favorited));
    }
}
