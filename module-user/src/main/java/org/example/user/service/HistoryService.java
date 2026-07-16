package org.example.user.service;

import org.example.user.entity.PlayHistory;

import java.util.List;

public interface HistoryService {

    boolean add(Long userId, Long songId);

    List<PlayHistory> list(Long userId);

    boolean clear(Long userId);

}