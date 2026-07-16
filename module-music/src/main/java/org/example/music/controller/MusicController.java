package org.example.music.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MusicController {

    @GetMapping("/music/hello")
    public String hello() {
        return "🎵 音乐中心服务已就绪！";
    }
}
