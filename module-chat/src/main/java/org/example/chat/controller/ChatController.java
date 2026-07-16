package org.example.chat.controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private static final long DEFAULT_USER_ID = 1L;
    private static final int HISTORY_LIMIT = 10;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<Long, Deque<ChatMessage>> HISTORIES = new ConcurrentHashMap<>();

    @GetMapping("/chat/hello")
    public String hello() {
        return "👋 DJ 对话服务已就绪！";
    }

    @PostMapping("/chat/send")
    public ChatSendResponse send(@RequestBody ChatSendRequest request) {
        long userId = request.userId() == null ? DEFAULT_USER_ID : request.userId();
        String content = normalize(request.content());
        ChatMessage userMessage = new ChatMessage("user", content, now());
        ChatMessage reply = new ChatMessage("dj", buildReply(content), now());
        List<String> songs = recommendSongs(content);

        appendHistory(userId, userMessage);
        appendHistory(userId, reply);

        return new ChatSendResponse(reply, songs);
    }

    @GetMapping("/chat/history")
    public List<ChatMessage> history(@RequestParam(defaultValue = "1") Long userId) {
        Deque<ChatMessage> history = HISTORIES.get(userId == null ? DEFAULT_USER_ID : userId);
        if (history == null || history.isEmpty()) {
            return List.of(new ChatMessage("dj", greeting() + "我是你的 DJ 助手，今天想听点什么？", now()));
        }
        return new ArrayList<>(history);
    }

    @GetMapping("/chat/weather")
    public WeatherResponse weather(@RequestParam(defaultValue = "北京") String city) {
        String safeCity = normalizeCity(city);
        WeatherSnapshot snapshot = demoWeather(safeCity);
        return new WeatherResponse(safeCity, snapshot.icon(), snapshot.temp(), snapshot.text(), greeting());
    }

    private static void appendHistory(long userId, ChatMessage message) {
        Deque<ChatMessage> history = HISTORIES.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(message);
            while (history.size() > HISTORY_LIMIT) {
                history.removeFirst();
            }
        }
    }

    private static String buildReply(String content) {
        String lower = content.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "摇滚", "rock")) {
            return "收到，给你切到摇滚模式。先来三首有力量感的歌，把节奏拉起来。";
        }
        if (containsAny(lower, "电子", "电音", "edm")) {
            return "安排电子氛围。适合工作、写代码或者夜里放空，我先给你推三首。";
        }
        if (containsAny(lower, "舒缓", "放松", "睡前", "安静")) {
            return "那就放慢一点。给你挑几首舒缓的歌，适合休息和整理心情。";
        }
        if (containsAny(lower, "推荐", "随便", "不知道")) {
            return "没问题，我按当前时间和默认偏好给你先做一组推荐。";
        }
        return "我记下了。现在先按你的描述做一组音乐推荐，后面会继续根据播放和收藏调整。";
    }

    private static List<String> recommendSongs(String content) {
        String lower = content.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "摇滚", "rock")) {
            return List.of("示例摇滚 01 - Aurora Band", "城市失眠 - Echo Road", "逆光电台 - The North");
        }
        if (containsAny(lower, "电子", "电音", "edm")) {
            return List.of("Neon Drive - DJ Sample", "午夜合成器 - Metro Pulse", "蓝色频率 - Signal Lab");
        }
        if (containsAny(lower, "舒缓", "放松", "睡前", "安静")) {
            return List.of("晚风练习曲 - Soft Lake", "雨后房间 - Quiet Room", "慢速星光 - Mellow Sky");
        }
        return List.of("今日开场 - DJ Radio", "晴天漫游 - Sample Artist", "低速公路 - Demo Band");
    }

    private static WeatherSnapshot demoWeather(String city) {
        if (city.contains("上海")) {
            return new WeatherSnapshot("🌦️", "30°", "多云");
        }
        if (city.contains("广州") || city.contains("深圳")) {
            return new WeatherSnapshot("🌧️", "31°", "阵雨");
        }
        if (city.contains("成都")) {
            return new WeatherSnapshot("🌥️", "27°", "阴");
        }
        return new WeatherSnapshot("☀️", "28°", "晴");
    }

    private static boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "推荐一些歌";
        }
        return value.trim();
    }

    private static String normalizeCity(String city) {
        if (city == null || city.isBlank()) {
            return "北京";
        }
        return city.trim();
    }

    private static String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 6) {
            return "夜深了，来点轻一点的音乐？";
        }
        if (hour < 12) {
            return "早上好，来点有活力的音乐？";
        }
        if (hour < 18) {
            return "下午好，想听点什么？";
        }
        return "晚上好，今天想用什么歌收尾？";
    }

    private static String now() {
        return LocalTime.now().format(TIME_FORMATTER);
    }

    public record ChatSendRequest(Long userId, String content) {
    }

    public record ChatSendResponse(ChatMessage reply, List<String> songs) {
    }

    public record ChatMessage(String role, String text, String time) {
    }

    public record WeatherResponse(String city, String icon, String temp, String text, String greeting) {
    }

    private record WeatherSnapshot(String icon, String temp, String text) {
    }
}
