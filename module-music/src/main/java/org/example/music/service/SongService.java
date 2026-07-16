package org.example.music.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.music.entity.Song;
import java.util.List;

public interface SongService {

    /** 分页获取歌曲列表 */
    Page<Song> getSongList(int page, int size, String genre);

    /** 获取歌曲详情 */
    Song getSongById(Long id);

    /** 关键词搜索歌曲 */
    Page<Song> searchSongs(String kw, int page, int size);

    /** 获取所有流派 */
    List<String> getAllGenres();

    /** 增加播放次数 */
    void incrementPlayCount(Long id);
}
