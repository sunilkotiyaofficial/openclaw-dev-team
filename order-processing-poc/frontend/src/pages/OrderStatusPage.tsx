import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { ArrowLeft, Wifi, WifiOff } from 'lucide-react'
import { orderApi } from '../api/orderApi'
import OrderStatusStepper from '../components/OrderStatusStepper'
import { useSSE } from '../hooks/useSSE'
import type { OrderStatusEvent } from '../types/order'

export default function OrderStatusPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const [events, setEvents] = useState<OrderStatusEvent[]>([])

  const { data: order, isLoading, refetch } = useQuery({
    queryKey: ['order', orderId],
    queryFn: () => orderApi.getOrder(orderId!),
    enabled: !!orderId,
    refetchInterval: 3000,
  })

  const { connected } = useSSE(orderId ? `/api/v1/orders/${orderId}/events` : null, {
    onMessage: (data) => {
      setEvents(prev => [data as OrderStatusEvent, ...prev].slice(0, 20))
      refetch()
    },
  })

  if (isLoading) return (
    <div className="max-w-4xl mx-auto animate-pulse space-y-4">
      <div className="h-8 bg-gray-200 rounded w-1/4"/><div className="h-32 bg-gray-200 rounded"/>
    </div>
  )
  if (!order) return <div className="text-center py-12 text-gray-500">Order not found. <Link to="/" className="text-blue-600">← Back</Link></div>

  return (
    <div className="max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <Link to="/" className="flex items-center gap-2 text-gray-600 hover:text-gray-900"><ArrowLeft className="h-4 w-4"/>Back</Link>
        <div className={`flex items-center gap-2 text-sm px-3 py-1 rounded-full ${connected?'bg-green-100 text-green-700':'bg-gray-100 text-gray-500'}`}>
          {connected?<Wifi className="h-4 w-4"/>:<WifiOff className="h-4 w-4"/>}
          {connected?'Live SSE':'Polling'}
        </div>
      </div>

      <div className="bg-white rounded-lg border p-6 mb-6">
        <h1 className="text-xl font-bold mb-1">Order Tracking</h1>
        <p className="text-sm text-gray-500 font-mono mb-6">{order.id}</p>
        <OrderStatusStepper currentStatus={order.status}/>
      </div>

      <div className="grid grid-cols-2 gap-6">
        <div className="bg-white rounded-lg border p-6">
          <h2 className="font-semibold mb-4">Order Details</h2>
          <div className="space-y-2 text-sm">
            {[['Customer', order.customerId], ['Total', `$${order.totalAmount?.toFixed(2)} ${order.currency}`],
              ['Status', order.status],
              ...(order.sagaState?.trackingNumber ? [['Tracking', order.sagaState.trackingNumber]] : []),
              ...(order.sagaState?.failureReason ? [['Failure', order.sagaState.failureReason]] : []),
            ].map(([k,v])=>(
              <div key={k} className="flex justify-between">
                <span className="text-gray-500">{k}</span>
                <span className="font-medium text-right">{v}</span>
              </div>
            ))}
          </div>
          <div className="mt-4 pt-4 border-t">
            <p className="text-xs text-gray-500 font-medium uppercase tracking-wider mb-2">Items</p>
            {order.items?.map((item, i) => (
              <div key={i} className="flex justify-between text-sm py-1">
                <span>{item.name || item.skuId} × {item.quantity}</span>
                <span className="text-gray-600">${((item.unitPrice||0) * item.quantity).toFixed(2)}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white rounded-lg border p-6">
          <h2 className="font-semibold mb-4">Live Event Log</h2>
          {events.length === 0
            ? <p className="text-gray-400 text-sm">Waiting for events...</p>
            : <div className="space-y-2 max-h-64 overflow-y-auto">
                {events.map((ev, i) => (
                  <div key={i} className="text-xs border-l-2 border-blue-400 pl-3 py-1">
                    <p className="font-medium">{ev.previousStatus} → {ev.newStatus}</p>
                    <p className="text-gray-500">{new Date(ev.timestamp).toLocaleTimeString()}</p>
                  </div>
                ))}
              </div>
          }
        </div>
      </div>
    </div>
  )
}
