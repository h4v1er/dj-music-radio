package org.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.user.entity.PlayHistory;
import org.example.user.mapper.PlayHistoryMapper;
import org.example.user.service.HistoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final PlayHistoryMapper historyMapper;

    @Override
    public boolean add(Long userId, Long songId) {

        PlayHistory history = new PlayHistory();

        history.setUserId(userId);

        history.setSongId(songId);

        history.setPlayTime(LocalDateTime.now());

        return historyMapper.insert(history) > 0;
    }

    @Override
    public List<PlayHistory> list(Long userId) {

        LambdaQueryWrapper<PlayHistory> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(PlayHistory::getUserId, userId);

        wrapper.orderByDesc(PlayHistory::getPlayTime);

        return historyMapper.selectList(wrapper);

    }

    @Override
    public boolean clear(Long userId) {

        LambdaQueryWrapper<PlayHistory> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(PlayHistory::getUserId, userId);

        return historyMapper.delete(wrapper) > 0;

    }

}