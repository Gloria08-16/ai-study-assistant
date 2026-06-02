package com.study.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.study.ai.entity.ChatMessage;
import com.study.ai.mapper.ChatMessageMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ChatMessageMapper messageMapper;

    public MessageController(ChatMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * 获取某个会话的所有消息
     */
    @GetMapping("/{sessionId}")
    public List<ChatMessage> getBySession(@PathVariable Long sessionId) {
        return messageMapper.selectList(
                new QueryWrapper<ChatMessage>().eq("session_id", sessionId)
                        .orderByAsc("create_time"));
    }

    /**
     * 保存一条消息
     */
    @PostMapping
    public ChatMessage save(@RequestBody ChatMessage message) {
        messageMapper.insert(message);
        return message;
    }
}
