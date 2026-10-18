package com.anjing.module.knowledge;

import com.anjing.model.constants.ApiConstants;
import com.anjing.model.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 知识中心 Controller
 * 负责处理各类知识（商品、活动、FAQ、行业、解决方案）的管理
 */
@RestController
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    // ==================== 知识总览 ====================

    /**
     * 获取知识总览
     */
    @PostMapping(ApiConstants.Knowledge.OVERVIEW)
    public R<KnowledgeVO.KnowledgeOverviewVO> getOverview() {
        return R.ok(knowledgeService.getOverview());
    }

    // ==================== 商品知识 ====================

    /**
     * 获取商品知识列表
     */
    @PostMapping(ApiConstants.Knowledge.PRODUCT_LIST)
    public R<KnowledgeVO.PageVO<KnowledgeVO.ProductVO>> listProducts(@RequestBody KnowledgeDTO.QueryProductDTO dto) {
        return R.ok(knowledgeService.listProducts(dto));
    }

    /**
     * 创建商品知识
     */
    @PostMapping(ApiConstants.Knowledge.PRODUCT_CREATE)
    public R<KnowledgeVO.ProductVO> createProduct(@RequestBody KnowledgeDTO.CreateProductDTO dto) {
        return R.ok(knowledgeService.createProduct(dto));
    }

    /**
     * 更新商品知识
     */
    @PostMapping(ApiConstants.Knowledge.PRODUCT_UPDATE)
    public R<KnowledgeVO.ProductVO> updateProduct(@RequestBody KnowledgeDTO.UpdateProductDTO dto) {
        return R.ok(knowledgeService.updateProduct(dto));
    }

    /**
     * 删除商品知识
     */
    @PostMapping(ApiConstants.Knowledge.PRODUCT_DELETE)
    public R<Void> deleteProduct(@RequestBody KnowledgeDTO.IdDTO dto) {
        knowledgeService.deleteProduct(dto.getId());
        return R.ok();
    }

    /**
     * 获取商品知识详情
     */
    @PostMapping(ApiConstants.Knowledge.PRODUCT_DETAIL)
    public R<KnowledgeVO.ProductVO> getProductDetail(@RequestBody KnowledgeDTO.IdDTO dto) {
        return R.ok(knowledgeService.getProductDetail(dto.getId()));
    }

    // ==================== 活动知识 ====================

    /**
     * 获取活动知识列表
     */
    @PostMapping(ApiConstants.Knowledge.ACTIVITY_LIST)
    public R<KnowledgeVO.PageVO<KnowledgeVO.ActivityVO>> listActivities(@RequestBody KnowledgeDTO.QueryActivityDTO dto) {
        return R.ok(knowledgeService.listActivities(dto));
    }

    /**
     * 创建活动知识
     */
    @PostMapping(ApiConstants.Knowledge.ACTIVITY_CREATE)
    public R<KnowledgeVO.ActivityVO> createActivity(@RequestBody KnowledgeDTO.CreateActivityDTO dto) {
        return R.ok(knowledgeService.createActivity(dto));
    }

    /**
     * 更新活动知识
     */
    @PostMapping(ApiConstants.Knowledge.ACTIVITY_UPDATE)
    public R<KnowledgeVO.ActivityVO> updateActivity(@RequestBody KnowledgeDTO.UpdateActivityDTO dto) {
        return R.ok(knowledgeService.updateActivity(dto));
    }

    /**
     * 删除活动知识
     */
    @PostMapping(ApiConstants.Knowledge.ACTIVITY_DELETE)
    public R<Void> deleteActivity(@RequestBody KnowledgeDTO.IdDTO dto) {
        knowledgeService.deleteActivity(dto.getId());
        return R.ok();
    }

    /**
     * 获取活动知识详情
     */
    @PostMapping(ApiConstants.Knowledge.ACTIVITY_DETAIL)
    public R<KnowledgeVO.ActivityVO> getActivityDetail(@RequestBody KnowledgeDTO.IdDTO dto) {
        return R.ok(knowledgeService.getActivityDetail(dto.getId()));
    }

    // ==================== FAQ问答 ====================

    /**
     * 获取FAQ列表
     */
    @PostMapping(ApiConstants.Knowledge.FAQ_LIST)
    public R<KnowledgeVO.PageVO<KnowledgeVO.FaqVO>> listFaqs(@RequestBody KnowledgeDTO.QueryFaqDTO dto) {
        return R.ok(knowledgeService.listFaqs(dto));
    }

    /**
     * 创建FAQ
     */
    @PostMapping(ApiConstants.Knowledge.FAQ_CREATE)
    public R<KnowledgeVO.FaqVO> createFaq(@RequestBody KnowledgeDTO.CreateFaqDTO dto) {
        return R.ok(knowledgeService.createFaq(dto));
    }

    /**
     * 更新FAQ
     */
    @PostMapping(ApiConstants.Knowledge.FAQ_UPDATE)
    public R<KnowledgeVO.FaqVO> updateFaq(@RequestBody KnowledgeDTO.UpdateFaqDTO dto) {
        return R.ok(knowledgeService.updateFaq(dto));
    }

    /**
     * 删除FAQ
     */
    @PostMapping(ApiConstants.Knowledge.FAQ_DELETE)
    public R<Void> deleteFaq(@RequestBody KnowledgeDTO.IdDTO dto) {
        knowledgeService.deleteFaq(dto.getId());
        return R.ok();
    }

    /**
     * 获取FAQ详情
     */
    @PostMapping(ApiConstants.Knowledge.FAQ_DETAIL)
    public R<KnowledgeVO.FaqVO> getFaqDetail(@RequestBody KnowledgeDTO.IdDTO dto) {
        return R.ok(knowledgeService.getFaqDetail(dto.getId()));
    }

    // ==================== 行业知识 ====================

    /**
     * 获取行业知识列表
     */
    @PostMapping(ApiConstants.Knowledge.INDUSTRY_LIST)
    public R<KnowledgeVO.PageVO<KnowledgeVO.IndustryVO>> listIndustries(@RequestBody KnowledgeDTO.QueryIndustryDTO dto) {
        return R.ok(knowledgeService.listIndustries(dto));
    }

    /**
     * 创建行业知识
     */
    @PostMapping(ApiConstants.Knowledge.INDUSTRY_CREATE)
    public R<KnowledgeVO.IndustryVO> createIndustry(@RequestBody KnowledgeDTO.CreateIndustryDTO dto) {
        return R.ok(knowledgeService.createIndustry(dto));
    }

    /**
     * 更新行业知识
     */
    @PostMapping(ApiConstants.Knowledge.INDUSTRY_UPDATE)
    public R<KnowledgeVO.IndustryVO> updateIndustry(@RequestBody KnowledgeDTO.UpdateIndustryDTO dto) {
        return R.ok(knowledgeService.updateIndustry(dto));
    }

    /**
     * 删除行业知识
     */
    @PostMapping(ApiConstants.Knowledge.INDUSTRY_DELETE)
    public R<Void> deleteIndustry(@RequestBody KnowledgeDTO.IdDTO dto) {
        knowledgeService.deleteIndustry(dto.getId());
        return R.ok();
    }

    // ==================== 场景解决方案 ====================

    /**
     * 获取解决方案列表
     */
    @PostMapping(ApiConstants.Knowledge.SOLUTION_LIST)
    public R<KnowledgeVO.PageVO<KnowledgeVO.SolutionVO>> listSolutions(@RequestBody KnowledgeDTO.QuerySolutionDTO dto) {
        return R.ok(knowledgeService.listSolutions(dto));
    }

    /**
     * 创建解决方案
     */
    @PostMapping(ApiConstants.Knowledge.SOLUTION_CREATE)
    public R<KnowledgeVO.SolutionVO> createSolution(@RequestBody KnowledgeDTO.CreateSolutionDTO dto) {
        return R.ok(knowledgeService.createSolution(dto));
    }

    /**
     * 更新解决方案
     */
    @PostMapping(ApiConstants.Knowledge.SOLUTION_UPDATE)
    public R<KnowledgeVO.SolutionVO> updateSolution(@RequestBody KnowledgeDTO.UpdateSolutionDTO dto) {
        return R.ok(knowledgeService.updateSolution(dto));
    }

    /**
     * 删除解决方案
     */
    @PostMapping(ApiConstants.Knowledge.SOLUTION_DELETE)
    public R<Void> deleteSolution(@RequestBody KnowledgeDTO.IdDTO dto) {
        knowledgeService.deleteSolution(dto.getId());
        return R.ok();
    }

    /**
     * 获取解决方案详情
     */
    @PostMapping(ApiConstants.Knowledge.SOLUTION_DETAIL)
    public R<KnowledgeVO.SolutionVO> getSolutionDetail(@RequestBody KnowledgeDTO.IdDTO dto) {
        return R.ok(knowledgeService.getSolutionDetail(dto.getId()));
    }

    // ==================== 知识向量化 ====================

    /**
     * 触发知识向量化
     */
    @PostMapping(ApiConstants.Knowledge.VECTORIZE)
    public R<KnowledgeVO.VectorizeResultVO> vectorize(@RequestBody KnowledgeDTO.VectorizeDTO dto) {
        return R.ok(knowledgeService.vectorize(dto));
    }

    /**
     * 查询向量化状态
     */
    @PostMapping(ApiConstants.Knowledge.VECTORIZE_STATUS)
    public R<KnowledgeVO.VectorizeStatusVO> getVectorizeStatus(@RequestBody KnowledgeDTO.VectorizeStatusDTO dto) {
        return R.ok(knowledgeService.getVectorizeStatus(dto));
    }

    // ==================== 知识导入导出 ====================

    /**
     * 导入商品知识
     */
    @PostMapping(ApiConstants.Knowledge.PRODUCT_IMPORT)
    public R<KnowledgeVO.ImportResultVO> importProducts(@RequestBody KnowledgeDTO.ImportDTO dto) {
        return R.ok(knowledgeService.importKnowledge(dto));
    }

    /**
     * 导出商品知识
     */
    @PostMapping(ApiConstants.Knowledge.PRODUCT_EXPORT)
    public R<KnowledgeVO.ExportResultVO> exportProducts(@RequestBody KnowledgeDTO.ExportDTO dto) {
        return R.ok(knowledgeService.exportKnowledge(dto));
    }

    /**
     * 导入FAQ
     */
    @PostMapping(ApiConstants.Knowledge.FAQ_IMPORT)
    public R<KnowledgeVO.ImportResultVO> importFaqs(@RequestBody KnowledgeDTO.ImportDTO dto) {
        return R.ok(knowledgeService.importKnowledge(dto));
    }
}
