package com.anjing.module.scene;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 场景配置模块 DTO 定义
 */
public class SceneDTO {

    /**
     * 通用ID请求
     */
    @Data
    public static class IdDTO {
        private Long id;
    }

    /**
     * 通用分页参数
     */
    @Data
    public static class PageDTO {
        private Integer page = 1;
        private Integer size = 20;
    }

    // ==================== 意图管理 ====================

    @Data
    public static class QueryIntentDTO extends PageDTO {
        private String keyword;
        private String category;
        private String status;
    }

    @Data
    public static class CreateIntentDTO {
        /** 意图名称 */
        private String intentName;
        /** 意图编码 */
        private String intentCode;
        /** 分类 */
        private String category;
        /** 触发关键词 */
        private List<String> triggerKeywords;
        /** 置信度阈值 */
        private Double confidenceThreshold;
        /** 描述 */
        private String description;
    }

    @Data
    public static class UpdateIntentDTO {
        private Long id;
        private String intentName;
        private String intentCode;
        private String category;
        private List<String> triggerKeywords;
        private Double confidenceThreshold;
        private String description;
        private String status;
    }

    // ==================== 提示词模板 ====================

    @Data
    public static class QueryPromptDTO extends PageDTO {
        private String keyword;
        private String promptType;
        private String status;
    }

    @Data
    public static class CreatePromptDTO {
        /** 提示词名称 */
        private String promptName;
        /** 提示词编码 */
        private String promptCode;
        /** 提示词类型：SYSTEM/USER/ASSISTANT */
        private String promptType;
        /** 提示词内容 */
        private String content;
        /** 描述 */
        private String description;
        /** 变量定义 */
        private List<PromptVariableDTO> variables;
    }

    @Data
    public static class PromptVariableDTO {
        private String name;
        private String description;
        private String defaultValue;
        private Boolean required;
    }

    @Data
    public static class UpdatePromptDTO {
        private Long id;
        private String promptName;
        private String promptCode;
        private String promptType;
        private String content;
        private String description;
        private List<PromptVariableDTO> variables;
        private String status;
    }

    @Data
    public static class TestPromptDTO {
        /** 提示词ID */
        private Long promptId;
        /** 测试输入 */
        private String input;
        /** 变量值 */
        private Map<String, String> variables;
    }

    // ==================== 场景规则 ====================

    @Data
    public static class QueryRuleDTO extends PageDTO {
        private String keyword;
        private String ruleType;
        private Boolean enabled;
    }

    @Data
    public static class CreateRuleDTO {
        /** 规则名称 */
        private String ruleName;
        /** 规则编码 */
        private String ruleCode;
        /** 规则类型 */
        private String ruleType;
        /** 规则描述 */
        private String description;
        /** 规则条件（JSON格式） */
        private String conditions;
        /** 规则动作（JSON格式） */
        private String actions;
        /** 优先级 */
        private Integer priority;
        /** 生效时间 */
        private String effectiveTime;
        /** 过期时间 */
        private String expireTime;
    }

    @Data
    public static class UpdateRuleDTO {
        private Long id;
        private String ruleName;
        private String ruleCode;
        private String ruleType;
        private String description;
        private String conditions;
        private String actions;
        private Integer priority;
        private String effectiveTime;
        private String expireTime;
        private Boolean enabled;
    }
}

