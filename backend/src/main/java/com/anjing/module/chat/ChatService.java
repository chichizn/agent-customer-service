package com.anjing.module.chat;

import com.anjing.module.chat.entity.ChatMessage;
import com.anjing.module.chat.entity.ChatSession;
import com.anjing.module.chat.repository.ChatMessageRepository;
import com.anjing.module.chat.repository.ChatSessionRepository;
import com.anjing.module.knowledge.entity.*;
import com.anjing.module.knowledge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 对话中心服务 - JPA持久化版本
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final LlmService llmService;
    private final ProductRepository productRepository;
    private final ActivityRepository activityRepository;
    private final FaqRepository faqRepository;

    /**
     * 创建会话
     */
    @Transactional
    public ChatVO.SessionVO createSession(ChatDTO.CreateSessionDTO dto) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        // 持久化会话
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setUserId(dto.getUserId());
        session.setUserName(dto.getUserName());
        session.setChannel(dto.getChannel() != null ? dto.getChannel() : "web");
        session.setStatus("active");
        sessionRepository.save(session);

        // 添加欢迎消息
        ChatMessage welcomeMsg = new ChatMessage();
        welcomeMsg.setMessageId(UUID.randomUUID().toString());
        welcomeMsg.setSessionId(sessionId);
        welcomeMsg.setRole("assistant");
        welcomeMsg.setContent("您好！我是智能客服小助手，请问有什么可以帮助您的吗？");
        messageRepository.save(welcomeMsg);

        // 构建返回
        ChatVO.SessionVO vo = new ChatVO.SessionVO();
        vo.setSessionId(sessionId);
        vo.setUserId(dto.getUserId());
        vo.setUserName(dto.getUserName());
        vo.setStatus("active");
        vo.setCreatedAt(session.getCreatedAt());
        vo.setMessageCount(1);

        log.info("创建会话成功: sessionId={}", sessionId);
        return vo;
    }

    /**
     * 获取会话列表
     */
    public List<ChatVO.SessionVO> listSessions(ChatDTO.QuerySessionDTO dto) {
        List<ChatSession> sessions;

        if (dto.getUserId() != null && dto.getStatus() != null) {
            sessions = sessionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(dto.getUserId(), dto.getStatus());
        } else if (dto.getUserId() != null) {
            sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(dto.getUserId());
        } else if (dto.getStatus() != null) {
            sessions = sessionRepository.findByStatusOrderByCreatedAtDesc(dto.getStatus());
        } else {
            sessions = sessionRepository.findAllByOrderByCreatedAtDesc();
        }

        List<ChatVO.SessionVO> result = new ArrayList<>();
        for (ChatSession s : sessions) {
            ChatVO.SessionVO vo = new ChatVO.SessionVO();
            vo.setSessionId(s.getSessionId());
            vo.setUserId(s.getUserId());
            vo.setUserName(s.getUserName());
            vo.setStatus(s.getStatus());
            vo.setCreatedAt(s.getCreatedAt());
            vo.setMessageCount((int) messageRepository.countBySessionId(s.getSessionId()));

            // 获取最后一条消息
            List<ChatMessage> msgs = messageRepository.findBySessionIdOrderByCreatedAtAsc(s.getSessionId());
            if (!msgs.isEmpty()) {
                vo.setLastMessage(msgs.get(msgs.size() - 1).getContent());
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 获取会话详情
     */
    public ChatVO.SessionDetailVO getSession(String sessionId) {
        return sessionRepository.findById(sessionId).map(s -> {
            ChatVO.SessionDetailVO detail = new ChatVO.SessionDetailVO();
            detail.setSessionId(s.getSessionId());
            detail.setUserId(s.getUserId());
            detail.setUserName(s.getUserName());
            detail.setStatus(s.getStatus());
            detail.setCreatedAt(s.getCreatedAt());

            // 加载消息
            List<ChatMessage> msgs = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            List<ChatVO.MessageVO> messageVOs = new ArrayList<>();
            for (ChatMessage m : msgs) {
                ChatVO.MessageVO mv = new ChatVO.MessageVO();
                mv.setMessageId(m.getMessageId());
                mv.setSessionId(m.getSessionId());
                mv.setRole(m.getRole());
                mv.setContent(m.getContent());
                mv.setCardType(m.getCardType());
                mv.setCreatedAt(m.getCreatedAt());
                messageVOs.add(mv);
            }
            detail.setMessages(messageVOs);
            detail.setContext(new ChatVO.ContextVO());
            return detail;
        }).orElse(null);
    }

    /**
     * 删除会话
     */
    @Transactional
    public void deleteSession(String sessionId) {
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
        log.info("删除会话成功: sessionId={}", sessionId);
    }

    /**
     * 发送消息（核心链路）
     */
    @Transactional
    public ChatVO.SendMessageVO sendMessage(ChatDTO.SendMessageDTO dto) {
        String sessionId = dto.getSessionId();
        String userContent = dto.getContent();
        log.info("========== 对话链路开始 ==========");
        log.info("【步骤1/8】保存用户消息: sessionId={}, content={}", sessionId, userContent);

        // 1. 保存用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setMessageId(UUID.randomUUID().toString());
        userMessage.setSessionId(sessionId);
        userMessage.setRole("user");
        userMessage.setContent(userContent);
        messageRepository.save(userMessage);

        // 2. LLM 一次性分析（场景+意图+情绪），关键词兜底
        log.info("【步骤2/8】智能分析（场景+意图+情绪）...");
        String sceneType;
        ChatVO.IntentVO intent;
        String emotion;

        Map<String, String> llmAnalysis = llmService.analyzeUserInput(userContent);
        if (llmAnalysis != null) {
            sceneType = llmAnalysis.get("scene");
            intent = new ChatVO.IntentVO();
            intent.setIntentCode(llmAnalysis.get("intentCode"));
            intent.setIntentName(llmAnalysis.get("intentName"));
            try { intent.setConfidence(Double.parseDouble(llmAnalysis.get("confidence"))); }
            catch (Exception e) { intent.setConfidence(0.85); }
            emotion = llmAnalysis.get("emotion");
            log.info("【步骤2/8】分析引擎=LLM大模型 | 场景={} | 意图={}(置信度{}) | 情绪={}",
                    sceneType, intent.getIntentName(), intent.getConfidence(), emotion);
        } else {
            sceneType = recognizeSceneFallback(userContent);
            intent = analyzeIntentFallback(userContent);
            emotion = analyzeEmotionFallback(userContent);
            log.info("【步骤2/8】分析引擎=关键词兜底(LLM不可用) | 场景={} | 意图={}(置信度{}) | 情绪={}",
                    sceneType, intent.getIntentName(), intent.getConfidence(), emotion);
        }

        // 3. 知识检索（从数据库真实查询）
        List<Long> selectedProductIds = parseIds(dto.getExtra(), "selectedProducts");
        List<Long> selectedActivityIds = parseIds(dto.getExtra(), "selectedActivities");
        ChatVO.KnowledgeRecallVO knowledgeRecall = retrieveKnowledge(userContent, intent, selectedProductIds, selectedActivityIds);
        log.info("【步骤3/8】知识检索完成: 商品{}条, 活动{}条, FAQ{}条 | 手动选择: 商品{}个, 活动{}个",
                knowledgeRecall.getProducts().size(), knowledgeRecall.getActivities().size(), knowledgeRecall.getFaqs().size(),
                selectedProductIds.size(), selectedActivityIds.size());

        // 4. 生成回复（优先使用LLM，回退到规则引擎）
        log.info("【步骤4/8】生成回复（LLM优先，规则兜底）...");
        Map<String, Object> llmContext = new HashMap<>();
        llmContext.put("sceneType", sceneType);
        llmContext.put("intentName", intent.getIntentName());
        llmContext.put("emotion", emotion);
        llmContext.put("knowledge", buildKnowledgeText(knowledgeRecall));

        // 构建多轮对话历史（取同 session 最近 10 条，不含刚保存的本轮用户消息）
        List<ChatMessage> historyMessages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<Map<String, String>> chatHistory = new ArrayList<>();
        int historySize = historyMessages.size() - 1; // 排除刚保存的本条
        int start = Math.max(0, historySize - 10);
        for (int i = start; i < historySize; i++) {
            ChatMessage m = historyMessages.get(i);
            chatHistory.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        log.info("【步骤4/8】多轮上下文: 历史消息{}条（最近10条）", chatHistory.size());

        String llmReply = llmService.generateReply(userContent, llmContext, chatHistory);
        String aiReply = llmReply != null ? llmReply : generateReply(userContent, intent, knowledgeRecall);
        log.info("【步骤4/8】回复引擎={} | 回复长度={}字",
                llmReply != null ? "LLM大模型" : "规则引擎兜底", aiReply.length());

        // 5. 工具选择
        String toolChoice = selectTool(intent, knowledgeRecall);
        log.info("【步骤5/8】工具选择: {}", toolChoice != null ? toolChoice : "无需工具卡片");

        // 6. 保存AI回复
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setMessageId(UUID.randomUUID().toString());
        aiMessage.setSessionId(sessionId);
        aiMessage.setRole("assistant");
        aiMessage.setContent(aiReply);
        aiMessage.setCardType(toolChoice);
        messageRepository.save(aiMessage);
        log.info("【步骤6/8】AI回复已持久化: messageId={}", aiMessage.getMessageId());

        // 7. 构建推理过程
        boolean usedLlm = llmAnalysis != null;
        List<ChatVO.ReasoningStepVO> reasoningProcess = buildReasoningProcess(sceneType, intent, emotion, knowledgeRecall, usedLlm);
        log.info("【步骤7/8】推理过程构建完成: {}个步骤", reasoningProcess.size());

        // 8. 构建响应
        ChatVO.SendMessageVO response = new ChatVO.SendMessageVO();
        response.setMessageId(aiMessage.getMessageId());
        response.setContent(aiReply);
        response.setCardType(toolChoice);
        response.setSceneType(sceneType);
        response.setIntent(intent);
        response.setEmotion(emotion);
        response.setKnowledgeRecall(knowledgeRecall);
        response.setReasoningProcess(reasoningProcess);
        response.setCreatedAt(aiMessage.getCreatedAt());

        log.info("【步骤8/8】响应构建完成 → 返回前端");
        log.info("========== 对话链路结束 | 场景={} | 意图={} | 情绪={} | 分析引擎={} | 回复引擎={} ==========",
                sceneType, intent.getIntentName(), emotion,
                usedLlm ? "LLM" : "关键词", llmReply != null ? "LLM" : "规则");
        return response;
    }

    /**
     * 获取消息历史
     */
    public List<ChatVO.MessageVO> getMessages(ChatDTO.QueryMessagesDTO dto) {
        int page = dto.getPage() != null ? dto.getPage() - 1 : 0;
        int size = dto.getSize() != null ? dto.getSize() : 20;

        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(
                dto.getSessionId(), PageRequest.of(page, size));

        List<ChatVO.MessageVO> result = new ArrayList<>();
        for (ChatMessage m : messages) {
            ChatVO.MessageVO vo = new ChatVO.MessageVO();
            vo.setMessageId(m.getMessageId());
            vo.setSessionId(m.getSessionId());
            vo.setRole(m.getRole());
            vo.setContent(m.getContent());
            vo.setCardType(m.getCardType());
            vo.setCreatedAt(m.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    /**
     * 更新上下文（暂存内存，后续可扩展为Redis缓存）
     */
    public void updateContext(ChatDTO.UpdateContextDTO dto) {
        log.info("更新上下文: sessionId={}", dto.getSessionId());
    }

    /**
     * 获取上下文
     */
    public ChatVO.ContextVO getContext(String sessionId) {
        return new ChatVO.ContextVO();
    }

    // ==================== 关键词兜底分析（LLM 不可用时使用） ====================

    private String recognizeSceneFallback(String content) {
        if (containsAny(content, "优惠", "活动", "打折", "促销", "价格", "多少钱")) return "售前咨询";
        if (containsAny(content, "退货", "换货", "退款", "售后", "投诉", "质量")) return "售后服务";
        if (containsAny(content, "快递", "物流", "发货", "到货", "配送")) return "物流配送";
        if (containsAny(content, "支付", "付款", "发票", "账单")) return "支付问题";
        return "通用咨询";
    }

    private ChatVO.IntentVO analyzeIntentFallback(String content) {
        ChatVO.IntentVO intent = new ChatVO.IntentVO();
        if (containsAny(content, "优惠", "活动", "打折", "促销")) {
            intent.setIntentCode("PRODUCT_DISCOUNT"); intent.setIntentName("商品优惠查询"); intent.setConfidence(0.95);
        } else if (containsAny(content, "退货", "换货", "退款")) {
            intent.setIntentCode("RETURN_EXCHANGE"); intent.setIntentName("退换货咨询"); intent.setConfidence(0.92);
        } else if (containsAny(content, "尺码", "大小", "尺寸")) {
            intent.setIntentCode("SIZE_CONSULT"); intent.setIntentName("尺码咨询"); intent.setConfidence(0.88);
        } else if (containsAny(content, "快递", "物流", "发货")) {
            intent.setIntentCode("LOGISTICS_QUERY"); intent.setIntentName("物流查询"); intent.setConfidence(0.90);
        } else {
            intent.setIntentCode("GENERAL_QUERY"); intent.setIntentName("通用咨询"); intent.setConfidence(0.75);
        }
        return intent;
    }

    private String analyzeEmotionFallback(String content) {
        if (containsAny(content, "投诉", "差评", "不满", "生气", "垃圾",
                "烦", "气死", "愤怒", "恼火", "失望", "坑", "骗", "太差",
                "什么破", "无语", "崩溃", "受不了", "忍无可忍", "糟糕",
                "郁闷", "难受", "恶心", "讨厌", "离谱", "扯淡")) return "负面";
        if (containsAny(content, "感谢", "满意", "好评", "不错", "很好",
                "赞", "棒", "优秀", "喜欢", "开心", "太好了", "完美",
                "靠谱", "给力", "厉害", "贴心", "周到", "快速")) return "正面";
        return "中性";
    }

    private ChatVO.KnowledgeRecallVO retrieveKnowledge(String content, ChatVO.IntentVO intent,
                                                       List<Long> selectedProductIds, List<Long> selectedActivityIds) {
        ChatVO.KnowledgeRecallVO recall = new ChatVO.KnowledgeRecallVO();
        recall.setProducts(new ArrayList<>());
        recall.setFaqs(new ArrayList<>());
        recall.setActivities(new ArrayList<>());

        // 1. 如果前端选中了商品，优先使用选中的
        if (selectedProductIds != null && !selectedProductIds.isEmpty()) {
            log.info("  ↳ 商品检索策略: 人工选择模式（前端选中{}个商品ID={}）", selectedProductIds.size(), selectedProductIds);
            for (Long pid : selectedProductIds) {
                productRepository.findById(pid).ifPresent(p -> {
                    ChatVO.ProductRecallVO vo = new ChatVO.ProductRecallVO();
                    vo.setProductId(String.valueOf(p.getId()));
                    vo.setProductName(p.getProductName());
                    vo.setScore(0.99);
                    vo.setSource("人工选择");
                    recall.getProducts().add(vo);
                    log.info("  ↳ 命中商品: [{}] {}（人工选择，score=0.99）", p.getId(), p.getProductName());
                });
            }
        }

        // 2. 如果前端选中了活动，优先使用选中的
        if (selectedActivityIds != null && !selectedActivityIds.isEmpty()) {
            log.info("  ↳ 活动检索策略: 人工选择模式（前端选中{}个活动ID={}）", selectedActivityIds.size(), selectedActivityIds);
            for (Long aid : selectedActivityIds) {
                activityRepository.findById(aid).ifPresent(a -> {
                    ChatVO.ActivityRecallVO vo = new ChatVO.ActivityRecallVO();
                    vo.setActivityId(String.valueOf(a.getId()));
                    vo.setActivityName(a.getActivityName());
                    vo.setDescription(a.getDescription());
                    vo.setScore(0.99);
                    recall.getActivities().add(vo);
                    log.info("  ↳ 命中活动: [{}] {}（人工选择，score=0.99）", a.getId(), a.getActivityName());
                });
            }
        }

        // 3. 自动关键词检索商品（没有手动选择时）
        if (recall.getProducts().isEmpty()) {
            log.info("  ↳ 商品检索策略: 自动关键词匹配（无人工选择，全表扫描）");
            List<Product> allProducts = productRepository.findAll();
            for (Product p : allProducts) {
                double score = calculateMatchScore(content, p.getProductName(), p.getDescription(), p.getFeatures());
                if (score > 0.3) {
                    ChatVO.ProductRecallVO vo = new ChatVO.ProductRecallVO();
                    vo.setProductId(String.valueOf(p.getId()));
                    vo.setProductName(p.getProductName());
                    vo.setScore(score);
                    vo.setSource("关键词匹配");
                    recall.getProducts().add(vo);
                }
            }
            recall.getProducts().sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            if (recall.getProducts().size() > 3) {
                recall.setProducts(new ArrayList<>(recall.getProducts().subList(0, 3)));
            }
            for (ChatVO.ProductRecallVO p : recall.getProducts()) {
                log.info("  ↳ 命中商品: [{}] {}（关键词匹配，score={})）", p.getProductId(), p.getProductName(), String.format("%.2f", p.getScore()));
            }
            if (recall.getProducts().isEmpty()) {
                log.info("  ↳ 商品检索结果: 无匹配（阈值0.3）");
            }
        }

        // 4. 自动检索活动（没有手动选择时）
        if (recall.getActivities().isEmpty() && containsAny(content, "优惠", "活动", "打折", "促销", "价格", "便宜")) {
            log.info("  ↳ 活动检索策略: 关键词触发（命中优惠/活动等关键词）");
            List<Activity> allActivities = activityRepository.findAll();
            for (Activity a : allActivities) {
                ChatVO.ActivityRecallVO vo = new ChatVO.ActivityRecallVO();
                vo.setActivityId(String.valueOf(a.getId()));
                vo.setActivityName(a.getActivityName());
                vo.setDescription(a.getDescription());
                vo.setScore(0.85);
                recall.getActivities().add(vo);
                log.info("  ↳ 命中活动: [{}] {}（自动检索，score=0.85）", a.getId(), a.getActivityName());
            }
        }

        // 5. FAQ 检索 — 始终执行
        List<Faq> allFaqs = faqRepository.findAll();
        for (Faq f : allFaqs) {
            double score = calculateMatchScore(content, f.getQuestion(), f.getAnswer(), f.getRelatedQuestions());
            if (score > 0.3) {
                ChatVO.FaqRecallVO vo = new ChatVO.FaqRecallVO();
                vo.setFaqId(String.valueOf(f.getId()));
                vo.setQuestion(f.getQuestion());
                vo.setAnswer(f.getAnswer());
                vo.setScore(score);
                recall.getFaqs().add(vo);
            }
        }
        recall.getFaqs().sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        if (recall.getFaqs().size() > 3) {
            recall.setFaqs(new ArrayList<>(recall.getFaqs().subList(0, 3)));
        }
        for (ChatVO.FaqRecallVO f : recall.getFaqs()) {
            log.info("  ↳ 命中FAQ: [{}] {}（score={}）", f.getFaqId(), f.getQuestion(), String.format("%.2f", f.getScore()));
        }

        return recall;
    }

    private double calculateMatchScore(String query, String... fields) {
        if (query == null || query.isEmpty()) return 0;
        String q = query.toLowerCase();
        int totalHits = 0;
        int totalChars = 0;
        for (String field : fields) {
            if (field == null || field.isEmpty()) continue;
            String f = field.toLowerCase();
            totalChars += f.length();
            for (int i = 0; i < q.length(); i++) {
                if (f.indexOf(q.charAt(i)) >= 0) totalHits++;
            }
            for (int len = 2; len <= Math.min(q.length(), 6); len++) {
                for (int i = 0; i <= q.length() - len; i++) {
                    if (f.contains(q.substring(i, i + len))) totalHits += len;
                }
            }
        }
        if (totalChars == 0) return 0;
        return Math.min(1.0, totalHits / (double) (q.length() * 3 + totalChars * 0.5));
    }

    private List<Long> parseIds(Map<String, Object> extra, String key) {
        if (extra == null || !extra.containsKey(key)) return Collections.emptyList();
        Object val = extra.get(key);
        if (val instanceof List) {
            List<Long> result = new ArrayList<>();
            for (Object item : (List<?>) val) {
                if (item instanceof Number) result.add(((Number) item).longValue());
                else if (item instanceof String) {
                    try { result.add(Long.parseLong((String) item)); } catch (NumberFormatException ignored) {}
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private String buildKnowledgeText(ChatVO.KnowledgeRecallVO recall) {
        StringBuilder sb = new StringBuilder();
        if (!recall.getProducts().isEmpty()) {
            sb.append("【相关商品】");
            for (ChatVO.ProductRecallVO p : recall.getProducts()) {
                sb.append("\n- ").append(p.getProductName());
            }
        }
        if (!recall.getActivities().isEmpty()) {
            sb.append("\n【优惠活动】");
            for (ChatVO.ActivityRecallVO a : recall.getActivities()) {
                sb.append("\n- ").append(a.getActivityName()).append("：").append(a.getDescription());
            }
        }
        if (!recall.getFaqs().isEmpty()) {
            sb.append("\n【FAQ参考】");
            for (ChatVO.FaqRecallVO f : recall.getFaqs()) {
                sb.append("\n- Q: ").append(f.getQuestion()).append(" A: ").append(f.getAnswer());
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String generateReply(String userContent, ChatVO.IntentVO intent, ChatVO.KnowledgeRecallVO knowledge) {
        StringBuilder reply = new StringBuilder();

        if (!knowledge.getFaqs().isEmpty()) {
            ChatVO.FaqRecallVO topFaq = knowledge.getFaqs().get(0);
            reply.append(topFaq.getAnswer());
        }

        if (!knowledge.getActivities().isEmpty()) {
            if (reply.length() > 0) reply.append("\n\n");
            reply.append("当前可用优惠活动：");
            for (ChatVO.ActivityRecallVO a : knowledge.getActivities()) {
                reply.append("\n- ").append(a.getActivityName()).append("：").append(a.getDescription());
            }
        }

        if (!knowledge.getProducts().isEmpty() && "SIZE_CONSULT".equals(intent.getIntentCode())) {
            if (reply.length() > 0) reply.append("\n\n");
            reply.append("相关商品：");
            for (ChatVO.ProductRecallVO p : knowledge.getProducts()) {
                reply.append("\n- ").append(p.getProductName());
            }
        }

        if (reply.length() == 0) {
            return switch (intent.getIntentCode()) {
                case "RETURN_EXCHANGE" -> "退换货步骤：\n1. 进入【我的订单】找到对应订单\n2. 点击【申请售后】\n3. 选择退换货原因并提交\n\n审核通过后，退款将在3-5个工作日内到账。";
                case "LOGISTICS_QUERY" -> "您可以在订单详情页查看物流信息。一般发货后2-3天送达，如需修改地址请在发货前联系客服。";
                default -> "感谢您的咨询！请问还有什么其他问题需要帮助吗？";
            };
        }

        return reply.toString();
    }

    private String selectTool(ChatVO.IntentVO intent, ChatVO.KnowledgeRecallVO knowledge) {
        return switch (intent.getIntentCode()) {
            case "PRODUCT_DISCOUNT" -> "ACTIVITY_CARD";
            case "SIZE_CONSULT" -> "PRODUCT_CARD";
            case "LOGISTICS_QUERY" -> "LOGISTICS_CARD";
            default -> null;
        };
    }

    private List<ChatVO.ReasoningStepVO> buildReasoningProcess(String sceneType, ChatVO.IntentVO intent,
                                                                String emotion, ChatVO.KnowledgeRecallVO knowledge, boolean usedLlm) {
        List<ChatVO.ReasoningStepVO> steps = new ArrayList<>();
        String engine = usedLlm ? "LLM 大模型" : "关键词规则（LLM 不可用）";
        addStep(steps, 1, "输入解析", "解析用户输入，分析引擎：" + engine);
        addStep(steps, 2, "场景识别", "识别场景类型：" + sceneType + "（" + engine + "）");
        addStep(steps, 3, "意图分析", "意图：" + intent.getIntentName() + "，置信度：" + intent.getConfidence() + "（" + engine + "）");
        addStep(steps, 4, "情绪判断", "用户情绪：" + emotion + "（" + engine + "）");
        addStep(steps, 5, "知识检索", String.format("检索到相关知识：活动%d条，商品%d条，FAQ%d条",
                knowledge.getActivities().size(), knowledge.getProducts().size(), knowledge.getFaqs().size()));
        addStep(steps, 6, "生成回复", usedLlm ? "基于 LLM + 知识检索生成个性化回复" : "基于规则引擎 + 知识检索生成兜底回复");
        return steps;
    }

    private void addStep(List<ChatVO.ReasoningStepVO> steps, int stepNum, String title, String content) {
        ChatVO.ReasoningStepVO step = new ChatVO.ReasoningStepVO();
        step.setStep(stepNum); step.setTitle(title); step.setContent(content); step.setTimestamp(LocalDateTime.now());
        steps.add(step);
    }

    private boolean containsAny(String content, String... keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword)) return true;
        }
        return false;
    }
}
