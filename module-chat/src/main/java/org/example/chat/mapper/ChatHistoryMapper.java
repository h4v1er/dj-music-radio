package org.example.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.chat.entity.ChatHistory;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
}
