package com.study.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.study.ai.entity.ChatMessage;
import com.study.ai.entity.ChatSession;
import com.study.ai.mapper.ChatSessionMapper;
import com.study.ai.mapper.ChatMessageMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    public SessionController(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    /**
     * 获取所有会话列表
     */
    @GetMapping
    public List<ChatSession> list() {
        return sessionMapper.selectList(null);
    }

    /**
     * 创建新会话
     */
    @PostMapping
    public ChatSession create(@RequestBody ChatSession session) {
        sessionMapper.insert(session);
        return session;
    }

    /**
     * 重命名会话
     */
    @PutMapping("/{id}")
    public String rename(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ChatSession session = sessionMapper.selectById(id);
        if (session == null) {
            return "会话不存在";
        }
        session.setTitle(body.getOrDefault("title", session.getTitle()));
        sessionMapper.updateById(session);
        return "ok";
    }

    /**
     * 删除会话（同时删除关联的消息）
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        // 删除该会话下的所有消息
        messageMapper.delete(new QueryWrapper<ChatMessage>().eq("session_id", id));
        sessionMapper.deleteById(id);
        return "ok";
    }
}
