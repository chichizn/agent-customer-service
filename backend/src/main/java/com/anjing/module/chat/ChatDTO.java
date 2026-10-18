package com.anjing.module.chat;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 对话模块 DTO 定义
 */
public class ChatDTO {

    /**
     * 创建会话请求
     */
    @Data
    public static class CreateSessionDTO {
        /** 用户ID */
        private String userId;
        /** 用户名称 */
        private String userName;
        /** 渠道来源 */
        private String channel;
    }

    /**
     * 会话ID请求
     */
    @Data
    public static class SessionIdDTO {
        /** 会话ID */
        private String sessionId;
    }

    /**
     * 查询会话列表请求
     */
    @Data
    public static class QuerySessionDTO {
        /** 用户ID */
        private String userId;
        /** 会话状态 */
        private String status;
        /** 页码 */
        private Integer page;
        /** 每页数量 */
        private Integer size;
    }

    /**
     * 发送消息请求
     */
    @Data
    public static class SendMessageDTO {
        /** 会话ID */
        private String sessionId;
        /** 消息内容 */
        private String content;
        /** 消息类型：text/image/file */
        private String messageType;
        /** 附加数据 */
        private Map<String, Object> extra;
    }

    /**
     * 查询消息历史请求
     */
    @Data
    public static class QueryMessagesDTO {
        /** 会话ID */
        private String sessionId;
        /** 页码 */
        private Integer page;
        /** 每页数量 */
        private Integer size;
    }

    /**
     * 更新上下文请求
     */
    @Data
    public static class UpdateContextDTO {
        /** 会话ID */
        private String sessionId;
        /** 选中的商品ID列表 */
        private List<String> selectedProducts;
        /** 选中的活动ID列表 */
        private List<String> selectedActivities;
        /** 用户画像信息 */
        private Map<String, Object> userProfile;
    }
}

