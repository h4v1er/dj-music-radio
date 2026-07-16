package org.example.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.mall.entity.Item;

@Mapper
public interface ItemMapper extends BaseMapper<Item> {
}
