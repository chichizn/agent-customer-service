package com.anjing.module.knowledge;

import com.anjing.module.knowledge.entity.*;
import com.anjing.module.knowledge.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识中心服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final ProductRepository productRepository;
    private final ActivityRepository activityRepository;
    private final FaqRepository faqRepository;
    private final IndustryRepository industryRepository;
    private final SolutionRepository solutionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initMockData() {
        if (productRepository.count() > 0) {
            log.info("知识库已有数据，跳过初始化");
            return;
        }
        log.info("知识库为空，开始初始化模拟数据");
        initProducts();
        initActivities();
        initFaqs();
        initIndustries();
        initSolutions();
    }

    private void initProducts() {
        saveProduct("夏季清凉短袖T恤", "TSH-001", "服装", "AJ时尚", 99.0, "纯棉透气短袖T恤，经典百搭", "纯棉透气,经典百搭,多色可选");
        saveProduct("运动休闲裤", "PNT-002", "服装", "AJ时尚", 149.0, "弹力运动面料，舒适修身", "弹力面料,舒适修身,速干");
        saveProduct("透气网面跑步鞋", "SHO-003", "鞋类", "AJ运动", 299.0, "轻量缓震跑步鞋，网面透气", "轻量设计,缓震科技,网面透气");
        saveProduct("防晒遮阳帽", "HAT-004", "配饰", "AJ户外", 49.0, "UPF50+防晒，可折叠收纳", "UPF50+,可折叠,轻便");
        saveProduct("智能运动手表", "WAT-005", "数码", "AJ智能", 599.0, "运动心率监测，GPS定位，7天续航", "心率监测,GPS定位,防水50米");
        saveProduct("真皮单肩包", "BAG-006", "箱包", "AJ精品", 399.0, "头层牛皮，大容量通勤包", "头层牛皮,大容量,经典款");
    }

    private void saveProduct(String name, String code, String category, String brand, double price, String desc, String features) {
        Product p = new Product();
        p.setProductName(name);
        p.setProductCode(code);
        p.setCategory(category);
        p.setBrand(brand);
        p.setPrice(price);
        p.setDescription(desc);
        p.setFeatures(features);
        p.setViewCount((int) (Math.random() * 5000));
        p.setStatus("上架");
        p.setVectorized(true);
        productRepository.save(p);
    }

    private void initActivities() {
        saveActivity("新人首单立减", "NEW-USER", "满减", "新用户首次下单立减20元，无门槛", "满0减20，限首单", 8900);
        saveActivity("满300减50", "FULL-300", "满减", "全场商品满300减50，满600减120", "满300减50，满600减120，上不封顶", 5600);
        saveActivity("会员积分翻倍", "VIP-POINTS", "积分", "会员日消费积分双倍，每周五", "会员日双倍积分，可兑换优惠券", 3200);
    }

    private void saveActivity(String name, String code, String type, String desc, String rules, int usage) {
        Activity a = new Activity();
        a.setActivityName(name);
        a.setActivityCode(code);
        a.setActivityType(type);
        a.setDescription(desc);
        a.setRules(rules);
        a.setDiscountRate(0.85);
        a.setUsageCount(usage);
        a.setStatus("进行中");
        a.setVectorized(true);
        activityRepository.save(a);
    }

    private void initFaqs() {
        saveFaq("这款商品有什么优惠活动吗？", "当前有以下优惠活动：1. 新人首单立减20元 2. 满300减50 3. 会员日积分翻倍。您可以在结算页面查看可用优惠。", "售前咨询", "优惠,活动,打折,促销", 8900);
        saveFaq("商品的尺码怎么选择？", "建议参考商品详情页的尺码表：S码适合155-160cm，M码160-165cm，L码165-170cm，XL码170-175cm。如果您平时穿M码，建议选M码。", "售前咨询", "尺码,大小,尺寸,选码", 6500);
        saveFaq("有没有类似款式推荐？", "您可以在商品详情页底部查看【相似推荐】，或告诉我您的偏好（颜色、价位、风格），我来帮您推荐。", "售前咨询", "推荐,类似,相似", 3200);
        saveFaq("商品什么时候发货？", "下单后1-2个工作日内发货，大促期间可能延迟至3天。您可以在订单详情页查看预计发货时间。", "售前咨询", "发货,时间,什么时候", 7200);

        saveFaq("如何申请退换货？", "退换货步骤：1. 进入【我的订单】 2. 找到对应订单点击【申请售后】 3. 选择退货/换货原因 4. 提交申请等待审核。7天无理由退货，审核通过后3-5个工作日退款到账。", "售后服务", "退货,换货,退款,售后", 12300);
        saveFaq("退款多久到账？", "退款到账时间：1. 支付宝/微信：1-3个工作日 2. 银行卡：3-5个工作日 3. 信用卡：5-7个工作日。具体以银行处理速度为准。", "售后服务", "退款,到账,多久", 8700);
        saveFaq("商品质量问题怎么处理？", "如遇质量问题，请您拍照留证后申请售后，我们提供免费退换货服务。15天内质量问题可直接退货退款，超过15天可联系我们协商处理。", "售后服务", "质量,问题,瑕疵,破损", 5400);

        saveFaq("我的快递到哪里了？", "您可以在订单详情页点击【查看物流】实时跟踪快递位置。一般发货后2-3天送达，偏远地区3-5天。", "物流配送", "快递,到哪,物流,查询", 15600);
        saveFaq("可以修改收货地址吗？", "如果订单还未发货，您可以在订单详情页修改地址。如果已发货，请联系客服协助处理，可能需要快递拦截。", "物流配送", "修改,地址,收货", 4300);
        saveFaq("支持哪些快递公司？", "我们与顺丰、中通、圆通、韵达、申通合作。默认随机分配，VIP会员可指定快递公司。部分大件商品走德邦物流。", "物流配送", "快递,公司,顺丰,中通", 2800);

        saveFaq("支持哪些支付方式？", "支持以下支付方式：1. 支付宝 2. 微信支付 3. 银行卡 4. 信用卡 5. 花呗/白条分期。大额订单推荐使用分期支付。", "支付问题", "支付,方式,付款,怎么付", 6100);
        saveFaq("可以开发票吗？", "可以，下单时选择【需要发票】。支持电子发票（实时开具）和纸质发票（7个工作日内邮寄）。如需修改发票信息，请在发货前联系客服。", "支付问题", "发票,开票,报销", 3900);
    }

    private void saveFaq(String question, String answer, String category, String tags, int hitCount) {
        Faq faq = new Faq();
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setCategory(category);
        faq.setTags(tags);
        faq.setHitCount(hitCount);
        faq.setPriority(1);
        faq.setStatus("启用");
        faq.setVectorized(true);
        faqRepository.save(faq);
    }

    private void initIndustries() {
        Industry industry1 = new Industry();
        industry1.setTitle("2025年智能家居行业趋势报告");
        industry1.setIndustryType("智能家居");
        industry1.setContent("智能家居市场持续增长，AI技术驱动产品升级...");
        industry1.setSource("艾瑞咨询");
        industry1.setKeywords(listToCommaString(Arrays.asList("智能家居", "AI", "物联网")));
        industry1.setViewCount(567);
        industry1.setStatus("启用");
        industry1.setVectorized(false);
        industryRepository.save(industry1);
    }

    private void initSolutions() {
        List<KnowledgeVO.SolutionStepVO> steps = new ArrayList<>();
        KnowledgeVO.SolutionStepVO step1 = new KnowledgeVO.SolutionStepVO();
        step1.setStepOrder(1);
        step1.setStepName("确认订单信息");
        step1.setStepAction("查询用户订单，确认商品状态");
        step1.setExpectedResult("获取订单详情");
        steps.add(step1);
        KnowledgeVO.SolutionStepVO step2 = new KnowledgeVO.SolutionStepVO();
        step2.setStepOrder(2);
        step2.setStepName("了解退货原因");
        step2.setStepAction("询问用户退货原因");
        step2.setExpectedResult("获取退货原因");
        steps.add(step2);
        KnowledgeVO.SolutionStepVO step3 = new KnowledgeVO.SolutionStepVO();
        step3.setStepOrder(3);
        step3.setStepName("指导提交申请");
        step3.setStepAction("引导用户在APP提交退货申请");
        step3.setExpectedResult("用户成功提交申请");
        steps.add(step3);

        Solution solution1 = new Solution();
        solution1.setSolutionName("退货退款标准流程");
        solution1.setSolutionCode("REFUND-STANDARD");
        solution1.setSceneType("售后服务");
        solution1.setDescription("标准退货退款处理流程");
        solution1.setTriggerCondition("用户意图=退货退款");
        solution1.setSteps(stepsToJson(steps));
        solution1.setExpectedOutcome("用户成功提交退货申请");
        solution1.setUsageCount(3456);
        solution1.setSuccessRate(0.92);
        solution1.setStatus("启用");
        solution1.setVectorized(true);
        solutionRepository.save(solution1);
    }

    // ==================== 转换辅助方法 ====================

    private String listToCommaString(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }

    private List<String> commaStringToList(String s) {
        if (s == null || s.isEmpty()) return Collections.emptyList();
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).collect(Collectors.toList());
    }

    private String stepsToJson(List<KnowledgeVO.SolutionStepVO> steps) {
        if (steps == null || steps.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception e) {
            log.warn("序列化steps失败", e);
            return null;
        }
    }

    private List<KnowledgeVO.SolutionStepVO> jsonToSteps(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<KnowledgeVO.SolutionStepVO>>() {});
        } catch (Exception e) {
            log.warn("反序列化steps失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    private KnowledgeVO.ProductVO toProductVO(Product entity) {
        if (entity == null) return null;
        KnowledgeVO.ProductVO vo = new KnowledgeVO.ProductVO();
        vo.setId(entity.getId());
        vo.setProductName(entity.getProductName());
        vo.setProductCode(entity.getProductCode());
        vo.setCategory(entity.getCategory());
        vo.setBrand(entity.getBrand());
        vo.setPrice(entity.getPrice());
        vo.setDescription(entity.getDescription());
        vo.setSpecifications(entity.getSpecifications());
        vo.setFeatures(entity.getFeatures());
        vo.setImageUrl(entity.getImageUrl());
        vo.setTags(commaStringToList(entity.getTags()));
        vo.setViewCount(entity.getViewCount());
        vo.setStatus(entity.getStatus());
        vo.setVectorized(entity.getVectorized());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private KnowledgeVO.ActivityVO toActivityVO(Activity entity) {
        if (entity == null) return null;
        KnowledgeVO.ActivityVO vo = new KnowledgeVO.ActivityVO();
        vo.setId(entity.getId());
        vo.setActivityName(entity.getActivityName());
        vo.setActivityCode(entity.getActivityCode());
        vo.setActivityType(entity.getActivityType());
        vo.setDescription(entity.getDescription());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setRules(entity.getRules());
        vo.setApplicableProducts(commaStringToList(entity.getApplicableProducts()));
        vo.setDiscountRate(entity.getDiscountRate());
        vo.setUsageCount(entity.getUsageCount());
        vo.setStatus(entity.getStatus());
        vo.setVectorized(entity.getVectorized());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private KnowledgeVO.FaqVO toFaqVO(Faq entity) {
        if (entity == null) return null;
        KnowledgeVO.FaqVO vo = new KnowledgeVO.FaqVO();
        vo.setId(entity.getId());
        vo.setQuestion(entity.getQuestion());
        vo.setAnswer(entity.getAnswer());
        vo.setCategory(entity.getCategory());
        vo.setRelatedQuestions(commaStringToList(entity.getRelatedQuestions()));
        vo.setTags(commaStringToList(entity.getTags()));
        vo.setHitCount(entity.getHitCount());
        vo.setPriority(entity.getPriority());
        vo.setStatus(entity.getStatus());
        vo.setVectorized(entity.getVectorized());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private KnowledgeVO.IndustryVO toIndustryVO(Industry entity) {
        if (entity == null) return null;
        KnowledgeVO.IndustryVO vo = new KnowledgeVO.IndustryVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setIndustryType(entity.getIndustryType());
        vo.setContent(entity.getContent());
        vo.setSource(entity.getSource());
        vo.setKeywords(commaStringToList(entity.getKeywords()));
        vo.setViewCount(entity.getViewCount());
        vo.setStatus(entity.getStatus());
        vo.setVectorized(entity.getVectorized());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private KnowledgeVO.SolutionVO toSolutionVO(Solution entity) {
        if (entity == null) return null;
        KnowledgeVO.SolutionVO vo = new KnowledgeVO.SolutionVO();
        vo.setId(entity.getId());
        vo.setSolutionName(entity.getSolutionName());
        vo.setSolutionCode(entity.getSolutionCode());
        vo.setSceneType(entity.getSceneType());
        vo.setDescription(entity.getDescription());
        vo.setTriggerCondition(entity.getTriggerCondition());
        vo.setSteps(jsonToSteps(entity.getSteps()));
        vo.setExpectedOutcome(entity.getExpectedOutcome());
        vo.setUsageCount(entity.getUsageCount());
        vo.setSuccessRate(entity.getSuccessRate());
        vo.setStatus(entity.getStatus());
        vo.setVectorized(entity.getVectorized());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    // ==================== 知识总览 ====================

    public KnowledgeVO.KnowledgeOverviewVO getOverview() {
        KnowledgeVO.KnowledgeOverviewVO overview = new KnowledgeVO.KnowledgeOverviewVO();
        overview.setProductCount(productRepository.count());
        overview.setActivityCount(activityRepository.count());
        overview.setFaqCount(faqRepository.count());
        overview.setIndustryCount(industryRepository.count());
        overview.setSolutionCount(solutionRepository.count());
        overview.setTotalCount(overview.getProductCount() + overview.getActivityCount()
                + overview.getFaqCount() + overview.getIndustryCount()
                + overview.getSolutionCount());

        long vectorizedCount = productRepository.countByVectorized(true)
                + activityRepository.countByVectorized(true)
                + faqRepository.countByVectorized(true)
                + industryRepository.countByVectorized(true)
                + solutionRepository.countByVectorized(true);
        overview.setVectorizedCount(vectorizedCount);
        overview.setLastUpdateTime(LocalDateTime.now().toString());
        return overview;
    }

    // ==================== 商品知识 ====================

    public KnowledgeVO.PageVO<KnowledgeVO.ProductVO> listProducts(KnowledgeDTO.QueryProductDTO dto) {
        List<KnowledgeVO.ProductVO> list = productRepository.findAll().stream()
                .map(this::toProductVO)
                .collect(Collectors.toList());

        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            String keyword = dto.getKeyword().toLowerCase();
            list.removeIf(p -> !p.getProductName().toLowerCase().contains(keyword)
                    && !p.getProductCode().toLowerCase().contains(keyword));
        }
        if (dto.getCategory() != null && !dto.getCategory().isEmpty()) {
            list.removeIf(p -> !dto.getCategory().equals(p.getCategory()));
        }
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            list.removeIf(p -> !dto.getStatus().equals(p.getStatus()));
        }

        return buildPage(list, dto.getPage(), dto.getSize());
    }

    @Transactional
    public KnowledgeVO.ProductVO createProduct(KnowledgeDTO.CreateProductDTO dto) {
        Product product = new Product();
        product.setProductName(dto.getProductName());
        product.setProductCode(dto.getProductCode());
        product.setCategory(dto.getCategory());
        product.setBrand(dto.getBrand());
        product.setPrice(dto.getPrice());
        product.setDescription(dto.getDescription());
        product.setSpecifications(dto.getSpecifications());
        product.setFeatures(dto.getFeatures());
        product.setImageUrl(dto.getImageUrl());
        product.setTags(listToCommaString(dto.getTags()));
        product.setViewCount(0);
        product.setStatus("草稿");
        product.setVectorized(false);
        product = productRepository.save(product);
        log.info("创建商品知识成功: id={}", product.getId());
        return toProductVO(product);
    }

    @Transactional
    public KnowledgeVO.ProductVO updateProduct(KnowledgeDTO.UpdateProductDTO dto) {
        Product product = productRepository.findById(dto.getId()).orElse(null);
        if (product != null) {
            if (dto.getProductName() != null) product.setProductName(dto.getProductName());
            if (dto.getProductCode() != null) product.setProductCode(dto.getProductCode());
            if (dto.getCategory() != null) product.setCategory(dto.getCategory());
            if (dto.getBrand() != null) product.setBrand(dto.getBrand());
            if (dto.getPrice() != null) product.setPrice(dto.getPrice());
            if (dto.getDescription() != null) product.setDescription(dto.getDescription());
            if (dto.getSpecifications() != null) product.setSpecifications(dto.getSpecifications());
            if (dto.getFeatures() != null) product.setFeatures(dto.getFeatures());
            if (dto.getImageUrl() != null) product.setImageUrl(dto.getImageUrl());
            if (dto.getTags() != null) product.setTags(listToCommaString(dto.getTags()));
            if (dto.getStatus() != null) product.setStatus(dto.getStatus());
            product = productRepository.save(product);
            log.info("更新商品知识成功: id={}", product.getId());
            return toProductVO(product);
        }
        return null;
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        log.info("删除商品知识成功: id={}", id);
    }

    @Transactional
    public KnowledgeVO.ProductVO getProductDetail(Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            product.setViewCount(product.getViewCount() + 1);
            product = productRepository.save(product);
            return toProductVO(product);
        }
        return null;
    }

    // ==================== 活动知识 ====================

    public KnowledgeVO.PageVO<KnowledgeVO.ActivityVO> listActivities(KnowledgeDTO.QueryActivityDTO dto) {
        List<KnowledgeVO.ActivityVO> list = activityRepository.findAll().stream()
                .map(this::toActivityVO)
                .collect(Collectors.toList());

        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            String keyword = dto.getKeyword().toLowerCase();
            list.removeIf(a -> !a.getActivityName().toLowerCase().contains(keyword)
                    && !a.getActivityCode().toLowerCase().contains(keyword));
        }
        if (dto.getActivityType() != null && !dto.getActivityType().isEmpty()) {
            list.removeIf(a -> !dto.getActivityType().equals(a.getActivityType()));
        }
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            list.removeIf(a -> !dto.getStatus().equals(a.getStatus()));
        }

        return buildPage(list, dto.getPage(), dto.getSize());
    }

    @Transactional
    public KnowledgeVO.ActivityVO createActivity(KnowledgeDTO.CreateActivityDTO dto) {
        Activity activity = new Activity();
        activity.setActivityName(dto.getActivityName());
        activity.setActivityCode(dto.getActivityCode());
        activity.setActivityType(dto.getActivityType());
        activity.setDescription(dto.getDescription());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setRules(dto.getRules());
        activity.setApplicableProducts(listToCommaString(dto.getApplicableProducts()));
        activity.setDiscountRate(dto.getDiscountRate());
        activity.setUsageCount(0);
        activity.setStatus("未开始");
        activity.setVectorized(false);
        activity = activityRepository.save(activity);
        log.info("创建活动知识成功: id={}", activity.getId());
        return toActivityVO(activity);
    }

    @Transactional
    public KnowledgeVO.ActivityVO updateActivity(KnowledgeDTO.UpdateActivityDTO dto) {
        Activity activity = activityRepository.findById(dto.getId()).orElse(null);
        if (activity != null) {
            if (dto.getActivityName() != null) activity.setActivityName(dto.getActivityName());
            if (dto.getActivityCode() != null) activity.setActivityCode(dto.getActivityCode());
            if (dto.getActivityType() != null) activity.setActivityType(dto.getActivityType());
            if (dto.getDescription() != null) activity.setDescription(dto.getDescription());
            if (dto.getStartTime() != null) activity.setStartTime(dto.getStartTime());
            if (dto.getEndTime() != null) activity.setEndTime(dto.getEndTime());
            if (dto.getRules() != null) activity.setRules(dto.getRules());
            if (dto.getApplicableProducts() != null) activity.setApplicableProducts(listToCommaString(dto.getApplicableProducts()));
            if (dto.getDiscountRate() != null) activity.setDiscountRate(dto.getDiscountRate());
            if (dto.getStatus() != null) activity.setStatus(dto.getStatus());
            activity = activityRepository.save(activity);
            log.info("更新活动知识成功: id={}", activity.getId());
            return toActivityVO(activity);
        }
        return null;
    }

    @Transactional
    public void deleteActivity(Long id) {
        activityRepository.deleteById(id);
        log.info("删除活动知识成功: id={}", id);
    }

    public KnowledgeVO.ActivityVO getActivityDetail(Long id) {
        return toActivityVO(activityRepository.findById(id).orElse(null));
    }

    // ==================== FAQ问答 ====================

    public KnowledgeVO.PageVO<KnowledgeVO.FaqVO> listFaqs(KnowledgeDTO.QueryFaqDTO dto) {
        List<KnowledgeVO.FaqVO> list = faqRepository.findAll().stream()
                .map(this::toFaqVO)
                .collect(Collectors.toList());

        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            String keyword = dto.getKeyword().toLowerCase();
            list.removeIf(f -> !f.getQuestion().toLowerCase().contains(keyword)
                    && !f.getAnswer().toLowerCase().contains(keyword));
        }
        if (dto.getCategory() != null && !dto.getCategory().isEmpty()) {
            list.removeIf(f -> !dto.getCategory().equals(f.getCategory()));
        }
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            list.removeIf(f -> !dto.getStatus().equals(f.getStatus()));
        }

        list.sort((a, b) -> {
            int priorityCompare = a.getPriority().compareTo(b.getPriority());
            if (priorityCompare != 0) return priorityCompare;
            return b.getHitCount().compareTo(a.getHitCount());
        });

        return buildPage(list, dto.getPage(), dto.getSize());
    }

    @Transactional
    public KnowledgeVO.FaqVO createFaq(KnowledgeDTO.CreateFaqDTO dto) {
        Faq faq = new Faq();
        faq.setQuestion(dto.getQuestion());
        faq.setAnswer(dto.getAnswer());
        faq.setCategory(dto.getCategory());
        faq.setRelatedQuestions(listToCommaString(dto.getRelatedQuestions()));
        faq.setTags(listToCommaString(dto.getTags()));
        faq.setPriority(dto.getPriority() != null ? dto.getPriority() : 5);
        faq.setHitCount(0);
        faq.setStatus("草稿");
        faq.setVectorized(false);
        faq = faqRepository.save(faq);
        log.info("创建FAQ成功: id={}", faq.getId());
        return toFaqVO(faq);
    }

    @Transactional
    public KnowledgeVO.FaqVO updateFaq(KnowledgeDTO.UpdateFaqDTO dto) {
        Faq faq = faqRepository.findById(dto.getId()).orElse(null);
        if (faq != null) {
            if (dto.getQuestion() != null) faq.setQuestion(dto.getQuestion());
            if (dto.getAnswer() != null) faq.setAnswer(dto.getAnswer());
            if (dto.getCategory() != null) faq.setCategory(dto.getCategory());
            if (dto.getRelatedQuestions() != null) faq.setRelatedQuestions(listToCommaString(dto.getRelatedQuestions()));
            if (dto.getTags() != null) faq.setTags(listToCommaString(dto.getTags()));
            if (dto.getPriority() != null) faq.setPriority(dto.getPriority());
            if (dto.getStatus() != null) faq.setStatus(dto.getStatus());
            faq = faqRepository.save(faq);
            log.info("更新FAQ成功: id={}", faq.getId());
            return toFaqVO(faq);
        }
        return null;
    }

    @Transactional
    public void deleteFaq(Long id) {
        faqRepository.deleteById(id);
        log.info("删除FAQ成功: id={}", id);
    }

    @Transactional
    public KnowledgeVO.FaqVO getFaqDetail(Long id) {
        Faq faq = faqRepository.findById(id).orElse(null);
        if (faq != null) {
            faq.setHitCount(faq.getHitCount() + 1);
            faq = faqRepository.save(faq);
            return toFaqVO(faq);
        }
        return null;
    }

    // ==================== 行业知识 ====================

    public KnowledgeVO.PageVO<KnowledgeVO.IndustryVO> listIndustries(KnowledgeDTO.QueryIndustryDTO dto) {
        List<KnowledgeVO.IndustryVO> list = industryRepository.findAll().stream()
                .map(this::toIndustryVO)
                .collect(Collectors.toList());

        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            String keyword = dto.getKeyword().toLowerCase();
            list.removeIf(i -> !i.getTitle().toLowerCase().contains(keyword));
        }
        if (dto.getIndustryType() != null && !dto.getIndustryType().isEmpty()) {
            list.removeIf(i -> !dto.getIndustryType().equals(i.getIndustryType()));
        }
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            list.removeIf(i -> !dto.getStatus().equals(i.getStatus()));
        }

        return buildPage(list, dto.getPage(), dto.getSize());
    }

    @Transactional
    public KnowledgeVO.IndustryVO createIndustry(KnowledgeDTO.CreateIndustryDTO dto) {
        Industry industry = new Industry();
        industry.setTitle(dto.getTitle());
        industry.setIndustryType(dto.getIndustryType());
        industry.setContent(dto.getContent());
        industry.setSource(dto.getSource());
        industry.setKeywords(listToCommaString(dto.getKeywords()));
        industry.setViewCount(0);
        industry.setStatus("草稿");
        industry.setVectorized(false);
        industry = industryRepository.save(industry);
        log.info("创建行业知识成功: id={}", industry.getId());
        return toIndustryVO(industry);
    }

    @Transactional
    public KnowledgeVO.IndustryVO updateIndustry(KnowledgeDTO.UpdateIndustryDTO dto) {
        Industry industry = industryRepository.findById(dto.getId()).orElse(null);
        if (industry != null) {
            if (dto.getTitle() != null) industry.setTitle(dto.getTitle());
            if (dto.getIndustryType() != null) industry.setIndustryType(dto.getIndustryType());
            if (dto.getContent() != null) industry.setContent(dto.getContent());
            if (dto.getSource() != null) industry.setSource(dto.getSource());
            if (dto.getKeywords() != null) industry.setKeywords(listToCommaString(dto.getKeywords()));
            if (dto.getStatus() != null) industry.setStatus(dto.getStatus());
            industry = industryRepository.save(industry);
            log.info("更新行业知识成功: id={}", industry.getId());
            return toIndustryVO(industry);
        }
        return null;
    }

    @Transactional
    public void deleteIndustry(Long id) {
        industryRepository.deleteById(id);
        log.info("删除行业知识成功: id={}", id);
    }

    // ==================== 场景解决方案 ====================

    public KnowledgeVO.PageVO<KnowledgeVO.SolutionVO> listSolutions(KnowledgeDTO.QuerySolutionDTO dto) {
        List<KnowledgeVO.SolutionVO> list = solutionRepository.findAll().stream()
                .map(this::toSolutionVO)
                .collect(Collectors.toList());

        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            String keyword = dto.getKeyword().toLowerCase();
            list.removeIf(s -> !s.getSolutionName().toLowerCase().contains(keyword)
                    && !s.getSolutionCode().toLowerCase().contains(keyword));
        }
        if (dto.getSceneType() != null && !dto.getSceneType().isEmpty()) {
            list.removeIf(s -> !dto.getSceneType().equals(s.getSceneType()));
        }
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            list.removeIf(s -> !dto.getStatus().equals(s.getStatus()));
        }

        return buildPage(list, dto.getPage(), dto.getSize());
    }

    @Transactional
    public KnowledgeVO.SolutionVO createSolution(KnowledgeDTO.CreateSolutionDTO dto) {
        Solution solution = new Solution();
        solution.setSolutionName(dto.getSolutionName());
        solution.setSolutionCode(dto.getSolutionCode());
        solution.setSceneType(dto.getSceneType());
        solution.setDescription(dto.getDescription());
        solution.setTriggerCondition(dto.getTriggerCondition());
        solution.setExpectedOutcome(dto.getExpectedOutcome());

        if (dto.getSteps() != null) {
            List<KnowledgeVO.SolutionStepVO> steps = new ArrayList<>();
            for (KnowledgeDTO.SolutionStepDTO stepDTO : dto.getSteps()) {
                KnowledgeVO.SolutionStepVO step = new KnowledgeVO.SolutionStepVO();
                step.setStepOrder(stepDTO.getStepOrder());
                step.setStepName(stepDTO.getStepName());
                step.setStepAction(stepDTO.getStepAction());
                step.setExpectedResult(stepDTO.getExpectedResult());
                steps.add(step);
            }
            solution.setSteps(stepsToJson(steps));
        }

        solution.setUsageCount(0);
        solution.setSuccessRate(0.0);
        solution.setStatus("草稿");
        solution.setVectorized(false);
        solution = solutionRepository.save(solution);
        log.info("创建解决方案成功: id={}", solution.getId());
        return toSolutionVO(solution);
    }

    @Transactional
    public KnowledgeVO.SolutionVO updateSolution(KnowledgeDTO.UpdateSolutionDTO dto) {
        Solution solution = solutionRepository.findById(dto.getId()).orElse(null);
        if (solution != null) {
            if (dto.getSolutionName() != null) solution.setSolutionName(dto.getSolutionName());
            if (dto.getSolutionCode() != null) solution.setSolutionCode(dto.getSolutionCode());
            if (dto.getSceneType() != null) solution.setSceneType(dto.getSceneType());
            if (dto.getDescription() != null) solution.setDescription(dto.getDescription());
            if (dto.getTriggerCondition() != null) solution.setTriggerCondition(dto.getTriggerCondition());
            if (dto.getExpectedOutcome() != null) solution.setExpectedOutcome(dto.getExpectedOutcome());
            if (dto.getStatus() != null) solution.setStatus(dto.getStatus());

            if (dto.getSteps() != null) {
                List<KnowledgeVO.SolutionStepVO> steps = new ArrayList<>();
                for (KnowledgeDTO.SolutionStepDTO stepDTO : dto.getSteps()) {
                    KnowledgeVO.SolutionStepVO step = new KnowledgeVO.SolutionStepVO();
                    step.setStepOrder(stepDTO.getStepOrder());
                    step.setStepName(stepDTO.getStepName());
                    step.setStepAction(stepDTO.getStepAction());
                    step.setExpectedResult(stepDTO.getExpectedResult());
                    steps.add(step);
                }
                solution.setSteps(stepsToJson(steps));
            }

            solution = solutionRepository.save(solution);
            log.info("更新解决方案成功: id={}", solution.getId());
            return toSolutionVO(solution);
        }
        return null;
    }

    @Transactional
    public void deleteSolution(Long id) {
        solutionRepository.deleteById(id);
        log.info("删除解决方案成功: id={}", id);
    }

    public KnowledgeVO.SolutionVO getSolutionDetail(Long id) {
        return toSolutionVO(solutionRepository.findById(id).orElse(null));
    }

    // ==================== 知识向量化 ====================

    @Transactional
    public KnowledgeVO.VectorizeResultVO vectorize(KnowledgeDTO.VectorizeDTO dto) {
        KnowledgeVO.VectorizeResultVO result = new KnowledgeVO.VectorizeResultVO();
        result.setTaskId(UUID.randomUUID().toString());
        result.setStatus("PROCESSING");
        result.setMessage("向量化任务已提交，正在处理中...");

        if ("product".equals(dto.getKnowledgeType())) {
            Product product = productRepository.findById(dto.getKnowledgeId()).orElse(null);
            if (product != null) {
                product.setVectorized(true);
                productRepository.save(product);
            }
        } else if ("faq".equals(dto.getKnowledgeType())) {
            Faq faq = faqRepository.findById(dto.getKnowledgeId()).orElse(null);
            if (faq != null) {
                faq.setVectorized(true);
                faqRepository.save(faq);
            }
        }

        log.info("触发知识向量化: type={}, id={}", dto.getKnowledgeType(), dto.getKnowledgeId());
        return result;
    }

    public KnowledgeVO.VectorizeStatusVO getVectorizeStatus(KnowledgeDTO.VectorizeStatusDTO dto) {
        KnowledgeVO.VectorizeStatusVO status = new KnowledgeVO.VectorizeStatusVO();
        status.setTaskId(dto.getTaskId());
        status.setStatus("COMPLETED");
        status.setProgress(100);
        status.setTotalCount(1);
        status.setProcessedCount(1);
        status.setStartTime(LocalDateTime.now().minusMinutes(5).toString());
        status.setEndTime(LocalDateTime.now().toString());
        return status;
    }

    // ==================== 知识导入导出 ====================

    public KnowledgeVO.ImportResultVO importKnowledge(KnowledgeDTO.ImportDTO dto) {
        KnowledgeVO.ImportResultVO result = new KnowledgeVO.ImportResultVO();
        result.setTotalCount(10);
        result.setSuccessCount(8);
        result.setFailCount(2);
        result.setFailReasons(Arrays.asList("第3行数据格式错误", "第7行必填字段为空"));
        log.info("导入知识: type={}, file={}", dto.getKnowledgeType(), dto.getFileUrl());
        return result;
    }

    public KnowledgeVO.ExportResultVO exportKnowledge(KnowledgeDTO.ExportDTO dto) {
        KnowledgeVO.ExportResultVO result = new KnowledgeVO.ExportResultVO();
        result.setFileUrl("/export/" + dto.getKnowledgeType() + "_" + System.currentTimeMillis() + ".xlsx");
        result.setFileName(dto.getKnowledgeType() + "_export.xlsx");
        result.setFileSize(1024L * 100);
        log.info("导出知识: type={}, ids={}", dto.getKnowledgeType(), dto.getIds());
        return result;
    }

    // ==================== 辅助方法 ====================

    private <T> KnowledgeVO.PageVO<T> buildPage(List<T> list, Integer page, Integer size) {
        if (page == null) page = 1;
        if (size == null) size = 20;

        KnowledgeVO.PageVO<T> pageVO = new KnowledgeVO.PageVO<>();
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
