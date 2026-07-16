package org.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.music.entity.Song;
import org.example.music.entity.UserFavorite;
import org.example.music.mapper.SongMapper;
import org.example.music.mapper.UserFavoriteMapper;
import org.example.music.service.FavoriteService;
import org.example.music.service.UserTasteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private UserFavoriteMapper favoriteMapper;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private UserTasteService userTasteService;

    @Override
    @Transactional
    public void addFavorite(Long userId, Long songId) {
        // 检查是否已收藏
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
               .eq(UserFavorite::getSongId, songId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            return; // 已收藏
        }
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setSongId(songId);
        favoriteMapper.insert(favorite);

        // 品味更新
        try { userTasteService.onFavorite(userId, songId, true); } catch (Exception ignored) {}
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long songId) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
               .eq(UserFavorite::getSongId, songId);
        favoriteMapper.delete(wrapper);

        // 品味更新
        try { userTasteService.onFavorite(userId, songId, false); } catch (Exception ignored) {}
    }

    @Override
    public boolean isFavorited(Long userId, Long songId) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
               .eq(UserFavorite::getSongId, songId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<Song> getFavoriteSongs(Long userId) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
               .orderByDesc(UserFavorite::getCreatedAt);

        List<UserFavorite> favorites = favoriteMapper.selectList(wrapper);
        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> songIds = favorites.stream()
                .map(UserFavorite::getSongId)
                .collect(Collectors.toList());

        return songMapper.selectBatchIds(songIds);
    }
}
