package org.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.user.entity.PlayHistory;
import org.example.user.mapper.PlayHistoryMapper;
import org.example.user.service.PlayHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayHistoryServiceImpl extends ServiceImpl<PlayHistoryMapper, PlayHistory>
        implements PlayHistoryService {

    @Override
    public void recordPlay(Long userId, Long songId) {
        PlayHistory history = new PlayHistory();
        history.setUserId(userId);
        history.setSongId(songId);
        save(history);
    }

    @Override
    public List<PlayHistory> listByUserId(Long userId, int limit) {
        return list(new LambdaQueryWrapper<PlayHistory>()
                .eq(PlayHistory::getUserId, userId)
                .orderByDesc(PlayHistory::getPlayTime)
                .last("LIMIT " + limit));
    }
}
