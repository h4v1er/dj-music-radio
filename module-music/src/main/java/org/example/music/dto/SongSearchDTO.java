package org.example.music.dto;

/**
 * 歌曲搜索请求参数
 */
public class SongSearchDTO {
    private String kw;
    private String genre;
    private Integer page = 1;
    private Integer size = 20;

    public String getKw() { return kw; }
    public void setKw(String kw) { this.kw = kw; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
