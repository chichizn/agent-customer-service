package com.anjing.example.statemachine;

/**
 * ⚡ 订单事件枚举
 * 
 * <p>定义可以触发订单状态转换的各种业务事件</p>
 * 
 * <h3>🎯 事件分类：</h3>
 * <ul>
 *   <li><b>用户操作事件</b> - 用户主动触发的事件</li>
 *   <li><b>商家操作事件</b> - 商家或系统触发的事件</li>
 *   <li><b>系统事件</b> - 系统自动触发的事件</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 * @since 1.0.0
 */
public enum OrderEvent {
    
    // ==================== 用户操作事件 ====================
    
    /**
     * 确认订单 - 用户确认订单信息
     */
    CONFIRM("确认订单", "用户确认订单信息", EventType.USER_ACTION),
    
    /**
     * 支付订单 - 用户完成支付
     */
    PAY("支付订单", "用户完成订单支付", EventType.USER_ACTION),
    
    /**
     * 取消订单 - 用户主动取消订单
     */
    CANCEL("取消订单", "用户取消订单", EventType.USER_ACTION),
    
    /**
     * 确认收货 - 用户确认收到商品
     */
    CONFIRM_RECEIPT("确认收货", "用户确认收到商品", EventType.USER_ACTION),
    
    /**
     * 申请退货 - 用户申请退货
     */
    REQUEST_RETURN("申请退货", "用户申请退货", EventType.USER_ACTION),

    // ==================== 商家操作事件 ====================
    
    /**
     * 发货 - 商家发货
     */
    SHIP("发货", "商家发货", EventType.MERCHANT_ACTION),
    
    /**
     * 同意退货 - 商家同意用户退货申请
     */
    APPROVE_RETURN("同意退货", "商家同意退货申请", EventType.MERCHANT_ACTION),
    
    /**
     * 拒绝退货 - 商家拒绝用户退货申请
     */
    REJECT_RETURN("拒绝退货", "商家拒绝退货申请", EventType.MERCHANT_ACTION),

    // ==================== 系统事件 ====================
    
    /**
     * 订单超时 - 系统检测到订单超时
     */
    TIMEOUT("订单超时", "订单支付超时", EventType.SYSTEM_EVENT),
    
    /**
     * 自动发货 - 系统自动发货（虚拟商品等）
     */
    AUTO_SHIP("自动发货", "系统自动发货", EventType.SYSTEM_EVENT),
    
    /**
     * 物流送达 - 物流系统通知商品已送达
     */
    DELIVER("物流送达", "物流系统通知商品已送达", EventType.SYSTEM_EVENT),
    
    /**
     * 自动完成 - 系统自动完成订单（超过确认收货期限）
     */
    AUTO_FINISH("自动完成", "系统自动完成订单", EventType.SYSTEM_EVENT),
    
    /**
     * 退货完成 - 系统确认退货流程完成
     */
    RETURN_COMPLETED("退货完成", "退货流程已完成", EventType.SYSTEM_EVENT);

    private final String displayName;
    private final String description;
    private final EventType eventType;

    OrderEvent(String displayName, String description, EventType eventType) {
        this.displayName = displayName;
        this.description = description;
        this.eventType = eventType;
    }

    /**
     * 获取事件显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取事件描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取事件类型
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * 判断是否为用户操作事件
     */
    public boolean isUserAction() {
        return eventType == EventType.USER_ACTION;
    }

    /**
     * 判断是否为商家操作事件
     */
    public boolean isMerchantAction() {
        return eventType == EventType.MERCHANT_ACTION;
    }

    /**
     * 判断是否为系统事件
     */
    public boolean isSystemEvent() {
        return eventType == EventType.SYSTEM_EVENT;
    }

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /**
         * 用户操作事件
         */
        USER_ACTION("用户操作"),
        
        /**
         * 商家操作事件
         */
        MERCHANT_ACTION("商家操作"),
        
        /**
         * 系统事件
         */
        SYSTEM_EVENT("系统事件");

        private final String displayName;

        EventType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
