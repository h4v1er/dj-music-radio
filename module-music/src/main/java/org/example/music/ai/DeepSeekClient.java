package org.example.music.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * DeepSeek API 客户端 — OpenAI 兼容接口
 * 用于 AI 歌词情绪分析
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
        你是一个专业的音乐情绪分析专家。请深入理解以下歌词的意境、情感和氛围，返回JSON格式的完整情绪画像。

        JSON字段说明：
        - primaryEmotion: 主情绪，从以下20种标签中选择最匹配的一个（必填）
        - secondaryEmotion: 次情绪（必填，选第二匹配的）
        - valence: 效价，反映歌词情感的正负倾向。-100为极度消极，100为极度积极，0为中性
        - arousal: 唤醒度，反映歌曲的能量水平。0为极度舒缓（安眠曲级别），100为极度亢奋（重金属嘶吼级别）
        - emotionIntensity: 情绪强度，0为平淡如水，100为撕心裂肺
        - lyricTheme: 歌词主题，从以下选择最匹配的：爱情、自然、社会、哲思、叙事、励志、乡愁、都市、青春、其他
        - suitableScenes: 3个适合听这首歌的场景（逗号分隔，如"深夜独处,雨天咖啡馆,通勤路上"）
        - analysis: 一段50字以内的情绪解读

        20种情绪标签：
        温柔缱绻, 甜蜜浪漫, 温暖治愈, 静谧安宁,
        忧郁感伤, 孤独寂寥, 深沉内敛, 追忆怀旧,
        热烈奔放, 热血激昂, 洒脱不羁, 俏皮灵动,
        颓废迷离, 悲愤抗争, 空灵梦幻, 荒凉苍茫,
        市井烟火, 乡愁思念, 青春悸动, 触动感怀

        **重要：只返回纯JSON对象，不要包含任何markdown格式、代码块标记或其他文字。**""";

    /**
     * 调用 DeepSeek API 分析歌词情绪
     * @param lyric 歌词文本
     * @param title 歌曲标题（可选，辅助判断）
     * @return 解析后的分析结果，失败返回null
     */
    public AnalysisResult analyzeLyric(String lyric, String title) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your-deepseek-api-key")) {
            log.warn("DeepSeek API Key 未配置，跳过AI分析");
            return null;
        }
        if (lyric == null || lyric.trim().isEmpty()) {
            return null;
        }

        // 截断过长歌词（避免超出token限制）
        String truncatedLyric = lyric.length() > 3000 ? lyric.substring(0, 3000) : lyric;

        String userPrompt = "歌曲标题：" + (title != null ? title : "未知") +
                "\n\n歌词内容：\n" + truncatedLyric;

        try {
            // 构建请求体
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.3);  // 低温度，保证输出稳定
            requestBody.put("max_tokens", 600);
            requestBody.put("response_format", Map.of("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            messages.add(Map.of("role", "user", "content", userPrompt));
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("调用DeepSeek API分析歌词: title={}, lyricLength={}", title, truncatedLyric.length());
            ResponseEntity<Map> response = rest.exchange(apiUrl, HttpMethod.POST, request, Map.class);

            // 解析响应
            Map<String, Object> respBody = response.getBody();
            if (respBody == null) return null;

            List<Map<String, Object>> choices = (List<Map<String, Object>>) respBody.get("choices");
            if (choices == null || choices.isEmpty()) return null;

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return null;

            String content = (String) message.get("content");
            if (content == null || content.isEmpty()) return null;

            // 清理可能的 markdown 代码块
            content = content.trim();
            if (content.startsWith("```json")) {
                content = content.substring(7);
            }
            if (content.startsWith("```")) {
                content = content.substring(3);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();

            // 解析JSON
            Map<String, Object> result = mapper.readValue(content, Map.class);
            AnalysisResult ar = new AnalysisResult();
            ar.primaryEmotion = getString(result, "primaryEmotion");
            ar.secondaryEmotion = getString(result, "secondaryEmotion");
            ar.valence = getInt(result, "valence", 0);
            ar.arousal = getInt(result, "arousal", 50);
            ar.emotionIntensity = getInt(result, "emotionIntensity", 50);
            ar.lyricTheme = getString(result, "lyricTheme");
            ar.suitableScenes = getString(result, "suitableScenes");
            ar.analysis = getString(result, "analysis");

            // 校验主情绪在20种标签内
            if (ar.primaryEmotion != null && !ar.primaryEmotion.isEmpty()) {
                log.info("DeepSeek分析完成: emotion={}, theme={}, valence={}, arousal={}",
                        ar.primaryEmotion, ar.lyricTheme, ar.valence, ar.arousal);
                return ar;
            }

        } catch (Exception e) {
            log.error("DeepSeek API调用失败: {}", e.getMessage());
        }
        return null;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private int getInt(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException e) { }
        }
        return defaultVal;
    }

    /**
     * AI 分析结果
     */
    public static class AnalysisResult {
        public String primaryEmotion;
        public String secondaryEmotion;
        public int valence;
        public int arousal;
        public int emotionIntensity;
        public String lyricTheme;
        public String suitableScenes;
        public String analysis;
    }
}
