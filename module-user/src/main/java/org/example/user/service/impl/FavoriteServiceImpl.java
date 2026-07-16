package org.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.user.entity.Favorite;
import org.example.user.mapper.FavoriteMapper;
import org.example.user.service.FavoriteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite>
        implements FavoriteService {

    @Override
    public boolean addFavorite(Long userId, Long songId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getSongId, songId);
        if (getOne(wrapper) != null) {
            return true;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setSongId(songId);
        return save(favorite);
    }

    @Override
    public boolean removeFavorite(Long userId, Long songId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getSongId, songId);
        return remove(wrapper);
    }

    @Override
    public List<Favorite> listByUserId(Long userId) {
        return list(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime));
    }
}
