package org.example.user.service.impl;

import org.example.user.client.MusicClient;
import org.example.user.service.MusicLibraryService;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.function.Supplier;

@Service
public class MusicLibraryServiceImpl implements MusicLibraryService {

    private static final String MUSIC_BASE_URL = "http://127.0.0.1:8082";

    private final MusicClient musicClient;
    private final RestTemplate restTemplate = new RestTemplate();

    public MusicLibraryServiceImpl(MusicClient musicClient) {
        this.musicClient = musicClient;
    }

    @Override
    public Object favoriteList(Long userId) {
        return dataOrThrow(withMusicFallback(
                () -> musicClient.favoriteList(userId),
                () -> getForMap("/music/favorite/list?userId={userId}", userId)
        ));
    }

    @Override
    public void addFavorite(Long userId, Long songId) {
        dataOrThrow(withMusicFallback(
                () -> musicClient.addFavorite(songId, userId),
                () -> postForMap("/music/favorite/{songId}?userId={userId}", null, songId, userId)
        ));
    }

    @Override
    public void removeFavorite(Long userId, Long songId) {
        dataOrThrow(withMusicFallback(
                () -> musicClient.removeFavorite(songId, userId),
                () -> exchangeForMap("/music/favorite/{songId}?userId={userId}", HttpMethod.DELETE, songId, userId)
        ));
    }

    @Override
    public Object historyList(Long userId) {
        return dataOrThrow(withMusicFallback(
                () -> musicClient.historyList(userId),
                () -> getForMap("/music/history/list?userId={userId}", userId)
        ));
    }

    @Override
    public void recordHistory(Long userId, Long songId) {
        Map<String, Long> body = Map.of("userId", userId, "songId", songId);
        dataOrThrow(withMusicFallback(
                () -> musicClient.recordHistory(body),
                () -> postForMap("/music/history", body)
        ));
    }

    private Map<String, Object> withMusicFallback(Supplier<Map<String, Object>> feignCall,
                                                  Supplier<Map<String, Object>> directCall) {
        try {
            return feignCall.get();
        } catch (RuntimeException e) {
            return directCall.get();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getForMap(String path, Object... uriVariables) {
        return restTemplate.getForObject(MUSIC_BASE_URL + path, Map.class, uriVariables);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postForMap(String path, Object body, Object... uriVariables) {
        return restTemplate.postForObject(MUSIC_BASE_URL + path, body, Map.class, uriVariables);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeForMap(String path, HttpMethod method, Object... uriVariables) {
        ResponseEntity<Map> response = restTemplate.exchange(MUSIC_BASE_URL + path, method, null, Map.class, uriVariables);
        return response.getBody();
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
