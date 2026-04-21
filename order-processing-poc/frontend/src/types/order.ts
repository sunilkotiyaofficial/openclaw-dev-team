export type OrderStatus =
  | 'PENDING' | 'PAYMENT_PROCESSING' | 'INVENTORY_RESERVING'
  | 'SHIPPING_SCHEDULED' | 'COMPLETED' | 'CANCELLED'
  | 'PAYMENT_FAILED' | 'INVENTORY_FAILED' | 'SHIPPING_FAILED'

export interface OrderItem { skuId: string; quantity: number; unitPrice: number; name?: string }
export interface ShippingAddress { street: string; city: string; state: string; zip: string; country: string }
export interface SagaState {
  paymentAttemptId?: string; transactionId?: string; reservationId?: string;
  shipmentId?: string; trackingNumber?: string; failureReason?: string
}
export interface Order {
  id: string; customerId: string; status: OrderStatus; items: OrderItem[];
  totalAmount: number; currency: string; shippingAddress: ShippingAddress;
  sagaState: SagaState; createdAt: string; updatedAt: string
}
export interface CreateOrderRequest {
  customerId: string; items: Array<{ skuId: string; quantity: number; unitPrice?: number }>;
  shippingAddress: ShippingAddress; paymentMethodId: string; idempotencyKey?: string
}
export interface PagedOrders {
  content: Order[]; totalElements: number; totalPages: number; number: number; size: number
}
export interface OrderStatusEvent {
  orderId: string; previousStatus: OrderStatus; newStatus: OrderStatus; timestamp: string
}
