package com.study.ai.controller;

import com.study.ai.service.AiChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * 非流式 AI 对话
     */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.getOrDefault("message", "");
        boolean useKnowledge = Boolean.TRUE.equals(request.getOrDefault("useKnowledge", false));
        String reply = aiChatService.chat(message, useKnowledge);
        return Map.of("reply", reply);
    }

    /**
     * SSE 流式 AI 对话 —— 打字机效果
     */
    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> request) {
        String message = (String) request.getOrDefault("message", "");
        boolean useKnowledge = Boolean.TRUE.equals(request.getOrDefault("useKnowledge", false));
        SseEmitter emitter = new SseEmitter(120000L); // 2分钟超时

        // 在独立线程中执行流式调用
        new Thread(() -> aiChatService.chatStream(message, useKnowledge, emitter)).start();

        return emitter;
    }
}
