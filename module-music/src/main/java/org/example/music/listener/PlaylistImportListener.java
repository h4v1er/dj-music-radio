package org.example.music.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.music.entity.Playlist;
import org.example.music.entity.PlaylistSong;
import org.example.music.entity.Song;
import org.example.music.mapper.PlaylistMapper;
import org.example.music.mapper.PlaylistSongMapper;
import org.example.music.mapper.SongMapper;
import org.example.music.service.EmotionAnalysisService;
import org.example.music.service.UserTasteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.example.music.config.RabbitMQConfig.QUEUE_IMPORT;

@Component
public class PlaylistImportListener {

    private static final Logger log = LoggerFactory.getLogger(PlaylistImportListener.class);

    @Autowired
    private PlaylistMapper playlistMapper;

    @Autowired
    private PlaylistSongMapper playlistSongMapper;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private EmotionAnalysisService emotionAnalysisService;

    @Autowired
    private UserTasteService userTasteService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String NETEASE_API = "http://localhost:3000";
    private final org.springframework.web.client.RestTemplate rest = new org.springframework.web.client.RestTemplate();

    @RabbitListener(queues = QUEUE_IMPORT)
    @Transactional
    public void handleImport(Map<String, Object> message) {
        String taskId = (String) message.get("taskId");
        String name = (String) message.get("name");
        String content = (String) message.get("content");
        Long userId = message.get("userId") != null
                ? ((Number) message.get("userId")).longValue()
                : 1L;

        log.info("开始处理歌单导入任务: taskId={}, name={}", taskId, name);

        try {
            // 解析导入内容（JSON格式的歌曲列表）
            List<Map<String, Object>> songList = objectMapper.readValue(
                    content, new TypeReference<List<Map<String, Object>>>() {});

            // 创建歌单
            Playlist playlist = new Playlist();
            playlist.setName(name);
            playlist.setDescription("导入的歌单");
            playlist.setUserId(userId);
            playlist.setSongCount(0);
            playlist.setCoverUrl("");
            playlistMapper.insert(playlist);

            // 收集导入的歌曲，用于后续情绪分析
            List<Long> importedSongIds = new ArrayList<>();

            // 处理歌曲
            int sortOrder = 1;
            for (Map<String, Object> item : songList) {
                String title = (String) item.getOrDefault("title", "未知歌曲");
                String artist = (String) item.getOrDefault("artist", "未知歌手");

                // 先查数据库中是否已有同名同歌手歌曲
                Song song = songMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Song>()
                                .eq(Song::getTitle, title)
                                .eq(Song::getArtist, artist)
                                .last("LIMIT 1")
                );

                // 没有则创建新歌曲
                if (song == null) {
                    song = new Song();
                    song.setTitle(title);
                    song.setArtist(artist);
                    song.setAlbum((String) item.getOrDefault("album", ""));
                    song.setGenre((String) item.getOrDefault("genre", "网易云"));
                    song.setDuration(item.get("duration") != null
                            ? ((Number) item.get("duration")).intValue() : 240);
                    // 从导入数据读取真实字段
                    song.setSource((String) item.getOrDefault("source", "NETEASE"));
                    song.setSourceId((String) item.getOrDefault("sourceId", ""));
                    song.setCoverUrl((String) item.getOrDefault("coverUrl", ""));
                    song.setPlayCount(0);

                    // 尝试从网易云 API 获取歌词
                    String lyric = fetchLyric(song.getSourceId());
                    song.setLyric(lyric);

                    songMapper.insert(song);
                } else if (song.getLyric() == null || song.getLyric().isEmpty()) {
                    // 已有歌曲但缺少歌词，补充获取
                    String lyric = fetchLyric(song.getSourceId());
                    if (lyric != null && !lyric.isEmpty()) {
                        song.setLyric(lyric);
                        songMapper.updateById(song);
                    }
                }

                importedSongIds.add(song.getId());

                // 添加到歌单
                PlaylistSong ps = new PlaylistSong();
                ps.setPlaylistId(playlist.getId());
                ps.setSongId(song.getId());
                ps.setSortOrder(sortOrder++);
                playlistSongMapper.insert(ps);
            }

            // 更新歌单歌曲数量
            playlist.setSongCount(sortOrder - 1);
            playlistMapper.updateById(playlist);

            log.info("歌单导入完成: taskId={}, playlistId={}, songCount={}",
                    taskId, playlist.getId(), sortOrder - 1);

            // ── 情绪分析集成 ──
            try {
                emotionAnalysisService.analyzePlaylistAsync(playlist.getId());
                log.info("情绪分析任务已提交: playlistId={}", playlist.getId());
            } catch (Exception e) {
                log.warn("提交情绪分析任务失败(非致命): playlistId={}", playlist.getId(), e);
            }
            // ── 品味更新集成 ──
            try {
                userTasteService.updateFromPlaylist(userId, playlist.getId());
                log.info("用户品味已更新: userId={}, playlistId={}", userId, playlist.getId());
            } catch (Exception e) {
                log.warn("更新用户品味失败(非致命): userId={}", userId, e);
            }

        } catch (Exception e) {
            log.error("歌单导入失败: taskId={}", taskId, e);
        }
    }

    /**
     * 从网易云 API 获取单首歌曲的歌词
     * @param sourceId 网易云歌曲 ID
     * @return 歌词文本（LRC 格式），失败返回 null
     */
    private String fetchLyric(String sourceId) {
        if (sourceId == null || sourceId.isEmpty()) return null;
        try {
            String url = NETEASE_API + "/lyric?id=" + sourceId;
            Map resp = rest.getForObject(url, Map.class);
            if (resp != null) {
                // 返回结构: { lrc: { lyric: "..." }, tlyric: { lyric: "..." } }
                Map lrc = (Map) resp.get("lrc");
                if (lrc != null && lrc.get("lyric") != null) {
                    String lyric = (String) lrc.get("lyric");
                    if (lyric != null && !lyric.isEmpty()) {
                        log.debug("歌词获取成功: sourceId={}, length={}", sourceId, lyric.length());
                        return lyric;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取歌词失败: sourceId={}, error={}", sourceId, e.getMessage());
        }
        return null;
    }
}
