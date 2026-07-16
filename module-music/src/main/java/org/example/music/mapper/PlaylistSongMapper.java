package org.example.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.music.entity.PlaylistSong;
import java.util.List;

@Mapper
public interface PlaylistSongMapper extends BaseMapper<PlaylistSong> {

    /** 批量插入歌曲到歌单 */
    int batchInsert(@Param("list") List<PlaylistSong> list);

    /** 获取歌单中最大的排序号 */
    Integer selectMaxSortOrder(@Param("playlistId") Long playlistId);
}
