package org.example.chat.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    @GetMapping("/chat/hello")
    public String hello() {
        return "👋 DJ 对话服务已就绪！";
    }
}
