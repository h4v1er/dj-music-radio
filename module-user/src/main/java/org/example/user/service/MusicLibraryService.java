package org.example.user.service;

public interface MusicLibraryService {

    Object favoriteList(Long userId);

    void addFavorite(Long userId, Long songId);

    void removeFavorite(Long userId, Long songId);

    Object historyList(Long userId);

    void recordHistory(Long userId, Long songId);
}
