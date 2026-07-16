package org.example.music.analysis;

import org.example.music.taxonomy.MusicEmotion;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 歌词情绪分析引擎 — 核心分析器
 *
 * 输入: LRC 格式歌词文本
 * 输出: AnalysisResult（包含主次情绪、效价、唤醒度、强度、主题、关键词等）
 *
 * 不依赖外部 NLP 库，纯字符串匹配实现，单首 < 50ms
 */
public class LyricsAnalyzer {

    // LRC 时间标签正则: [MM:SS.CC] 或 [MM:SS.CCC]
    private static final Pattern LRC_LINE = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");
    // LRC 元数据标签: [ti:xxx] [ar:xxx] 等
    private static final Pattern LRC_META = Pattern.compile("\\[[a-z]+:.*]");
    // 中文标点
    private static final Pattern CN_PUNCT = Pattern.compile("[，。！？、；：「」\"\"''（）\\(\\)]");
    // 否定词
    private static final Set<String> NEGATIONS = Set.of(
            "不", "没", "无", "别", "未", "莫", "非", "勿", "休", "不再", "从未", "无法", "不要再"
    );

    // 歌词主题词典
    private static final Map<String, String> THEME_DICT = new LinkedHashMap<>();
    static {
        THEME_DICT.put("爱情", "爱,情,吻,拥抱,恋人,分手,思念,爱上,喜欢,心动,恋爱,甜蜜,浪漫,心碎,告白");
        THEME_DICT.put("自然", "风,雨,雪,花,海,山,天空,阳光,月亮,星星,云,春天,秋天,冬天,夏天,落叶");
        THEME_DICT.put("社会", "世界,城市,人群,金钱,挣扎,社会,现实,生活,日子,工作,街头");
        THEME_DICT.put("哲思", "时间,生命,意义,存在,命运,轮回,岁月,灵魂,人生,死亡,活着");
        THEME_DICT.put("叙事", "那天,从前,曾经,故事,后来,记得,说过,想起,某个");
        THEME_DICT.put("励志", "梦想,坚持,勇敢,不怕,向前,奋斗,拼搏,胜利,加油,相信");
        THEME_DICT.put("乡愁", "故乡,回家,妈妈,老房子,远方,童年,家乡,过年,外婆,炊烟");
    }

    /**
     * 分析结果
     */
    public static class AnalysisResult {
        public String primaryEmotion;       // 主情绪（中文名）
        public String secondaryEmotion;     // 次情绪
        public int valence;                 // 效价 -100~100
        public int arousal;                 // 唤醒度 0~100
        public int emotionIntensity;        // 情绪强度 0~100
        public Set<String> matchedKeywords; // 匹配到的关键词集合
        public Map<MusicEmotion, Integer> emotionScores; // 各情绪得分
        public String lyricTheme;           // 歌词主题
        public String suitableScenes;       // 适合场景

        AnalysisResult() {
            this.matchedKeywords = new LinkedHashSet<>();
            this.emotionScores = new LinkedHashMap<>();
        }
    }

    /**
     * 主入口：分析 LRC 歌词
     */
    public AnalysisResult analyze(String lrcLyric) {
        if (lrcLyric == null || lrcLyric.trim().isEmpty()) {
            return emptyResult();
        }

        // 1. 解析 LRC → 纯文本
        String plainText = parseLrc(lrcLyric);
        if (plainText.isEmpty()) {
            return emptyResult();
        }

        // 2. 分句
        List<String> sentences = splitSentences(plainText);

        // 3. 对每句做关键词匹配 + 否定检测
        AnalysisResult result = new AnalysisResult();
        Map<MusicEmotion, Integer> scores = result.emotionScores;
        for (MusicEmotion em : MusicEmotion.values()) {
            scores.put(em, 0);
        }

        for (String sentence : sentences) {
            matchKeywords(sentence, scores, result.matchedKeywords);
        }

        // 4. 确定主次情绪
        List<Map.Entry<MusicEmotion, Integer>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        if (!sorted.isEmpty() && sorted.get(0).getValue() > 0) {
            MusicEmotion primary = sorted.get(0).getKey();
            result.primaryEmotion = primary.getChineseName();
            result.valence = primary.getValence();
            result.arousal = primary.getArousal();

            if (sorted.size() > 1 && sorted.get(1).getValue() > 0) {
                result.secondaryEmotion = sorted.get(1).getKey().getChineseName();
                // 加权计算效价和唤醒度
                int totalHits = sorted.get(0).getValue() + sorted.get(1).getValue();
                result.valence = (primary.getValence() * sorted.get(0).getValue()
                        + sorted.get(1).getKey().getValence() * sorted.get(1).getValue()) / totalHits;
                result.arousal = (primary.getArousal() * sorted.get(0).getValue()
                        + sorted.get(1).getKey().getArousal() * sorted.get(1).getValue()) / totalHits;
            }
        } else {
            result.primaryEmotion = "深沉内敛"; // 默认
            result.valence = 0;
            result.arousal = 25;
        }

        // 5. 情绪强度
        result.emotionIntensity = calcIntensity(scores, result.matchedKeywords.size(), sentences.size());

        // 6. 歌词主题
        result.lyricTheme = classifyTheme(plainText);

        // 7. 适合场景
        result.suitableScenes = inferScenes(result);

        return result;
    }

    /**
     * LRC 解析 → 纯文本
     */
    String parseLrc(String lrc) {
        StringBuilder sb = new StringBuilder();
        // 先移除元数据行
        String cleaned = LRC_META.matcher(lrc).replaceAll("");
        Matcher m = LRC_LINE.matcher(cleaned);
        while (m.find()) {
            String text = m.group(4).trim();
            if (!text.isEmpty()) {
                sb.append(text).append(" ");
            }
        }
        // 如果没有 LRC 标签，说明是纯文本歌词
        if (sb.isEmpty() && !lrc.contains("[")) {
            return lrc.trim();
        }
        return sb.toString().trim();
    }

    /**
     * 按中文标点分句
     */
    List<String> splitSentences(String text) {
        String[] parts = CN_PUNCT.split(text);
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (trimmed.length() >= 2) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * 关键词匹配（含否定检测）
     */
    void matchKeywords(String sentence, Map<MusicEmotion, Integer> scores, Set<String> matchedKeywords) {
        for (MusicEmotion em : MusicEmotion.values()) {
            for (String kw : em.getKeywords()) {
                int idx = sentence.indexOf(kw);
                if (idx >= 0) {
                    // 否定检测：关键词前有否定词
                    if (hasNegationBefore(sentence, idx)) {
                        continue; // 不计入
                    }
                    scores.merge(em, 1, Integer::sum);
                    matchedKeywords.add(kw);
                }
            }
        }
    }

    /**
     * 检查关键词前3个字符内是否有否定词
     */
    boolean hasNegationBefore(String sentence, int keywordIdx) {
        int start = Math.max(0, keywordIdx - 5);
        String prefix = sentence.substring(start, keywordIdx);
        for (String neg : NEGATIONS) {
            if (prefix.contains(neg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算情绪强度 (0~100)
     */
    int calcIntensity(Map<MusicEmotion, Integer> scores, int totalMatches, int totalSentences) {
        if (totalSentences == 0 || totalMatches == 0) return 50;
        double density = (double) totalMatches / totalSentences;
        // 密度映射: 0.0→0, 0.5→30, 1.0→50, 2.0→70, 3.0+→90
        int raw = (int) (Math.min(density, 3.0) / 3.0 * 90);
        // 再加情绪的分散程度：如果只有一个情绪高，说明情绪很集中，强度高
        long activeEmotions = scores.values().stream().filter(v -> v > 0).count();
        if (activeEmotions <= 2 && totalMatches > 3) {
            raw = Math.min(100, raw + 15);
        }
        return Math.max(10, Math.min(100, raw));
    }

    /**
     * 主题分类
     */
    String classifyTheme(String text) {
        String bestTheme = "";
        int bestScore = 0;
        for (Map.Entry<String, String> entry : THEME_DICT.entrySet()) {
            int score = 0;
            for (String kw : entry.getValue().split(",")) {
                if (text.contains(kw)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestTheme = entry.getKey();
            }
        }
        return bestTheme.isEmpty() ? "叙事" : bestTheme;
    }

    /**
     * 根据情绪结果推断适合场景
     */
    String inferScenes(AnalysisResult result) {
        MusicEmotion em = MusicEmotion.fromChineseName(result.primaryEmotion);
        if (em != null) {
            return em.getSuitableScenes();
        }
        if (result.arousal < 20) return "深夜,独处,睡前";
        if (result.arousal < 40) return "午后,咖啡馆,雨天";
        if (result.arousal < 60) return "通勤,工作,驾驶";
        if (result.arousal < 80) return "运动,派对,旅行";
        return "运动,狂欢,释放";
    }

    private AnalysisResult emptyResult() {
        AnalysisResult r = new AnalysisResult();
        r.primaryEmotion = "深沉内敛";
        r.secondaryEmotion = "";
        r.valence = 0;
        r.arousal = 25;
        r.emotionIntensity = 30;
        r.lyricTheme = "";
        r.suitableScenes = "通勤,工作";
        return r;
    }
}
