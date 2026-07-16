package org.example.chat.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
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
    private static final int MAX_CANDIDATE_SONGS = 80;
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

        DeepSeekChatClient.ToolPlan plan = deepSeekChatClient.planToolUse(content, recentHistory);
        List<SongCandidate> candidates = executeTools(userId, content, plan);
        DeepSeekChatClient.AiReply aiReply = deepSeekChatClient.composeReply(content, plan, candidates, recentHistory);

        List<String> songs = aiReply.songs();
        if (songs.isEmpty() && plan.needTools() && !candidates.isEmpty() && aiReply.reply().isBlank()) {
            songs = candidates.stream().limit(3).map(SongCandidate::label).toList();
        }
        String replyText = aiReply.reply();
        if (replyText.isBlank()) {
            replyText = buildFallbackReply(content, plan, candidates);
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

    private List<SongCandidate> executeTools(long userId, String content, DeepSeekChatClient.ToolPlan plan) {
        if (plan == null || !plan.needTools()) {
            return List.of();
        }

        Map<String, SongCandidate> candidates = new LinkedHashMap<>();
        List<String> tools = plan.tools().isEmpty() ? List.of("music.list") : plan.tools();
        for (String tool : tools) {
            if (candidates.size() >= MAX_CANDIDATE_SONGS) {
                break;
            }
            try {
                switch (tool) {
                    case "music.search" -> collectSongCandidates(
                            musicRecommendationClient.searchSongs(searchKeyword(content, plan)),
                            candidates);
                    case "music.list" -> collectSongCandidates(
                            musicRecommendationClient.listSongs(1, Math.min(plan.limit(), MAX_CANDIDATE_SONGS)),
                            candidates);
                    case "rec.daily" -> collectSongCandidates(recRecommendationClient.daily(userId), candidates);
                    case "rec.hot" -> collectSongCandidates(recRecommendationClient.hot(), candidates);
                    case "rec.preferences" -> {
                        // Preference tags are useful for planning, but they are not songs by themselves.
                        recRecommendationClient.preferences(userId);
                    }
                    default -> {
                        // Ignore unknown model-planned tools so the chat path stays resilient.
                    }
                }
            } catch (Exception ignored) {
                // A single tool failure should not break normal chat.
            }
        }

        if (candidates.isEmpty()) {
            try {
                collectSongCandidates(musicRecommendationClient.listSongs(1, 50), candidates);
            } catch (Exception ignored) {
                // No local fake songs: an empty candidate pool should stay visible to the AI/fallback.
            }
        }

        return candidates.values().stream().limit(MAX_CANDIDATE_SONGS).toList();
    }

    private static String searchKeyword(String content, DeepSeekChatClient.ToolPlan plan) {
        if (plan != null && plan.keyword() != null && !plan.keyword().isBlank()) {
            return plan.keyword();
        }
        return content;
    }

    private static String buildFallbackReply(
            String content,
            DeepSeekChatClient.ToolPlan plan,
            List<SongCandidate> candidates) {
        if (plan != null && plan.needTools()) {
            if (candidates == null || candidates.isEmpty()) {
                return "我理解你想找歌，但现在项目音乐服务没有返回可用歌曲。你可以换个关键词，或者先确认 music/rec 服务和歌库数据。";
            }
            return "我先从项目歌库里挑了几首真实可用的歌。等 AI 服务恢复后，我会再按你的语境细选。";
        }
        if (content.contains("什么模型") || content.contains("你是谁")) {
            return "我是这个音乐电台里的 AI 助手，可以正常聊天，也可以在需要时读取项目里的音乐和推荐数据。";
        }
        return "我在，可以继续聊。需要音乐、推荐、故事或别的问题，都可以直接说。";
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

    @SuppressWarnings("unchecked")
    private static void collectSongCandidates(Object value, Map<String, SongCandidate> candidates) {
        if (value == null || candidates.size() >= MAX_CANDIDATE_SONGS) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectSongCandidates(item, candidates);
                if (candidates.size() >= MAX_CANDIDATE_SONGS) {
                    return;
                }
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            SongCandidate song = toSongCandidate((Map<String, Object>) map);
            if (song != null) {
                candidates.putIfAbsent(song.key(), song);
                return;
            }
            for (String key : List.of("data", "records", "list", "songs", "items")) {
                collectSongCandidates(map.get(key), candidates);
                if (candidates.size() >= MAX_CANDIDATE_SONGS) {
                    return;
                }
            }
        }
    }

    private static SongCandidate toSongCandidate(Map<String, Object> map) {
        String title = firstText(map, "title", "name", "songName", "song");
        if (title.isBlank()) {
            return null;
        }
        return new SongCandidate(
                longValue(firstValue(map, "id", "songId", "song_id")),
                title,
                firstText(map, "artist", "singer", "author"),
                firstText(map, "album", "albumName"),
                firstText(map, "genre", "style"),
                firstText(map, "emotionTags", "emotion_tags", "tags"),
                intValue(firstValue(map, "playCount", "play_count", "score", "rank")),
                firstText(map, "source"),
                firstText(map, "reason")
        );
    }

    private static Object firstValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String firstText(Map<String, Object> map, String... keys) {
        Object value = firstValue(map, keys);
        return value == null ? "" : value.toString().trim();
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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

    public record SongCandidate(
            Long id,
            String title,
            String artist,
            String album,
            String genre,
            String emotionTags,
            Integer playCount,
            String source,
            String reason) {

        public String key() {
            if (id != null) {
                return "id:" + id;
            }
            return (title + "||" + artist).toLowerCase();
        }

        public String label() {
            return artist == null || artist.isBlank() ? title : title + " - " + artist;
        }

        public String brief() {
            List<String> parts = new ArrayList<>();
            parts.add(label());
            if (genre != null && !genre.isBlank()) {
                parts.add("genre=" + genre);
            }
            if (emotionTags != null && !emotionTags.isBlank()) {
                parts.add("tags=" + emotionTags);
            }
            if (album != null && !album.isBlank()) {
                parts.add("album=" + album);
            }
            if (reason != null && !reason.isBlank()) {
                parts.add("reason=" + reason);
            }
            if (playCount != null) {
                parts.add("score=" + playCount);
            }
            return String.join(" | ", parts);
        }
    }
}
