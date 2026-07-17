package org.example.rec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.rec.client.MusicFeignClient;
import org.example.rec.dto.ResultDTO;
import org.example.rec.dto.SongDTO;
import org.example.rec.entity.DailyRecommend;
import org.example.rec.entity.UserBehavior;
import org.example.rec.mapper.DailyRecommendMapper;
import org.example.rec.mapper.UserBehaviorMapper;
import org.example.rec.service.RecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecServiceImpl extends ServiceImpl<DailyRecommendMapper, DailyRecommend> implements RecService {

    private static final String REDIS_HOT_KEY = "song:hot:rank";
    private static final int MAX_HISTORY = 50;

    @Autowired
    private UserBehaviorMapper behaviorMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired(required = false)
    private MusicFeignClient musicFeignClient;
    private final RestTemplate restTemplate = new RestTemplate();

    // ============ 热门榜单 ============
    @Override
    public List<Map<String, Object>> getHotRanking(int topN) {
        Set<ZSetOperations.TypedTuple<String>> set =
                redisTemplate.opsForZSet().reverseRangeWithScores(REDIS_HOT_KEY, 0, topN - 1);

        if (set == null || set.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : set) {
            Integer songId = Integer.valueOf(tuple.getValue());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("songId", songId);
            item.put("score", tuple.getScore() != null ? tuple.getScore().longValue() : 0);
            // 通过 Feign 补全歌曲名称和歌手
            enrichSongInfo(item, songId);
            result.add(item);
        }
        return result;
    }

    // ============ 今日推荐 ============
    @Override
    public List<Map<String, Object>> getDailyRecommend(Integer userId) {
        LambdaQueryWrapper<DailyRecommend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyRecommend::getUserId, userId)
               .eq(DailyRecommend::getPushDate, LocalDate.now())
               .orderByAsc(DailyRecommend::getId)
               .last("LIMIT 10");

        List<DailyRecommend> list = baseMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DailyRecommend dr : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("songId", dr.getSongId());
            item.put("reason", dr.getReason());
            enrichSongInfo(item, dr.getSongId());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> refreshDailyRecommend(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> topSongs = getUserTopSongs(userId, 8);
        if (topSongs.isEmpty()) {
            deleteTodayRecommendations(userId);
            return Collections.emptyList();
        }

        Set<Integer> listenedSongIds = topSongs.stream()
                .map(row -> toInteger(row.get("song_id")))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Integer> recommendedSongIds = new LinkedHashSet<>();
        List<DailyRecommend> generated = new ArrayList<>();

        for (Map<String, Object> row : topSongs) {
            Integer sourceSongId = toInteger(row.get("song_id"));
            if (sourceSongId == null) {
                continue;
            }

            SongDTO sourceSong = findSongById(sourceSongId);
            if (sourceSong != null) {
                appendDailyRecommendations(
                        userId,
                        generated,
                        recommendedSongIds,
                        listenedSongIds,
                        searchByEmotionTags(sourceSongId, sourceSong.getEmotionTags())
                );
            }

            if (generated.size() < 10 && sourceSong != null && isUsefulGenre(sourceSong.getGenre())) {
                appendDailyRecommendations(
                        userId,
                        generated,
                        recommendedSongIds,
                        listenedSongIds,
                        searchByGenre(sourceSongId, sourceSong.getGenre())
                );
            }

            if (generated.size() < 10 && sourceSong != null && sourceSong.getArtist() != null && !sourceSong.getArtist().isBlank()) {
                appendDailyRecommendations(
                        userId,
                        generated,
                        recommendedSongIds,
                        listenedSongIds,
                        searchByArtist(sourceSongId, sourceSong.getArtist())
                );
            }

            if (generated.size() < 10) {
                appendDailyRecommendations(
                        userId,
                        generated,
                        recommendedSongIds,
                        listenedSongIds,
                        getBehaviorBasedSimilar(sourceSongId)
                );
            }

            if (generated.size() < 10 && sourceSong != null && sourceSong.getAlbum() != null && !sourceSong.getAlbum().isBlank()) {
                appendDailyRecommendations(
                        userId,
                        generated,
                        recommendedSongIds,
                        listenedSongIds,
                        searchByAlbum(sourceSongId, sourceSong.getAlbum())
                );
            }

            if (generated.size() < 10 && sourceSong != null && sourceSong.getDuration() != null && sourceSong.getDuration() > 0) {
                appendDailyRecommendations(
                        userId,
                        generated,
                        recommendedSongIds,
                        listenedSongIds,
                        searchByDuration(sourceSongId, sourceSong.getDuration())
                );
            }

            if (generated.size() >= 10) {
                break;
            }
        }

        deleteTodayRecommendations(userId);
        for (DailyRecommend rec : generated) {
            baseMapper.insert(rec);
        }
        return getDailyRecommend(userId);
    }

    private void appendDailyRecommendations(Integer userId,
                                            List<DailyRecommend> target,
                                            Set<Integer> recommendedSongIds,
                                            Set<Integer> listenedSongIds,
                                            List<Map<String, Object>> candidates) {
        for (Map<String, Object> candidate : candidates) {
            Integer songId = toInteger(candidate.get("songId"));
            if (songId == null || listenedSongIds.contains(songId) || !recommendedSongIds.add(songId)) {
                continue;
            }
            DailyRecommend rec = new DailyRecommend();
            rec.setUserId(userId);
            rec.setSongId(songId);
            rec.setReason(stringValue(candidate.get("reason")));
            rec.setPushDate(LocalDate.now());
            target.add(rec);
            if (target.size() >= 10) {
                return;
            }
        }
    }

    private void deleteTodayRecommendations(Integer userId) {
        LambdaQueryWrapper<DailyRecommend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyRecommend::getUserId, userId)
               .eq(DailyRecommend::getPushDate, LocalDate.now());
        baseMapper.delete(wrapper);
    }

    /**
     * 通过 Feign 补全歌曲的标题和歌手信息
     * 若音乐服务不可用，至少保留 songId，前端可降级展示
     */
    private void enrichSongInfo(Map<String, Object> item, Integer songId) {
        SongDTO song = findSongById(songId);
        if (song != null) {
            item.put("title", song.getTitle());
            item.put("artist", song.getArtist());
            return;
        }
        item.putIfAbsent("title", "歌曲#" + songId);
        item.putIfAbsent("artist", "未知歌手");
    }

    // ============ 用户偏好 ============
    @Override
    public List<String> getUserPreferences(Integer userId) {
        // 1. 获取用户播放/收藏最多的歌曲
        List<Map<String, Object>> topSongs = getUserTopSongs(userId, 20);
        if (topSongs.isEmpty()) {
            return List.of("摇滚", "电子", "流行"); // 新用户，给默认标签
        }

        // 2. 综合统计情绪标签和有效流派。当前 genre 可能只是“网易云”，情绪标签更能表达偏好。
        Map<String, Integer> tagCount = new LinkedHashMap<>();
        for (Map<String, Object> row : topSongs) {
            Integer songId = toInteger(row.get("song_id"));
            SongDTO song = findSongById(songId);
            if (song == null) {
                continue;
            }
            for (String tag : splitTags(song.getEmotionTags())) {
                tagCount.merge(tag, 2, Integer::sum);
            }
            if (isUsefulGenre(song.getGenre())) {
                tagCount.merge(song.getGenre(), 1, Integer::sum);
            }
        }

        // 3. 按频次排序，取前 5 个
        if (tagCount.isEmpty()) {
            return List.of("摇滚", "电子", "流行");
        }
        return tagCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** 获取用户播放最多的歌曲（供偏好分析和定时任务复用） */
    private List<Map<String, Object>> getUserTopSongs(Integer userId, int limit) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserBehavior> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.select("song_id, COUNT(*) AS cnt")
               .eq("user_id", userId)
               .in("action", "play", "like")
               .groupBy("song_id")
               .orderByDesc("cnt")
               .last("LIMIT " + limit);
        return behaviorMapper.selectMaps(wrapper);
    }

    // ============ 相似歌曲 ============
    @Override
    public List<Map<String, Object>> getSimilarSongs(Integer songId) {

        // 第一步: 尝试通过音乐服务获取歌曲画像信息
        SongDTO sourceSong = findSongById(songId);
        if (sourceSong == null) {
            return getBehaviorBasedSimilar(songId);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        seen.add(songId);

        appendSimilar(result, seen, searchByEmotionTags(songId, sourceSong.getEmotionTags()));
        if (result.size() < 10 && isUsefulGenre(sourceSong.getGenre())) {
            appendSimilar(result, seen, searchByGenre(songId, sourceSong.getGenre()));
        }
        if (result.size() < 10 && sourceSong.getArtist() != null && !sourceSong.getArtist().isBlank()) {
            appendSimilar(result, seen, searchByArtist(songId, sourceSong.getArtist()));
        }
        if (result.size() < 10 && sourceSong.getAlbum() != null && !sourceSong.getAlbum().isBlank()) {
            appendSimilar(result, seen, searchByAlbum(songId, sourceSong.getAlbum()));
        }
        if (result.size() < 10) {
            appendSimilar(result, seen, getBehaviorBasedSimilar(songId));
        }
        if (result.size() < 10 && sourceSong.getDuration() != null && sourceSong.getDuration() > 0) {
            appendSimilar(result, seen, searchByDuration(songId, sourceSong.getDuration()));
        }
        return result;
    }

    private void appendSimilar(List<Map<String, Object>> target,
                               Set<Integer> seen,
                               List<Map<String, Object>> candidates) {
        for (Map<String, Object> candidate : candidates) {
            Integer songId = toInteger(candidate.get("songId"));
            if (songId == null || !seen.add(songId)) {
                continue;
            }
            target.add(candidate);
            if (target.size() >= 10) {
                return;
            }
        }
    }

    /** 按流派搜索同风格歌曲 */
    private List<Map<String, Object>> searchByGenre(Integer songId, String genre) {
        try {
            List<SongDTO> songs = findSongsByKeyword(genre);
            if (!songs.isEmpty()) {
                return songs.stream()
                        .filter(s -> !s.getId().equals(songId))   // 排除自己
                        .limit(10)
                        .map(s -> {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("songId", s.getId());
                            item.put("title", s.getTitle());
                            item.put("artist", s.getArtist());
                            item.put("reason", "同流派 · " + genre);
                            return item;
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) { }
        return Collections.emptyList();
    }

    /** 按歌手搜索同风格歌曲 */
    private List<Map<String, Object>> searchByArtist(Integer songId, String artist) {
        try {
            List<SongDTO> songs = findSongsByKeyword(artist);
            if (!songs.isEmpty()) {
                return songs.stream()
                        .filter(s -> !s.getId().equals(songId))
                        .limit(10)
                        .map(s -> {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("songId", s.getId());
                            item.put("title", s.getTitle());
                            item.put("artist", s.getArtist());
                            item.put("reason", "同歌手 · " + artist);
                            return item;
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) { }
        return Collections.emptyList();
    }

    /** 按情绪标签搜索相近歌曲 */
    private List<Map<String, Object>> searchByEmotionTags(Integer songId, String emotionTags) {
        List<String> tags = splitTags(emotionTags);
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }
        return findAllSongs().stream()
                .filter(s -> !Objects.equals(s.getId(), songId))
                .map(s -> {
                    int score = tagOverlap(tags, splitTags(s.getEmotionTags()));
                    if (score <= 0) {
                        return null;
                    }
                    Map<String, Object> item = basicSongItem(s);
                    item.put("score", score);
                    item.put("reason", "同情绪 · " + firstSharedTag(tags, splitTags(s.getEmotionTags())));
                    return item;
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    int scoreCompare = Integer.compare(toInteger(b.get("score")), toInteger(a.get("score")));
                    if (scoreCompare != 0) return scoreCompare;
                    return Integer.compare(toInteger(b.get("playCount")), toInteger(a.get("playCount")));
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    /** 按专辑搜索同一作品集内的歌曲 */
    private List<Map<String, Object>> searchByAlbum(Integer songId, String album) {
        if (album == null || album.isBlank()) {
            return Collections.emptyList();
        }
        return findAllSongs().stream()
                .filter(s -> !Objects.equals(s.getId(), songId))
                .filter(s -> album.equalsIgnoreCase(stringValue(s.getAlbum())))
                .sorted(Comparator.comparing(SongDTO::getPlayCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(s -> {
                    Map<String, Object> item = basicSongItem(s);
                    item.put("reason", "同专辑 · " + album);
                    return item;
                })
                .collect(Collectors.toList());
    }

    /** 按时长接近度补充歌曲，避免数据稀疏时推荐为空 */
    private List<Map<String, Object>> searchByDuration(Integer songId, Integer duration) {
        if (duration == null || duration <= 0) {
            return Collections.emptyList();
        }
        return findAllSongs().stream()
                .filter(s -> !Objects.equals(s.getId(), songId))
                .filter(s -> s.getDuration() != null && s.getDuration() > 0)
                .sorted(Comparator
                        .comparingInt((SongDTO s) -> Math.abs(s.getDuration() - duration))
                        .thenComparing(SongDTO::getPlayCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(s -> {
                    Map<String, Object> item = basicSongItem(s);
                    item.put("reason", "相近时长");
                    return item;
                })
                .collect(Collectors.toList());
    }

    /** 回退方案: 基于用户行为的协同过滤 — 和你有相似口味的人还喜欢… */
    private List<Map<String, Object>> getBehaviorBasedSimilar(Integer songId) {
        List<Map<String, Object>> rows = behaviorMapper.findSimilarByBehavior(songId, 10);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            Integer recommendedSongId = toInteger(row.get("song_id"));
            item.put("songId", recommendedSongId);
            item.put("reason", "和你口味相似的人也喜欢这首歌");
            enrichSongInfo(item, recommendedSongId);
            result.add(item);
        }
        return result;
    }

    // ============ 用户行为上报 ============
    @Override
    public void reportBehavior(UserBehavior behavior) {
        // 1. 写数据库
        behavior.setCreateTime(LocalDateTime.now());
        behaviorMapper.insert(behavior);

        // 2. 更新 Redis 热门榜单：播放 +1 / 收藏 +3 / 跳过 +0 / 分享 +2
        int scoreIncr = switch (behavior.getAction()) {
            case "like" -> 3;
            case "share" -> 2;
            case "play" -> 1;
            default -> 0;
        };
        if (scoreIncr > 0) {
            String songIdStr = String.valueOf(behavior.getSongId());
            redisTemplate.opsForZSet().incrementScore(REDIS_HOT_KEY, songIdStr, scoreIncr);
        }
    }

    private SongDTO findSongById(Integer songId) {
        if (songId == null) {
            return null;
        }
        if (musicFeignClient != null) {
            try {
                ResultDTO<SongDTO> r = musicFeignClient.getSongById(songId);
                if (r != null && r.isSuccess() && r.getData() != null) {
                    return r.getData();
                }
            } catch (Exception ignored) { }
        }
        try {
            Map<?, ?> response = restTemplate.getForObject(
                    "http://127.0.0.1:8082/music/song/{id}",
                    Map.class,
                    songId
            );
            Object data = response == null ? null : response.get("data");
            if (data instanceof Map<?, ?> map) {
                return toSongDTO(map);
            }
        } catch (Exception ignored) { }
        return null;
    }

    private List<SongDTO> findSongsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        if (musicFeignClient != null) {
            try {
                ResultDTO<List<SongDTO>> r = musicFeignClient.searchSongs(keyword);
                if (r != null && r.isSuccess() && r.getData() != null) {
                    return r.getData();
                }
            } catch (Exception ignored) { }
        }
        try {
            Map<?, ?> response = restTemplate.getForObject(
                    "http://127.0.0.1:8082/music/song/search?kw={keyword}&page=1&size=20",
                    Map.class,
                    keyword
            );
            Object data = response == null ? null : response.get("data");
            if (data instanceof Map<?, ?> page) {
                Object records = page.get("records");
                if (records instanceof List<?> list) {
                    return list.stream()
                            .filter(Map.class::isInstance)
                            .map(item -> toSongDTO((Map<?, ?>) item))
                            .toList();
                }
            }
        } catch (Exception ignored) { }
        return Collections.emptyList();
    }

    private List<SongDTO> findAllSongs() {
        try {
            Map<?, ?> response = restTemplate.getForObject(
                    "http://127.0.0.1:8082/music/song/list?page=1&size=300",
                    Map.class
            );
            Object data = response == null ? null : response.get("data");
            if (data instanceof Map<?, ?> page) {
                Object records = page.get("records");
                if (records instanceof List<?> list) {
                    return list.stream()
                            .filter(Map.class::isInstance)
                            .map(item -> toSongDTO((Map<?, ?>) item))
                            .toList();
                }
            }
        } catch (Exception ignored) { }
        return Collections.emptyList();
    }

    private Map<String, Object> basicSongItem(SongDTO song) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("songId", song.getId());
        item.put("title", song.getTitle());
        item.put("artist", song.getArtist());
        item.put("playCount", song.getPlayCount() == null ? 0 : song.getPlayCount());
        return item;
    }

    private List<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split("[,，|/、\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private int tagOverlap(List<String> sourceTags, List<String> candidateTags) {
        if (sourceTags.isEmpty() || candidateTags.isEmpty()) {
            return 0;
        }
        Set<String> candidateSet = new HashSet<>(candidateTags);
        int count = 0;
        for (String tag : sourceTags) {
            if (candidateSet.contains(tag)) {
                count++;
            }
        }
        return count;
    }

    private String firstSharedTag(List<String> sourceTags, List<String> candidateTags) {
        Set<String> candidateSet = new HashSet<>(candidateTags);
        for (String tag : sourceTags) {
            if (candidateSet.contains(tag)) {
                return tag;
            }
        }
        return sourceTags.isEmpty() ? "相近" : sourceTags.get(0);
    }

    private boolean isUsefulGenre(String genre) {
        if (genre == null || genre.isBlank()) {
            return false;
        }
        String value = genre.trim();
        return !"网易云".equals(value) && !"NETEASE".equalsIgnoreCase(value);
    }

    private SongDTO toSongDTO(Map<?, ?> map) {
        SongDTO dto = new SongDTO();
        dto.setId(toInteger(map.get("id")));
        dto.setTitle(stringValue(map.get("title")));
        dto.setArtist(stringValue(map.get("artist")));
        dto.setAlbum(stringValue(map.get("album")));
        dto.setCoverUrl(stringValue(map.get("coverUrl")));
        dto.setGenre(stringValue(map.get("genre")));
        dto.setDuration(toInteger(map.get("duration")));
        dto.setEmotionTags(stringValue(map.get("emotionTags")));
        dto.setPlayCount(toInteger(map.get("playCount")));
        return dto;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
