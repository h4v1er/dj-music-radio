package org.example.user.service.impl;

import org.example.user.client.MusicClient;
import org.example.user.service.MusicLibraryService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MusicLibraryServiceImpl implements MusicLibraryService {

    private final MusicClient musicClient;

    public MusicLibraryServiceImpl(MusicClient musicClient) {
        this.musicClient = musicClient;
    }

    @Override
    public Object favoriteList(Long userId) {
        return dataOrThrow(musicClient.favoriteList(userId));
    }

    @Override
    public void addFavorite(Long userId, Long songId) {
        dataOrThrow(musicClient.addFavorite(songId, userId));
    }

    @Override
    public void removeFavorite(Long userId, Long songId) {
        dataOrThrow(musicClient.removeFavorite(songId, userId));
    }

    @Override
    public Object historyList(Long userId) {
        return dataOrThrow(musicClient.historyList(userId));
    }

    @Override
    public void recordHistory(Long userId, Long songId) {
        dataOrThrow(musicClient.recordHistory(Map.of("userId", userId, "songId", songId)));
    }

    private Object dataOrThrow(Map<String, Object> response) {
        if (response == null) {
            throw new RuntimeException("音乐服务无响应");
        }
        Object code = response.get("code");
        if (code instanceof Number number && number.intValue() == 200) {
            return response.get("data");
        }
        throw new RuntimeException(String.valueOf(response.getOrDefault("message", "音乐服务调用失败")));
    }
}
