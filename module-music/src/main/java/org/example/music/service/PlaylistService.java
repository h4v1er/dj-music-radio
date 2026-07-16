package org.example.music.service;

import org.example.music.entity.Playlist;
import org.example.music.entity.Song;
import java.util.List;
import java.util.Map;

public interface PlaylistService {

    /** 获取用户的所有歌单 */
    List<Playlist> getUserPlaylists(Long userId);

    /** 获取歌单详情（含歌曲列表） */
    Playlist getPlaylistDetail(Long playlistId);

    /** 获取歌单中的歌曲列表 */
    List<Song> getPlaylistSongs(Long playlistId);

    /** 创建歌单 */
    Playlist createPlaylist(String name, String description, Long userId);

    /** 更新歌单信息 */
    Playlist updatePlaylist(Long id, String name, String description);

    /** 删除歌单 */
    void deletePlaylist(Long id);

    /** 添加歌曲到歌单 */
    void addSongToPlaylist(Long playlistId, Long songId);

    /** 从歌单移除歌曲 */
    void removeSongFromPlaylist(Long playlistId, Long songId);

    /** 更新歌单歌曲排序 */
    void updateSortOrder(Long playlistId, List<Long> songIds);

    /** 异步导入歌单（发送消息到RabbitMQ） */
    String importPlaylistAsync(String name, String content, Long userId);
}
