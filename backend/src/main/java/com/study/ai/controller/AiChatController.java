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
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        String reply = aiChatService.chat(message);
        return Map.of("reply", reply);
    }

    /**
     * SSE 流式 AI 对话 —— 打字机效果
     */
    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        SseEmitter emitter = new SseEmitter(120000L); // 2分钟超时

        // 在独立线程中执行流式调用
        new Thread(() -> aiChatService.chatStream(message, emitter)).start();

        return emitter;
    }
}
