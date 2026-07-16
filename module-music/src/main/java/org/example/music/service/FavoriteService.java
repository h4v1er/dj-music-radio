package org.example.music.service;

import org.example.music.entity.Song;
import java.util.List;

public interface FavoriteService {

    /** 收藏歌曲 */
    void addFavorite(Long userId, Long songId);

    /** 取消收藏 */
    void removeFavorite(Long userId, Long songId);

    /** 检查是否已收藏 */
    boolean isFavorited(Long userId, Long songId);

    /** 获取用户收藏列表 */
    List<Song> getFavoriteSongs(Long userId);
}
