package org.example.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.mall.entity.Category;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
