package com.anjing.module.scene;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 场景配置模块 VO 定义
 */
public class SceneVO {

    /**
     * 分页包装
     */
    @Data
    public static class PageVO<T> {
        private List<T> records;
        private Integer page;
        private Integer size;
        private Long total;
    }

    /**
     * 意图配置
     */
    @Data
    public static class IntentVO {
        private Long id;
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
        /** 命中次数 */
        private Integer hitCount;
        /** 状态 */
        private String status;
        /** 创建时间 */
        private LocalDateTime createdAt;
        /** 更新时间 */
        private LocalDateTime updatedAt;
    }

    /**
     * 提示词模板
     */
    @Data
    public static class PromptVO {
        private Long id;
        /** 提示词名称 */
        private String promptName;
        /** 提示词编码 */
        private String promptCode;
        /** 提示词类型 */
        private String promptType;
        /** 提示词内容 */
        private String content;
        /** 描述 */
        private String description;
        /** 变量定义 */
        private List<PromptVariableVO> variables;
        /** 使用次数 */
        private Integer usageCount;
        /** 版本 */
        private String version;
        /** 状态 */
        private String status;
        /** 创建时间 */
        private LocalDateTime createdAt;
        /** 更新时间 */
        private LocalDateTime updatedAt;
    }

    @Data
    public static class PromptVariableVO {
        private String name;
        private String description;
        private String defaultValue;
        private Boolean required;
    }

    /**
     * 提示词测试结果
     */
    @Data
    public static class PromptTestResultVO {
        /** 渲染后的提示词 */
        private String renderedPrompt;
        /** AI回复 */
        private String aiResponse;
        /** 耗时(ms) */
        private Long duration;
        /** Token消耗 */
        private Integer tokenUsage;
    }

    /**
     * 场景规则
     */
    @Data
    public static class RuleVO {
        private Long id;
        /** 规则名称 */
        private String ruleName;
        /** 规则编码 */
        private String ruleCode;
        /** 规则类型 */
        private String ruleType;
        /** 规则描述 */
        private String description;
        /** 规则条件 */
        private String conditions;
        /** 规则动作 */
        private String actions;
        /** 优先级 */
        private Integer priority;
        /** 触发次数 */
        private Integer triggerCount;
        /** 是否启用 */
        private Boolean enabled;
        /** 生效时间 */
        private String effectiveTime;
        /** 过期时间 */
        private String expireTime;
        /** 创建时间 */
        private LocalDateTime createdAt;
        /** 更新时间 */
        private LocalDateTime updatedAt;
    }
}

