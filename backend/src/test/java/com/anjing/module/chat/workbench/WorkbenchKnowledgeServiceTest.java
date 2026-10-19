package com.anjing.module.chat.workbench;

import com.anjing.module.chat.entity.ScopedKnowledge;
import com.anjing.module.chat.repository.ScopedKnowledgeRepository;
import com.anjing.module.demo.DemoBusinessTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkbenchKnowledgeServiceTest {
    private final ScopedKnowledgeRepository repository = mock(ScopedKnowledgeRepository.class);
    private final DemoBusinessTime time = mock(DemoBusinessTime.class);
    private final WorkbenchKnowledgeService service = new WorkbenchKnowledgeService(repository, new ObjectMapper(), time);
    private static final LocalDateTime START = LocalDateTime.parse("2026-10-01T00:00:00");
    private static final LocalDateTime END = LocalDateTime.parse("2026-10-31T23:59:59");

    @Test void activityIsAvailableAtBothBoundariesAndHiddenOutsideTheWindow() {
        ScopedKnowledge activity = activity("demo-shop-tb", true);
        when(repository.findByMerchantIdAndStatus("demo-merchant", "ACTIVE")).thenReturn(List.of(activity));
        for (LocalDateTime now : List.of(START, END, START.plusDays(17))) {
            at(now, now);
            assertThat(service.search("demo-merchant", "demo-shop-tb", "优惠活动")).containsExactly(activity);
        }
        for (LocalDateTime now : List.of(START.minusNanos(1), END.plusNanos(1))) {
            at(now, now);
            assertThat(service.search("demo-merchant", "demo-shop-tb", "优惠活动")).isEmpty();
        }
    }

    @Test void fixedDemoClockDoesNotReviveExpiredNonDemoKnowledge() {
        ScopedKnowledge demo = activity("demo-shop-tb", true);
        ScopedKnowledge real = activity("demo-shop-tb", false);
        when(repository.findByMerchantIdAndStatus("demo-merchant", "ACTIVE")).thenReturn(List.of(demo, real));
        at(START.plusDays(17), END.plusYears(4));
        assertThat(service.applicable("demo-merchant", "demo-shop-tb")).containsExactly(demo);
    }

    @Test void activityWithoutCompleteValidityAndOtherShopsAreExcluded() {
        ScopedKnowledge missing = activity("demo-shop-tb", true); missing.setEndsAt(null);
        ScopedKnowledge legacy = activity("demo-shop-tb", true); legacy.setKind(null); legacy.setStartsAt(null);
        ScopedKnowledge otherShop = activity("demo-shop-dy", true);
        when(repository.findByMerchantIdAndStatus("demo-merchant", "ACTIVE")).thenReturn(List.of(missing, legacy, otherShop));
        at(START, START);
        assertThat(service.applicable("demo-merchant", "demo-shop-tb")).isEmpty();
    }

    @Test void undatedMerchantRulesRemainAvailable() {
        ScopedKnowledge rule = new ScopedKnowledge();
        rule.setScope("MERCHANT"); rule.setTopic("RETURN_WINDOW"); rule.setDemoData(true);
        when(repository.findByMerchantIdAndStatus("demo-merchant", "ACTIVE")).thenReturn(List.of(rule));
        at(END.plusYears(4), END.plusYears(4));
        assertThat(service.search("demo-merchant", "demo-shop-tb", "退货期限")).containsExactly(rule);
    }

    @Test void loadingPersistsActivityDatesInShanghaiTime() {
        service.load(false);
        ArgumentCaptor<ScopedKnowledge> saved = ArgumentCaptor.forClass(ScopedKnowledge.class);
        verify(repository, times(10)).save(saved.capture());
        assertThat(saved.getAllValues().stream().filter(k -> "ACTIVITY".equals(k.getKind())).toList()).hasSize(2)
            .allSatisfy(k -> { assertThat(k.getStartsAt()).isEqualTo(START); assertThat(k.getEndsAt()).isEqualTo(END); });
    }

    @Test void legacyMetadataIsBackfilledWithoutResettingEditedContentOrStatus() {
        ScopedKnowledge legacy = new ScopedKnowledge();
        legacy.setId("demo-knowledge-tb-activity"); legacy.setMerchantId("demo-merchant");
        legacy.setShopId("demo-shop-tb"); legacy.setScope("SHOP"); legacy.setDemoData(true);
        legacy.setContent("商家编辑的活动文案"); legacy.setStatus("DISABLED");
        when(repository.findById(legacy.getId())).thenReturn(Optional.of(legacy));
        service.load(false);
        assertThat(legacy.getKind()).isEqualTo("ACTIVITY");
        assertThat(legacy.getStartsAt()).isEqualTo(START);
        assertThat(legacy.getEndsAt()).isEqualTo(END);
        assertThat(legacy.getContent()).isEqualTo("商家编辑的活动文案");
        assertThat(legacy.getStatus()).isEqualTo("DISABLED");
        verify(repository).save(legacy);
    }

    private void at(LocalDateTime demo, LocalDateTime real) {
        when(time.now(true)).thenReturn(demo); when(time.now(false)).thenReturn(real);
    }

    private ScopedKnowledge activity(String shopId, boolean demo) {
        ScopedKnowledge row = new ScopedKnowledge(); row.setId(shopId + (demo ? "-demo" : "-real"));
        row.setScope("SHOP"); row.setShopId(shopId); row.setTopic("DISCOUNT"); row.setKind("ACTIVITY");
        row.setDemoData(demo); row.setStartsAt(START); row.setEndsAt(END);
        return row;
    }
}
