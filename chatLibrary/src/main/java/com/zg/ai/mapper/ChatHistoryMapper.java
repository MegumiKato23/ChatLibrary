package com.zg.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zg.ai.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
}
