package org.example.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "module-music")
public interface MusicClient {

    @GetMapping("/music/favorite/list")
    Map<String, Object> favoriteList(@RequestParam("userId") Long userId);

    @PostMapping("/music/favorite/{songId}")
    Map<String, Object> addFavorite(@PathVariable("songId") Long songId,
                                    @RequestParam("userId") Long userId);

    @DeleteMapping("/music/favorite/{songId}")
    Map<String, Object> removeFavorite(@PathVariable("songId") Long songId,
                                       @RequestParam("userId") Long userId);

    @GetMapping("/music/history/list")
    Map<String, Object> historyList(@RequestParam("userId") Long userId);

    @PostMapping("/music/history")
    Map<String, Object> recordHistory(@RequestBody Map<String, Long> body);
}
