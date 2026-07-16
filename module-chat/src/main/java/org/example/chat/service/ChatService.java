package org.example.chat.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.chat.client.MusicRecommendationClient;
import org.example.chat.client.RecRecommendationClient;
import org.example.chat.entity.ChatHistory;
import org.example.chat.mapper.ChatHistoryMapper;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final long DEFAULT_USER_ID = 1L;
    private static final int HISTORY_LIMIT = 10;
    private static final long DB_RETRY_INTERVAL_MS = 30_000L;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<Long, Deque<ChatMessage>> HISTORIES = new ConcurrentHashMap<>();

    private final ChatHistoryMapper chatHistoryMapper;
    private final MusicRecommendationClient musicRecommendationClient;
    private final RecRecommendationClient recRecommendationClient;
    private final DeepSeekChatClient deepSeekChatClient;
    private volatile long dbRetryAfter;

    public ChatService(
            ChatHistoryMapper chatHistoryMapper,
            MusicRecommendationClient musicRecommendationClient,
            RecRecommendationClient recRecommendationClient,
            DeepSeekChatClient deepSeekChatClient) {
        this.chatHistoryMapper = chatHistoryMapper;
        this.musicRecommendationClient = musicRecommendationClient;
        this.recRecommendationClient = recRecommendationClient;
        this.deepSeekChatClient = deepSeekChatClient;
    }

    public ChatSendResponse send(ChatSendRequest request) {
        ChatSendRequest safeRequest = request == null ? new ChatSendRequest(DEFAULT_USER_ID, "") : request;
        long userId = safeRequest.userId() == null ? DEFAULT_USER_ID : safeRequest.userId();
        String content = normalize(safeRequest.content());
        List<ChatMessage> recentHistory = history(userId);
        ChatMessage userMessage = new ChatMessage("user", content, now());
        DeepSeekChatClient.ChatIntent intent = deepSeekChatClient.analyzeIntent(content, recentHistory);
        List<String> songs = recommendSongs(userId, content, intent);
        String replyText = deepSeekChatClient.composeReply(content, intent, songs, recentHistory);
        if (replyText.isBlank()) {
            replyText = buildReply(content, intent);
        }
        ChatMessage reply = new ChatMessage("dj", replyText, now());

        appendHistory(userId, userMessage);
        appendHistory(userId, reply);

        return new ChatSendResponse(reply, songs);
    }

    public List<ChatMessage> history(Long userId) {
        long safeUserId = userId == null ? DEFAULT_USER_ID : userId;
        if (canUseDatabase()) {
            try {
                List<ChatHistory> records = chatHistoryMapper.selectList(new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, safeUserId)
                        .orderByDesc(ChatHistory::getCreateTime)
                        .orderByDesc(ChatHistory::getId)
                        .last("LIMIT " + HISTORY_LIMIT));
                if (!records.isEmpty()) {
                    Collections.reverse(records);
                    return records.stream().map(ChatService::toMessage).toList();
                }
            } catch (Exception e) {
                markDatabaseUnavailable();
            }
        }

        Deque<ChatMessage> history = HISTORIES.get(safeUserId);
        if (history == null || history.isEmpty()) {
            return List.of(new ChatMessage("dj", greeting() + "我是你的 DJ 助手，今天想听点什么？", now()));
        }
        return new ArrayList<>(history);
    }

    private void appendHistory(long userId, ChatMessage message) {
        if (canUseDatabase()) {
            try {
                chatHistoryMapper.insert(toEntity(userId, message));
            } catch (Exception e) {
                markDatabaseUnavailable();
            }
        }
        appendMemoryHistory(userId, message);
    }

    private boolean canUseDatabase() {
        return System.currentTimeMillis() >= dbRetryAfter;
    }

    private void markDatabaseUnavailable() {
        dbRetryAfter = System.currentTimeMillis() + DB_RETRY_INTERVAL_MS;
    }

    private static void appendMemoryHistory(long userId, ChatMessage message) {
        Deque<ChatMessage> history = HISTORIES.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(message);
            while (history.size() > HISTORY_LIMIT) {
                history.removeFirst();
            }
        }
    }

    private static ChatHistory toEntity(long userId, ChatMessage message) {
        ChatHistory entity = new ChatHistory();
        entity.setUserId(userId);
        entity.setRole(toDbRole(message.role()));
        entity.setContent(message.text());
        entity.setCreateTime(LocalDateTime.now());
        return entity;
    }

    private static ChatMessage toMessage(ChatHistory record) {
        String time = record.getCreateTime() == null
                ? now()
                : record.getCreateTime().toLocalTime().format(TIME_FORMATTER);
        return new ChatMessage(toClientRole(record.getRole()), record.getContent(), time);
    }

    private static String toDbRole(String role) {
        return "dj".equals(role) ? "assistant" : "user";
    }

    private static String toClientRole(String role) {
        return "assistant".equals(role) ? "dj" : "user";
    }

    private static String buildReply(String content, DeepSeekChatClient.ChatIntent intent) {
        if (intent != null && !intent.needRecommend()) {
            String lower = content.toLowerCase(Locale.ROOT);
            if (containsAny(lower, "故事", "讲吧", "继续")) {
                return "可以，我接着讲。夜色落下来，一个人沿着安静的街往前走，耳边的风声像在提醒他：答案就在下一盏灯后面。";
            }
            return "我在。你可以继续跟我聊，也可以告诉我现在的心情，我再决定要不要给你配歌。";
        }
        if (intent != null && intent.hasMeaningfulTags()) {
            String brief = intent.brief();
            if (!brief.isBlank() && !brief.equals(content)) {
                return "我听懂了，先按「" + brief + "」这个方向给你挑几首，节奏会尽量贴合你的状态。";
            }
        }

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

    private List<String> recommendSongs(long userId, String content, DeepSeekChatClient.ChatIntent intent) {
        if (intent != null && !intent.needRecommend()) {
            return List.of();
        }
        List<String> remoteSongs = remoteRecommendSongs(userId, content, intent);
        if (!remoteSongs.isEmpty()) {
            return remoteSongs;
        }
        return localRecommendSongs(intent == null ? content : intent.searchKeyword(content));
    }

    private List<String> remoteRecommendSongs(long userId, String content, DeepSeekChatClient.ChatIntent intent) {
        try {
            if (shouldUseDailyRecommend(content, intent)) {
                return extractSongNames(recRecommendationClient.daily(userId));
            }
            return extractSongNames(musicRecommendationClient.searchSongs(searchKeyword(content, intent)));
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<String> localRecommendSongs(String content) {
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

    private static boolean shouldUseDailyRecommend(String content, DeepSeekChatClient.ChatIntent intent) {
        String lower = content.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "推荐", "随便", "不知道", "今日", "daily")) {
            return intent == null || !intent.hasSpecificMusicRequest();
        }
        return false;
    }

    private static String searchKeyword(String content, DeepSeekChatClient.ChatIntent intent) {
        if (intent != null) {
            return intent.searchKeyword(content);
        }
        if (containsAny(content.toLowerCase(Locale.ROOT), "摇滚", "rock")) {
            return "摇滚";
        }
        if (containsAny(content.toLowerCase(Locale.ROOT), "电子", "电音", "edm")) {
            return "电子";
        }
        if (containsAny(content.toLowerCase(Locale.ROOT), "舒缓", "放松", "睡前", "安静")) {
            return "舒缓";
        }
        return content;
    }

    private static List<String> extractSongNames(Object body) {
        List<String> songs = new ArrayList<>();
        collectSongNames(body, songs);
        return songs.stream().filter(song -> !song.isBlank()).limit(3).toList();
    }

    @SuppressWarnings("unchecked")
    private static void collectSongNames(Object value, List<String> songs) {
        if (value == null || songs.size() >= 3) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectSongNames(item, songs);
                if (songs.size() >= 3) {
                    return;
                }
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            String song = formatSong((Map<String, Object>) map);
            if (!song.isBlank()) {
                songs.add(song);
                return;
            }
            for (String key : List.of("data", "records", "list", "songs", "items")) {
                collectSongNames(map.get(key), songs);
                if (songs.size() >= 3) {
                    return;
                }
            }
        }
    }

    private static String formatSong(Map<String, Object> map) {
        String title = firstText(map, "title", "name", "songName", "song");
        if (title.isBlank()) {
            return "";
        }
        String artist = firstText(map, "artist", "singer", "author");
        return artist.isBlank() ? title : title + " - " + artist;
    }

    private static String firstText(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return "";
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

}
