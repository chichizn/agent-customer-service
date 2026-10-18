package com.anjing.module.scene;

import com.anjing.module.chat.LlmService;
import com.anjing.module.scene.entity.Intent;
import com.anjing.module.scene.entity.Prompt;
import com.anjing.module.scene.entity.Rule;
import com.anjing.module.scene.repository.IntentRepository;
import com.anjing.module.scene.repository.PromptRepository;
import com.anjing.module.scene.repository.RuleRepository;
import com.anjing.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 场景配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneService {

    private final IntentRepository intentRepository;
    private final PromptRepository promptRepository;
    private final RuleRepository ruleRepository;
    private final LlmService llmService;

    @PostConstruct
    public void initData() {
        if (intentRepository.count() > 0) {
            log.info("场景数据已存在，跳过初始化");
            return;
        }
        log.info("初始化场景配置数据...");
        initMockData();
    }

    @Transactional
    protected void initMockData() {
        // 初始化意图数据
        Intent intent1 = new Intent();
        intent1.setIntentName("商品优惠查询");
        intent1.setIntentCode("PRODUCT_DISCOUNT");
        intent1.setSceneType("售前咨询");
        intent1.setTriggerKeywords("优惠,活动,打折,促销");
        intent1.setConfidenceThreshold(0.8);
        intent1.setHitCount(2345);
        intent1.setStatus("启用");
        intentRepository.save(intent1);

        Intent intent2 = new Intent();
        intent2.setIntentName("退换货咨询");
        intent2.setIntentCode("RETURN_EXCHANGE");
        intent2.setSceneType("售后服务");
        intent2.setTriggerKeywords("退货,换货,退款,售后");
        intent2.setConfidenceThreshold(0.85);
        intent2.setHitCount(1876);
        intent2.setStatus("启用");
        intentRepository.save(intent2);

        Intent intent3 = new Intent();
        intent3.setIntentName("物流查询");
        intent3.setIntentCode("LOGISTICS_QUERY");
        intent3.setSceneType("物流配送");
        intent3.setTriggerKeywords("快递,物流,发货,到货");
        intent3.setConfidenceThreshold(0.75);
        intent3.setHitCount(1234);
        intent3.setStatus("启用");
        intentRepository.save(intent3);

        // 初始化提示词数据
        Prompt prompt1 = new Prompt();
        prompt1.setPromptName("商品推荐话术");
        prompt1.setPromptCode("PRODUCT_RECOMMEND");
        prompt1.setPromptType("SYSTEM");
        prompt1.setPromptContent("你是一个专业的电商客服，请根据用户画像和购买历史，生成个性化商品推荐...");
        prompt1.setDescription("基于用户画像和购买历史，生成个性化商品推荐话术");
        prompt1.setUsageCount(3456);
        prompt1.setVersion("v2.1");
        prompt1.setStatus("启用");
        promptRepository.save(prompt1);

        Prompt prompt2 = new Prompt();
        prompt2.setPromptName("售后安抚话术");
        prompt2.setPromptCode("AFTER_SALE_COMFORT");
        prompt2.setPromptType("SYSTEM");
        prompt2.setPromptContent("你是一个专业的售后客服，请安抚用户情绪并提供解决方案...");
        prompt2.setDescription("处理用户投诉和售后问题时的安抚话术模板");
        prompt2.setUsageCount(2187);
        prompt2.setVersion("v1.5");
        prompt2.setStatus("启用");
        promptRepository.save(prompt2);

        // 初始化规则数据
        Rule rule1 = new Rule();
        rule1.setRuleName("高价值客户优先响应");
        rule1.setRuleCode("VIP_PRIORITY");
        rule1.setRuleType("路由规则");
        rule1.setDescription("VIP会员咨询时，自动提升响应优先级");
        rule1.setPriority(1);
        rule1.setTriggerCount(1256);
        rule1.setEnabled(true);
        rule1.setEffectiveTime("2025-01-01");
        rule1.setExpireTime("2025-12-31");
        ruleRepository.save(rule1);

        Rule rule2 = new Rule();
        rule2.setRuleName("敏感词过滤");
        rule2.setRuleCode("SENSITIVE_FILTER");
        rule2.setRuleType("安全规则");
        rule2.setDescription("回复内容自动过滤敏感词汇");
        rule2.setPriority(1);
        rule2.setTriggerCount(892);
        rule2.setEnabled(true);
        rule2.setEffectiveTime("2025-01-01");
        rule2.setExpireTime("2099-12-31");
        ruleRepository.save(rule2);

        Rule rule3 = new Rule();
        rule3.setRuleName("转人工阈值");
        rule3.setRuleCode("TRANSFER_THRESHOLD");
        rule3.setRuleType("转接规则");
        rule3.setDescription("当置信度低于0.6或连续3次未解决时，自动转人工");
        rule3.setPriority(2);
        rule3.setTriggerCount(567);
        rule3.setEnabled(true);
        rule3.setEffectiveTime("2025-01-01");
        rule3.setExpireTime("2025-12-31");
        ruleRepository.save(rule3);

        log.info("场景配置数据初始化完成");
    }

    // ==================== 意图管理 ====================

    public SceneVO.PageVO<SceneVO.IntentVO> listIntents(SceneDTO.QueryIntentDTO dto) {
        List<SceneVO.IntentVO> list = StreamSupport.stream(intentRepository.findAll().spliterator(), false)
                .map(this::intentToVO)
                .collect(Collectors.toList());

        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            String keyword = dto.getKeyword().toLowerCase();
            list.removeIf(i -> !i.getIntentName().toLowerCase().contains(keyword)
                    && !i.getIntentCode().toLowerCase().contains(keyword));
        }
        if (dto.getCategory() != null && !dto.getCategory().isEmpty()) {
            list.removeIf(i -> !dto.getCategory().equals(i.getCategory()));
        }
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            list.removeIf(i -> !dto.getStatus().equals(i.getStatus()));
        }

        return buildPage(list, dto.getPage(), dto.getSize());
    }

    @Transactional
    public SceneVO.IntentVO createIntent(SceneDTO.CreateIntentDTO dto) {
        Intent entity = new Intent();
        entity.setIntentName(dto.getIntentName());
        entity.setIntentCode(dto.getIntentCode());
        entity.setSceneType(dto.getCategory());
        entity.setTriggerKeywords(listToCommaSeparated(dto.getTriggerKeywords()));
        entity.setConfidenceThreshold(dto.getConfidenceThreshold());
        entity.setDescription(dto.getDescription());
        entity.setHitCount(0);
        entity.setStatus("启用");
        Intent saved = intentRepository.save(entity);
        log.info("创建意图成功: id={}", saved.getId());
        return intentToVO(saved);
    }

    @Transactional
    public SceneVO.IntentVO updateIntent(SceneDTO.UpdateIntentDTO dto) {
        Intent intent = intentRepository.findById(dto.getId()).orElse(null);
        if (intent != null) {
            if (dto.getIntentName() != null) intent.setIntentName(dto.getIntentName());
            if (dto.getIntentCode() != null) intent.setIntentCode(dto.getIntentCode());
            if (dto.getCategory() != null) intent.setSceneType(dto.getCategory());
            if (dto.getTriggerKeywords() != null) intent.setTriggerKeywords(listToCommaSeparated(dto.getTriggerKeywords()));
            if (dto.getConfidenceThreshold() != null) intent.setConfidenceThreshold(dto.getConfidenceThreshold());
            if (dto.getDescription() != null) intent.setDescription(dto.getDescription());
            if (dto.getStatus() != null) intent.setStatus(dto.getStatus());
            intent = intentRepository.save(intent);
            log.info("更新意图成功: id={}", intent.getId());
            return intentToVO(intent);
        }
        return null;
    }

    @Transactional
    public void deleteIntent(Long id) {
        intentRepository.deleteById(id);
        log.info("删除意图成功: id={}", id);
    }

    public SceneVO.IntentVO getIntentDetail(Long id) {
        return intentRepository.findById(id).map(this::intentToVO).orElse(null);
    }

    // ==================== 提示词模板 ====================

    public SceneVO.PageVO<SceneVO.PromptVO> listPrompts(SceneDTO.QueryPromptDTO dto) {
        List<SceneVO.PromptVO> list = StreamSupport.stream(promptRepository.findAll().spliterator(), false)
                .map(this::promptToVO)
                .collect(Collectors.toList());

        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            String keyword = dto.getKeyword().toLowerCase();
            list.removeIf(p -> !p.getPromptName().toLowerCase().contains(keyword)
                    && !p.getPromptCode().toLowerCase().contains(keyword));
        }
        if (dto.getPromptType() != null && !dto.getPromptType().isEmpty()) {
            list.removeIf(p -> !dto.getPromptType().equals(p.getPromptType()));
        }
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            list.removeIf(p -> !dto.getStatus().equals(p.getStatus()));
        }

        return buildPage(list, dto.getPage(), dto.getSize());
    }

    @Transactional
    public SceneVO.PromptVO createPrompt(SceneDTO.CreatePromptDTO dto) {
        Prompt entity = new Prompt();
        entity.setPromptName(dto.getPromptName());
        entity.setPromptCode(dto.getPromptCode());
        entity.setPromptType(dto.getPromptType());
        entity.setPromptContent(dto.getContent());
        entity.setDescription(dto.getDescription());
        entity.setVariables(variablesToJson(dto.getVariables()));
        entity.setUsageCount(0);
        entity.setVersion("v1.0");
        entity.setStatus("启用");
        Prompt saved = promptRepository.save(entity);
        log.info("创建提示词成功: id={}", saved.getId());
        return promptToVO(saved);
    }

    @Transactional
    public SceneVO.PromptVO updatePrompt(SceneDTO.UpdatePromptDTO dto) {
        Prompt prompt = promptRepository.findById(dto.getId()).orElse(null);
        if (prompt != null) {
            if (dto.getPromptName() != null) prompt.setPromptName(dto.getPromptName());
            if (dto.getPromptCode() != null) prompt.setPromptCode(dto.getPromptCode());
            if (dto.getPromptType() != null) prompt.setPromptType(dto.getPromptType());
            if (dto.getContent() != null) prompt.setPromptContent(dto.getContent());
            if (dto.getDescription() != null) prompt.setDescription(dto.getDescription());
            if (dto.getStatus() != null) prompt.setStatus(dto.getStatus());
            if (dto.getVariables() != null) prompt.setVariables(variablesToJson(dto.getVariables()));

            // 更新版本
            String currentVersion = prompt.getVersion();
            if (currentVersion != null && currentVersion.startsWith("v")) {
                try {
                    double v = Double.parseDouble(currentVersion.substring(1));
                    prompt.setVersion("v" + (v + 0.1));
                } catch (NumberFormatException e) {
                    prompt.setVersion("v1.1");
                }
            }

            prompt = promptRepository.save(prompt);
            log.info("更新提示词成功: id={}", prompt.getId());
            return promptToVO(prompt);
        }
        return null;
    }

    @Transactional
    public void deletePrompt(Long id) {
        promptRepository.deleteById(id);
        log.info("删除提示词成功: id={}", id);
    }

    public SceneVO.PromptVO getPromptDetail(Long id) {
        return promptRepository.findById(id).map(this::promptToVO).orElse(null);
    }

    public SceneVO.PromptTestResultVO testPrompt(SceneDTO.TestPromptDTO dto) {
        SceneVO.PromptVO prompt = getPromptDetail(dto.getPromptId());

        SceneVO.PromptTestResultVO result = new SceneVO.PromptTestResultVO();

        String renderedPrompt = prompt != null ? prompt.getContent() : "未找到提示词";
        if (dto.getVariables() != null) {
            for (Map.Entry<String, String> entry : dto.getVariables().entrySet()) {
                renderedPrompt = renderedPrompt.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        result.setRenderedPrompt(renderedPrompt);

        long startTime = System.currentTimeMillis();
        Map<String, Object> context = new HashMap<>();
        context.put("sceneType", "提示词测试");
        String testInput = dto.getInput() != null ? dto.getInput() : "你好";
        String aiReply = llmService.generateReply(testInput, context, null);
        long duration = System.currentTimeMillis() - startTime;

        result.setAiResponse(aiReply != null ? aiReply : "（LLM未启用）基于提示词模板的测试回复：" + testInput);
        result.setDuration(duration);
        result.setTokenUsage(aiReply != null ? aiReply.length() : 0);

        log.info("测试提示词完成: promptId={}, duration={}ms", dto.getPromptId(), duration);
        return result;
    }

    // ==================== 场景规则 ====================

    public SceneVO.PageVO<SceneVO.RuleVO> listRules(SceneDTO.QueryRuleDTO dto) {
        List<SceneVO.RuleVO> list = StreamSupport.stream(ruleRepository.findAll().spliterator(), false)
                .map(this::ruleToVO)
                .collect(Collectors.toList());

        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            String keyword = dto.getKeyword().toLowerCase();
            list.removeIf(r -> !r.getRuleName().toLowerCase().contains(keyword)
                    && !r.getRuleCode().toLowerCase().contains(keyword));
        }
        if (dto.getRuleType() != null && !dto.getRuleType().isEmpty()) {
            list.removeIf(r -> !dto.getRuleType().equals(r.getRuleType()));
        }
        if (dto.getEnabled() != null) {
            list.removeIf(r -> !dto.getEnabled().equals(r.getEnabled()));
        }

        // 按优先级排序
        list.sort(Comparator.comparing(SceneVO.RuleVO::getPriority));

        return buildPage(list, dto.getPage(), dto.getSize());
    }

    @Transactional
    public SceneVO.RuleVO createRule(SceneDTO.CreateRuleDTO dto) {
        Rule entity = new Rule();
        entity.setRuleName(dto.getRuleName());
        entity.setRuleCode(dto.getRuleCode());
        entity.setRuleType(dto.getRuleType());
        entity.setDescription(dto.getDescription());
        entity.setConditions(dto.getConditions());
        entity.setActions(dto.getActions());
        entity.setPriority(dto.getPriority() != null ? dto.getPriority() : 5);
        entity.setTriggerCount(0);
        entity.setEnabled(true);
        entity.setEffectiveTime(dto.getEffectiveTime());
        entity.setExpireTime(dto.getExpireTime());
        Rule saved = ruleRepository.save(entity);
        log.info("创建规则成功: id={}", saved.getId());
        return ruleToVO(saved);
    }

    @Transactional
    public SceneVO.RuleVO updateRule(SceneDTO.UpdateRuleDTO dto) {
        Rule rule = ruleRepository.findById(dto.getId()).orElse(null);
        if (rule != null) {
            if (dto.getRuleName() != null) rule.setRuleName(dto.getRuleName());
            if (dto.getRuleCode() != null) rule.setRuleCode(dto.getRuleCode());
            if (dto.getRuleType() != null) rule.setRuleType(dto.getRuleType());
            if (dto.getDescription() != null) rule.setDescription(dto.getDescription());
            if (dto.getConditions() != null) rule.setConditions(dto.getConditions());
            if (dto.getActions() != null) rule.setActions(dto.getActions());
            if (dto.getPriority() != null) rule.setPriority(dto.getPriority());
            if (dto.getEnabled() != null) rule.setEnabled(dto.getEnabled());
            if (dto.getEffectiveTime() != null) rule.setEffectiveTime(dto.getEffectiveTime());
            if (dto.getExpireTime() != null) rule.setExpireTime(dto.getExpireTime());
            rule = ruleRepository.save(rule);
            log.info("更新规则成功: id={}", rule.getId());
            return ruleToVO(rule);
        }
        return null;
    }

    @Transactional
    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
        log.info("删除规则成功: id={}", id);
    }

    public SceneVO.RuleVO getRuleDetail(Long id) {
        return ruleRepository.findById(id).map(this::ruleToVO).orElse(null);
    }

    @Transactional
    public void enableRule(Long id) {
        ruleRepository.findById(id).ifPresent(rule -> {
            rule.setEnabled(true);
            ruleRepository.save(rule);
            log.info("启用规则成功: id={}", id);
        });
    }

    @Transactional
    public void disableRule(Long id) {
        ruleRepository.findById(id).ifPresent(rule -> {
            rule.setEnabled(false);
            ruleRepository.save(rule);
            log.info("禁用规则成功: id={}", id);
        });
    }

    // ==================== Entity <-> VO 转换 ====================

    private SceneVO.IntentVO intentToVO(Intent entity) {
        SceneVO.IntentVO vo = new SceneVO.IntentVO();
        vo.setId(entity.getId());
        vo.setIntentName(entity.getIntentName());
        vo.setIntentCode(entity.getIntentCode());
        vo.setCategory(entity.getSceneType());
        vo.setTriggerKeywords(commaSeparatedToList(entity.getTriggerKeywords()));
        vo.setConfidenceThreshold(entity.getConfidenceThreshold());
        vo.setDescription(entity.getDescription());
        vo.setHitCount(entity.getHitCount());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private SceneVO.PromptVO promptToVO(Prompt entity) {
        SceneVO.PromptVO vo = new SceneVO.PromptVO();
        vo.setId(entity.getId());
        vo.setPromptName(entity.getPromptName());
        vo.setPromptCode(entity.getPromptCode());
        vo.setPromptType(entity.getPromptType());
        vo.setContent(entity.getPromptContent());
        vo.setDescription(entity.getDescription());
        vo.setVariables(jsonToVariables(entity.getVariables()));
        vo.setUsageCount(entity.getUsageCount());
        vo.setVersion(entity.getVersion());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private SceneVO.RuleVO ruleToVO(Rule entity) {
        SceneVO.RuleVO vo = new SceneVO.RuleVO();
        vo.setId(entity.getId());
        vo.setRuleName(entity.getRuleName());
        vo.setRuleCode(entity.getRuleCode());
        vo.setRuleType(entity.getRuleType());
        vo.setDescription(entity.getDescription());
        vo.setConditions(entity.getConditions());
        vo.setActions(entity.getActions());
        vo.setPriority(entity.getPriority());
        vo.setTriggerCount(entity.getTriggerCount());
        vo.setEnabled(entity.getEnabled());
        vo.setEffectiveTime(entity.getEffectiveTime());
        vo.setExpireTime(entity.getExpireTime());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String listToCommaSeparated(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }

    private List<String> commaSeparatedToList(String str) {
        if (str == null || str.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(str.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    private String variablesToJson(List<SceneDTO.PromptVariableDTO> variables) {
        if (variables == null || variables.isEmpty()) return null;
        List<SceneVO.PromptVariableVO> voList = new ArrayList<>();
        for (SceneDTO.PromptVariableDTO dto : variables) {
            SceneVO.PromptVariableVO vo = new SceneVO.PromptVariableVO();
            vo.setName(dto.getName());
            vo.setDescription(dto.getDescription());
            vo.setDefaultValue(dto.getDefaultValue());
            vo.setRequired(dto.getRequired());
            voList.add(vo);
        }
        return JsonUtils.toJson(voList);
    }

    private List<SceneVO.PromptVariableVO> jsonToVariables(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        try {
            return JsonUtils.fromJson(json, new TypeReference<List<SceneVO.PromptVariableVO>>() {});
        } catch (Exception e) {
            log.warn("解析 variables JSON 失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    // ==================== 辅助方法 ====================

    private <T> SceneVO.PageVO<T> buildPage(List<T> list, Integer page, Integer size) {
        if (page == null) page = 1;
        if (size == null) size = 20;

        SceneVO.PageVO<T> pageVO = new SceneVO.PageVO<>();
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setTotal((long) list.size());

        int start = (page - 1) * size;
        int end = Math.min(start + size, list.size());

        if (start >= list.size()) {
            pageVO.setRecords(Collections.emptyList());
        } else {
            pageVO.setRecords(list.subList(start, end));
        }

        return pageVO;
    }
}
