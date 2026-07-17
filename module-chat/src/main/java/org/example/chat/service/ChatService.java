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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String DEFAULT_WEATHER_LOCATION = "北京";
    private static final Pattern WEATHER_LOCATION_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fa5]{2,8}(?:市|县|区|州|盟|旗|省)?)"
                    + "(?:现在|今天|明天|后天|未来|这几天|最近)?"
                    + "(?:天气|气温|温度|下雨|晴|阴|小雨|大雨|多云)"
    );
    private static final List<String> WEATHER_LOCATIONS = List.of(
            "北京", "上海", "广州", "深圳", "成都", "杭州", "南京", "武汉", "重庆", "西安",
            "威海", "青岛", "济南", "烟台", "天津", "苏州", "宁波", "厦门", "福州", "长沙",
            "郑州", "合肥", "南昌", "昆明", "贵阳", "南宁", "海口", "三亚", "兰州", "银川",
            "西宁", "乌鲁木齐", "呼和浩特", "哈尔滨", "长春", "沈阳", "大连", "石家庄", "太原"
    );
    private static final Map<Long, Deque<ChatMessage>> HISTORIES = new ConcurrentHashMap<>();
    private static final String TOOL_LOCATION_CURRENT = "location.current";

    private final ChatHistoryMapper chatHistoryMapper;
    private final MusicRecommendationClient musicRecommendationClient;
    private final RecRecommendationClient recRecommendationClient;
    private final WeatherService weatherService;
    private final DeepSeekChatClient deepSeekChatClient;
    private volatile long dbRetryAfter;

    public ChatService(
            ChatHistoryMapper chatHistoryMapper,
            MusicRecommendationClient musicRecommendationClient,
            RecRecommendationClient recRecommendationClient,
            WeatherService weatherService,
            DeepSeekChatClient deepSeekChatClient) {
        this.chatHistoryMapper = chatHistoryMapper;
        this.musicRecommendationClient = musicRecommendationClient;
        this.recRecommendationClient = recRecommendationClient;
        this.weatherService = weatherService;
        this.deepSeekChatClient = deepSeekChatClient;
    }

    public ChatSendResponse send(ChatSendRequest request) {
        ChatSendRequest safeRequest = request == null ? new ChatSendRequest(DEFAULT_USER_ID, "", "", null) : request;
        long userId = safeRequest.userId() == null ? DEFAULT_USER_ID : safeRequest.userId();
        String content = normalize(safeRequest.content());
        LocationContext requestLocation = requestLocation(safeRequest);
        List<ChatMessage> recentHistory = history(userId);
        ChatMessage userMessage = new ChatMessage("user", content, now());

        String weatherLocation = resolveWeatherLocation(content, locationValue(requestLocation), recentHistory);
        DeepSeekChatClient.ToolPlan plan = deepSeekChatClient.planToolUse(content, recentHistory);
        plan = enrichLocationPlan(content, plan);
        plan = enrichWeatherPlan(content, weatherLocation, plan);
        if (needsClientLocation(plan) && requestLocation == null) {
            return new ChatSendResponse(
                    new ChatMessage("tool", "", now()),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(new ClientToolRequest("location-current", TOOL_LOCATION_CURRENT, "获取浏览器当前位置"))
            );
        }
        if (plan.needClarification()) {
            String question = plan.clarificationQuestion().isBlank()
                    ? "你想让我具体帮你做什么？"
                    : plan.clarificationQuestion();
            ChatMessage reply = new ChatMessage("dj", question, now());
            appendHistory(userId, userMessage);
            appendHistory(userId, reply);
            return new ChatSendResponse(reply, List.of(), List.of(), List.of());
        }
        ToolExecution execution = executeTools(userId, content, weatherLocation, requestLocation, plan);
        DeepSeekChatClient.AiReply aiReply = deepSeekChatClient.composeReply(
                content,
                plan,
                execution.toolResults(),
                execution.candidates(),
                recentHistory
        );

        List<SelectedSong> selectedSongs = selectedSongs(aiReply.selectedIndexes(), execution.candidates());
        if (selectedSongs.isEmpty() && plansMusicTools(plan) && !execution.candidates().isEmpty() && aiReply.reply().isBlank()) {
            selectedSongs = execution.candidates().stream().limit(3).map(ChatService::toSelectedSong).toList();
        }
        List<String> songs = selectedSongs.stream().map(SelectedSong::label).toList();
        String replyText = aiReply.reply();
        if (replyText.isBlank()) {
            replyText = buildFallbackReply(content, plan, execution);
        }

        ChatMessage reply = new ChatMessage("dj", replyText, now());
        appendHistory(userId, userMessage);
        appendHistory(userId, reply);

        return new ChatSendResponse(reply, songs, selectedSongs, execution.toolResults());
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

    private ToolExecution executeTools(
            long userId,
            String content,
            String weatherLocation,
            LocationContext requestLocation,
            DeepSeekChatClient.ToolPlan plan) {
        if (plan == null || !plan.needTools()) {
            return new ToolExecution(List.of(), List.of());
        }

        Map<String, SongCandidate> candidates = new LinkedHashMap<>();
        List<ToolResult> toolResults = new ArrayList<>();
        for (DeepSeekChatClient.ToolCallPlan call : plan.tools()) {
            if (call == null) {
                continue;
            }
            try {
                int before = candidates.size();
                switch (call.name()) {
                    case TOOL_LOCATION_CURRENT -> toolResults.add(locationResult(requestLocation));
                    case "music.search" -> {
                        Object body = musicRecommendationClient.searchSongs(keyword(content, call));
                        collectSongCandidates(body, candidates, "PROJECT_SEARCH", call.purpose());
                        toolResults.add(okResult(call, "本地歌库关键词搜索完成", candidates.size() - before));
                    }
                    case "music.catalog" -> {
                        Object body = musicRecommendationClient.listSongs(1, Math.min(call.limit(), MAX_CANDIDATE_SONGS));
                        collectSongCandidates(body, candidates, "PROJECT_CATALOG", call.purpose());
                        toolResults.add(okResult(call, "已获取本地真实歌曲候选池", candidates.size() - before));
                    }
                    case "music.neteaseSearch" -> {
                        Object body = musicRecommendationClient.searchNetease(keyword(content, call), Math.min(call.limit(), 30));
                        collectSongCandidates(body, candidates, "NETEASE_SEARCH", call.purpose());
                        toolResults.add(okResult(call, "网易云搜索完成", candidates.size() - before));
                    }
                    case "rec.daily" -> {
                        Object body = recRecommendationClient.daily(userId);
                        collectSongCandidates(body, candidates, "REC_DAILY", call.purpose());
                        toolResults.add(okResult(call, "每日推荐已返回", candidates.size() - before));
                    }
                    case "rec.hot" -> {
                        Object body = recRecommendationClient.hot();
                        collectSongCandidates(body, candidates, "REC_HOT", call.purpose());
                        toolResults.add(okResult(call, "热门榜单已返回", candidates.size() - before));
                    }
                    case "rec.preferences" -> {
                        Object body = recRecommendationClient.preferences(userId);
                        toolResults.add(new ToolResult(
                                call.name(),
                                call.purpose(),
                                "ok",
                                "用户偏好标签：" + summarize(body, 180),
                                0
                        ));
                    }
                    case "weather.now" -> {
                        WeatherService.WeatherResponse weather = weatherService.weather(location(content, weatherLocation, call));
                        toolResults.add(new ToolResult(
                                call.name(),
                                call.purpose(),
                                "ok",
                                "天气：" + weather.city() + " " + weather.text() + " " + weather.temp()
                                        + "，source=" + weather.source()
                                        + (weather.message() == null || weather.message().isBlank()
                                                ? ""
                                                : "，message=" + weather.message()),
                                0
                        ));
                    }
                    default -> toolResults.add(new ToolResult(
                            call.name(),
                            call.purpose(),
                            "ignored",
                            "未知工具，已跳过",
                            0
                    ));
                }
                if (candidates.size() >= MAX_CANDIDATE_SONGS) {
                    break;
                }
            } catch (Exception e) {
                toolResults.add(new ToolResult(
                        call.name(),
                        call.purpose(),
                        "failed",
                        trim(e.getMessage(), 180),
                        0
                ));
            }
        }

        if (plansMusicTools(plan) && candidates.isEmpty()) {
            try {
                Object body = musicRecommendationClient.listSongs(1, 50);
                collectSongCandidates(body, candidates, "PROJECT_CATALOG", "音乐工具无结果后的本地候选池兜底");
                toolResults.add(new ToolResult(
                        "music.catalog",
                        "音乐工具无结果后的本地候选池兜底",
                        "ok",
                        "已补充本地真实歌曲候选池",
                        candidates.size()
                ));
            } catch (Exception e) {
                toolResults.add(new ToolResult(
                        "music.catalog",
                        "音乐工具无结果后的本地候选池兜底",
                        "failed",
                        trim(e.getMessage(), 180),
                        0
                ));
            }
        }

        return new ToolExecution(
                candidates.values().stream().limit(MAX_CANDIDATE_SONGS).toList(),
                toolResults
        );
    }

    private static boolean plansMusicTools(DeepSeekChatClient.ToolPlan plan) {
        return plan != null && plan.tools().stream().anyMatch(call -> call.name().startsWith("music.") || call.name().startsWith("rec."));
    }

    private static boolean needsClientLocation(DeepSeekChatClient.ToolPlan plan) {
        return plan != null && plan.tools().stream().anyMatch(call -> TOOL_LOCATION_CURRENT.equals(call.name()));
    }

    private static ToolResult locationResult(LocationContext location) {
        if (location == null) {
            return new ToolResult(TOOL_LOCATION_CURRENT, "获取浏览器当前位置", "client_required", "需要前端执行浏览器定位", 0);
        }
        List<String> parts = new ArrayList<>();
        if (location.city() != null && !location.city().isBlank()) {
            parts.add("city=" + location.city());
        }
        if (location.latitude() != null && location.longitude() != null) {
            parts.add("lat=" + location.latitude());
            parts.add("lon=" + location.longitude());
        }
        if (location.source() != null && !location.source().isBlank()) {
            parts.add("source=" + location.source());
        }
        return new ToolResult(TOOL_LOCATION_CURRENT, "获取浏览器当前位置", "ok", String.join("，", parts), 0);
    }

    private static String keyword(String content, DeepSeekChatClient.ToolCallPlan call) {
        return call.keyword() == null || call.keyword().isBlank() ? content : call.keyword();
    }

    private static String location(String content, String contextLocation, DeepSeekChatClient.ToolCallPlan call) {
        if (call.location() != null && !call.location().isBlank()) {
            return call.location();
        }
        String explicitLocation = extractWeatherLocation(content);
        if (!explicitLocation.isBlank()) {
            return explicitLocation;
        }
        if (contextLocation != null && !contextLocation.isBlank()) {
            return contextLocation;
        }
        return DEFAULT_WEATHER_LOCATION;
    }

    private static DeepSeekChatClient.ToolPlan enrichWeatherPlan(
            String content,
            String weatherLocation,
            DeepSeekChatClient.ToolPlan plan) {
        if (!looksLikeWeatherRequest(content)) {
            return plan;
        }
        if (weatherLocation == null || weatherLocation.isBlank()) {
            return plan;
        }

        String explicitLocation = extractWeatherLocation(content);
        List<DeepSeekChatClient.ToolCallPlan> originalTools = plan == null || plan.tools() == null
                ? List.of()
                : plan.tools();
        List<DeepSeekChatClient.ToolCallPlan> tools = new ArrayList<>();
        boolean hasWeatherTool = originalTools.stream().anyMatch(tool -> "weather.now".equals(tool.name()));
        if (hasWeatherTool && plan != null) {
            for (DeepSeekChatClient.ToolCallPlan tool : originalTools) {
                boolean shouldUseContextLocation = "weather.now".equals(tool.name())
                        && (tool.location() == null
                        || tool.location().isBlank()
                        || (DEFAULT_WEATHER_LOCATION.equals(tool.location()) && explicitLocation.isBlank()));
                if (shouldUseContextLocation) {
                    tools.add(new DeepSeekChatClient.ToolCallPlan(
                            tool.name(),
                            tool.purpose(),
                            tool.keyword(),
                            weatherLocation,
                            tool.limit()
                    ));
                } else {
                    tools.add(tool);
                }
            }
        } else {
            tools.add(new DeepSeekChatClient.ToolCallPlan("weather.now", "按上下文城市获取当前天气", "", weatherLocation, 1));
            for (DeepSeekChatClient.ToolCallPlan tool : originalTools) {
                if (!"weather.now".equals(tool.name())) {
                    tools.add(tool);
                }
            }
            if (looksLikeMusicRequest(content)) {
                tools.add(new DeepSeekChatClient.ToolCallPlan("music.catalog", "根据天气场景获取本地歌曲候选池", content, "", 50));
                tools.add(new DeepSeekChatClient.ToolCallPlan("rec.preferences", "结合用户偏好调整天气推荐", "", "", 10));
            }
        }

        if (plan != null && !plan.needClarification() && !tools.isEmpty()) {
            return new DeepSeekChatClient.ToolPlan(
                    plan.chatMode(),
                    plan.answerGoal(),
                    false,
                    "",
                    true,
                    tools,
                    plan.responseStyle()
            );
        }

        return new DeepSeekChatClient.ToolPlan(
                looksLikeMusicRequest(content) ? "music" : "weather",
                "使用上下文城市回答天气相关问题",
                false,
                "",
                true,
                tools,
                ""
        );
    }

    private static DeepSeekChatClient.ToolPlan enrichLocationPlan(String content, DeepSeekChatClient.ToolPlan plan) {
        if (!looksLikeLocationRequest(content)) {
            return plan;
        }
        List<DeepSeekChatClient.ToolCallPlan> originalTools = plan == null || plan.tools() == null
                ? List.of()
                : plan.tools();
        boolean hasLocationTool = originalTools.stream().anyMatch(tool -> TOOL_LOCATION_CURRENT.equals(tool.name()));
        List<DeepSeekChatClient.ToolCallPlan> tools = new ArrayList<>();
        if (!hasLocationTool) {
            tools.add(new DeepSeekChatClient.ToolCallPlan(TOOL_LOCATION_CURRENT, "获取浏览器当前位置", "", "", 1));
        }
        tools.addAll(originalTools);
        return new DeepSeekChatClient.ToolPlan(
                plan == null ? "general" : plan.chatMode(),
                plan == null ? "使用当前位置回答用户问题" : plan.answerGoal(),
                false,
                "",
                true,
                tools,
                plan == null ? "" : plan.responseStyle()
        );
    }

    private static String resolveWeatherLocation(String content, String requestCity, List<ChatMessage> recentHistory) {
        String explicitLocation = extractWeatherLocation(content);
        if (!explicitLocation.isBlank()) {
            return explicitLocation;
        }
        if (requestCity != null && !requestCity.isBlank()) {
            return requestCity.trim();
        }
        if (recentHistory != null) {
            for (int i = recentHistory.size() - 1; i >= 0; i--) {
                String location = extractWeatherLocation(recentHistory.get(i).text());
                if (!location.isBlank()) {
                    return location;
                }
            }
        }
        return "";
    }

    private static LocationContext requestLocation(ChatSendRequest request) {
        if (request == null) {
            return null;
        }
        if (request.context() != null && request.context().location() != null && hasLocationValue(request.context().location())) {
            return request.context().location();
        }
        if (request.city() != null && !request.city().isBlank()) {
            return new LocationContext(request.city().trim(), null, null, "legacy_city");
        }
        return null;
    }

    private static boolean hasLocationValue(LocationContext location) {
        return location.city() != null && !location.city().isBlank()
                || location.latitude() != null && location.longitude() != null;
    }

    private static String locationValue(LocationContext location) {
        if (location == null) {
            return "";
        }
        if (location.city() != null && !location.city().isBlank()) {
            return location.city().trim();
        }
        if (location.latitude() != null && location.longitude() != null) {
            return location.longitude() + "," + location.latitude();
        }
        return "";
    }

    private static String extractWeatherLocation(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        for (String city : WEATHER_LOCATIONS) {
            if (content.contains(city)) {
                return city;
            }
        }
        Matcher matcher = WEATHER_LOCATION_PATTERN.matcher(content);
        if (matcher.find()) {
            String candidate = matcher.group(1);
            if (!looksLikeLocationReference(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private static boolean looksLikeLocationReference(String value) {
        return value == null
                || value.isBlank()
                || value.contains("我")
                || value.contains("这")
                || value.contains("那")
                || value.contains("哪")
                || value.contains("当地")
                || value.contains("本地")
                || value.contains("当前")
                || value.contains("今天")
                || value.contains("现在")
                || value.contains("根据");
    }

    private static boolean looksLikeWeatherRequest(String content) {
        return content != null
                && (content.contains("天气")
                || content.contains("气温")
                || content.contains("温度")
                || content.contains("下雨")
                || content.contains("晴")
                || content.contains("阴")
                || content.contains("多云"));
    }

    private static boolean looksLikeLocationRequest(String content) {
        return content != null
                && (content.contains("我现在在哪")
                || content.contains("我在哪")
                || content.contains("你不能看到我的定位")
                || content.contains("我的定位")
                || content.contains("我这里")
                || content.contains("当前位置")
                || content.contains("当前城市")
                || content.contains("当地")
                || content.contains("本地"));
    }

    private static boolean looksLikeMusicRequest(String content) {
        return content != null
                && (content.contains("歌")
                || content.contains("音乐")
                || content.contains("听")
                || content.contains("推荐")
                || content.contains("播放"));
    }

    private static ToolResult okResult(DeepSeekChatClient.ToolCallPlan call, String summary, int songCount) {
        return new ToolResult(call.name(), call.purpose(), "ok", summary, Math.max(songCount, 0));
    }

    private static List<SelectedSong> selectedSongs(List<Integer> indexes, List<SongCandidate> candidates) {
        if (indexes == null || indexes.isEmpty() || candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<SelectedSong> selected = new ArrayList<>();
        for (Integer index : indexes) {
            if (index == null || index < 1 || index > candidates.size()) {
                continue;
            }
            SelectedSong song = toSelectedSong(candidates.get(index - 1));
            if (selected.stream().noneMatch(item -> item.key().equals(song.key()))) {
                selected.add(song);
            }
            if (selected.size() >= 8) {
                break;
            }
        }
        return selected;
    }

    private static SelectedSong toSelectedSong(SongCandidate song) {
        boolean netease = song.source() != null && song.source().contains("NETEASE");
        String source = netease ? "NETEASE" : song.source();
        String sourceId = song.sourceId() == null || song.sourceId().isBlank()
                ? song.id() == null ? "" : String.valueOf(song.id())
                : song.sourceId();
        return new SelectedSong(
                song.id(),
                netease ? null : song.id(),
                sourceId,
                source,
                song.title(),
                song.artist(),
                song.album(),
                song.genre(),
                song.coverUrl(),
                song.filePath(),
                song.duration(),
                netease,
                true
        );
    }

    private static String buildFallbackReply(
            String content,
            DeepSeekChatClient.ToolPlan plan,
            ToolExecution execution) {
        if (plan != null && "weather".equals(plan.chatMode())) {
            return execution.toolResults().stream()
                    .filter(result -> "weather.now".equals(result.name()) && "ok".equals(result.status()))
                    .findFirst()
                    .map(ToolResult::summary)
                    .orElse("我理解你在问天气，但天气服务现在没有返回可用结果。");
        }
        if (plansMusicTools(plan)) {
            if (execution.candidates().isEmpty()) {
                return "我理解你想找歌，但现在音乐工具没有返回可用歌曲。你可以换个关键词，或者先确认 music/rec/网易云代理服务。";
            }
            return "我先从真实音乐数据里挑了几首可用候选。AI 服务恢复后，我会再按你的语境细选。";
        }
        if (content.contains("什么模型") || content.contains("你是谁")) {
            return "我是这个音乐电台里的 AI 助手，可以正常聊天，也可以在需要时调用音乐、推荐、天气等项目工具。";
        }
        return "我在，可以继续聊。需要音乐、推荐、天气或别的问题，都可以直接说。";
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
    private static void collectSongCandidates(
            Object value,
            Map<String, SongCandidate> candidates,
            String defaultSource,
            String defaultReason) {
        if (value == null || candidates.size() >= MAX_CANDIDATE_SONGS) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectSongCandidates(item, candidates, defaultSource, defaultReason);
                if (candidates.size() >= MAX_CANDIDATE_SONGS) {
                    return;
                }
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            SongCandidate song = toSongCandidate((Map<String, Object>) map, defaultSource, defaultReason);
            if (song != null) {
                candidates.putIfAbsent(song.key(), song);
                return;
            }
            for (String key : List.of("data", "records", "result", "list", "songs", "items", "tracks")) {
                collectSongCandidates(map.get(key), candidates, defaultSource, defaultReason);
                if (candidates.size() >= MAX_CANDIDATE_SONGS) {
                    return;
                }
            }
        }
    }

    private static SongCandidate toSongCandidate(Map<String, Object> map, String defaultSource, String defaultReason) {
        String title = firstText(map, "title", "name", "songName", "song");
        if (title.isBlank() || looksLikeContainerOnly(map)) {
            return null;
        }
        String source = firstText(map, "source");
        if (source.isBlank()) {
            source = defaultSource;
        }
        String reason = firstText(map, "reason");
        if (reason.isBlank()) {
            reason = defaultReason;
        }
        return new SongCandidate(
                longValue(firstValue(map, "id", "songId", "song_id")),
                firstText(map, "sourceId", "source_id"),
                title,
                artistText(map),
                albumText(map),
                firstText(map, "genre", "style"),
                firstText(map, "emotionTags", "emotion_tags", "tags"),
                intValue(firstValue(map, "playCount", "play_count", "score", "rank")),
                firstText(map, "coverUrl", "cover_url", "picUrl"),
                firstText(map, "filePath", "file_path", "url"),
                intValue(firstValue(map, "duration", "dt")),
                source,
                reason
        );
    }

    private static boolean looksLikeContainerOnly(Map<String, Object> map) {
        return map.containsKey("songs") || map.containsKey("records") || map.containsKey("tracks");
    }

    @SuppressWarnings("unchecked")
    private static String artistText(Map<String, Object> map) {
        String direct = firstText(map, "artist", "singer", "author");
        if (!direct.isBlank()) {
            return direct;
        }
        for (String key : List.of("artists", "ar", "singers")) {
            Object value = map.get(key);
            if (value instanceof Collection<?> collection) {
                List<String> names = new ArrayList<>();
                for (Object item : collection) {
                    if (item instanceof Map<?, ?> artistMap) {
                        String name = firstText((Map<String, Object>) artistMap, "name");
                        if (!name.isBlank()) {
                            names.add(name);
                        }
                    }
                }
                if (!names.isEmpty()) {
                    return String.join("/", names);
                }
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static String albumText(Map<String, Object> map) {
        String direct = firstText(map, "album", "albumName");
        if (!direct.isBlank() && !direct.startsWith("{")) {
            return direct;
        }
        for (String key : List.of("album", "al")) {
            Object value = map.get(key);
            if (value instanceof Map<?, ?> albumMap) {
                String name = firstText((Map<String, Object>) albumMap, "name");
                if (!name.isBlank()) {
                    return name;
                }
            }
        }
        return "";
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

    private static String summarize(Object value, int maxLength) {
        if (value == null) {
            return "(empty)";
        }
        return trim(value.toString(), maxLength);
    }

    private static String trim(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String text = value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
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

    public record ChatSendRequest(Long userId, String content, String city, ChatContext context) {
    }

    public record ChatSendResponse(
            ChatMessage reply,
            List<String> songs,
            List<SelectedSong> selectedSongs,
            List<ToolResult> toolCalls,
            List<ClientToolRequest> clientToolRequests) {

        public ChatSendResponse(
                ChatMessage reply,
                List<String> songs,
                List<SelectedSong> selectedSongs,
                List<ToolResult> toolCalls) {
            this(reply, songs, selectedSongs, toolCalls, List.of());
        }
    }

    public record ChatMessage(String role, String text, String time) {
    }

    public record ToolResult(String name, String purpose, String status, String summary, int songCount) {
    }

    public record ClientToolRequest(String id, String name, String purpose) {
    }

    public record ChatContext(LocationContext location) {
    }

    public record LocationContext(String city, Double latitude, Double longitude, String source) {
    }

    public record SelectedSong(
            Long id,
            Long songId,
            String sourceId,
            String source,
            String title,
            String artist,
            String album,
            String genre,
            String coverUrl,
            String filePath,
            Integer duration,
            boolean netease,
            boolean playable) {

        public String label() {
            return artist == null || artist.isBlank() ? title : title + " - " + artist;
        }

        public String key() {
            return (source == null ? "" : source) + ":" + (sourceId == null || sourceId.isBlank() ? id : sourceId);
        }
    }

    private record ToolExecution(List<SongCandidate> candidates, List<ToolResult> toolResults) {
    }

    public record SongCandidate(
            Long id,
            String sourceId,
            String title,
            String artist,
            String album,
            String genre,
            String emotionTags,
            Integer playCount,
            String coverUrl,
            String filePath,
            Integer duration,
            String source,
            String reason) {

        public String key() {
            if (id != null) {
                return source + ":id:" + id;
            }
            return (source + "||" + title + "||" + artist).toLowerCase();
        }

        public String label() {
            return artist == null || artist.isBlank() ? title : title + " - " + artist;
        }

        public String brief() {
            List<String> parts = new ArrayList<>();
            parts.add(label());
            if (source != null && !source.isBlank()) {
                parts.add("source=" + source);
            }
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
