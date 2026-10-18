package com.anjing.model.constants;

/**
 * 智能客服系统 API 路径常量
 * 统一管理所有接口路径，便于维护和查找
 * 
 * 模块划分：
 * 1. Chat - 对话中心（核心模块）
 * 2. Knowledge - 知识中心
 * 3. Scene - 场景配置
 */
public class ApiConstants {

    /**
     * ==================== 对话中心模块 ====================
     * 核心对话功能，包含会话管理和消息处理
     */
    public static class Chat {
        // 会话管理
        public static final String CREATE_SESSION = "/api/chat/session/create";
        public static final String LIST_SESSIONS = "/api/chat/session/list";
        public static final String GET_SESSION = "/api/chat/session/get";
        public static final String DELETE_SESSION = "/api/chat/session/delete";
        
        // 消息处理（核心链路）
        public static final String SEND_MESSAGE = "/api/chat/message/send";
        public static final String GET_MESSAGES = "/api/chat/message/list";
        
        // 上下文管理
        public static final String UPDATE_CONTEXT = "/api/chat/context/update";
        public static final String GET_CONTEXT = "/api/chat/context/get";
    }

    /**
     * ==================== 知识中心模块 ====================
     * 知识库管理，包含商品、活动、FAQ、行业知识等
     */
    public static class Knowledge {
        // 知识总览
        public static final String OVERVIEW = "/api/knowledge/overview";
        
        // 商品知识
        public static final String PRODUCT_LIST = "/api/knowledge/product/list";
        public static final String PRODUCT_CREATE = "/api/knowledge/product/create";
        public static final String PRODUCT_UPDATE = "/api/knowledge/product/update";
        public static final String PRODUCT_DELETE = "/api/knowledge/product/delete";
        public static final String PRODUCT_DETAIL = "/api/knowledge/product/detail";
        public static final String PRODUCT_IMPORT = "/api/knowledge/product/import";
        public static final String PRODUCT_EXPORT = "/api/knowledge/product/export";
        
        // 活动知识
        public static final String ACTIVITY_LIST = "/api/knowledge/activity/list";
        public static final String ACTIVITY_CREATE = "/api/knowledge/activity/create";
        public static final String ACTIVITY_UPDATE = "/api/knowledge/activity/update";
        public static final String ACTIVITY_DELETE = "/api/knowledge/activity/delete";
        public static final String ACTIVITY_DETAIL = "/api/knowledge/activity/detail";
        
        // FAQ问答
        public static final String FAQ_LIST = "/api/knowledge/faq/list";
        public static final String FAQ_CREATE = "/api/knowledge/faq/create";
        public static final String FAQ_UPDATE = "/api/knowledge/faq/update";
        public static final String FAQ_DELETE = "/api/knowledge/faq/delete";
        public static final String FAQ_DETAIL = "/api/knowledge/faq/detail";
        public static final String FAQ_IMPORT = "/api/knowledge/faq/import";
        
        // 行业知识
        public static final String INDUSTRY_LIST = "/api/knowledge/industry/list";
        public static final String INDUSTRY_CREATE = "/api/knowledge/industry/create";
        public static final String INDUSTRY_UPDATE = "/api/knowledge/industry/update";
        public static final String INDUSTRY_DELETE = "/api/knowledge/industry/delete";
        
        // 场景解决方案
        public static final String SOLUTION_LIST = "/api/knowledge/solution/list";
        public static final String SOLUTION_CREATE = "/api/knowledge/solution/create";
        public static final String SOLUTION_UPDATE = "/api/knowledge/solution/update";
        public static final String SOLUTION_DELETE = "/api/knowledge/solution/delete";
        public static final String SOLUTION_DETAIL = "/api/knowledge/solution/detail";
        
        // 知识向量化（RAG相关）
        public static final String VECTORIZE = "/api/knowledge/vectorize";
        public static final String VECTORIZE_STATUS = "/api/knowledge/vectorize/status";
    }

    /**
     * ==================== 场景配置模块 ====================
     * 意图识别、提示词模板、场景规则配置
     */
    public static class Scene {
        // 意图管理
        public static final String INTENT_LIST = "/api/scene/intent/list";
        public static final String INTENT_CREATE = "/api/scene/intent/create";
        public static final String INTENT_UPDATE = "/api/scene/intent/update";
        public static final String INTENT_DELETE = "/api/scene/intent/delete";
        public static final String INTENT_DETAIL = "/api/scene/intent/detail";
        
        // 提示词模板
        public static final String PROMPT_LIST = "/api/scene/prompt/list";
        public static final String PROMPT_CREATE = "/api/scene/prompt/create";
        public static final String PROMPT_UPDATE = "/api/scene/prompt/update";
        public static final String PROMPT_DELETE = "/api/scene/prompt/delete";
        public static final String PROMPT_DETAIL = "/api/scene/prompt/detail";
        public static final String PROMPT_TEST = "/api/scene/prompt/test";
        
        // 场景规则
        public static final String RULE_LIST = "/api/scene/rule/list";
        public static final String RULE_CREATE = "/api/scene/rule/create";
        public static final String RULE_UPDATE = "/api/scene/rule/update";
        public static final String RULE_DELETE = "/api/scene/rule/delete";
        public static final String RULE_DETAIL = "/api/scene/rule/detail";
        public static final String RULE_ENABLE = "/api/scene/rule/enable";
        public static final String RULE_DISABLE = "/api/scene/rule/disable";
    }
}
