package com.anjing.statemachine;

/**
 * 🔄 状态机接口 - 定义状态转换的核心契约
 * 
 * <p>状态机用于管理系统中各种业务对象的状态转换，确保状态流转的合法性和一致性</p>
 * 
 * <h3>🎯 核心概念：</h3>
 * <ul>
 *   <li><b>状态(State)</b> - 系统可能处于的各种状态</li>
 *   <li><b>事件(Event)</b> - 触发状态转换的事件</li>
 *   <li><b>转换(Transition)</b> - 状态之间的转换规则</li>
 *   <li><b>动作(Action)</b> - 状态转换时执行的操作</li>
 * </ul>
 * 
 * <h3>📝 使用示例：</h3>
 * <pre>
 * // 定义订单状态机
 * public class OrderStateMachine extends BaseStateMachine&lt;OrderState, OrderEvent&gt; {
 *     // 配置状态转换规则
 *     putTransition(OrderState.CREATED, OrderEvent.PAY, OrderState.PAID);
 *     putTransition(OrderState.PAID, OrderEvent.SHIP, OrderState.SHIPPED);
 * }
 * 
 * // 使用状态机
 * OrderState newState = stateMachine.transition(OrderState.CREATED, OrderEvent.PAY);
 * // newState = OrderState.PAID
 * </pre>
 * 
 * <h3>🛡️ 状态转换保证：</h3>
 * <ul>
 *   <li>✅ <b>合法性检查</b> - 只允许预定义的状态转换</li>
 *   <li>🔒 <b>原子性</b> - 状态转换要么成功要么失败，不存在中间状态</li>
 *   <li>📋 <b>可追踪</b> - 状态转换过程可以被监听和记录</li>
 *   <li>🎯 <b>类型安全</b> - 通过泛型确保状态和事件的类型安全</li>
 * </ul>
 * 
 * @param <STATE> 状态类型，通常是枚举类型
 * @param <EVENT> 事件类型，通常是枚举类型
 * 
 * @author Backend Template Team
 * @version 1.0
 * @since 1.0.0
 */
public interface StateMachine<STATE, EVENT> {

    /**
     * 🔄 执行状态转换
     * 
     * <p>根据当前状态和触发事件，计算并返回目标状态</p>
     * 
     * <h3>📋 转换规则：</h3>
     * <ul>
     *   <li>如果存在合法的状态转换路径，返回目标状态</li>
     *   <li>如果不存在合法的转换路径，抛出BizException</li>
     *   <li>状态转换过程中会触发相应的监听器</li>
     * </ul>
     * 
     * <h3>⚠️ 异常情况：</h3>
     * <ul>
     *   <li>当前状态为null - 抛出BizException</li>
     *   <li>事件为null - 抛出BizException</li>
     *   <li>不存在对应的转换规则 - 抛出BizException</li>
     *   <li>转换条件不满足 - 抛出BizException</li>
     * </ul>
     * 
     * @param currentState 当前状态，不能为null
     * @param event        触发的事件，不能为null
     * @return 转换后的目标状态
     * @throws com.anjing.model.exception.BizException 当状态转换失败时抛出
     * 
     * @see com.anjing.model.errorcode.StateMachineErrorCode#STATE_TRANSITION_FAILED
     * @see com.anjing.model.errorcode.StateMachineErrorCode#ILLEGAL_STATE_TRANSITION
     */
    STATE transition(STATE currentState, EVENT event);
    
    /**
     * 🔍 检查状态转换是否合法
     * 
     * <p>检查从当前状态通过指定事件是否可以进行状态转换，不会实际执行转换</p>
     * 
     * @param currentState 当前状态
     * @param event        触发的事件
     * @return true表示可以转换，false表示不能转换
     */
    default boolean canTransition(STATE currentState, EVENT event) {
        try {
            transition(currentState, event);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 📋 获取当前状态支持的所有事件
     * 
     * <p>返回在当前状态下可以触发的所有合法事件列表</p>
     * 
     * @param currentState 当前状态
     * @return 支持的事件列表
     */
    default java.util.Set<EVENT> getSupportedEvents(STATE currentState) {
        return java.util.Collections.emptySet();
    }
}
