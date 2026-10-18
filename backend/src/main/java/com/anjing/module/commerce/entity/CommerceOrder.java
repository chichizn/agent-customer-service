package com.anjing.module.commerce.entity;

import com.anjing.module.commerce.CommerceTypes.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commerce_order",
    uniqueConstraints = @UniqueConstraint(name = "uk_commerce_order_number", columnNames = {"shopId", "orderNumber"}),
    indexes = @Index(name = "idx_commerce_order_scope", columnList = "merchantId,shopId,customerId"))
@Getter @Setter
public class CommerceOrder {
    @Id @Column(length = 64) private String id;
    @Column(nullable = false, length = 64) private String merchantId;
    @Column(nullable = false, length = 64) private String shopId;
    @Column(nullable = false, length = 64) private String customerId;
    @Column(nullable = false, length = 64) private String orderNumber;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal actualPaid;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private OrderStatus status;
    @Column(nullable = false) private LocalDateTime orderedAt;
    @Column(nullable = false) private boolean demoData;
    @ElementCollection
    @CollectionTable(name = "commerce_order_item", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "item_position")
    private List<Item> items = new ArrayList<>();

    @Embeddable @Getter @Setter
    public static class Item {
        @Column(nullable = false, length = 64) private String itemId;
        @Column(nullable = false, length = 64) private String productId;
        @Column(nullable = false) private String productName;
        private String specification;
        @Column(nullable = false) private int quantity;
        @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    }
}
