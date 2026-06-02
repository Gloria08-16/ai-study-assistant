package com.study.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public AiChatService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * AI 对话 —— 调用 DeepSeek 大模型 API（非流式）
     */
    public String chat(String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            messages.add(userMsg);

            Map<String, Object> body = new HashMap<>();
            body.put("model", "deepseek-chat");
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

            JSONObject json = JSON.parseObject(response.getBody());
            String content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            return content != null ? content : "AI 返回内容为空";

        } catch (Exception e) {
            System.err.println("DeepSeek API 调用失败: " + e.getMessage());
            return "AI 大脑暂时走神了，请稍后再试";
        }
    }

    /**
     * AI 流式对话 —— SSE 推送，打字机效果
     */
    public void chatStream(String message, SseEmitter emitter) {
        try {
            // 构建请求体（stream=true）
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            messages.add(userMsg);

            Map<String, Object> body = new HashMap<>();
            body.put("model", "deepseek-chat");
            body.put("messages", messages);
            body.put("stream", true);

            String jsonBody = JSON.toJSONString(body);

            // 使用 HttpURLConnection 发起流式请求
            HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);

            // 写入请求体
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
            }

            // 读取流式响应
            int status = conn.getResponseCode();
            if (status != 200) {
                emitter.send(SseEmitter.event().name("error").data("API 请求失败，状态码: " + status));
                emitter.complete();
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                StringBuilder fullContent = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    if (!line.startsWith("data: ")) continue;

                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;

                    try {
                        JSONObject json = JSON.parseObject(data);
                        if (json != null && json.containsKey("choices")) {
                            var choices = json.getJSONArray("choices");
                            if (choices != null && !choices.isEmpty()) {
                                var delta = choices.getJSONObject(0).getJSONObject("delta");
                                if (delta != null && delta.containsKey("content")) {
                                    String chunk = delta.getString("content");
                                    if (chunk != null && !chunk.isEmpty()) {
                                        fullContent.append(chunk);
                                        emitter.send(SseEmitter.event()
                                                .name("chunk")
                                                .data(chunk));
                                    }
                                }
                            }
                        }
                    } catch (Exception parseEx) {
                        // 跳过无法解析的行
                    }
                }

                // 发送完成事件，附带完整内容供前端保存
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(fullContent.toString()));
                emitter.complete();
            }

        } catch (Exception e) {
            System.err.println("DeepSeek SSE 流式调用失败: " + e.getMessage());
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("AI 大脑暂时走神了，请稍后再试"));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
    }
}
