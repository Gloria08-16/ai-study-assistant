package com.study.ai.controller;

import com.study.ai.service.AiChatService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        String reply = aiChatService.chat(message);
        return Map.of("reply", reply);
    }
}
