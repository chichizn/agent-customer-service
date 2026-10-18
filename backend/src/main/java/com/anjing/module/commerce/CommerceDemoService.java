package com.anjing.module.commerce;

import com.anjing.module.commerce.CommerceTypes.*;
import com.anjing.module.commerce.CommerceVO.*;
import com.anjing.module.commerce.entity.*;
import com.anjing.module.commerce.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommerceDemoService {
    public static final String MERCHANT_ID = "demo-merchant";
    private final MerchantRepository merchants;
    private final ShopRepository shops;
    private final CustomerRepository customers;
    private final CommerceOrderRepository orders;
    private final LogisticsRepository logistics;
    private final ServiceTicketRepository tickets;
    private final TicketSubmissionRepository submissions;
    private final ObjectMapper objectMapper;

    /** 只列出本模块演示身份，供 A 校验及 B 构建模拟入口。 */
    @Transactional(readOnly = true)
    public DemoIdentities identities() {
        Merchant merchant = merchants.findById(MERCHANT_ID).filter(Merchant::isDemoData)
            .orElseThrow(() -> CommerceService.error("DEMO_NOT_LOADED", "请先加载 commerce 演示数据"));
        return new DemoIdentities(merchant.getId(), merchant.getName(), shops.findByMerchantIdOrderById(MERCHANT_ID).stream()
            .filter(Shop::isDemoData).map(s -> new ShopIdentity(s.getId(), s.getName(), s.getPlatform(),
                customers.findByMerchantIdAndShopIdOrderById(MERCHANT_ID, s.getId()).stream().filter(Customer::isDemoData)
                    .map(c -> new CustomerIdentity(c.getId(), c.getPlatformUserId(), c.getName())).toList())).toList());
    }

    /** 不清除已有工单；已存在的演示订单也不覆盖。 */
    @Transactional
    public DemoRestored load() {
        Fixture fixture = fixture();
        lockOrCreateMerchant();
        checkCollisions(fixture);
        seed(fixture, false);
        return result(fixture, "已加载缺失的演示业务数据，已有工单和订单保持不变");
    }

    /** A 应在停止该演示空间业务写入后，在统一重置事务中调用。 */
    @Transactional
    public DemoRestored restore(boolean confirmed) {
        if (!confirmed) throw CommerceService.error("CONFIRMATION_REQUIRED", "请确认恢复 commerce 演示数据");
        Fixture fixture = fixture();
        lockOrCreateMerchant();
        checkCollisions(fixture);
        merchants.findById(MERCHANT_ID).orElseThrow().setName("安静生活演示商家");
        List<CommerceOrder> demoOrders = orders.findByMerchantIdAndDemoDataTrue(MERCHANT_ID);
        Set<String> demoOrderIds = new HashSet<>();
        demoOrders.forEach(o -> demoOrderIds.add(o.getId()));
        List<ServiceTicket> demoTickets = tickets.findByMerchantIdAndDemoDataTrue(MERCHANT_ID);
        Set<String> demoTicketIds = new HashSet<>();
        demoTickets.forEach(t -> demoTicketIds.add(t.getId()));
        // 不破坏非演示记录对演示资源的引用；遇到混合数据则整笔恢复回滚。
        if (tickets.findByMerchantIdAndDemoDataFalse(MERCHANT_ID).stream().anyMatch(t -> demoOrderIds.contains(t.getOrderId())) ||
            submissions.findByMerchantIdAndDemoDataFalse(MERCHANT_ID).stream().anyMatch(s -> demoTicketIds.contains(s.getTicketId())) ||
            logistics.findByMerchantIdAndDemoDataFalse(MERCHANT_ID).stream().anyMatch(l -> demoOrderIds.contains(l.getOrderId())))
            throw CommerceService.error("DEMO_DATA_CONFLICT", "非演示记录引用演示业务数据，已取消恢复以保留原数据");
        submissions.deleteAll(submissions.findByMerchantIdAndDemoDataTrue(MERCHANT_ID));
        tickets.deleteAll(demoTickets);
        logistics.deleteAll(logistics.findByMerchantIdAndDemoDataTrue(MERCHANT_ID));
        orders.deleteAll(demoOrders);
        orders.flush();
        seed(fixture, true);
        return result(fixture, "已恢复 commerce 演示订单、物流和身份，清除演示工单；请重新加载页面。会话与知识由统一重置入口处理");
    }

    private void lockOrCreateMerchant() {
        Merchant merchant = merchants.lockById(MERCHANT_ID).orElse(null);
        if (merchant == null) {
            merchant = new Merchant(); merchant.setId(MERCHANT_ID); merchant.setName("安静生活演示商家"); merchant.setDemoData(true);
            merchants.saveAndFlush(merchant);
        } else if (!merchant.isDemoData()) throw collision();
    }

    private void checkCollisions(Fixture f) {
        for (ShopSeed s : f.shops()) shops.findById(s.id()).ifPresent(existing -> {
            if (!existing.isDemoData() || !MERCHANT_ID.equals(existing.getMerchantId())) throw collision();
        });
        for (CustomerSeed c : f.customers()) customers.findById(c.id()).ifPresent(existing -> {
            if (!existing.isDemoData() || !MERCHANT_ID.equals(existing.getMerchantId()) || !c.shopId().equals(existing.getShopId())) throw collision();
        });
        for (OrderSeed o : f.orders()) {
            orders.findById(o.id()).ifPresent(existing -> {
                if (!existing.isDemoData() || !MERCHANT_ID.equals(existing.getMerchantId()) || !o.shopId().equals(existing.getShopId()) ||
                    !o.customerId().equals(existing.getCustomerId())) throw collision();
            });
            logistics.findById("log-" + o.id()).ifPresent(existing -> {
                if (!existing.isDemoData() || !MERCHANT_ID.equals(existing.getMerchantId()) || !o.id().equals(existing.getOrderId())) throw collision();
            });
        }
    }

    private void seed(Fixture f, boolean overwrite) {
        for (ShopSeed s : f.shops()) {
            if (!overwrite && shops.existsById(s.id())) continue;
            Shop entity = new Shop(); entity.setId(s.id()); entity.setMerchantId(MERCHANT_ID);
            entity.setName(s.name()); entity.setPlatform(s.platform()); entity.setDemoData(true); shops.save(entity);
        }
        for (CustomerSeed c : f.customers()) {
            if (!overwrite && customers.existsById(c.id())) continue;
            Customer entity = new Customer(); entity.setId(c.id()); entity.setMerchantId(MERCHANT_ID); entity.setShopId(c.shopId());
            entity.setPlatformUserId(c.platformUserId()); entity.setName(c.name()); entity.setDemoData(true); customers.save(entity);
        }
        for (OrderSeed s : f.orders()) {
            if (!orders.existsById(s.id())) {
                CommerceOrder order = new CommerceOrder(); order.setId(s.id()); order.setOrderNumber(s.orderNumber());
                order.setMerchantId(MERCHANT_ID); order.setShopId(s.shopId()); order.setCustomerId(s.customerId()); order.setStatus(s.status());
                order.setActualPaid(s.actualPaid()); order.setOrderedAt(s.orderedAt()); order.setDemoData(true);
                int index = 0;
                for (ItemSeed i : s.items()) {
                    CommerceOrder.Item item = new CommerceOrder.Item(); item.setItemId(s.id() + "-item-" + ++index);
                    item.setProductId(i.productId()); item.setProductName(i.productName()); item.setSpecification(i.specification());
                    item.setQuantity(i.quantity()); item.setUnitPrice(i.unitPrice()); order.getItems().add(item);
                }
                orders.save(order);
            }
            if (s.logistics() != null && logistics.findByOrderId(s.id()).isEmpty()) {
                LogisticsSeed l = s.logistics();
                Logistics entity = new Logistics(); entity.setId("log-" + s.id()); entity.setOrderId(s.id()); entity.setMerchantId(MERCHANT_ID);
                entity.setCarrier(l.carrier()); entity.setTrackingNumber(l.trackingNumber()); entity.setStatus(l.status()); entity.setDemoData(true);
                int index = 0;
                for (NodeSeed n : l.nodes()) {
                    Logistics.Node node = new Logistics.Node(); node.setNodeId(s.id() + "-node-" + ++index);
                    node.setOccurredAt(n.occurredAt()); node.setDescription(n.description()); entity.getNodes().add(node);
                }
                logistics.save(entity);
            }
        }
        orders.flush(); logistics.flush();
    }

    private Fixture fixture() {
        try (var input = new ClassPathResource("commerce/demo-data.json").getInputStream()) {
            Fixture f = objectMapper.readValue(input, Fixture.class);
            // 数据由仓库维护，仍检查关系与主键，防止更改样例时加载半套数据。
            Set<String> shopIds = new HashSet<>(), customerIds = new HashSet<>(), orderIds = new HashSet<>();
            for (ShopSeed s : f.shops()) if (s.id() == null || s.name() == null || s.platform() == null || !shopIds.add(s.id())) throw invalidFixture();
            Map<String, String> customerShops = new HashMap<>();
            for (CustomerSeed c : f.customers()) {
                if (c.id() == null || c.name() == null || c.platformUserId() == null || !shopIds.contains(c.shopId()) || !customerIds.add(c.id())) throw invalidFixture();
                customerShops.put(c.id(), c.shopId());
            }
            Set<String> orderNumbers = new HashSet<>();
            for (OrderSeed o : f.orders()) {
                if (o.id() == null || !orderIds.add(o.id()) || !Objects.equals(customerShops.get(o.customerId()), o.shopId()) ||
                    !shopIds.contains(o.shopId()) || o.orderNumber() == null || o.status() == null || o.orderedAt() == null ||
                    o.actualPaid() == null || o.actualPaid().signum() < 0 || o.items() == null || o.items().isEmpty() ||
                    !orderNumbers.add(o.shopId().length() + ":" + o.shopId() + o.orderNumber())) throw invalidFixture();
                for (ItemSeed i : o.items()) if (i.productId() == null || i.productName() == null || i.quantity() < 1 || i.unitPrice() == null || i.unitPrice().signum() < 0) throw invalidFixture();
                if (o.logistics() != null && (o.logistics().nodes() == null || o.logistics().carrier() == null || o.logistics().trackingNumber() == null || o.logistics().status() == null ||
                    o.logistics().nodes().stream().anyMatch(n -> n.occurredAt() == null || n.description() == null))) throw invalidFixture();
            }
            return f;
        } catch (IOException e) { throw new IllegalStateException("无法读取 commerce 演示数据", e); }
    }
    private DemoRestored result(Fixture f, String message) { return new DemoRestored(MERCHANT_ID, f.shops().size(), f.customers().size(), f.orders().size(), message); }
    private static CommerceException collision() { return CommerceService.error("DEMO_DATA_CONFLICT", "演示编号与其他数据冲突，未覆盖原数据"); }
    private static IllegalStateException invalidFixture() { return new IllegalStateException("commerce 演示数据的编号、归属或字段无效"); }
    public record Fixture(List<ShopSeed> shops, List<CustomerSeed> customers, List<OrderSeed> orders) {}
    public record ShopSeed(String id, String name, Platform platform) {}
    public record CustomerSeed(String id, String shopId, String platformUserId, String name) {}
    public record OrderSeed(String id, String orderNumber, String shopId, String customerId, OrderStatus status,
        BigDecimal actualPaid, LocalDateTime orderedAt, List<ItemSeed> items, LogisticsSeed logistics) {}
    public record ItemSeed(String productId, String productName, String specification, int quantity, BigDecimal unitPrice) {}
    public record LogisticsSeed(String carrier, String trackingNumber, String status, List<NodeSeed> nodes) {}
    public record NodeSeed(LocalDateTime occurredAt, String description) {}
}
