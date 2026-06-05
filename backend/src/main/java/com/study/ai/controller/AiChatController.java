package com.study.ai.controller;

import com.study.ai.service.AiChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
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
    @SuppressWarnings("unchecked")
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.getOrDefault("message", "");
        boolean useKnowledge = Boolean.TRUE.equals(request.getOrDefault("useKnowledge", false));
        // 接收对话历史上下文（前端传递的最近消息列表）
        List<Map<String, String>> history = (List<Map<String, String>>) request.get("messages");
        // 接收 AI 参数（温度、最大 token 数、模型选择）
        Double temperature = request.containsKey("temperature")
                ? ((Number) request.get("temperature")).doubleValue() : 0.7;
        Integer maxTokens = request.containsKey("maxTokens")
                ? ((Number) request.get("maxTokens")).intValue() : 2048;
        String model = (String) request.getOrDefault("model", "deepseek-chat");
        String reply = aiChatService.chat(message, useKnowledge, history, temperature, maxTokens, model);
        return Map.of("reply", reply);
    }

    /**
     * SSE 流式 AI 对话 —— 打字机效果
     */
    @SuppressWarnings("unchecked")
    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> request) {
        String message = (String) request.getOrDefault("message", "");
        boolean useKnowledge = Boolean.TRUE.equals(request.getOrDefault("useKnowledge", false));
        // 接收对话历史上下文（前端传递的最近消息列表）
        List<Map<String, String>> history = (List<Map<String, String>>) request.get("messages");
        // 接收 AI 参数（温度、最大 token 数、模型选择）
        Double temperature = request.containsKey("temperature")
                ? ((Number) request.get("temperature")).doubleValue() : 0.7;
        Integer maxTokens = request.containsKey("maxTokens")
                ? ((Number) request.get("maxTokens")).intValue() : 2048;
        String model = (String) request.getOrDefault("model", "deepseek-chat");
        SseEmitter emitter = new SseEmitter(120000L); // 2分钟超时

        // 在独立线程中执行流式调用
        new Thread(() -> aiChatService.chatStream(message, useKnowledge, emitter, history,
                temperature, maxTokens, model)).start();

        return emitter;
    }
}
