package org.example.rec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.rec.entity.UserBehavior;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    /**
     * 基于用户行为的协同过滤：找到与指定歌曲"口味相似"的其他歌曲
     * 逻辑：先找听过/收藏过这首歌的所有用户 → 再查这些用户还听过什么 → 按热度排序
     */
    @Select("SELECT song_id, COUNT(*) AS cnt " +
            "FROM user_behavior " +
            "WHERE user_id IN (" +
            "  SELECT DISTINCT user_id FROM user_behavior " +
            "  WHERE song_id = #{songId} AND action IN ('play','like')" +
            ") " +
            "AND song_id != #{songId} " +
            "AND action IN ('play','like') " +
            "GROUP BY song_id " +
            "ORDER BY cnt DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> findSimilarByBehavior(@Param("songId") Integer songId,
                                                     @Param("limit") int limit);
}
