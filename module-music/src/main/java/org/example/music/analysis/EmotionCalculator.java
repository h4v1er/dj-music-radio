package org.example.music.analysis;

import org.example.music.taxonomy.MusicEmotion;
import java.util.*;

/**
 * 情绪向量计算工具 — 辅助 LyricsAnalyzer
 * 提供批量统计、品味描述生成等工具方法
 */
public class EmotionCalculator {

    /**
     * 计算一组情绪标签的分布（用于歌单/用户总览）
     * @param emotions 情绪标签列表
     * @return 情绪→百分比 的分布
     */
    public static Map<String, Double> distribution(List<String> emotions) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        int total = 0;
        for (String e : emotions) {
            if (e != null && !e.isEmpty()) {
                counts.merge(e, 1, Integer::sum);
                total++;
            }
        }
        Map<String, Double> dist = new LinkedHashMap<>();
        if (total > 0) {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                dist.put(entry.getKey(), Math.round(entry.getValue() * 1000.0 / total) / 10.0);
            }
        }
        return dist;
    }

    /**
     * 获取 Top N 情绪
     */
    public static List<String> topEmotions(Map<String, Double> distribution, int n) {
        return distribution.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(n)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * 根据情绪分布生成品味描述
     */
    public static String describeTaste(Map<String, Double> distribution) {
        if (distribution.isEmpty()) return "品味待探索";

        List<Map.Entry<String, Double>> sorted = distribution.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .toList();

        if (sorted.isEmpty()) return "品味待探索";

        String top1 = sorted.get(0).getKey();
        double pct1 = sorted.get(0).getValue();

        MusicEmotion em1 = MusicEmotion.fromChineseName(top1);
        String family = em1 != null ? em1.getFamily() : "";

        if (pct1 > 50) {
            return "深度" + family + "型，尤其偏爱「" + top1 + "」氛围的音乐";
        } else if (pct1 > 30) {
            String second = sorted.size() > 1 ? "和「" + sorted.get(1).getKey() + "」" : "";
            return "以「" + top1 + "」为主" + second + "，情感层次丰富";
        } else {
            return "品味多元，没有明显的情绪偏好，音乐心态开放包容";
        }
    }

    /**
     * 根据效价和唤醒度返回能量描述
     */
    public static String energyLabel(int arousal) {
        if (arousal < 20) return "极低";
        if (arousal < 40) return "低";
        if (arousal < 60) return "中等";
        if (arousal < 80) return "高";
        return "极高";
    }

    /**
     * 效价文字描述
     */
    public static String valenceLabel(int valence) {
        if (valence < -60) return "非常消极";
        if (valence < -20) return "偏消极";
        if (valence < 20) return "中性";
        if (valence < 60) return "偏积极";
        return "非常积极";
    }

    /**
     * 生成曲风氛围标签
     */
    public static List<String> moodTags(MusicEmotion emotion) {
        List<String> tags = new ArrayList<>();
        if (emotion == null) return tags;
        int arousal = emotion.getArousal();
        int valence = emotion.getValence();
        if (arousal < 30) tags.add("舒缓");
        if (arousal > 70) tags.add("激烈");
        if (valence > 40) tags.add("明亮");
        if (valence < -40) tags.add("灰暗");
        if (valence > 0 && arousal < 30) tags.add("治愈");
        if (valence < 0 && arousal < 30) tags.add("伤感");
        tags.add(emotion.getChineseName());
        return tags;
    }
}
