package com.anjing.example.statemachine;

/**
 * 📋 订单状态枚举
 * 
 * <p>定义订单在整个生命周期中可能处于的各种状态</p>
 * 
 * <h3>🔄 状态流转路径：</h3>
 * <pre>
 * CREATED ──PAY──> PAID ──SHIP──> SHIPPED ──DELIVER──> DELIVERED ──FINISH──> FINISHED
 *    │                │              │                    │
 *    └──CANCEL──> CANCELLED      └──RETURN──> RETURNED ────┘
 *    │                │
 *    └──TIMEOUT──> CLOSED
 * </pre>
 * 
 * @author Backend Template Team
 * @version 1.0
 * @since 1.0.0
 */
public enum OrderState {
    
    /**
     * 已创建 - 订单刚创建，等待支付
     */
    CREATED("已创建", "订单已创建，等待支付"),
    
    /**
     * 已确认 - 订单已确认，等待支付（可选状态）
     */
    CONFIRMED("已确认", "订单已确认，等待支付"),
    
    /**
     * 已支付 - 订单已支付，等待发货
     */
    PAID("已支付", "订单已支付，等待商家发货"),
    
    /**
     * 已发货 - 商品已发货，等待收货
     */
    SHIPPED("已发货", "商品已发货，等待用户收货"),
    
    /**
     * 已送达 - 商品已送达，等待确认收货
     */
    DELIVERED("已送达", "商品已送达，等待用户确认收货"),
    
    /**
     * 已完成 - 订单交易完成
     */
    FINISHED("已完成", "订单交易已完成"),
    
    /**
     * 已取消 - 订单被取消
     */
    CANCELLED("已取消", "订单已被取消"),
    
    /**
     * 已关闭 - 订单因超时等原因关闭
     */
    CLOSED("已关闭", "订单已关闭"),
    
    /**
     * 已退货 - 订单商品已退货
     */
    RETURNED("已退货", "订单商品已退货");

    private final String displayName;
    private final String description;

    OrderState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * 获取状态显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 判断是否为终态（不能再转换的状态）
     */
    public boolean isFinalState() {
        return this == FINISHED || this == CANCELLED || this == CLOSED || this == RETURNED;
    }

    /**
     * 判断是否为可支付状态
     */
    public boolean isPayable() {
        return this == CREATED || this == CONFIRMED;
    }

    /**
     * 判断是否为可取消状态
     */
    public boolean isCancellable() {
        return this == CREATED || this == CONFIRMED;
    }

    /**
     * 判断是否为可发货状态
     */
    public boolean isShippable() {
        return this == PAID;
    }

    /**
     * 判断是否为可退货状态
     */
    public boolean isReturnable() {
        return this == SHIPPED || this == DELIVERED;
    }
}
