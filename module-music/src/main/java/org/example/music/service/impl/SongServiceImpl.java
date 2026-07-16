package org.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.music.entity.Song;
import org.example.music.mapper.SongMapper;
import org.example.music.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SongServiceImpl implements SongService {

    @Autowired
    private SongMapper songMapper;

    @Override
    public Page<Song> getSongList(int page, int size, String genre) {
        Page<Song> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        if (genre != null && !genre.isEmpty()) {
            wrapper.eq(Song::getGenre, genre);
        }
        wrapper.orderByDesc(Song::getPlayCount);
        return songMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Song getSongById(Long id) {
        return songMapper.selectById(id);
    }

    @Override
    public Page<Song> searchSongs(String kw, int page, int size) {
        Page<Song> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        if (kw != null && !kw.isEmpty()) {
            wrapper.and(w -> w
                    .like(Song::getTitle, kw)
                    .or()
                    .like(Song::getArtist, kw)
                    .or()
                    .like(Song::getAlbum, kw)
            );
        }
        wrapper.orderByDesc(Song::getPlayCount);
        return songMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<String> getAllGenres() {
        List<Song> songs = songMapper.selectList(null);
        return songs.stream()
                .map(Song::getGenre)
                .filter(g -> g != null && !g.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public void incrementPlayCount(Long id) {
        Song song = songMapper.selectById(id);
        if (song != null) {
            song.setPlayCount(song.getPlayCount() == null ? 1 : song.getPlayCount() + 1);
            songMapper.updateById(song);
        }
    }
}
