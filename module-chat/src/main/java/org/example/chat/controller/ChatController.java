package org.example.chat.controller;

import java.util.List;

import org.example.chat.service.ChatService;
import org.example.chat.service.ChatService.ChatMessage;
import org.example.chat.service.ChatService.ChatSendRequest;
import org.example.chat.service.ChatService.ChatSendResponse;
import org.example.chat.service.ChatService.WeatherResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat/hello")
    public String hello() {
        return "👋 DJ 对话服务已就绪！";
    }

    @PostMapping("/chat/send")
    public ChatSendResponse send(@RequestBody ChatSendRequest request) {
        return chatService.send(request);
    }

    @GetMapping("/chat/history")
    public List<ChatMessage> history(@RequestParam(defaultValue = "1") Long userId) {
        return chatService.history(userId);
    }

    @GetMapping("/chat/weather")
    public WeatherResponse weather(@RequestParam(defaultValue = "北京") String city) {
        return chatService.weather(city);
    }
}
