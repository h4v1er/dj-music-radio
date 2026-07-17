package org.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.music.dto.PlayQueueStateDTO;
import org.example.music.entity.Song;
import org.example.music.entity.UserPlayQueue;
import org.example.music.entity.UserPlayerState;
import org.example.music.mapper.UserPlayQueueMapper;
import org.example.music.mapper.UserPlayerStateMapper;
import org.example.music.service.PlayQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PlayQueueServiceImpl implements PlayQueueService {

    private static final String DEFAULT_PLAY_MODE = "order";

    @Autowired
    private UserPlayQueueMapper queueMapper;

    @Autowired
    private UserPlayerStateMapper stateMapper;

    @Override
    public PlayQueueStateDTO getState(Long userId) {
        Long realUserId = normalizeUserId(userId);
        UserPlayerState playerState = stateMapper.selectById(realUserId);

        LambdaQueryWrapper<UserPlayQueue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPlayQueue::getUserId, realUserId)
               .orderByAsc(UserPlayQueue::getSortOrder);
        List<UserPlayQueue> rows = queueMapper.selectList(wrapper);
        rows.sort(Comparator.comparingInt(row -> row.getSortOrder() == null ? 0 : row.getSortOrder()));

        List<Song> songs = rows.stream().map(this::toSong).toList();
        String currentKey = playerState != null ? playerState.getCurrentKey() : "";

        PlayQueueStateDTO dto = new PlayQueueStateDTO();
        dto.setUserId(realUserId);
        dto.setPlayMode(normalizePlayMode(playerState != null ? playerState.getPlayMode() : DEFAULT_PLAY_MODE));
        dto.setQueue(songs);
        dto.setCurrentSong(findCurrentSong(songs, currentKey));
        return dto;
    }

    @Override
    @Transactional
    public PlayQueueStateDTO saveState(PlayQueueStateDTO state) {
        Long realUserId = normalizeUserId(state != null ? state.getUserId() : null);
        List<Song> queue = state != null && state.getQueue() != null ? state.getQueue() : new ArrayList<>();
        Song currentSong = state != null ? state.getCurrentSong() : null;
        String currentKey = songKey(currentSong);

        LambdaQueryWrapper<UserPlayQueue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPlayQueue::getUserId, realUserId);
        queueMapper.delete(wrapper);

        int order = 1;
        for (Song song : queue) {
            if (song == null || isBlank(song.getTitle())) {
                continue;
            }
            UserPlayQueue row = toQueueRow(realUserId, song, order++);
            queueMapper.insert(row);
        }

        UserPlayerState playerState = new UserPlayerState();
        playerState.setUserId(realUserId);
        playerState.setPlayMode(normalizePlayMode(state != null ? state.getPlayMode() : DEFAULT_PLAY_MODE));
        playerState.setCurrentKey(currentKey);
        if (stateMapper.selectById(realUserId) == null) {
            stateMapper.insert(playerState);
        } else {
            stateMapper.updateById(playerState);
        }

        return getState(realUserId);
    }

    @Override
    @Transactional
    public void clearState(Long userId) {
        Long realUserId = normalizeUserId(userId);
        LambdaQueryWrapper<UserPlayQueue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPlayQueue::getUserId, realUserId);
        queueMapper.delete(wrapper);

        UserPlayerState playerState = new UserPlayerState();
        playerState.setUserId(realUserId);
        playerState.setPlayMode(DEFAULT_PLAY_MODE);
        playerState.setCurrentKey("");
        if (stateMapper.selectById(realUserId) == null) {
            stateMapper.insert(playerState);
        } else {
            stateMapper.updateById(playerState);
        }
    }

    private Long normalizeUserId(Long userId) {
        return userId != null && userId > 0 ? userId : 1L;
    }

    private String normalizePlayMode(String playMode) {
        if ("shuffle".equals(playMode) || "repeat".equals(playMode)) {
            return playMode;
        }
        return DEFAULT_PLAY_MODE;
    }

    private Song findCurrentSong(List<Song> songs, String currentKey) {
        if (songs == null || songs.isEmpty()) {
            return null;
        }
        if (!isBlank(currentKey)) {
            for (Song song : songs) {
                if (currentKey.equals(songKey(song))) {
                    return song;
                }
            }
        }
        return songs.get(0);
    }

    private UserPlayQueue toQueueRow(Long userId, Song song, int sortOrder) {
        UserPlayQueue row = new UserPlayQueue();
        row.setUserId(userId);
        row.setSongId(song.getId());
        row.setSource(valueOrDefault(song.getSource(), "PROJECT"));
        row.setSourceId(valueOrDefault(song.getSourceId(), ""));
        row.setTitle(valueOrDefault(song.getTitle(), "未知歌曲"));
        row.setArtist(valueOrDefault(song.getArtist(), ""));
        row.setAlbum(valueOrDefault(song.getAlbum(), ""));
        row.setGenre(valueOrDefault(song.getGenre(), ""));
        row.setCoverUrl(valueOrDefault(song.getCoverUrl(), ""));
        row.setFilePath(valueOrDefault(song.getFilePath(), ""));
        row.setDuration(song.getDuration() != null ? song.getDuration() : 0);
        row.setSortOrder(sortOrder);
        return row;
    }

    private Song toSong(UserPlayQueue row) {
        Song song = new Song();
        song.setId(row.getSongId() != null ? row.getSongId() : syntheticId(row));
        song.setTitle(row.getTitle());
        song.setArtist(row.getArtist());
        song.setAlbum(row.getAlbum());
        song.setGenre(row.getGenre());
        song.setCoverUrl(row.getCoverUrl());
        song.setFilePath(row.getFilePath());
        song.setDuration(row.getDuration());
        song.setSource(row.getSource());
        song.setSourceId(row.getSourceId());
        return song;
    }

    private Long syntheticId(UserPlayQueue row) {
        String sourceId = row.getSourceId();
        if (!isBlank(sourceId)) {
            try {
                return Long.valueOf(sourceId);
            } catch (NumberFormatException ignored) {
                return row.getId();
            }
        }
        return row.getId();
    }

    private String songKey(Song song) {
        if (song == null) {
            return "";
        }
        String source = valueOrDefault(song.getSource(), "");
        String sourceId = valueOrDefault(song.getSourceId(), "");
        if (!isBlank(sourceId)) {
            return source + ":" + sourceId;
        }
        if (song.getId() != null) {
            return "ID:" + song.getId();
        }
        return valueOrDefault(song.getTitle(), "") + "|" + valueOrDefault(song.getArtist(), "");
    }

    private String valueOrDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
