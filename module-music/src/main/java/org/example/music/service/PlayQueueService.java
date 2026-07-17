package org.example.music.service;

import org.example.music.dto.PlayQueueStateDTO;

public interface PlayQueueService {

    PlayQueueStateDTO getState(Long userId);

    PlayQueueStateDTO saveState(PlayQueueStateDTO state);

    void clearState(Long userId);
}
