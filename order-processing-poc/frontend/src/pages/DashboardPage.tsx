import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ShoppingCart, CheckCircle, XCircle, Clock } from 'lucide-react'
import { orderApi } from '../api/orderApi'
import type { Order } from '../types/order'

function Badge({ status }: { status: Order['status'] }) {
  const c: Record<Order['status'], string> = {
    PENDING: 'bg-yellow-100 text-yellow-800', PAYMENT_PROCESSING: 'bg-blue-100 text-blue-800',
    INVENTORY_RESERVING: 'bg-purple-100 text-purple-800', SHIPPING_SCHEDULED: 'bg-indigo-100 text-indigo-800',
    COMPLETED: 'bg-green-100 text-green-800', CANCELLED: 'bg-red-100 text-red-800',
    PAYMENT_FAILED: 'bg-red-100 text-red-800', INVENTORY_FAILED: 'bg-red-100 text-red-800',
    SHIPPING_FAILED: 'bg-red-100 text-red-800',
  }
  return <span className={`px-2 py-1 rounded-full text-xs font-medium ${c[status]}`}>{status.replace(/_/g,' ')}</span>
}

export default function DashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['orders'], queryFn: () => orderApi.listOrders({ size: 50 }), refetchInterval: 5000,
  })
  const orders = data?.content ?? []
  const stats = [
    { label: 'Total Orders', value: data?.totalElements ?? 0, Icon: ShoppingCart, color: 'text-blue-600 bg-blue-50' },
    { label: 'Completed', value: orders.filter(o=>o.status==='COMPLETED').length, Icon: CheckCircle, color: 'text-green-600 bg-green-50' },
    { label: 'In Progress', value: orders.filter(o=>!['COMPLETED','CANCELLED'].includes(o.status)).length, Icon: Clock, color: 'text-yellow-600 bg-yellow-50' },
    { label: 'Cancelled', value: orders.filter(o=>o.status==='CANCELLED').length, Icon: XCircle, color: 'text-red-600 bg-red-50' },
  ]
  return (
    <div className="max-w-6xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Orders Dashboard</h1>
        <Link to="/orders/new" className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm font-medium hover:bg-blue-700">+ Place New Order</Link>
      </div>
      <div className="grid grid-cols-4 gap-4 mb-8">
        {stats.map(s=>(
          <div key={s.label} className="bg-white rounded-lg border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className={`p-2 rounded-lg ${s.color}`}><s.Icon className="h-5 w-5"/></div>
              <div><p className="text-2xl font-bold">{s.value}</p><p className="text-sm text-gray-500">{s.label}</p></div>
            </div>
          </div>
        ))}
      </div>
      <div className="bg-white rounded-lg border border-gray-200">
        <div className="px-6 py-4 border-b border-gray-200"><h2 className="text-lg font-semibold">Recent Orders</h2></div>
        {isLoading ? <div className="p-8 text-center text-gray-500">Loading...</div>
        : orders.length === 0 ? (
          <div className="p-12 text-center">
            <ShoppingCart className="h-12 w-12 text-gray-300 mx-auto mb-3"/>
            <p className="text-gray-500 font-medium">No orders yet</p>
            <Link to="/orders/new" className="mt-4 inline-block px-4 py-2 bg-blue-600 text-white rounded-md text-sm">Place Order</Link>
          </div>
        ) : (
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>{['Order ID','Customer','Items','Total','Status','Created',''].map(h=>(
                <th key={h} className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{h}</th>
              ))}</tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {orders.map(o=>(
                <tr key={o.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 text-sm font-mono">{o.id.slice(0,14)}...</td>
                  <td className="px-6 py-4 text-sm">{o.customerId}</td>
                  <td className="px-6 py-4 text-sm">{o.items.length} item(s)</td>
                  <td className="px-6 py-4 text-sm font-medium">${o.totalAmount.toFixed(2)}</td>
                  <td className="px-6 py-4"><Badge status={o.status}/></td>
                  <td className="px-6 py-4 text-sm text-gray-500">{new Date(o.createdAt).toLocaleDateString()}</td>
                  <td className="px-6 py-4"><Link to={`/orders/${o.id}`} className="text-blue-600 text-sm">Track →</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
