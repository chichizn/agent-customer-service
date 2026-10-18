package com.anjing.module.knowledge;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识中心模块 VO
 */
public class KnowledgeVO {

    // ==================== 通用 ====================
    
    @Data
    public static class PageVO<T> {
        private List<T> records;
        private Integer page;
        private Integer size;
        private Long total;
    }

    @Data
    public static class KnowledgeOverviewVO {
        private Long totalCount;
        private Long productCount;
        private Long activityCount;
        private Long faqCount;
        private Long industryCount;
        private Long solutionCount;
        private Long vectorizedCount;
        private String lastUpdateTime;
    }

    // ==================== 商品知识 ====================
    
    @Data
    public static class ProductVO {
        private Long id;
        private String productName;
        private String productCode;
        private String category;
        private String brand;
        private Double price;
        private String description;
        private String specifications;
        private String features;
        private String imageUrl;
        private List<String> tags;
        private Integer viewCount;
        private String status;
        private Boolean vectorized;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ==================== 活动知识 ====================
    
    @Data
    public static class ActivityVO {
        private Long id;
        private String activityName;
        private String activityCode;
        private String activityType;
        private String description;
        private String startTime;
        private String endTime;
        private String rules;
        private List<String> applicableProducts;
        private Double discountRate;
        private Integer usageCount;
        private String status;
        private Boolean vectorized;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ==================== FAQ问答 ====================
    
    @Data
    public static class FaqVO {
        private Long id;
        private String question;
        private String answer;
        private String category;
        private List<String> relatedQuestions;
        private List<String> tags;
        private Integer hitCount;
        private Integer priority;
        private String status;
        private Boolean vectorized;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ==================== 行业知识 ====================
    
    @Data
    public static class IndustryVO {
        private Long id;
        private String title;
        private String industryType;
        private String content;
        private String source;
        private List<String> keywords;
        private Integer viewCount;
        private String status;
        private Boolean vectorized;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ==================== 场景解决方案 ====================
    
    @Data
    public static class SolutionVO {
        private Long id;
        private String solutionName;
        private String solutionCode;
        private String sceneType;
        private String description;
        private String triggerCondition;
        private List<SolutionStepVO> steps;
        private String expectedOutcome;
        private Integer usageCount;
        private Double successRate;
        private String status;
        private Boolean vectorized;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class SolutionStepVO {
        private Integer stepOrder;
        private String stepName;
        private String stepAction;
        private String expectedResult;
    }

    // ==================== 向量化相关 ====================
    
    @Data
    public static class VectorizeResultVO {
        private String taskId;
        private String status;
        private String message;
    }

    @Data
    public static class VectorizeStatusVO {
        private String taskId;
        private String status;
        private Integer progress;
        private Integer totalCount;
        private Integer processedCount;
        private String startTime;
        private String endTime;
    }

    // ==================== 导入导出相关 ====================
    
    @Data
    public static class ImportResultVO {
        private Integer totalCount;
        private Integer successCount;
        private Integer failCount;
        private List<String> failReasons;
    }

    @Data
    public static class ExportResultVO {
        private String fileUrl;
        private String fileName;
        private Long fileSize;
    }
}
