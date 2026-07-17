package org.example.music.dto;

import org.example.music.entity.Song;
import java.util.ArrayList;
import java.util.List;

public class PlayQueueStateDTO {
    private Long userId;
    private String playMode = "order";
    private Song currentSong;
    private List<Song> queue = new ArrayList<>();

    public Long getUserId() { return userId; }
    public String getPlayMode() { return playMode; }
    public Song getCurrentSong() { return currentSong; }
    public List<Song> getQueue() { return queue; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setPlayMode(String playMode) { this.playMode = playMode; }
    public void setCurrentSong(Song currentSong) { this.currentSong = currentSong; }
    public void setQueue(List<Song> queue) { this.queue = queue != null ? queue : new ArrayList<>(); }
}
