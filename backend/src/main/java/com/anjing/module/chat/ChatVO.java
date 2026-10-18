package com.anjing.module.chat;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 对话模块 VO 定义
 */
public class ChatVO {

    /**
     * 会话信息
     */
    @Data
    public static class SessionVO {
        /** 会话ID */
        private String sessionId;
        /** 用户ID */
        private String userId;
        /** 用户名称 */
        private String userName;
        /** 会话状态 */
        private String status;
        /** 消息数量 */
        private Integer messageCount;
        /** 最后一条消息 */
        private String lastMessage;
        /** 创建时间 */
        private LocalDateTime createdAt;
    }

    /**
     * 会话详情
     */
    @Data
    public static class SessionDetailVO {
        /** 会话ID */
        private String sessionId;
        /** 用户ID */
        private String userId;
        /** 用户名称 */
        private String userName;
        /** 会话状态 */
        private String status;
        /** 上下文信息 */
        private ContextVO context;
        /** 消息列表 */
        private List<MessageVO> messages;
        /** 创建时间 */
        private LocalDateTime createdAt;
    }

    /**
     * 消息信息
     */
    @Data
    public static class MessageVO {
        /** 消息ID */
        private String messageId;
        /** 会话ID */
        private String sessionId;
        /** 角色：user/assistant */
        private String role;
        /** 消息内容 */
        private String content;
        /** 卡片类型 */
        private String cardType;
        /** 创建时间 */
        private LocalDateTime createdAt;
    }

    /**
     * 发送消息响应（包含AI处理结果）
     */
    @Data
    public static class SendMessageVO {
        /** 消息ID */
        private String messageId;
        /** AI回复内容 */
        private String content;
        /** 卡片类型 */
        private String cardType;
        /** 场景类型 */
        private String sceneType;
        /** 意图识别结果 */
        private IntentVO intent;
        /** 情绪分析 */
        private String emotion;
        /** 知识召回结果 */
        private KnowledgeRecallVO knowledgeRecall;
        /** 推理过程 */
        private List<ReasoningStepVO> reasoningProcess;
        /** 创建时间 */
        private LocalDateTime createdAt;
    }

    /**
     * 意图识别结果
     */
    @Data
    public static class IntentVO {
        /** 意图编码 */
        private String intentCode;
        /** 意图名称 */
        private String intentName;
        /** 置信度 */
        private Double confidence;
    }

    /**
     * 知识召回结果
     */
    @Data
    public static class KnowledgeRecallVO {
        /** 召回的商品 */
        private List<ProductRecallVO> products;
        /** 召回的FAQ */
        private List<FaqRecallVO> faqs;
        /** 召回的活动 */
        private List<ActivityRecallVO> activities;
    }

    /**
     * 商品召回
     */
    @Data
    public static class ProductRecallVO {
        /** 商品ID */
        private String productId;
        /** 商品名称 */
        private String productName;
        /** 匹配分数 */
        private Double score;
        /** 来源 */
        private String source;
    }

    /**
     * FAQ召回
     */
    @Data
    public static class FaqRecallVO {
        /** FAQ ID */
        private String faqId;
        /** 问题 */
        private String question;
        /** 答案 */
        private String answer;
        /** 匹配分数 */
        private Double score;
    }

    /**
     * 活动召回
     */
    @Data
    public static class ActivityRecallVO {
        /** 活动ID */
        private String activityId;
        /** 活动名称 */
        private String activityName;
        /** 活动描述 */
        private String description;
        /** 匹配分数 */
        private Double score;
    }

    /**
     * 推理步骤
     */
    @Data
    public static class ReasoningStepVO {
        /** 步骤序号 */
        private Integer step;
        /** 步骤标题 */
        private String title;
        /** 步骤内容 */
        private String content;
        /** 时间戳 */
        private LocalDateTime timestamp;
    }

    /**
     * 上下文信息
     */
    @Data
    public static class ContextVO {
        /** 选中的商品 */
        private List<String> selectedProducts;
        /** 选中的活动 */
        private List<String> selectedActivities;
        /** 用户画像 */
        private Map<String, Object> userProfile;
    }
}

