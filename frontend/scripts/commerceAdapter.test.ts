import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  toDemoCatalog,
  toWorkbenchLogistics,
  toWorkbenchOrder,
  toWorkbenchTicket
} from '../src/api/customer-service/commerceAdapter'
import type {
  CommerceIdentities,
  CommerceLogisticsRecord,
  CommerceOrderRecord,
  CommerceTicketRecord
} from '../src/api/customer-service/commerceAdapter'

test('commerce 身份映射保留商家并将顾客绑定到所属店铺', () => {
  const identities: CommerceIdentities = {
    merchantId: 'merchant-1',
    merchantName: '演示商家',
    shops: [
      {
        shopId: 'shop-tb',
        name: '淘宝店',
        platform: 'TAOBAO',
        customers: [{ customerId: 'customer-1', name: '小林' }]
      },
      {
        shopId: 'shop-dy',
        name: '抖店',
        platform: 'DOUYIN',
        customers: [{ customerId: 'customer-1', name: '小林' }]
      }
    ]
  }

  assert.deepEqual(toDemoCatalog(identities), {
    merchantId: 'merchant-1',
    shops: [
      { shopId: 'shop-tb', name: '淘宝店', platform: 'TAOBAO' },
      { shopId: 'shop-dy', name: '抖店', platform: 'DOUYIN' }
    ],
    customers: [
      { customerId: 'customer-1', name: '小林', shopId: 'shop-tb' },
      { customerId: 'customer-1', name: '小林', shopId: 'shop-dy' }
    ]
  })
})

test('订单映射保留真实单号、商品规格、金额和状态', () => {
  const order: CommerceOrderRecord = {
    orderId: 'order-1',
    orderNumber: 'TB202610190001',
    shopId: 'shop-tb',
    customerId: 'customer-1',
    items: [{ productName: '保温杯', specification: '黑色 750ml', quantity: 2 }],
    actualPaid: 138,
    status: 'SHIPPED',
    orderedAt: '2026-10-19T10:00:00'
  }

  assert.deepEqual(toWorkbenchOrder(order), {
    orderId: 'order-1',
    orderNumber: 'TB202610190001',
    shopId: 'shop-tb',
    customerId: 'customer-1',
    items: [{ name: '保温杯 · 黑色 750ml', quantity: 2 }],
    paidAmount: 138,
    status: 'SHIPPED',
    createdAt: '2026-10-19T10:00:00'
  })
})

test('无物流记录映射为可读状态且没有虚构承运信息', () => {
  const logistics: CommerceLogisticsRecord = {
    orderId: 'order-1',
    available: false,
    unavailableReason: '订单尚未发货，暂无物流',
    carrier: null,
    trackingNumber: null,
    status: null,
    nodes: []
  }

  assert.deepEqual(toWorkbenchLogistics(logistics), {
    orderId: 'order-1',
    carrier: undefined,
    trackingNumber: undefined,
    status: '订单尚未发货，暂无物流',
    nodes: []
  })
})

test('Java 工单类型和处理历史映射到工作台模型', () => {
  const ticket: CommerceTicketRecord = {
    ticketId: 'ticket-1',
    ticketNumber: 'ST202610190001',
    orderNumber: 'DY202610190001',
    sessionId: 'session-1',
    orderId: 'order-1',
    shopId: 'shop-dy',
    customerId: 'customer-1',
    ticketType: 'EXCEPTION_COMPENSATION',
    reason: '商品破损',
    status: 'PROCESSING',
    createdAt: '2026-10-19T10:00:00',
    updatedAt: '2026-10-19T11:00:00',
    history: [
      {
        historyId: 'history-1',
        fromStatus: null,
        toStatus: 'PENDING_REVIEW',
        note: '已记录诉求，待商家审核',
        operatorId: 'customer-1',
        occurredAt: '2026-10-19T10:00:00'
      },
      {
        historyId: 'history-2',
        fromStatus: 'PENDING_REVIEW',
        toStatus: 'PROCESSING',
        note: '已联系顾客核实',
        operatorId: 'demo-operator',
        occurredAt: '2026-10-19T11:00:00'
      }
    ]
  }

  const mapped = toWorkbenchTicket(ticket)
  assert.equal(mapped.type, 'COMPENSATION')
  assert.equal(mapped.orderNumber, 'DY202610190001')
  assert.equal(mapped.processingNote, '已联系顾客核实')
  assert.deepEqual(mapped.history, ticket.history)
})
