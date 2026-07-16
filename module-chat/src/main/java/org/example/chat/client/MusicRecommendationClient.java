package org.example.chat.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "module-music", contextId = "musicRecommendationClient")
public interface MusicRecommendationClient {

    @GetMapping("/music/song/search")
    Object searchSongs(@RequestParam("kw") String keyword);

    @GetMapping("/music/song/list")
    Object listSongs(@RequestParam("page") int page, @RequestParam("size") int size);
}
