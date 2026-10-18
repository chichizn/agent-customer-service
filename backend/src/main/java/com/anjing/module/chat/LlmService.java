package com.anjing.module.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * LLM 服务 - 接入 OneRouter 统一网关
 * 支持 OpenAI 兼容的 Chat Completions API
 */
@Slf4j
@Service
public class LlmService {

    @Value("${llm.api-url:https://api.onerouter.top/v1/chat/completions}")
    private String apiUrl;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    @Value("${llm.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 客服系统提示词
     */
    private static final String SYSTEM_PROMPT = """
            你是一个专业的电商智能客服助手。请遵循以下规则：
            1. 用友好、专业的语气回答用户问题
            2. 回答要简洁明了，重点突出
            3. 如果涉及退换货、物流等问题，给出具体的操作步骤
            4. 如果涉及优惠活动，主动推荐当前可用的优惠
            5. 如果无法确定答案，诚实告知并建议用户联系人工客服
            6. 使用中文回答
            """;

    /**
     * 生成 AI 回复
     * 
     * @param userMessage 用户消息
     * @param context 上下文信息（意图、场景、知识检索结果等）
     * @return AI 回复内容
     */
    /**
     * @param chatHistory 同 session 的历史消息（role + content），传 null 则退化为单轮
     */
    public String generateReply(String userMessage, Map<String, Object> context,
                                List<Map<String, String>> chatHistory) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            log.debug("LLM 未启用，使用规则引擎回复");
            return null;
        }

        try {
            String enhancedSystemPrompt = buildEnhancedPrompt(context);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 500);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", enhancedSystemPrompt));

            if (chatHistory != null && !chatHistory.isEmpty()) {
                messages.addAll(chatHistory);
                log.info("LLM 多轮上下文: 注入{}条历史消息", chatHistory.size());
            }

            messages.add(Map.of("role", "user", "content", userMessage));
            requestBody.put("messages", messages);

            // 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody), headers);

            log.info("LLM 请求发送: url={}, model={}", apiUrl, model);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            // 打印响应头中的 request-id（用于对接排查）
            String requestId = extractRequestId(response.getHeaders());
            log.info("LLM 响应: status={}, request-id={}", response.getStatusCode(), requestId);

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && !choices.isEmpty()) {
                    String content = choices.get(0).get("message").get("content").asText();
                    log.info("LLM 回复生成成功: 长度={}, request-id={}", content.length(), requestId);
                    return content;
                }
            }

            log.warn("LLM 回复解析失败: request-id={}, body={}", requestId,
                    response.getBody() != null ? response.getBody().substring(0, Math.min(200, response.getBody().length())) : "null");
            return null;

        } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            String requestId = extractRequestId(e.getResponseHeaders());
            log.error("LLM 调用失败: url={}, status={}, request-id={}, error={}",
                    apiUrl, e.getStatusCode(), requestId, e.getResponseBodyAsString().substring(0, Math.min(200, e.getResponseBodyAsString().length())));
            return null;
        } catch (Exception e) {
            log.error("LLM 调用异常: url={}, error={}", apiUrl, e.getMessage());
            return null;
        }
    }

    /**
     * 用 LLM 一次性分析用户输入的场景、意图、情绪
     * 返回结构化 JSON，解析失败返回 null（由调用方走关键词兜底）
     */
    public Map<String, String> analyzeUserInput(String userMessage) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            return null;
        }

        try {
            String prompt = """
                    你是电商客服系统的智能分析引擎。请分析用户输入，返回纯JSON（不要markdown）。

                    分析维度：
                    1. scene：场景类型，从以下选择：售前咨询、售后服务、物流配送、支付问题、通用咨询
                    2. intentCode：意图编码，从以下选择：PRODUCT_DISCOUNT(商品优惠)、RETURN_EXCHANGE(退换货)、SIZE_CONSULT(尺码咨询)、LOGISTICS_QUERY(物流查询)、GENERAL_QUERY(通用咨询)
                    3. intentName：意图的中文名称
                    4. confidence：置信度(0-1之间的小数)
                    5. emotion：情绪，从以下选择：正面、中性、负面

                    返回格式示例：
                    {"scene":"售后服务","intentCode":"RETURN_EXCHANGE","intentName":"退换货咨询","confidence":0.92,"emotion":"负面"}
                    """;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 150);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", prompt));
            messages.add(Map.of("role", "user", "content", userMessage));
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && !choices.isEmpty()) {
                    String content = choices.get(0).get("message").get("content").asText().trim();
                    // 去除可能的 markdown 代码块包裹
                    if (content.startsWith("```")) {
                        content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                    }
                    JsonNode result = objectMapper.readTree(content);
                    Map<String, String> analysis = new HashMap<>();
                    analysis.put("scene", result.has("scene") ? result.get("scene").asText() : "通用咨询");
                    analysis.put("intentCode", result.has("intentCode") ? result.get("intentCode").asText() : "GENERAL_QUERY");
                    analysis.put("intentName", result.has("intentName") ? result.get("intentName").asText() : "通用咨询");
                    analysis.put("confidence", result.has("confidence") ? result.get("confidence").asText() : "0.75");
                    analysis.put("emotion", result.has("emotion") ? result.get("emotion").asText() : "中性");
                    log.info("LLM 分析完成: scene={}, intent={}, emotion={}, confidence={}",
                            analysis.get("scene"), analysis.get("intentName"), analysis.get("emotion"), analysis.get("confidence"));
                    return analysis;
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("LLM 分析失败，将使用关键词兜底: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从响应头提取 request-id（兼容多种常见 header 名称）
     */
    private String extractRequestId(HttpHeaders headers) {
        if (headers == null) return "N/A";
        for (String name : List.of("x-request-id", "x-req-id", "request-id", "cf-ray", "x-trace-id")) {
            String value = headers.getFirst(name);
            if (value != null) return name + "=" + value;
        }
        return "N/A (无 request-id header)";
    }

    /**
     * 构建增强提示词（包含知识检索结果和上下文）
     */
    private String buildEnhancedPrompt(Map<String, Object> context) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);

        if (context != null) {
            Object scene = context.get("sceneType");
            if (scene != null) {
                sb.append("\n当前场景：").append(scene);
            }

            Object intent = context.get("intentName");
            if (intent != null) {
                sb.append("\n用户意图：").append(intent);
            }

            Object emotion = context.get("emotion");
            if (emotion != null) {
                sb.append("\n用户情绪：").append(emotion);
                if ("负面".equals(emotion)) {
                    sb.append("\n请特别注意安抚用户情绪，表达理解和歉意。");
                }
            }

            Object knowledge = context.get("knowledge");
            if (knowledge != null) {
                sb.append("\n\n参考知识：\n").append(knowledge);
            }
        }

        return sb.toString();
    }
}
