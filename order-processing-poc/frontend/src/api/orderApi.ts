import axios from 'axios'
import type { CreateOrderRequest, Order, PagedOrders } from '../types/order'

const api = axios.create({ baseURL: '/api/v1', headers: { 'Content-Type': 'application/json' } })

export const orderApi = {
  createOrder: (r: CreateOrderRequest) => api.post<Order>('/orders', r).then(x => x.data),
  getOrder: (id: string) => api.get<Order>(`/orders/${id}`).then(x => x.data),
  listOrders: (p?: { customerId?: string; status?: string; page?: number; size?: number }) =>
    api.get<PagedOrders>('/orders', { params: p }).then(x => x.data),
}
