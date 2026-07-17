package org.example.music.controller;

import org.example.music.dto.PlaylistImportDTO;
import org.example.music.dto.Result;
import org.example.music.entity.Playlist;
import org.example.music.entity.Song;
import org.example.music.service.PlaylistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/music/playlist")
public class PlaylistController {

    @Autowired
    private PlaylistService playlistService;

    /** GET /music/playlist/list?userId=1 */
    @GetMapping("/list")
    public Result<List<Playlist>> list(@RequestParam(defaultValue = "1") Long userId) {
        return Result.ok(playlistService.getUserPlaylists(userId));
    }

    /** GET /music/playlist/{id} */
    @GetMapping("/{id}")
    public Result<Playlist> detail(@PathVariable Long id) {
        Playlist playlist = playlistService.getPlaylistDetail(id);
        if (playlist == null) {
            return Result.fail(404, "歌单不存在");
        }
        return Result.ok(playlist);
    }

    /** GET /music/playlist/{id}/songs */
    @GetMapping("/{id}/songs")
    public Result<List<Song>> songs(@PathVariable Long id) {
        return Result.ok(playlistService.getPlaylistSongs(id));
    }

    /** POST /music/playlist */
    @PostMapping
    public Result<Playlist> create(@RequestBody Map<String, Object> body) {
        String name = stringValue(body.get("name"));
        if (name == null || name.trim().isEmpty()) {
            return Result.fail(400, "歌单名称不能为空");
        }
        String description = stringValue(body.getOrDefault("description", ""));
        Long userId = longValue(body.get("userId"), 1L);
        return Result.ok(playlistService.createPlaylist(name, description, userId));
    }

    /** PUT /music/playlist/{id} */
    @PutMapping("/{id}")
    public Result<Playlist> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(playlistService.updatePlaylist(
                id, body.get("name"), body.get("description")));
    }

    /** DELETE /music/playlist/{id} */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        playlistService.deletePlaylist(id);
        return Result.ok();
    }

    /** POST /music/playlist/{id}/song/{songId} */
    @PostMapping("/{id}/song/{songId}")
    public Result<Void> addSong(@PathVariable Long id, @PathVariable Long songId) {
        playlistService.addSongToPlaylist(id, songId);
        return Result.ok();
    }

    /** DELETE /music/playlist/{id}/song/{songId} */
    @DeleteMapping("/{id}/song/{songId}")
    public Result<Void> removeSong(@PathVariable Long id, @PathVariable Long songId) {
        playlistService.removeSongFromPlaylist(id, songId);
        return Result.ok();
    }

    /** PUT /music/playlist/{id}/sort */
    @PutMapping("/{id}/sort")
    public Result<Void> sort(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        List<Long> songIds = body.get("songIds");
        if (songIds != null && !songIds.isEmpty()) {
            playlistService.updateSortOrder(id, songIds);
        }
        return Result.ok();
    }

    /** POST /music/playlist/import */
    @PostMapping("/import")
    public Result<Map<String, String>> importPlaylist(@RequestBody PlaylistImportDTO dto) {
        String taskId = playlistService.importPlaylistAsync(
                dto.getName(), dto.getContent(),
                dto.getUserId() != null ? dto.getUserId() : 1L);
        return Result.ok(Map.of("taskId", taskId, "status", "processing"));
    }

    /** GET /music/playlist/import/status/{taskId} */
    @GetMapping("/import/status/{taskId}")
    public Result<Map<String, String>> importStatus(@PathVariable String taskId) {
        // 简化实现：直接返回完成（实际可查Redis状态）
        return Result.ok(Map.of("taskId", taskId, "status", "completed"));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : Long.valueOf(text);
    }
}
