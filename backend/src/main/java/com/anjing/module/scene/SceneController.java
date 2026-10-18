package com.anjing.module.scene;

import com.anjing.model.constants.ApiConstants;
import com.anjing.model.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 场景配置控制器
 * 管理意图、提示词模板、场景规则
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    // ==================== 意图管理 ====================

    @PostMapping(ApiConstants.Scene.INTENT_LIST)
    public R<SceneVO.PageVO<SceneVO.IntentVO>> listIntents(@RequestBody SceneDTO.QueryIntentDTO dto) {
        log.info("查询意图列表: {}", dto);
        return R.ok(sceneService.listIntents(dto));
    }

    @PostMapping(ApiConstants.Scene.INTENT_CREATE)
    public R<SceneVO.IntentVO> createIntent(@RequestBody SceneDTO.CreateIntentDTO dto) {
        log.info("创建意图: {}", dto);
        return R.ok(sceneService.createIntent(dto));
    }

    @PostMapping(ApiConstants.Scene.INTENT_UPDATE)
    public R<SceneVO.IntentVO> updateIntent(@RequestBody SceneDTO.UpdateIntentDTO dto) {
        log.info("更新意图: {}", dto);
        return R.ok(sceneService.updateIntent(dto));
    }

    @PostMapping(ApiConstants.Scene.INTENT_DELETE)
    public R<Void> deleteIntent(@RequestBody SceneDTO.IdDTO dto) {
        log.info("删除意图: {}", dto);
        sceneService.deleteIntent(dto.getId());
        return R.ok();
    }

    @PostMapping(ApiConstants.Scene.INTENT_DETAIL)
    public R<SceneVO.IntentVO> getIntentDetail(@RequestBody SceneDTO.IdDTO dto) {
        log.info("获取意图详情: {}", dto);
        return R.ok(sceneService.getIntentDetail(dto.getId()));
    }

    // ==================== 提示词模板 ====================

    @PostMapping(ApiConstants.Scene.PROMPT_LIST)
    public R<SceneVO.PageVO<SceneVO.PromptVO>> listPrompts(@RequestBody SceneDTO.QueryPromptDTO dto) {
        log.info("查询提示词列表: {}", dto);
        return R.ok(sceneService.listPrompts(dto));
    }

    @PostMapping(ApiConstants.Scene.PROMPT_CREATE)
    public R<SceneVO.PromptVO> createPrompt(@RequestBody SceneDTO.CreatePromptDTO dto) {
        log.info("创建提示词: {}", dto);
        return R.ok(sceneService.createPrompt(dto));
    }

    @PostMapping(ApiConstants.Scene.PROMPT_UPDATE)
    public R<SceneVO.PromptVO> updatePrompt(@RequestBody SceneDTO.UpdatePromptDTO dto) {
        log.info("更新提示词: {}", dto);
        return R.ok(sceneService.updatePrompt(dto));
    }

    @PostMapping(ApiConstants.Scene.PROMPT_DELETE)
    public R<Void> deletePrompt(@RequestBody SceneDTO.IdDTO dto) {
        log.info("删除提示词: {}", dto);
        sceneService.deletePrompt(dto.getId());
        return R.ok();
    }

    @PostMapping(ApiConstants.Scene.PROMPT_DETAIL)
    public R<SceneVO.PromptVO> getPromptDetail(@RequestBody SceneDTO.IdDTO dto) {
        log.info("获取提示词详情: {}", dto);
        return R.ok(sceneService.getPromptDetail(dto.getId()));
    }

    @PostMapping(ApiConstants.Scene.PROMPT_TEST)
    public R<SceneVO.PromptTestResultVO> testPrompt(@RequestBody SceneDTO.TestPromptDTO dto) {
        log.info("测试提示词: {}", dto);
        return R.ok(sceneService.testPrompt(dto));
    }

    // ==================== 场景规则 ====================

    @PostMapping(ApiConstants.Scene.RULE_LIST)
    public R<SceneVO.PageVO<SceneVO.RuleVO>> listRules(@RequestBody SceneDTO.QueryRuleDTO dto) {
        log.info("查询规则列表: {}", dto);
        return R.ok(sceneService.listRules(dto));
    }

    @PostMapping(ApiConstants.Scene.RULE_CREATE)
    public R<SceneVO.RuleVO> createRule(@RequestBody SceneDTO.CreateRuleDTO dto) {
        log.info("创建规则: {}", dto);
        return R.ok(sceneService.createRule(dto));
    }

    @PostMapping(ApiConstants.Scene.RULE_UPDATE)
    public R<SceneVO.RuleVO> updateRule(@RequestBody SceneDTO.UpdateRuleDTO dto) {
        log.info("更新规则: {}", dto);
        return R.ok(sceneService.updateRule(dto));
    }

    @PostMapping(ApiConstants.Scene.RULE_DELETE)
    public R<Void> deleteRule(@RequestBody SceneDTO.IdDTO dto) {
        log.info("删除规则: {}", dto);
        sceneService.deleteRule(dto.getId());
        return R.ok();
    }

    @PostMapping(ApiConstants.Scene.RULE_DETAIL)
    public R<SceneVO.RuleVO> getRuleDetail(@RequestBody SceneDTO.IdDTO dto) {
        log.info("获取规则详情: {}", dto);
        return R.ok(sceneService.getRuleDetail(dto.getId()));
    }

    @PostMapping(ApiConstants.Scene.RULE_ENABLE)
    public R<Void> enableRule(@RequestBody SceneDTO.IdDTO dto) {
        log.info("启用规则: {}", dto);
        sceneService.enableRule(dto.getId());
        return R.ok();
    }

    @PostMapping(ApiConstants.Scene.RULE_DISABLE)
    public R<Void> disableRule(@RequestBody SceneDTO.IdDTO dto) {
        log.info("禁用规则: {}", dto);
        sceneService.disableRule(dto.getId());
        return R.ok();
    }
}

