package org.example.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.music.entity.Playlist;

@Mapper
public interface PlaylistMapper extends BaseMapper<Playlist> {
}
