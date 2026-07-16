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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekChatClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekChatClient.class);

    private static final String INTENT_SYSTEM_PROMPT = """
            You are the intent parser for a Chinese AI chat and DJ music radio app.
            Use the recent conversation when the user sends a short follow-up like "还有啥", "换一个", "继续", or "讲吧".
            Return only one JSON object. Do not wrap it in markdown.
            Fields:
            - chatMode: one of music, story, smalltalk, general
            - mood: user's emotion or desired vibe, empty string if unknown
            - scene: listening scene, empty string if unknown
            - genre: music genre, empty string if unknown
            - artist: requested artist, empty string if unknown
            - keyword: best short search keyword for a music search API
            - needRecommend: true only if the user wants songs, playlists, music suggestions, similar songs, or more song recommendations
            - replyStyle: short Chinese style label for the DJ reply
            For greetings, storytelling, explanations, and normal chat, needRecommend must be false.
            Keep keyword concise. Reply JSON only.
            """;

    private static final String MUSIC_REPLY_SYSTEM_PROMPT = """
            You are a warm but concise Chinese radio DJ in a music app.
            Write one natural Chinese reply to the user.
            Rules:
            - Use the provided songs only; do not invent song names.
            - Mention the user's mood, scene, genre, or artist when useful.
            - Keep it under 90 Chinese characters.
            - Do not output markdown or numbered lists.
            """;

    private static final String GENERAL_REPLY_SYSTEM_PROMPT = """
            You are a natural Chinese AI companion inside a DJ music radio app.
            You can chat normally, answer questions, tell short stories, and continue the user's previous request.
            Rules:
            - Do not force every answer back to music.
            - If the user asks for a story, tell or continue the story directly.
            - Avoid animal characters or animal-like names unless the user explicitly asks for them.
            - If the user greets you, respond naturally and briefly.
            - Keep replies friendly and under 160 Chinese characters unless a story continuation needs a little more room.
            - Do not output markdown.
            """;

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatIntent analyzeIntent(String userInput, List<ChatService.ChatMessage> history) {
        String normalizedInput = normalize(userInput, "recommend music");
        if (!isConfigured()) {
            return ChatIntent.empty(normalizedInput);
        }

        try {
            String content = callDeepSeek(
                    List.of(
                            Map.of("role", "system", "content", INTENT_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", buildIntentPrompt(normalizedInput, history))
                    ),
                    0.2,
                    400,
                    true
            );
            if (content.isBlank()) {
                return ChatIntent.empty(normalizedInput);
            }

            Map<String, Object> map = objectMapper.readValue(cleanJson(content), new TypeReference<>() {
            });
            ChatIntent intent = new ChatIntent(
                    text(map, "chatMode"),
                    text(map, "mood"),
                    text(map, "scene"),
                    text(map, "genre"),
                    text(map, "artist"),
                    text(map, "keyword"),
                    bool(map, "needRecommend", true),
                    text(map, "replyStyle")
            );
            if (intent.searchKeyword(normalizedInput).isBlank()) {
                return ChatIntent.empty(normalizedInput);
            }
            return intent;
        } catch (Exception e) {
            log.warn("DeepSeek intent parsing failed: {}", e.getMessage());
            return ChatIntent.empty(normalizedInput);
        }
    }

    public String composeReply(
            String userInput,
            ChatIntent intent,
            List<String> songs,
            List<ChatService.ChatMessage> history) {
        if (!isConfigured()) {
            return "";
        }
        try {
            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("Recent conversation:\n").append(formatHistory(history)).append('\n');
            userPrompt.append("User message: ").append(normalize(userInput, "")).append('\n');
            if (intent != null && intent.hasMeaningfulTags()) {
                userPrompt.append("Parsed intent: ").append(intent.brief()).append('\n');
            }
            if (intent != null && intent.needRecommend()) {
                userPrompt.append("Songs from project services: ").append(formatSongs(songs)).append('\n');
            }

            return callDeepSeek(
                    List.of(
                            Map.of("role", "system", "content", replySystemPrompt(intent)),
                            Map.of("role", "user", "content", userPrompt.toString())
                    ),
                    0.7,
                    intent != null && intent.needRecommend() ? 260 : 500,
                    false
            ).trim();
        } catch (Exception e) {
            log.warn("DeepSeek reply generation failed: {}", e.getMessage());
            return "";
        }
    }

    private static String buildIntentPrompt(String userInput, List<ChatService.ChatMessage> history) {
        return "Recent conversation:\n" + formatHistory(history) + "\nCurrent user message: " + userInput;
    }

    private static String replySystemPrompt(ChatIntent intent) {
        if (intent != null && intent.needRecommend()) {
            return MUSIC_REPLY_SYSTEM_PROMPT;
        }
        return GENERAL_REPLY_SYSTEM_PROMPT;
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

    private static String formatSongs(List<String> songs) {
        if (songs == null || songs.isEmpty()) {
            return "No songs found from the project services. Do not invent song names; briefly ask for another style or keyword.";
        }
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(3, songs.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(songs.get(i));
        }
        return builder.toString();
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

    private static String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString().trim();
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

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record ChatIntent(
            String chatMode,
            String mood,
            String scene,
            String genre,
            String artist,
            String keyword,
            boolean needRecommend,
            String replyStyle) {

        public static ChatIntent empty(String fallbackKeyword) {
            return new ChatIntent("music", "", "", "", "", normalize(fallbackKeyword, "recommend music"), true, "");
        }

        public String searchKeyword(String fallback) {
            for (String candidate : List.of(keyword, artist, genre, mood, scene, fallback)) {
                if (candidate != null && !candidate.isBlank()) {
                    return candidate.trim();
                }
            }
            return "recommend music";
        }

        public boolean hasMeaningfulTags() {
            return !joinNonBlank(chatMode, mood, scene, genre, artist, keyword, replyStyle).isBlank();
        }

        public boolean hasSpecificMusicRequest() {
            return !joinNonBlank(mood, scene, genre, artist).isBlank();
        }

        public String brief() {
            return joinNonBlank(chatMode, mood, scene, genre, artist, keyword, replyStyle);
        }

        private static String joinNonBlank(String... values) {
            List<String> result = new ArrayList<>();
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    result.add(value.trim());
                }
            }
            return String.join(" / ", result);
        }
    }
}
