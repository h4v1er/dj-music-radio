package org.example.music.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("playlist_song")
public class PlaylistSong {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long playlistId;
    private Long songId;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime addedAt;

    // ── Getters ──
    public Long getId() { return id; }
    public Long getPlaylistId() { return playlistId; }
    public Long getSongId() { return songId; }
    public Integer getSortOrder() { return sortOrder; }
    public LocalDateTime getAddedAt() { return addedAt; }

    // ── Setters ──
    public void setId(Long id) { this.id = id; }
    public void setPlaylistId(Long playlistId) { this.playlistId = playlistId; }
    public void setSongId(Long songId) { this.songId = songId; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}
