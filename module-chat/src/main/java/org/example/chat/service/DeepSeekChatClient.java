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

@Component
public class DeepSeekChatClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekChatClient.class);

    private static final String TOOL_PLAN_SYSTEM_PROMPT = """
            You are the reasoning and tool-planning brain for a Chinese AI companion inside a music radio app.
            Decide whether the assistant needs project data before answering.
            Available tools:
            - music.search: search songs by a concise keyword, artist, title, genre, or album.
            - music.list: fetch a pool of real songs from the project music database.
            - rec.daily: fetch the user's daily recommendations.
            - rec.hot: fetch hot songs.
            - rec.preferences: fetch the user's preference tags.
            Rules:
            - For ordinary chat, identity questions, stories, explanations, greetings, or emotional conversation without an explicit song request, set needTools=false.
            - For song requests, playlists, "what should I listen to", "recommend", "change songs", or follow-up requests like "还有啥" after music context, set needTools=true.
            - If the request is semantic like rainy day, running, healing, breakup, late night, or work focus, include music.list so the final model can choose from real candidates.
            - If the user asks for daily/personal/random recommendations, include rec.daily, rec.preferences, music.list, and rec.hot.
            - If the user names a singer, title, genre, or keyword, include music.search and music.list.
            Return only one JSON object:
            {
              "chatMode": "general|music|story|smalltalk",
              "needTools": true,
              "tools": ["music.list"],
              "keyword": "short search keyword, can be empty",
              "limit": 50,
              "responseStyle": "short Chinese style label"
            }
            """;

    private static final String FINAL_REPLY_SYSTEM_PROMPT = """
            You are a natural Chinese AI companion in a DJ music radio app.
            Answer like a normal helpful AI, not like a rigid template.
            If project tool results are provided, use them as factual data.
            Music rules:
            - Select songs only from the numbered candidate list.
            - Return selectedIndexes from the candidate list; do not invent song names.
            - Select 1 to 4 songs unless the user asks for more.
            - The UI will display selected songs separately, so the reply should explain the vibe and avoid a long song-name list.
            - Do not mention any song name that is not included in selectedIndexes.
            - It is fine to choose by title, artist, genre, emotion tags, lyrics summary, popularity, scene, and user context together.
            General chat rules:
            - Do not force the topic back to music when the user is just chatting.
            - Identity questions can be answered naturally: you are the AI assistant for this music radio app, powered by the configured model service.
            - Keep normal chat concise but not robotic.
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
                    500,
                    true
            );
            if (content.isBlank()) {
                return ToolPlan.local(normalizedInput);
            }

            Map<String, Object> map = objectMapper.readValue(cleanJson(content), new TypeReference<>() {
            });
            ToolPlan plan = new ToolPlan(
                    text(map, "chatMode", "general"),
                    bool(map, "needTools", false),
                    stringList(map.get("tools")),
                    text(map, "keyword", ""),
                    intValue(map.get("limit"), 50),
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
            userPrompt.append("Candidate songs from project services:\n").append(formatCandidates(candidates)).append('\n');

            String content = callDeepSeek(
                    List.of(
                            Map.of("role", "system", "content", FINAL_REPLY_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userPrompt.toString())
                    ),
                    0.75,
                    700,
                    true
            );
            if (content.isBlank()) {
                return AiReply.empty();
            }

            Map<String, Object> map = objectMapper.readValue(cleanJson(content), new TypeReference<>() {
            });
            String reply = text(map, "reply", "");
            List<String> songs = songsByIndexes(map.get("selectedIndexes"), candidates);
            return new AiReply(reply, songs);
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

    private static String formatCandidates(List<ChatService.SongCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "(none)";
        }
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(60, candidates.size());
        for (int i = 0; i < limit; i++) {
            ChatService.SongCandidate song = candidates.get(i);
            builder.append(i + 1).append(". ").append(song.brief()).append('\n');
        }
        return builder.toString().trim();
    }

    private static List<String> songsByIndexes(Object value, List<ChatService.SongCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<Integer> indexes = new ArrayList<>();
        collectIndexes(value, indexes);
        List<String> songs = new ArrayList<>();
        for (Integer index : indexes) {
            if (index == null || index < 1 || index > candidates.size()) {
                continue;
            }
            String label = candidates.get(index - 1).label();
            if (!label.isBlank() && !songs.contains(label)) {
                songs.add(label);
            }
            if (songs.size() >= 8) {
                break;
            }
        }
        return songs;
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

    private static List<String> stringList(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : collection) {
            if (item != null && !item.toString().isBlank()) {
                result.add(item.toString().trim());
            }
        }
        return result;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record ToolPlan(
            String chatMode,
            boolean needTools,
            List<String> tools,
            String keyword,
            int limit,
            String responseStyle) {

        public static ToolPlan local(String userInput) {
            String normalized = normalize(userInput, "");
            boolean music = looksLikeMusicRequest(normalized);
            List<String> tools = music ? List.of("music.search", "music.list", "rec.hot") : List.of();
            return new ToolPlan(music ? "music" : "general", music, tools, normalized, 50, "");
        }

        public ToolPlan normalized(String userInput) {
            boolean shouldUseTools = needTools || tools != null && !tools.isEmpty();
            List<String> safeTools = tools == null ? List.of() : tools.stream()
                    .filter(tool -> tool != null && !tool.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();
            if (shouldUseTools && safeTools.isEmpty()) {
                safeTools = List.of("music.list");
            }
            int safeLimit = limit <= 0 ? 50 : Math.min(limit, 80);
            String safeKeyword = normalize(keyword, userInput);
            return new ToolPlan(
                    normalize(chatMode, shouldUseTools ? "music" : "general"),
                    shouldUseTools,
                    safeTools,
                    safeKeyword,
                    safeLimit,
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
                    || lower.contains("music")
                    || lower.contains("song");
        }
    }

    public record AiReply(String reply, List<String> songs) {

        public static AiReply empty() {
            return new AiReply("", List.of());
        }
    }
}
