package com.study.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.study.ai.entity.KnowledgeBase;
import com.study.ai.mapper.KnowledgeBaseMapper;
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
import java.util.*;

@Service
public class AiChatService {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public AiChatService(RestTemplate restTemplate, KnowledgeBaseMapper knowledgeBaseMapper) {
        this.restTemplate = restTemplate;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    /**
     * 构建 RAG 上下文：使用 MySQL Ngram 全文索引检索 → 拼接为系统提示
     */
    private String buildRagContext(String message) {
        // 直接将用户原始提问传给 MySQL 全文索引，由 Ngram 解析器自动分词匹配
        List<KnowledgeBase> results = knowledgeBaseMapper.searchByFullText(message);
        if (results == null || results.isEmpty()) {
            return null;
        }

        // 拼接上下文
        StringBuilder sb = new StringBuilder();
        sb.append("请参考以下我的私人知识库内容来回答用户问题：\n");
        for (KnowledgeBase kb : results) {
            sb.append("---\n");
            sb.append("【标题】").append(kb.getTitle()).append("\n");
            sb.append("【内容】").append(kb.getContent()).append("\n");
        }
        sb.append("---\n");
        sb.append("如果以上知识库内容和用户问题相关，请优先依据知识库内容进行回答；如果不相关，则正常回答。");
        return sb.toString();
    }

    /**
     * AI 对话 —— 调用 DeepSeek 大模型 API（非流式）
     */
    public String chat(String message, boolean useKnowledge) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            List<Map<String, String>> messages = new ArrayList<>();

            // RAG 上下文注入
            if (useKnowledge) {
                String ragContext = buildRagContext(message);
                if (ragContext != null) {
                    Map<String, String> sysMsg = new HashMap<>();
                    sysMsg.put("role", "system");
                    sysMsg.put("content", ragContext);
                    messages.add(sysMsg);
                }
            }

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
    public void chatStream(String message, boolean useKnowledge, SseEmitter emitter) {
        try {
            // 构建请求体（stream=true）
            List<Map<String, String>> messages = new ArrayList<>();

            // RAG 上下文注入
            if (useKnowledge) {
                String ragContext = buildRagContext(message);
                if (ragContext != null) {
                    Map<String, String> sysMsg = new HashMap<>();
                    sysMsg.put("role", "system");
                    sysMsg.put("content", ragContext);
                    messages.add(sysMsg);
                }
            }

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
