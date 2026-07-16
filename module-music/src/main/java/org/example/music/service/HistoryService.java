package org.example.music.service;

import org.example.music.entity.Song;
import java.util.List;

public interface HistoryService {

    /** 记录播放历史 */
    void recordPlay(Long userId, Long songId);

    /** 获取播放历史（最近50首） */
    List<Song> getPlayHistory(Long userId);
}
