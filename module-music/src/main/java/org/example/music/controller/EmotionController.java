package org.example.music.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.music.analysis.EmotionCalculator;
import org.example.music.dto.Result;
import org.example.music.entity.*;
import org.example.music.mapper.*;
import org.example.music.service.EmotionAnalysisService;
import org.example.music.service.UserTasteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 情绪分析 REST API
 * 路径: /music/emotion/* 和 /music/taste/*
 */
@RestController
@RequestMapping("/music")
public class EmotionController {

    @Autowired
    private EmotionAnalysisService emotionAnalysisService;
    @Autowired
    private UserTasteService userTasteService;
    @Autowired
    private SongMapper songMapper;
    @Autowired
    private PlaylistSongMapper playlistSongMapper;

    // ═══════════ 情绪分析 ═══════════

    /** GET /music/emotion/{songId} — 获取歌曲情绪画像 */
    @GetMapping("/emotion/{songId}")
    public Result<Map<String, Object>> getEmotion(@PathVariable Long songId) {
        SongEmotion se = emotionAnalysisService.getSongEmotion(songId);
        Song song = songMapper.selectById(songId);

        if (se == null && song != null && song.getLyric() != null && !song.getLyric().isEmpty()) {
            se = emotionAnalysisService.analyzeSong(songId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("songId", songId);
        if (song != null) {
            result.put("title", song.getTitle());
            result.put("artist", song.getArtist());
        }
        if (se != null) {
            result.put("primaryEmotion", se.getPrimaryEmotion());
            result.put("secondaryEmotion", se.getSecondaryEmotion());
            result.put("valence", se.getValence());
            result.put("arousal", se.getArousal());
            result.put("emotionIntensity", se.getEmotionIntensity());
            result.put("moodTags", se.getMoodTags() != null && !se.getMoodTags().isEmpty()
                    ? Arrays.asList(se.getMoodTags().split(",")) : List.of());
            result.put("lyricTheme", se.getLyricTheme());
            result.put("suitableScenes", se.getSuitableScenes() != null && !se.getSuitableScenes().isEmpty()
                    ? Arrays.asList(se.getSuitableScenes().split(",")) : List.of());
            result.put("energyLabel", EmotionCalculator.energyLabel(se.getArousal()));
            result.put("valenceLabel", EmotionCalculator.valenceLabel(se.getValence()));
            result.put("analyzed", true);
        } else {
            result.put("analyzed", false);
        }
        return Result.ok(result);
    }

    /** POST /music/emotion/analyze/{songId} — 触发单首分析 */
    @PostMapping("/emotion/analyze/{songId}")
    public Result<Map<String, Object>> analyzeSong(@PathVariable Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) return Result.fail(404, "歌曲不存在");
        if (song.getLyric() == null || song.getLyric().isEmpty())
            return Result.fail(400, "歌曲无歌词数据，无法分析");

        SongEmotion se = emotionAnalysisService.analyzeSong(songId);
        if (se == null) return Result.fail(500, "分析失败");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("songId", songId);
        result.put("primaryEmotion", se.getPrimaryEmotion());
        result.put("secondaryEmotion", se.getSecondaryEmotion());
        result.put("valence", se.getValence());
        result.put("arousal", se.getArousal());
        result.put("energyLabel", EmotionCalculator.energyLabel(se.getArousal()));
        return Result.ok(result);
    }

    /** POST /music/emotion/batch/{playlistId} — 批量分析歌单 */
    @PostMapping("/emotion/batch/{playlistId}")
    public Result<String> batchAnalyze(@PathVariable Long playlistId) {
        emotionAnalysisService.analyzePlaylistAsync(playlistId);
        return Result.ok("批量分析任务已提交，将在后台异步处理");
    }

    /** GET /music/emotion/search?tag=温柔缱绻 — 按情绪搜索 */
    @GetMapping("/emotion/search")
    public Result<List<Map<String, Object>>> searchByEmotion(@RequestParam String tag) {
        List<Long> songIds = emotionAnalysisService.searchByEmotion(tag);
        if (songIds.isEmpty()) return Result.ok(List.of());

        List<Song> songs = songMapper.selectBatchIds(songIds);
        List<SongEmotion> emotions = emotionAnalysisService.getBatchEmotions(songIds);
        Map<Long, SongEmotion> emotionMap = emotions.stream()
                .collect(Collectors.toMap(SongEmotion::getSongId, e -> e, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Song song : songs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", song.getId());
            item.put("title", song.getTitle());
            item.put("artist", song.getArtist());
            item.put("album", song.getAlbum());
            item.put("coverUrl", song.getCoverUrl());
            item.put("duration", song.getDuration());
            SongEmotion se = emotionMap.get(song.getId());
            if (se != null) {
                item.put("primaryEmotion", se.getPrimaryEmotion());
                item.put("secondaryEmotion", se.getSecondaryEmotion());
                item.put("valence", se.getValence());
                item.put("arousal", se.getArousal());
            }
            result.add(item);
        }
        return Result.ok(result);
    }

    /** GET /music/emotion/playlist/{playlistId}/overview — 歌单情绪总览 */
    @GetMapping("/emotion/playlist/{playlistId}/overview")
    public Result<Map<String, Object>> playlistEmotionOverview(@PathVariable Long playlistId) {
        List<PlaylistSong> relations = playlistSongMapper.selectList(
                new LambdaQueryWrapper<PlaylistSong>()
                        .eq(PlaylistSong::getPlaylistId, playlistId));

        if (relations.isEmpty()) {
            return Result.fail(404, "歌单为空或不存在");
        }

        List<Long> songIds = relations.stream().map(PlaylistSong::getSongId).toList();
        List<SongEmotion> emotions = emotionAnalysisService.getBatchEmotions(songIds);

        // 统计情绪分布
        Map<String, Integer> counts = new LinkedHashMap<>();
        int analyzedCount = 0;
        for (SongEmotion se : emotions) {
            if (se.getPrimaryEmotion() != null) {
                counts.merge(se.getPrimaryEmotion(), 1, Integer::sum);
                analyzedCount++;
            }
        }

        Map<String, Double> distribution = EmotionCalculator.distribution(
                emotions.stream().map(SongEmotion::getPrimaryEmotion).toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("playlistId", playlistId);
        result.put("totalSongs", songIds.size());
        result.put("analyzedSongs", analyzedCount);
        result.put("distribution", distribution);
        result.put("topEmotions", EmotionCalculator.topEmotions(distribution, 3));
        result.put("summary", EmotionCalculator.describeTaste(distribution));

        return Result.ok(result);
    }

    // ═══════════ 用户品味 ═══════════

    /** GET /music/taste/{userId} — 获取用户品味画像 */
    @GetMapping("/taste/{userId}")
    public Result<Map<String, Object>> getTaste(@PathVariable Long userId) {
        UserTaste taste = userTasteService.getUserTaste(userId);
        Map<String, Double> distribution = userTasteService.getEmotionDistribution(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("topEmotions", taste.getTopEmotions() != null && !taste.getTopEmotions().isEmpty()
                ? Arrays.asList(taste.getTopEmotions().split(",")) : List.of());
        result.put("tasteDesc", taste.getTasteDesc());
        result.put("emotionDistribution", distribution);
        return Result.ok(result);
    }

    /** GET /music/taste/refresh/{userId} — 刷新品味 */
    @GetMapping("/taste/refresh/{userId}")
    public Result<Map<String, Object>> refreshTaste(@PathVariable Long userId) {
        UserTaste taste = userTasteService.refreshTaste(userId);
        Map<String, Double> distribution = userTasteService.getEmotionDistribution(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("topEmotions", taste.getTopEmotions() != null && !taste.getTopEmotions().isEmpty()
                ? Arrays.asList(taste.getTopEmotions().split(",")) : List.of());
        result.put("tasteDesc", taste.getTasteDesc());
        result.put("emotionDistribution", distribution);
        result.put("refreshed", true);
        return Result.ok(result);
    }
}
