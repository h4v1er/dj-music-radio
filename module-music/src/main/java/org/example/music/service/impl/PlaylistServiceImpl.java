package org.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.music.entity.Playlist;
import org.example.music.entity.PlaylistSong;
import org.example.music.entity.Song;
import org.example.music.mapper.PlaylistMapper;
import org.example.music.mapper.PlaylistSongMapper;
import org.example.music.mapper.SongMapper;
import org.example.music.service.PlaylistService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.example.music.config.RabbitMQConfig.EXCHANGE_NAME;
import static org.example.music.config.RabbitMQConfig.ROUTING_KEY_IMPORT;

@Service
public class PlaylistServiceImpl implements PlaylistService {

    @Autowired
    private PlaylistMapper playlistMapper;

    @Autowired
    private PlaylistSongMapper playlistSongMapper;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public List<Playlist> getUserPlaylists(Long userId) {
        LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Playlist::getUserId, userId)
               .orderByDesc(Playlist::getUpdatedAt);
        return playlistMapper.selectList(wrapper);
    }

    @Override
    public Playlist getPlaylistDetail(Long playlistId) {
        return playlistMapper.selectById(playlistId);
    }

    @Override
    public List<Song> getPlaylistSongs(Long playlistId) {
        // 查询歌单中的关联记录，按 sort_order 排序
        LambdaQueryWrapper<PlaylistSong> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaylistSong::getPlaylistId, playlistId)
               .orderByAsc(PlaylistSong::getSortOrder);

        List<PlaylistSong> relations = playlistSongMapper.selectList(wrapper);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询歌曲信息
        List<Long> songIds = relations.stream()
                .map(PlaylistSong::getSongId)
                .collect(Collectors.toList());

        List<Song> songs = songMapper.selectBatchIds(songIds);

        // 按 sort_order 排序返回
        Map<Long, Integer> sortMap = new HashMap<>();
        for (PlaylistSong ps : relations) {
            sortMap.put(ps.getSongId(), ps.getSortOrder());
        }
        songs.sort(Comparator.comparingInt(s -> sortMap.getOrDefault(s.getId(), 0)));

        return songs;
    }

    @Override
    @Transactional
    public Playlist createPlaylist(String name, String description, Long userId) {
        Playlist playlist = new Playlist();
        playlist.setName(name);
        playlist.setDescription(description != null ? description : "");
        playlist.setUserId(userId);
        playlist.setSongCount(0);
        playlist.setCoverUrl("");
        playlistMapper.insert(playlist);
        return playlist;
    }

    @Override
    public Playlist updatePlaylist(Long id, String name, String description) {
        Playlist playlist = playlistMapper.selectById(id);
        if (playlist != null) {
            if (name != null) playlist.setName(name);
            if (description != null) playlist.setDescription(description);
            playlistMapper.updateById(playlist);
        }
        return playlist;
    }

    @Override
    @Transactional
    public void deletePlaylist(Long id) {
        // 删除关联的歌曲
        LambdaQueryWrapper<PlaylistSong> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaylistSong::getPlaylistId, id);
        playlistSongMapper.delete(wrapper);
        // 删除歌单
        playlistMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void addSongToPlaylist(Long playlistId, Long songId) {
        // 检查是否已存在
        LambdaQueryWrapper<PlaylistSong> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaylistSong::getPlaylistId, playlistId)
               .eq(PlaylistSong::getSongId, songId);
        if (playlistSongMapper.selectCount(wrapper) > 0) {
            return; // 已存在，不重复添加
        }

        // 获取最大排序号
        Integer maxSort = playlistSongMapper.selectMaxSortOrder(playlistId);

        PlaylistSong ps = new PlaylistSong();
        ps.setPlaylistId(playlistId);
        ps.setSongId(songId);
        ps.setSortOrder(maxSort != null ? maxSort + 1 : 1);
        playlistSongMapper.insert(ps);

        // 更新歌单歌曲数量
        updateSongCount(playlistId);
    }

    @Override
    @Transactional
    public void removeSongFromPlaylist(Long playlistId, Long songId) {
        LambdaQueryWrapper<PlaylistSong> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaylistSong::getPlaylistId, playlistId)
               .eq(PlaylistSong::getSongId, songId);
        playlistSongMapper.delete(wrapper);
        updateSongCount(playlistId);
    }

    @Override
    @Transactional
    public void updateSortOrder(Long playlistId, List<Long> songIds) {
        // 删除旧关联
        LambdaQueryWrapper<PlaylistSong> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaylistSong::getPlaylistId, playlistId);
        playlistSongMapper.delete(wrapper);

        // 按新顺序批量插入
        List<PlaylistSong> list = new ArrayList<>();
        for (int i = 0; i < songIds.size(); i++) {
            PlaylistSong ps = new PlaylistSong();
            ps.setPlaylistId(playlistId);
            ps.setSongId(songIds.get(i));
            ps.setSortOrder(i + 1);
            list.add(ps);
        }
        if (!list.isEmpty()) {
            playlistSongMapper.batchInsert(list);
        }
        updateSongCount(playlistId);
    }

    @Override
    public String importPlaylistAsync(String name, String content, Long userId) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> message = new HashMap<>();
        message.put("taskId", taskId);
        message.put("name", name);
        message.put("content", content);
        message.put("userId", userId);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY_IMPORT, message);
        return taskId;
    }

    /** 更新歌单歌曲数量 */
    private void updateSongCount(Long playlistId) {
        LambdaQueryWrapper<PlaylistSong> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaylistSong::getPlaylistId, playlistId);
        long count = playlistSongMapper.selectCount(wrapper);

        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist != null) {
            playlist.setSongCount((int) count);
            playlistMapper.updateById(playlist);
        }
    }
}
