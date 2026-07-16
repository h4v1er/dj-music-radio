package org.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.music.entity.PlayHistory;
import org.example.music.entity.Song;
import org.example.music.mapper.PlayHistoryMapper;
import org.example.music.mapper.SongMapper;
import org.example.music.service.HistoryService;
import org.example.music.service.UserTasteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoryServiceImpl implements HistoryService {

    @Autowired
    private PlayHistoryMapper historyMapper;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private UserTasteService userTasteService;

    @Override
    @Transactional
    public void recordPlay(Long userId, Long songId) {
        PlayHistory history = new PlayHistory();
        history.setUserId(userId);
        history.setSongId(songId);
        historyMapper.insert(history);

        // 品味更新
        try { userTasteService.onPlay(userId, songId); } catch (Exception ignored) {}
    }

    @Override
    public List<Song> getPlayHistory(Long userId) {
        // 查询最近 50 条播放记录
        LambdaQueryWrapper<PlayHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayHistory::getUserId, userId)
               .orderByDesc(PlayHistory::getPlayedAt)
               .last("LIMIT 50");

        List<PlayHistory> histories = historyMapper.selectList(wrapper);
        if (histories.isEmpty()) {
            return Collections.emptyList();
        }

        // 按 playedAt 倒序去重取 songId
        List<Long> songIds = histories.stream()
                .map(PlayHistory::getSongId)
                .distinct()
                .collect(Collectors.toList());

        List<Song> songs = songMapper.selectBatchIds(songIds);

        // 按播放时间倒序排列
        songs.sort((a, b) -> {
            int idxA = songIds.indexOf(a.getId());
            int idxB = songIds.indexOf(b.getId());
            return Integer.compare(idxA, idxB);
        });

        return songs;
    }
}
