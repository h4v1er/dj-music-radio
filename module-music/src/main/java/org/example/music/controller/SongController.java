package org.example.music.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.music.dto.Result;
import org.example.music.entity.Song;
import org.example.music.entity.SongEmotion;
import org.example.music.mapper.SongMapper;
import org.example.music.service.EmotionAnalysisService;
import org.example.music.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/music/song")
public class SongController {

    @Autowired
    private SongService songService;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private EmotionAnalysisService emotionAnalysisService;

    /** GET /music/song/list?page=1&size=20&genre=摇滚 */
    @GetMapping("/list")
    public Result<Page<Song>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String genre) {
        return Result.ok(songService.getSongList(page, size, genre));
    }

    /** GET /music/song/{id} */
    @GetMapping("/{id}")
    public Result<Song> detail(@PathVariable Long id) {
        Song song = songService.getSongById(id);
        if (song == null) {
            return Result.fail(404, "歌曲不存在");
        }
        // 每次获取详情时增加播放次数
        songService.incrementPlayCount(id);
        return Result.ok(song);
    }

    /** GET /music/song/search?kw=xxx&page=1&size=20 */
    @GetMapping("/search")
    public Result<Page<Song>> search(
            @RequestParam String kw,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(songService.searchSongs(kw, page, size));
    }

    /** GET /music/song/genres */
    @GetMapping("/genres")
    public Result<List<String>> genres() {
        return Result.ok(songService.getAllGenres());
    }

    /** PUT /music/song/{id}/lyric — 保存/更新歌词（播放时异步缓存） */
    @PutMapping("/{id}/lyric")
    public Result<String> saveLyric(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Song song = songMapper.selectById(id);
        if (song == null) return Result.fail(404, "歌曲不存在");
        String lyric = body.get("lyric");
        if (lyric == null || lyric.isEmpty()) return Result.fail(400, "歌词不能为空");
        song.setLyric(lyric);
        songMapper.updateById(song);
        return Result.ok("歌词已保存");
    }

    /** POST /music/song/{id}/analyze-emotion — 手动触发情绪分析 */
    @PostMapping("/{id}/analyze-emotion")
    public Result<Map<String, Object>> analyzeEmotion(@PathVariable Long id) {
        Song song = songMapper.selectById(id);
        if (song == null) return Result.fail(404, "歌曲不存在");
        if (song.getLyric() == null || song.getLyric().isEmpty())
            return Result.fail(400, "歌曲无歌词，请先获取歌词");

        SongEmotion se = emotionAnalysisService.analyzeSong(id);
        if (se == null) return Result.fail(500, "分析失败");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("songId", id);
        result.put("primaryEmotion", se.getPrimaryEmotion());
        result.put("secondaryEmotion", se.getSecondaryEmotion());
        result.put("valence", se.getValence());
        result.put("arousal", se.getArousal());
        result.put("emotionIntensity", se.getEmotionIntensity());
        result.put("emotionTags", song.getEmotionTags());
        result.put("analyzed", true);
        return Result.ok(result);
    }
}
