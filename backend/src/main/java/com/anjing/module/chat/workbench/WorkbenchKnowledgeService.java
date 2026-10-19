package com.anjing.module.chat.workbench;

import com.anjing.module.chat.entity.ScopedKnowledge;
import com.anjing.module.chat.repository.ScopedKnowledgeRepository;
import com.anjing.module.commerce.CommerceDemoService;
import com.anjing.module.commerce.CommerceException;
import com.anjing.module.demo.DemoBusinessTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service @RequiredArgsConstructor
public class WorkbenchKnowledgeService {
    private final ScopedKnowledgeRepository repository;
    private final ObjectMapper mapper;
    private final DemoBusinessTime businessTime;

    @Transactional
    public void load(boolean overwrite) {
        try (var input = new ClassPathResource("chat/knowledge.integration.v1.json").getInputStream()) {
            var root = mapper.readTree(input);
            if (!CommerceDemoService.MERCHANT_ID.equals(root.path("merchantId").asText()))
                throw new IllegalStateException("知识商家与演示空间不一致");
            for (var item : root.withArray("knowledge")) {
                String id = item.path("id").asText();
                var old = repository.findById(id);
                if (old.isPresent() && (!old.get().isDemoData() || !CommerceDemoService.MERCHANT_ID.equals(old.get().getMerchantId())))
                    throw new CommerceException("DEMO_DATA_CONFLICT", "知识编号与非演示数据冲突，未覆盖原数据");
                if (!overwrite && old.isPresent()) {
                    // Upgrade legacy demo metadata without replacing edited content, scope or status.
                    if (old.get().getKind() == null) {
                        applyValidity(old.get(), item);
                        repository.save(old.get());
                    }
                    continue;
                }
                String scope = item.path("scope").asText();
                String shop = item.path("shopId").isNull() ? null : item.path("shopId").asText();
                if (id.isBlank() || !CommerceDemoService.MERCHANT_ID.equals(item.path("merchantId").asText()) ||
                    !(scope.equals("MERCHANT") && shop == null || scope.equals("SHOP") &&
                      List.of("demo-shop-tb", "demo-shop-dy").contains(shop)))
                    throw new IllegalStateException("知识范围无效");
                ScopedKnowledge k = new ScopedKnowledge();
                k.setId(id); k.setMerchantId(CommerceDemoService.MERCHANT_ID); k.setShopId(shop); k.setScope(scope);
                k.setTopic(item.path("topic").asText()); k.setTitle(item.path("title").asText());
                k.setContent(item.path("content").asText()); k.setSource(item.path("source").asText());
                k.setStatus(item.path("status").asText()); k.setDemoData(true);
                applyValidity(k, item);
                repository.save(k);
            }
        } catch (IOException ex) { throw new IllegalStateException("无法读取集成知识种子", ex); }
    }

    @Transactional(readOnly=true)
    public List<ScopedKnowledge> search(String merchantId, String shopId, String text) {
        Set<String> topics = new HashSet<>();
        if (has(text,"退货","退款","售后","几天","期限")) topics.add("RETURN_WINDOW");
        if (has(text,"退货","运费","邮费")) topics.add("RETURN_SHIPPING");
        if (has(text,"补偿","赔偿","延迟","赔付")) topics.add("COMPENSATION");
        if (has(text,"物流","快递","到哪","发货","送达","到货","收到","明天","预计")) topics.add("NO_ETA");
        if (has(text,"工单","申请","平台")) topics.add("TICKET_SCOPE");
        if (has(text,"杯","容量","材质","保温")) topics.add("MATERIAL");
        if (has(text,"收纳","尺寸","长宽","微波")) topics.add("DIMENSIONS");
        if (has(text,"活动","优惠","折扣")) topics.add("DISCOUNT");
        return applicable(merchantId,shopId).stream().filter(k -> topics.contains(k.getTopic()))
            .sorted(Comparator.comparing(ScopedKnowledge::getId)).toList();
    }

    public List<ScopedKnowledge> applicable(String merchantId, String shopId) {
        LocalDateTime demoNow = businessTime.now(true);
        LocalDateTime systemNow = businessTime.now(false);
        return repository.findByMerchantIdAndStatus(merchantId,"ACTIVE").stream()
            .filter(k -> "MERCHANT".equals(k.getScope()) && k.getShopId()==null ||
                "SHOP".equals(k.getScope()) && Objects.equals(shopId,k.getShopId()))
            .filter(k -> validAt(k, k.isDemoData() ? demoNow : systemNow)).toList();
    }

    private void applyValidity(ScopedKnowledge knowledge, JsonNode item) {
        LocalDateTime start = timestamp(item.path("policy").path("startsAt"));
        LocalDateTime end = timestamp(item.path("policy").path("endsAt"));
        String kind = item.path("kind").asText();
        if (("ACTIVITY".equals(kind) && (start == null || end == null)) ||
            (start != null && end != null && start.isAfter(end))) {
            throw new IllegalStateException("知识有效期无效: " + knowledge.getId());
        }
        knowledge.setKind(kind);
        knowledge.setStartsAt(start);
        knowledge.setEndsAt(end);
    }

    private LocalDateTime timestamp(JsonNode value) {
        if (value.isMissingNode() || value.isNull()) return null;
        try {
            return OffsetDateTime.parse(value.asText()).atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("知识有效期必须包含有效日期和时区", ex);
        }
    }

    private boolean validAt(ScopedKnowledge knowledge, LocalDateTime now) {
        LocalDateTime start = knowledge.getStartsAt(), end = knowledge.getEndsAt();
        if (("ACTIVITY".equals(knowledge.getKind()) || "DISCOUNT".equals(knowledge.getTopic())) &&
            (start == null || end == null)) return false;
        return (start == null || !now.isBefore(start)) && (end == null || !now.isAfter(end));
    }
    private boolean has(String text,String... words) { return Arrays.stream(words).anyMatch(text::contains); }
}
