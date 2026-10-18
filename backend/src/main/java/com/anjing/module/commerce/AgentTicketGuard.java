package com.anjing.module.commerce;

/**
 * A 的接入点。调用时已有数据库事务：实现必须锁定会话行，核对 scope、messageId、
 * 已确认诉求与当前 AI 模式，锁保持至事务提交。人工接管必须竞争同一会话锁。
 * 禁止仅相信模型参数或在事务外读取模式。没有实现时 AI 写入关闭。
 */
public interface AgentTicketGuard {
    void lockAndVerify(CommerceDTO.CreateTicket request);
}
