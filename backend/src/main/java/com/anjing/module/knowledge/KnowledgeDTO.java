package com.anjing.module.knowledge;

import lombok.Data;

import java.util.List;

/**
 * 知识中心模块 DTO
 */
public class KnowledgeDTO {

    // ==================== 通用 ====================
    
    @Data
    public static class IdDTO {
        private Long id;
    }

    @Data
    public static class QueryDTO {
        private String keyword;
        private String status;
        private Integer page = 1;
        private Integer size = 20;
    }

    // ==================== 商品知识 ====================
    
    @Data
    public static class QueryProductDTO extends QueryDTO {
        private String category;
        private String priceRange;
    }

    @Data
    public static class CreateProductDTO {
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
    }

    @Data
    public static class UpdateProductDTO extends CreateProductDTO {
        private Long id;
        private String status;
    }

    // ==================== 活动知识 ====================
    
    @Data
    public static class QueryActivityDTO extends QueryDTO {
        private String activityType;
    }

    @Data
    public static class CreateActivityDTO {
        private String activityName;
        private String activityCode;
        private String activityType;
        private String description;
        private String startTime;
        private String endTime;
        private String rules;
        private List<String> applicableProducts;
        private Double discountRate;
    }

    @Data
    public static class UpdateActivityDTO extends CreateActivityDTO {
        private Long id;
        private String status;
    }

    // ==================== FAQ问答 ====================
    
    @Data
    public static class QueryFaqDTO extends QueryDTO {
        private String category;
    }

    @Data
    public static class CreateFaqDTO {
        private String question;
        private String answer;
        private String category;
        private List<String> relatedQuestions;
        private List<String> tags;
        private Integer priority;
    }

    @Data
    public static class UpdateFaqDTO extends CreateFaqDTO {
        private Long id;
        private String status;
    }

    // ==================== 行业知识 ====================
    
    @Data
    public static class QueryIndustryDTO extends QueryDTO {
        private String industryType;
    }

    @Data
    public static class CreateIndustryDTO {
        private String title;
        private String industryType;
        private String content;
        private String source;
        private List<String> keywords;
    }

    @Data
    public static class UpdateIndustryDTO extends CreateIndustryDTO {
        private Long id;
        private String status;
    }

    // ==================== 场景解决方案 ====================
    
    @Data
    public static class QuerySolutionDTO extends QueryDTO {
        private String sceneType;
    }

    @Data
    public static class CreateSolutionDTO {
        private String solutionName;
        private String solutionCode;
        private String sceneType;
        private String description;
        private String triggerCondition;
        private List<SolutionStepDTO> steps;
        private String expectedOutcome;
    }

    @Data
    public static class UpdateSolutionDTO extends CreateSolutionDTO {
        private Long id;
        private String status;
    }

    @Data
    public static class SolutionStepDTO {
        private Integer stepOrder;
        private String stepName;
        private String stepAction;
        private String expectedResult;
    }

    // ==================== 知识向量化 ====================
    
    @Data
    public static class VectorizeDTO {
        private String knowledgeType;
        private Long knowledgeId;
    }

    @Data
    public static class VectorizeStatusDTO {
        private String taskId;
    }

    // ==================== 知识导入 ====================
    
    @Data
    public static class ImportDTO {
        private String knowledgeType;
        private String fileUrl;
        private String fileType;
    }

    @Data
    public static class ExportDTO {
        private String knowledgeType;
        private List<Long> ids;
    }
}
