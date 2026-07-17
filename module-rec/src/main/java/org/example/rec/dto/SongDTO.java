package org.example.rec.dto;

/**
 * 歌曲数据传输对象 — 对应音乐服务返回的歌曲信息
 * 与 API 规范中的歌曲对象结构保持一致
 */
public class SongDTO {

    private Integer id;
    private String title;
    private String artist;
    private String album;
    private String coverUrl;
    private String audioUrl;
    private Integer duration;
    private String genre;
    private String emotionTags;
    private Integer playCount;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getEmotionTags() {
        return emotionTags;
    }

    public void setEmotionTags(String emotionTags) {
        this.emotionTags = emotionTags;
    }

    public Integer getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Integer playCount) {
        this.playCount = playCount;
    }
}
