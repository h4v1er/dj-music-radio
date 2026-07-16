package org.example.music.dto;

/**
 * 歌单导入请求
 */
public class PlaylistImportDTO {
    private String name;
    private String content;
    private Long userId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
