package org.example.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.user.entity.PlayHistory;

import java.util.List;

public interface PlayHistoryService extends IService<PlayHistory> {

    void recordPlay(Long userId, Long songId);

    List<PlayHistory> listByUserId(Long userId, int limit);
}
