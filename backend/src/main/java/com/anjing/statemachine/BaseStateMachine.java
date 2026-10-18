package com.anjing.statemachine;

import com.anjing.model.exception.BizException;
import com.anjing.model.errorcode.StateMachineErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔄 状态机基础实现类 - 提供通用的状态转换逻辑
 * 
 * <p>基于HashMap实现的高性能状态机，支持状态转换、条件检查、监听器等功能</p>
 * 
 * <h3>🚀 核心特性：</h3>
 * <ul>
 *   <li>⚡ <b>高性能</b> - 基于HashMap的O(1)状态转换查找</li>
 *   <li>🔒 <b>线程安全</b> - 使用ConcurrentHashMap保证并发安全</li>
 *   <li>🎯 <b>类型安全</b> - 泛型约束确保编译时类型检查</li>
 *   <li>📋 <b>可扩展</b> - 支持转换监听器和条件检查</li>
 *   <li>🛡️ <b>异常清晰</b> - 详细的错误信息和异常处理</li>
 * </ul>
 * 
 * <h3>📝 使用方式：</h3>
 * <pre>
 * public class OrderStateMachine extends BaseStateMachine&lt;OrderState, OrderEvent&gt; {
 *     
 *     public static final OrderStateMachine INSTANCE = new OrderStateMachine();
 *     
 *     // 在构造函数或初始化块中配置转换规则
 *     {
 *         // 基础转换
 *         putTransition(OrderState.CREATED, OrderEvent.PAY, OrderState.PAID);
 *         putTransition(OrderState.PAID, OrderEvent.SHIP, OrderState.SHIPPED);
 *         
 *         // 带条件的转换
 *         putTransition(OrderState.CREATED, OrderEvent.CANCEL, OrderState.CANCELLED, 
 *                      (state, event, context) -> context.get("reason") != null);
 *     }
 * }
 * </pre>
 * 
 * <h3>🔧 高级功能：</h3>
 * <ul>
 *   <li><b>条件转换</b> - 支持基于上下文的条件检查</li>
 *   <li><b>转换监听</b> - 状态转换前后的事件通知</li>
 *   <li><b>批量配置</b> - 支持批量添加转换规则</li>
 *   <li><b>状态查询</b> - 查询当前状态支持的事件</li>
 * </ul>
 * 
 * @param <STATE> 状态类型，建议使用枚举
 * @param <EVENT> 事件类型，建议使用枚举
 * 
 * @author Backend Template Team
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
public class BaseStateMachine<STATE, EVENT> implements StateMachine<STATE, EVENT> {

    /**
     * 状态转换映射表
     * Key: "currentState_event"
     * Value: 转换规则对象
     */
    private final Map<String, TransitionRule<STATE, EVENT>> stateTransitions = new ConcurrentHashMap<>();
    
    /**
     * 状态转换监听器列表
     */
    private final List<StateTransitionListener<STATE, EVENT>> listeners = new ArrayList<>();

    /**
     * 🔧 添加状态转换规则
     * 
     * @param fromState 源状态
     * @param event     触发事件
     * @param toState   目标状态
     */
    protected void putTransition(STATE fromState, EVENT event, STATE toState) {
        putTransition(fromState, event, toState, null);
    }

    /**
     * 🔧 添加带条件的状态转换规则
     * 
     * @param fromState 源状态
     * @param event     触发事件
     * @param toState   目标状态
     * @param condition 转换条件，为null表示无条件转换
     */
    protected void putTransition(STATE fromState, EVENT event, STATE toState, 
                                TransitionCondition<STATE, EVENT> condition) {
        validateTransitionParams(fromState, event, toState);
        
        String key = buildTransitionKey(fromState, event);
        TransitionRule<STATE, EVENT> rule = new TransitionRule<>(fromState, event, toState, condition);
        
        // 检查重复定义
        if (stateTransitions.containsKey(key)) {
            log.warn("发现重复的状态转换定义: {} -> {} (事件: {}), 将被覆盖", 
                    fromState, toState, event);
        }
        
        stateTransitions.put(key, rule);
        log.debug("添加状态转换规则: {} --[{}]--> {}", fromState, event, toState);
    }

    /**
     * 🔧 批量添加状态转换规则
     * 
     * @param transitions 转换规则列表
     */
    protected void putTransitions(List<TransitionRule<STATE, EVENT>> transitions) {
        for (TransitionRule<STATE, EVENT> rule : transitions) {
            putTransition(rule.getFromState(), rule.getEvent(), rule.getToState(), rule.getCondition());
        }
    }

    /**
     * 🔧 添加状态转换监听器
     * 
     * @param listener 监听器
     */
    public void addListener(StateTransitionListener<STATE, EVENT> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public STATE transition(STATE currentState, EVENT event) {
        return transition(currentState, event, new HashMap<>());
    }

    /**
     * 🔄 执行带上下文的状态转换
     * 
     * @param currentState 当前状态
     * @param event        触发事件
     * @param context      转换上下文
     * @return 目标状态
     */
    public STATE transition(STATE currentState, EVENT event, Map<String, Object> context) {
        // 参数校验
        validateTransitionParams(currentState, event);
        
        // 查找转换规则
        String key = buildTransitionKey(currentState, event);
        TransitionRule<STATE, EVENT> rule = stateTransitions.get(key);
        
        if (rule == null) {
            String errorMsg = String.format("不支持的状态转换: %s --[%s]--> ?", currentState, event);
            log.warn(errorMsg);
            throw new BizException(errorMsg, StateMachineErrorCode.ILLEGAL_STATE_TRANSITION);
        }
        
        // 检查转换条件
        if (rule.getCondition() != null && !rule.getCondition().test(currentState, event, context)) {
            String errorMsg = String.format("状态转换条件不满足: %s --[%s]--> %s", 
                    currentState, event, rule.getToState());
            log.warn(errorMsg);
            throw new BizException(errorMsg, StateMachineErrorCode.STATE_TRANSITION_CONDITION_NOT_MET);
        }
        
        STATE targetState = rule.getToState();
        
        // 执行状态转换前监听器
        try {
            for (StateTransitionListener<STATE, EVENT> listener : listeners) {
                listener.beforeTransition(currentState, event, targetState, context);
            }
        } catch (Exception e) {
            log.error("状态转换前监听器执行失败: {} --[{}]--> {}", currentState, event, targetState, e);
            throw new BizException("状态转换前监听器执行失败: " + e.getMessage(), 
                    StateMachineErrorCode.STATE_TRANSITION_LISTENER_FAILED);
        }
        
        // 记录状态转换日志
        log.info("🔄 状态转换: {} --[{}]--> {}", currentState, event, targetState);
        
        // 执行状态转换后监听器
        try {
            for (StateTransitionListener<STATE, EVENT> listener : listeners) {
                listener.afterTransition(currentState, event, targetState, context);
            }
        } catch (Exception e) {
            log.error("状态转换后监听器执行失败: {} --[{}]--> {}", currentState, event, targetState, e);
            // 注意：转换后监听器失败不影响状态转换的结果
        }
        
        return targetState;
    }

    @Override
    public Set<EVENT> getSupportedEvents(STATE currentState) {
        if (currentState == null) {
            return Collections.emptySet();
        }
        
        Set<EVENT> supportedEvents = new HashSet<>();
        for (Map.Entry<String, TransitionRule<STATE, EVENT>> entry : stateTransitions.entrySet()) {
            TransitionRule<STATE, EVENT> rule = entry.getValue();
            if (Objects.equals(rule.getFromState(), currentState)) {
                supportedEvents.add(rule.getEvent());
            }
        }
        
        return supportedEvents;
    }

    /**
     * 📊 获取所有状态转换规则
     * 
     * @return 转换规则映射表的只读视图
     */
    public Map<String, TransitionRule<STATE, EVENT>> getAllTransitions() {
        return Collections.unmodifiableMap(stateTransitions);
    }

    /**
     * 📋 获取状态转换图的字符串表示（用于调试）
     * 
     * @return 状态转换图
     */
    public String getTransitionGraph() {
        if (stateTransitions.isEmpty()) {
            return "状态机未配置任何转换规则";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("状态转换图:\n");
        
        stateTransitions.values().stream()
                .sorted((r1, r2) -> {
                    int result = r1.getFromState().toString().compareTo(r2.getFromState().toString());
                    if (result == 0) {
                        result = r1.getEvent().toString().compareTo(r2.getEvent().toString());
                    }
                    return result;
                })
                .forEach(rule -> {
                    sb.append(String.format("  %s --[%s]--> %s%s\n", 
                            rule.getFromState(), 
                            rule.getEvent(), 
                            rule.getToState(),
                            rule.getCondition() != null ? " (有条件)" : ""));
                });
        
        return sb.toString();
    }

    /**
     * 🔧 构建转换键
     */
    private String buildTransitionKey(STATE state, EVENT event) {
        return state + "_" + event;
    }

    /**
     * ✅ 校验转换参数
     */
    private void validateTransitionParams(STATE state, EVENT event) {
        if (state == null || event == null) {
            throw new BizException("状态和事件不能为空", StateMachineErrorCode.STATE_OR_EVENT_IS_NULL);
        }
    }

    /**
     * ✅ 校验转换参数（包含目标状态）
     */
    private void validateTransitionParams(STATE fromState, EVENT event, STATE toState) {
        if (fromState == null || event == null || toState == null) {
            throw new BizException("状态和事件不能为空", StateMachineErrorCode.STATE_OR_EVENT_IS_NULL);
        }
    }

    /**
     * 🔄 状态转换规则
     */
    public static class TransitionRule<STATE, EVENT> {
        private final STATE fromState;
        private final EVENT event;
        private final STATE toState;
        private final TransitionCondition<STATE, EVENT> condition;

        public TransitionRule(STATE fromState, EVENT event, STATE toState, 
                            TransitionCondition<STATE, EVENT> condition) {
            this.fromState = fromState;
            this.event = event;
            this.toState = toState;
            this.condition = condition;
        }

        // Getters
        public STATE getFromState() { return fromState; }
        public EVENT getEvent() { return event; }
        public STATE getToState() { return toState; }
        public TransitionCondition<STATE, EVENT> getCondition() { return condition; }
    }

    /**
     * 🔍 状态转换条件接口
     */
    @FunctionalInterface
    public interface TransitionCondition<STATE, EVENT> {
        /**
         * 检查转换条件是否满足
         * 
         * @param fromState 源状态
         * @param event     触发事件
         * @param context   转换上下文
         * @return true表示条件满足，可以转换；false表示条件不满足，不能转换
         */
        boolean test(STATE fromState, EVENT event, Map<String, Object> context);
    }

    /**
     * 👂 状态转换监听器接口
     */
    public interface StateTransitionListener<STATE, EVENT> {
        /**
         * 状态转换前回调
         * 
         * @param fromState 源状态
         * @param event     触发事件
         * @param toState   目标状态
         * @param context   转换上下文
         */
        default void beforeTransition(STATE fromState, EVENT event, STATE toState, Map<String, Object> context) {
            // 默认空实现
        }

        /**
         * 状态转换后回调
         * 
         * @param fromState 源状态
         * @param event     触发事件
         * @param toState   目标状态
         * @param context   转换上下文
         */
        default void afterTransition(STATE fromState, EVENT event, STATE toState, Map<String, Object> context) {
            // 默认空实现
        }
    }
}
