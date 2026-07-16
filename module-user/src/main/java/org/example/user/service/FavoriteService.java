package org.example.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.user.entity.Favorite;

import java.util.List;

public interface FavoriteService extends IService<Favorite> {

    boolean addFavorite(Long userId, Long songId);

    boolean removeFavorite(Long userId, Long songId);

    List<Favorite> listByUserId(Long userId);
}
