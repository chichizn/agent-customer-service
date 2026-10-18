package com.anjing.module.commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commerce_logistics", uniqueConstraints = @UniqueConstraint(name = "uk_commerce_logistics_order", columnNames = "orderId"))
@Getter @Setter
public class Logistics {
    @Id @Column(length = 64) private String id;
    @Column(nullable = false, length = 64) private String merchantId;
    @Column(nullable = false, length = 64) private String orderId;
    @Column(nullable = false) private String carrier;
    @Column(nullable = false, length = 64) private String trackingNumber;
    @Column(nullable = false) private String status;
    @Column(nullable = false) private boolean demoData;
    @ElementCollection
    @CollectionTable(name = "commerce_logistics_node", joinColumns = @JoinColumn(name = "logistics_id"))
    @OrderBy("occurredAt ASC, nodeId ASC")
    private List<Node> nodes = new ArrayList<>();

    @Embeddable @Getter @Setter
    public static class Node {
        @Column(nullable = false, length = 64) private String nodeId;
        @Column(nullable = false) private LocalDateTime occurredAt;
        @Column(nullable = false) private String description;
    }
}
