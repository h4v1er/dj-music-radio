package org.example.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DeepSeekChatClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekChatClient.class);

    private static final Set<String> ALLOWED_TOOLS = Set.of(
            "music.search",
            "music.catalog",
            "music.neteaseSearch",
            "rec.daily",
            "rec.hot",
            "rec.preferences",
            "location.current",
            "weather.now"
    );

    private static final String TOOL_PLAN_SYSTEM_PROMPT = """
            You are the tool-planning brain for a Chinese AI assistant inside a music radio app.
            The assistant should chat normally, and only use tools when real project or external data is useful.

            Available tools:
            - music.search: targeted local database search. Use for explicit song title, artist, album, genre, or keyword.
            - music.catalog: local database candidate pool. Use for semantic music requests like rainy day, healing, breakup, running, studying, coding, sleeping, late night.
            - music.neteaseSearch: external NetEase Cloud Music search through the project's music service. Use when the user asks for NetEase, a specific popular song/artist not likely in the local DB, or local results may be too narrow.
            - rec.daily: user's daily recommendation list.
            - rec.hot: current hot ranking from the recommendation service.
            - rec.preferences: user's preference tags; not songs, but useful context for personalized recommendation.
            - location.current: client-side browser geolocation. Use when the user asks where they are, says "我这里/当前位置/当地", or asks location-aware questions without naming a city.
            - weather.now: current weather by city. Use for weather questions or weather-aware music recommendations.

            Planning rules:
            - Behave like a normal AI assistant first. Tool use is optional, not the default.
            - Ordinary chat, identity questions, stories, explanations, greetings, and emotional conversation do not need tools unless the user asks for factual app data, weather, or songs.
            - If the message is only a bare name, artist, topic, or fragment without a clear request, set needClarification=true and ask what the user wants to do with it. Do not call tools yet.
            - If the user intent is ambiguous, ask one short clarification question instead of guessing.
            - For music recommendation, choose tools only when the user clearly asks to find, recommend, play, search, or compare songs. Do not call every tool by default.
            - For semantic scene-based music, prefer music.catalog plus rec.preferences; add weather.now only if weather or city matters.
            - If the user asks about their current location or says "我这里/当地/当前位置" and no city is known from the message, call location.current first.
            - If the user asks weather or weather-aware music for "我这里/当地/当前位置", call location.current plus weather.now. Leave weather.now.location empty so the system can fill it from location.current.
            - For explicit artist/title, prefer music.search; add music.neteaseSearch when the request asks NetEase or the local DB may miss it.
            - For "daily", "random", "不知道听什么", prefer rec.daily plus rec.preferences; add rec.hot as a fallback.
            - For weather-only questions, use weather.now and no music tools.
            - Web crawling is not currently available. Do not invent a web.search tool.

            Return only one JSON object:
            {
              "chatMode": "general|music|weather|story|smalltalk",
              "answerGoal": "what the assistant should accomplish",
              "needClarification": false,
              "clarificationQuestion": "",
              "needTools": true,
              "tools": [
                {
                  "name": "music.catalog",
                  "purpose": "why this tool is needed",
                  "keyword": "optional keyword",
                  "location": "optional city",
                  "limit": 40
                }
              ],
              "responseStyle": "short Chinese style label"
            }
            """;

    private static final String FINAL_REPLY_SYSTEM_PROMPT = """
            You are a natural Chinese AI assistant in a DJ music radio app.
            Answer like a helpful human assistant, not like a rigid DJ template.
            Use tool results as factual context. If a tool failed or returned demo data, be honest and do not pretend it is complete.

            Music rules:
            - Select songs only from the numbered candidate list.
            - Return selectedIndexes from the candidate list; do not invent song names.
            - Select 1 to 4 songs unless the user asks for more.
            - The UI displays selected songs separately, so the reply should explain the reasoning/vibe and avoid repeating a long song-name list.
            - Do not mention any song name that is not included in selectedIndexes.
            - You may combine title, artist, album, source, genre, emotion tags, reason, weather, user preference, and conversation context.

            General rules:
            - Do not force ordinary chat back to music.
            - Identity questions can be answered naturally: you are the AI assistant for this music radio app, powered by the configured model service.
            - If the plan says needClarification=true, ask the clarification question naturally and selectedIndexes=[].
            - For weather-only questions, answer using weather tool results and selectedIndexes=[].
            - Keep replies concise but natural.

            Return only JSON:
            {
              "reply": "Chinese reply text",
              "selectedIndexes": [1, 2, 3]
            }
            Use selectedIndexes=[] when no songs should be attached.
            """;

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolPlan planToolUse(String userInput, List<ChatService.ChatMessage> history) {
        String normalizedInput = normalize(userInput, "");
        if (!isConfigured()) {
            return ToolPlan.local(normalizedInput);
        }

        try {
            String content = callDeepSeek(
                    List.of(
                            Map.of("role", "system", "content", TOOL_PLAN_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", buildPlanPrompt(normalizedInput, history))
                    ),
                    0.2,
                    700,
                    true
            );
            if (content.isBlank()) {
                return ToolPlan.local(normalizedInput);
            }

            Map<String, Object> map = objectMapper.readValue(cleanJson(content), new TypeReference<>() {
            });
            ToolPlan plan = new ToolPlan(
                    text(map, "chatMode", "general"),
                    text(map, "answerGoal", ""),
                    bool(map, "needClarification", false),
                    text(map, "clarificationQuestion", ""),
                    bool(map, "needTools", false),
                    parseToolCalls(map.get("tools"), normalizedInput),
                    text(map, "responseStyle", "")
            );
            return plan.normalized(normalizedInput);
        } catch (Exception e) {
            log.warn("DeepSeek tool planning failed: {}", e.getMessage());
            return ToolPlan.local(normalizedInput);
        }
    }

    public AiReply composeReply(
            String userInput,
            ToolPlan plan,
            List<ChatService.ToolResult> toolResults,
            List<ChatService.SongCandidate> candidates,
            List<ChatService.ChatMessage> history) {
        if (!isConfigured()) {
            return AiReply.empty();
        }
        try {
            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("Recent conversation:\n").append(formatHistory(history)).append('\n');
            userPrompt.append("Current user message: ").append(normalize(userInput, "")).append('\n');
            userPrompt.append("Configured model: ").append(model).append('\n');
            userPrompt.append("Tool plan: ").append(plan == null ? ToolPlan.local(userInput) : plan).append('\n');
            userPrompt.append("Tool results:\n").append(formatToolResults(toolResults)).append('\n');
            userPrompt.append("Candidate songs from project/external services:\n").append(formatCandidates(candidates)).append('\n');

            String content = callDeepSeek(
                    List.of(
                            Map.of("role", "system", "content", FINAL_REPLY_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userPrompt.toString())
                    ),
                    0.75,
                    800,
                    true
            );
            if (content.isBlank()) {
                return AiReply.empty();
            }

            Map<String, Object> map = objectMapper.readValue(cleanJson(content), new TypeReference<>() {
            });
            String reply = text(map, "reply", "");
            List<Integer> selectedIndexes = selectedIndexes(map.get("selectedIndexes"), candidates);
            return new AiReply(reply, selectedIndexes);
        } catch (Exception e) {
            log.warn("DeepSeek final reply failed: {}", e.getMessage());
            return AiReply.empty();
        }
    }

    private static String buildPlanPrompt(String userInput, List<ChatService.ChatMessage> history) {
        return "Recent conversation:\n" + formatHistory(history) + "\nCurrent user message: " + userInput;
    }

    private String callDeepSeek(
            List<Map<String, String>> messages,
            double temperature,
            int maxTokens,
            boolean jsonResponse) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("messages", messages);
        if (jsonResponse) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                Map.class
        );

        Map<?, ?> body = response.getBody();
        if (body == null) {
            return "";
        }
        Object choicesValue = body.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
            return "";
        }
        Object messageValue = choiceMap.get("message");
        if (!(messageValue instanceof Map<?, ?> messageMap)) {
            return "";
        }
        Object content = messageMap.get("content");
        return content == null ? "" : stripCodeFence(content.toString());
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"your-deepseek-api-key".equals(apiKey);
    }

    private static String formatToolResults(List<ChatService.ToolResult> results) {
        if (results == null || results.isEmpty()) {
            return "(none)";
        }
        StringBuilder builder = new StringBuilder();
        for (ChatService.ToolResult result : results) {
            builder.append("- ")
                    .append(result.name())
                    .append(" [")
                    .append(result.status())
                    .append("] ")
                    .append(result.summary());
            if (result.songCount() > 0) {
                builder.append(" | songs=").append(result.songCount());
            }
            builder.append('\n');
        }
        return builder.toString().trim();
    }

    private static String formatCandidates(List<ChatService.SongCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "(none)";
        }
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(70, candidates.size());
        for (int i = 0; i < limit; i++) {
            ChatService.SongCandidate song = candidates.get(i);
            builder.append(i + 1).append(". ").append(song.brief()).append('\n');
        }
        return builder.toString().trim();
    }

    private static List<Integer> selectedIndexes(Object value, List<ChatService.SongCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<Integer> indexes = new ArrayList<>();
        collectIndexes(value, indexes);
        List<Integer> selected = new ArrayList<>();
        for (Integer index : indexes) {
            if (index == null || index < 1 || index > candidates.size()) {
                continue;
            }
            if (!selected.contains(index)) {
                selected.add(index);
            }
            if (selected.size() >= 8) {
                break;
            }
        }
        return selected;
    }

    private static void collectIndexes(Object value, List<Integer> indexes) {
        if (value == null) {
            return;
        }
        if (value instanceof Number number) {
            indexes.add(number.intValue());
            return;
        }
        if (value instanceof String text) {
            try {
                indexes.add(Integer.parseInt(text.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore non-numeric model output.
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectIndexes(item, indexes);
            }
        }
    }

    private static String formatHistory(List<ChatService.ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "(none)";
        }
        int start = Math.max(0, history.size() - 8);
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < history.size(); i++) {
            ChatService.ChatMessage message = history.get(i);
            if (message == null || message.text() == null || message.text().isBlank()) {
                continue;
            }
            String role = "dj".equals(message.role()) ? "assistant" : "user";
            builder.append(role).append(": ").append(message.text().trim()).append('\n');
        }
        String result = builder.toString().trim();
        return result.isBlank() ? "(none)" : result;
    }

    private static String cleanJson(String value) {
        String cleaned = stripCodeFence(value).trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private static String stripCodeFence(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return cleaned;
    }

    private static String text(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString().trim();
    }

    private static boolean bool(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return defaultValue;
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static List<ToolCallPlan> parseToolCalls(Object value, String fallbackKeyword) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        List<ToolCallPlan> result = new ArrayList<>();
        for (Object item : collection) {
            ToolCallPlan call = parseToolCall(item, fallbackKeyword);
            if (call != null) {
                result.add(call);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static ToolCallPlan parseToolCall(Object value, String fallbackKeyword) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return new ToolCallPlan(text.trim(), "", fallbackKeyword, "", 50).normalized(fallbackKeyword);
        }
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            return new ToolCallPlan(
                    text(map, "name", ""),
                    text(map, "purpose", ""),
                    text(map, "keyword", fallbackKeyword),
                    text(map, "location", ""),
                    intValue(map.get("limit"), 50)
            ).normalized(fallbackKeyword);
        }
        return null;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record ToolPlan(
            String chatMode,
            String answerGoal,
            boolean needClarification,
            String clarificationQuestion,
            boolean needTools,
            List<ToolCallPlan> tools,
            String responseStyle) {

        public static ToolPlan local(String userInput) {
            String normalized = normalize(userInput, "");
            List<ToolCallPlan> calls = new ArrayList<>();
            boolean music = looksLikeMusicRequest(normalized);
            boolean weather = looksLikeWeatherRequest(normalized);
            boolean location = looksLikeLocationRequest(normalized);
            if (location) {
                calls.add(new ToolCallPlan("location.current", "获取浏览器当前位置", "", "", 1));
            }
            if (weather) {
                calls.add(new ToolCallPlan("weather.now", "回答天气或结合天气推荐", "", location ? "" : extractLocation(normalized), 1));
            }
            if (music) {
                calls.add(new ToolCallPlan("music.search", "先按关键词查本地歌库", normalized, "", 20));
                calls.add(new ToolCallPlan("music.catalog", "语义场景候选池", normalized, "", 50));
                calls.add(new ToolCallPlan("rec.hot", "本地歌库不足时补充热门候选", "", "", 10));
                if (normalized.contains("网易云")) {
                    calls.add(new ToolCallPlan("music.neteaseSearch", "按用户要求搜索网易云", normalized, "", 20));
                }
            }
            String mode = location && !weather && !music ? "general" : weather && !music ? "weather" : music ? "music" : "general";
            boolean ambiguous = looksLikeBareFragment(normalized);
            return new ToolPlan(
                    ambiguous ? "general" : mode,
                    "",
                    ambiguous,
                    ambiguous ? "你是想了解这个人/话题，还是想让我帮你找相关音乐？" : "",
                    !ambiguous && !calls.isEmpty(),
                    ambiguous ? List.of() : calls,
                    ""
            ).normalized(normalized);
        }

        public ToolPlan normalized(String userInput) {
            if (needClarification) {
                return new ToolPlan(
                        normalize(chatMode, "general"),
                        normalize(answerGoal, ""),
                        true,
                        normalize(clarificationQuestion, "你想让我具体帮你做什么？"),
                        false,
                        List.of(),
                        normalize(responseStyle, "")
                );
            }
            List<ToolCallPlan> safeTools = tools == null ? List.of() : tools.stream()
                    .map(tool -> tool == null ? null : tool.normalized(userInput))
                    .filter(tool -> tool != null && ALLOWED_TOOLS.contains(tool.name()))
                    .distinct()
                    .toList();
            boolean shouldUseTools = needTools || !safeTools.isEmpty();
            if (shouldUseTools && safeTools.isEmpty()) {
                if ("weather".equals(chatMode) || looksLikeWeatherRequest(userInput)) {
                    safeTools = List.of(new ToolCallPlan("weather.now", "获取天气", "", extractLocation(userInput), 1));
                } else if (looksLikeLocationRequest(userInput)) {
                    safeTools = List.of(new ToolCallPlan("location.current", "获取浏览器当前位置", "", "", 1));
                } else if ("music".equals(chatMode) || looksLikeMusicRequest(userInput)) {
                    safeTools = List.of(new ToolCallPlan("music.catalog", "获取本地歌曲候选池", userInput, "", 50));
                }
            }
            return new ToolPlan(
                    normalize(chatMode, shouldUseTools ? "music" : "general"),
                    normalize(answerGoal, ""),
                    false,
                    "",
                    shouldUseTools,
                    safeTools,
                    normalize(responseStyle, "")
            );
        }

        private static boolean looksLikeMusicRequest(String value) {
            String lower = value.toLowerCase();
            return lower.contains("歌")
                    || lower.contains("音乐")
                    || lower.contains("听")
                    || lower.contains("推荐")
                    || lower.contains("播放")
                    || lower.contains("网易云")
                    || lower.contains("music")
                    || lower.contains("song");
        }

        private static boolean looksLikeWeatherRequest(String value) {
            return value.contains("天气")
                    || value.contains("下雨")
                    || value.contains("晴")
                    || value.contains("阴天")
                    || value.contains("气温");
        }

        private static boolean looksLikeLocationRequest(String value) {
            return value.contains("我现在在哪")
                    || value.contains("我在哪")
                    || value.contains("我这里")
                    || value.contains("当前位置")
                    || value.contains("当前城市")
                    || value.contains("当地")
                    || value.contains("本地");
        }

        private static boolean looksLikeBareFragment(String value) {
            String text = value == null ? "" : value.trim();
            if (text.isBlank() || text.length() > 40) {
                return false;
            }
            if (looksLikeWeatherRequest(text)) {
                return false;
            }
            for (String marker : List.of("吗", "?", "？", "谁", "什么", "怎么", "为什么", "推荐", "播放", "搜", "搜索", "听", "歌", "音乐", "天气")) {
                if (text.contains(marker)) {
                    return false;
                }
            }
            String[] parts = text.split("\\s+");
            return parts.length <= 3;
        }

        private static String extractLocation(String value) {
            for (String city : List.of("北京", "上海", "广州", "深圳", "成都", "杭州", "南京", "武汉", "重庆", "西安")) {
                if (value.contains(city)) {
                    return city;
                }
            }
            return "北京";
        }
    }

    public record ToolCallPlan(
            String name,
            String purpose,
            String keyword,
            String location,
            int limit) {

        public ToolCallPlan normalized(String fallbackKeyword) {
            String safeName = normalize(name, "");
            if (!ALLOWED_TOOLS.contains(safeName)) {
                return null;
            }
            int safeLimit = limit <= 0 ? 20 : Math.min(limit, 80);
            return new ToolCallPlan(
                    safeName,
                    normalize(purpose, ""),
                    normalize(keyword, fallbackKeyword),
                    normalize(location, ""),
                    safeLimit
            );
        }
    }

    public record AiReply(String reply, List<Integer> selectedIndexes) {

        public static AiReply empty() {
            return new AiReply("", List.of());
        }
    }
}
