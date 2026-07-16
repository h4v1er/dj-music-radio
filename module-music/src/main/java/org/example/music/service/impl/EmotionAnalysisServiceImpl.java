package org.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.music.ai.DeepSeekClient;
import org.example.music.analysis.LyricsAnalyzer;
import org.example.music.analysis.LyricsAnalyzer.AnalysisResult;
import org.example.music.entity.*;
import org.example.music.mapper.*;
import org.example.music.service.EmotionAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmotionAnalysisServiceImpl implements EmotionAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(EmotionAnalysisServiceImpl.class);

    @Autowired
    private SongMapper songMapper;
    @Autowired
    private SongEmotionMapper songEmotionMapper;
    @Autowired
    private PlaylistSongMapper playlistSongMapper;
    @Autowired
    private PlaylistMapper playlistMapper;

    @Autowired(required = false)
    private DeepSeekClient deepSeekClient;

    private final LyricsAnalyzer keywordAnalyzer = new LyricsAnalyzer();

    @Override
    @Transactional
    public SongEmotion analyzeSong(Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            log.warn("歌曲不存在: id={}", songId);
            return null;
        }

        String lyric = song.getLyric();
        if (lyric == null || lyric.trim().isEmpty()) {
            log.info("歌曲无歌词数据，跳过分析: id={}, title={}", songId, song.getTitle());
            // 标记为已分析（无需分析）
            song.setEmotionAnalyzed(1);
            songMapper.updateById(song);
            return null;
        }

        // 1️⃣ 优先尝试 AI 分析 (DeepSeek)
        DeepSeekClient.AnalysisResult aiResult = null;
        if (deepSeekClient != null) {
            aiResult = deepSeekClient.analyzeLyric(lyric, song.getTitle());
        }

        // 2️⃣ 回退到关键词分析
        AnalysisResult keywordResult = null;
        if (aiResult == null) {
            log.info("AI分析不可用，回退到关键词匹配: songId={}", songId);
            keywordResult = keywordAnalyzer.analyze(lyric);
        }

        // 构建或更新 SongEmotion
        SongEmotion emotion = getOrCreate(songId);

        if (aiResult != null) {
            // 使用 AI 结果
            emotion.setPrimaryEmotion(aiResult.primaryEmotion);
            emotion.setSecondaryEmotion(aiResult.secondaryEmotion != null ? aiResult.secondaryEmotion : "");
            emotion.setValence(aiResult.valence);
            emotion.setArousal(aiResult.arousal);
            emotion.setEmotionIntensity(aiResult.emotionIntensity);
            emotion.setMoodTags(buildMoodTagsFromAI(aiResult));
            emotion.setLyricTheme(aiResult.lyricTheme != null ? aiResult.lyricTheme : "");
            emotion.setLyricKeywords("[]");
            emotion.setSuitableScenes(aiResult.suitableScenes != null ? aiResult.suitableScenes : "");
            emotion.setAnalyzed(1);

            log.info("AI情绪分析完成: songId={}, emotion={}, theme={}, valence={}, arousal={}",
                    songId, aiResult.primaryEmotion, aiResult.lyricTheme, aiResult.valence, aiResult.arousal);
        } else if (keywordResult != null) {
            // 使用关键词匹配结果
            emotion.setPrimaryEmotion(keywordResult.primaryEmotion);
            emotion.setSecondaryEmotion(keywordResult.secondaryEmotion != null ? keywordResult.secondaryEmotion : "");
            emotion.setValence(keywordResult.valence);
            emotion.setArousal(keywordResult.arousal);
            emotion.setEmotionIntensity(keywordResult.emotionIntensity);
            emotion.setMoodTags(String.join(",", getMoodTagList(keywordResult)));
            emotion.setLyricTheme(keywordResult.lyricTheme);
            emotion.setLyricKeywords(toJsonArray(keywordResult.matchedKeywords));
            emotion.setSuitableScenes(keywordResult.suitableScenes);
            emotion.setAnalyzed(1);

            log.info("关键词情绪分析完成: songId={}, emotion={}, theme={}, valence={}, arousal={}",
                    songId, keywordResult.primaryEmotion, keywordResult.lyricTheme,
                    keywordResult.valence, keywordResult.arousal);
        } else {
            return null;
        }

        if (emotion.getId() == null) {
            songEmotionMapper.insert(emotion);
        } else {
            songEmotionMapper.updateById(emotion);
        }

        // 更新 song 冗余字段
        song.setEmotionTags(emotion.getPrimaryEmotion()
                + (emotion.getSecondaryEmotion() != null && !emotion.getSecondaryEmotion().isEmpty()
                   ? "," + emotion.getSecondaryEmotion() : ""));
        song.setEmotionAnalyzed(1);
        songMapper.updateById(song);

        log.info("情绪分析完成: id={}, title={}, emotion={}, source={}",
                songId, song.getTitle(), emotion.getPrimaryEmotion(),
                aiResult != null ? "AI" : "keyword");

        return emotion;
    }

    @Override
    public void analyzePlaylistAsync(Long playlistId) {
        // 异步执行批量分析（使用虚拟线程或简单新线程）
        Thread.startVirtualThread(() -> {
            try {
                log.info("开始批量分析歌单: playlistId={}", playlistId);
                List<PlaylistSong> relations = playlistSongMapper.selectList(
                        new LambdaQueryWrapper<PlaylistSong>()
                                .eq(PlaylistSong::getPlaylistId, playlistId));

                int analyzed = 0;
                for (PlaylistSong ps : relations) {
                    try {
                        SongEmotion se = analyzeSong(ps.getSongId());
                        if (se != null) analyzed++;
                    } catch (Exception e) {
                        log.error("分析歌曲失败: songId={}", ps.getSongId(), e);
                    }
                }

                // 更新歌单封面（用第一首歌的封面）
                if (analyzed > 0 && relations.size() > 0) {
                    Playlist pl = playlistMapper.selectById(playlistId);
                    if (pl != null && (pl.getCoverUrl() == null || pl.getCoverUrl().isEmpty())) {
                        Song firstSong = songMapper.selectById(relations.get(0).getSongId());
                        if (firstSong != null && firstSong.getCoverUrl() != null
                                && !firstSong.getCoverUrl().isEmpty()) {
                            pl.setCoverUrl(firstSong.getCoverUrl());
                            playlistMapper.updateById(pl);
                        }
                    }
                }

                log.info("批量分析完成: playlistId={}, total={}, analyzed={}",
                        playlistId, relations.size(), analyzed);
            } catch (Exception e) {
                log.error("批量分析歌单失败: playlistId={}", playlistId, e);
            }
        });
    }

    @Override
    public SongEmotion getSongEmotion(Long songId) {
        return songEmotionMapper.selectOne(
                new LambdaQueryWrapper<SongEmotion>()
                        .eq(SongEmotion::getSongId, songId));
    }

    @Override
    public List<Long> searchByEmotion(String emotionTag) {
        LambdaQueryWrapper<SongEmotion> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .like(SongEmotion::getPrimaryEmotion, emotionTag)
                .or()
                .like(SongEmotion::getSecondaryEmotion, emotionTag)
        );
        return songEmotionMapper.selectList(wrapper).stream()
                .map(SongEmotion::getSongId)
                .collect(Collectors.toList());
    }

    @Override
    public List<SongEmotion> getBatchEmotions(List<Long> songIds) {
        if (songIds == null || songIds.isEmpty()) return Collections.emptyList();
        return songEmotionMapper.selectList(
                new LambdaQueryWrapper<SongEmotion>()
                        .in(SongEmotion::getSongId, songIds));
    }

    // ── 私有方法 ──

    private SongEmotion getOrCreate(Long songId) {
        SongEmotion existing = songEmotionMapper.selectOne(
                new LambdaQueryWrapper<SongEmotion>()
                        .eq(SongEmotion::getSongId, songId));
        if (existing != null) return existing;
        SongEmotion se = new SongEmotion();
        se.setSongId(songId);
        return se;
    }

    private String buildMoodTagsFromAI(DeepSeekClient.AnalysisResult ar) {
        List<String> tags = new ArrayList<>();
        if (ar.primaryEmotion != null) tags.add(ar.primaryEmotion);
        if (ar.secondaryEmotion != null && !ar.secondaryEmotion.isEmpty()) tags.add(ar.secondaryEmotion);
        if (ar.arousal < 30) tags.add("舒缓");
        if (ar.arousal > 70) tags.add("激烈");
        if (ar.valence > 40) tags.add("明亮");
        if (ar.valence < -40) tags.add("灰暗");
        return String.join(",", tags);
    }

    private List<String> getMoodTagList(AnalysisResult result) {
        List<String> tags = new ArrayList<>();
        if (result.primaryEmotion != null) tags.add(result.primaryEmotion);
        if (result.secondaryEmotion != null && !result.secondaryEmotion.isEmpty())
            tags.add(result.secondaryEmotion);
        if (result.arousal < 30) tags.add("舒缓");
        if (result.arousal > 70) tags.add("激烈");
        if (result.valence > 40) tags.add("明亮");
        if (result.valence < -40) tags.add("灰暗");
        return tags;
    }

    private String toJsonArray(Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String kw : keywords) {
            if (!first) sb.append(",");
            sb.append("\"").append(kw.replace("\"", "\\\"")).append("\"");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}
