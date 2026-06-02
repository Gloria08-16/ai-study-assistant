package com.study.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.ai.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
