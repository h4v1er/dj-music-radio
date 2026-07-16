package org.example.music.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.music.entity.Playlist;
import org.example.music.entity.PlaylistSong;
import org.example.music.entity.Song;
import org.example.music.mapper.PlaylistMapper;
import org.example.music.mapper.PlaylistSongMapper;
import org.example.music.mapper.SongMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                    song.setLyric(null);  // 网易云歌曲歌词在线获取
                    songMapper.insert(song);
                }

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

        } catch (Exception e) {
            log.error("歌单导入失败: taskId={}", taskId, e);
        }
    }
}
