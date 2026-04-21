import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { useMutation } from '@tanstack/react-query'
import { orderApi } from '../api/orderApi'
import type { CreateOrderRequest } from '../types/order'

const MOCK_SKUS = [
  { skuId: 'SKU-001', name: 'Widget A', price: 29.99 },
  { skuId: 'SKU-002', name: 'Widget B', price: 49.99 },
  { skuId: 'SKU-003', name: 'Gadget Pro', price: 99.99 },
  { skuId: 'SKU-004', name: 'Connector Kit', price: 14.99 },
]

export default function PlaceOrderPage() {
  const navigate = useNavigate()
  const [customerId, setCustomerId] = useState('cust_demo_001')
  const [paymentMethod, setPaymentMethod] = useState('pm_mock_success')
  const [items, setItems] = useState([{ skuId: 'SKU-001', quantity: 1 }])
  const [addr, setAddr] = useState({ street: '123 Main St', city: 'Austin', state: 'TX', zip: '78701', country: 'US' })

  const mutation = useMutation({
    mutationFn: (req: CreateOrderRequest) => orderApi.createOrder(req),
    onSuccess: (order) => navigate(`/orders/${order.id}`),
  })

  const total = items.reduce((sum, i) => {
    const sku = MOCK_SKUS.find(s => s.skuId === i.skuId)
    return sum + (sku?.price ?? 0) * i.quantity
  }, 0)

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    mutation.mutate({
      customerId,
      paymentMethodId: paymentMethod,
      items: items.map(i => {
        const sku = MOCK_SKUS.find(s => s.skuId === i.skuId)
        return { skuId: i.skuId, quantity: i.quantity, unitPrice: sku?.price }
      }),
      shippingAddress: addr,
      idempotencyKey: `order_${Date.now()}`,
    })
  }

  return (
    <div className="max-w-2xl mx-auto">
      <Link to="/" className="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-6"><ArrowLeft className="h-4 w-4"/>Back</Link>
      <h1 className="text-2xl font-bold mb-6">Place New Order</h1>
      <form onSubmit={submit} className="space-y-6">
        <div className="bg-white rounded-lg border p-6">
          <h2 className="font-semibold mb-4">Customer</h2>
          <input className="w-full border rounded px-3 py-2 text-sm" value={customerId} onChange={e=>setCustomerId(e.target.value)} placeholder="Customer ID"/>
          <select className="w-full border rounded px-3 py-2 text-sm mt-3" value={paymentMethod} onChange={e=>setPaymentMethod(e.target.value)}>
            <option value="pm_mock_success">💳 Mock Card (always succeeds)</option>
            <option value="pm_mock_fail">❌ Mock Card (will decline)</option>
          </select>
        </div>

        <div className="bg-white rounded-lg border p-6">
          <h2 className="font-semibold mb-4">Items</h2>
          {items.map((item, idx)=>(
            <div key={idx} className="flex gap-3 mb-3">
              <select className="flex-1 border rounded px-3 py-2 text-sm" value={item.skuId}
                onChange={e=>{ const n=[...items]; n[idx]={...n[idx],skuId:e.target.value}; setItems(n) }}>
                {MOCK_SKUS.map(s=><option key={s.skuId} value={s.skuId}>{s.name} (${s.price})</option>)}
              </select>
              <input type="number" min={1} max={10} className="w-20 border rounded px-3 py-2 text-sm" value={item.quantity}
                onChange={e=>{ const n=[...items]; n[idx]={...n[idx],quantity:+e.target.value}; setItems(n) }}/>
              {items.length>1&&<button type="button" onClick={()=>setItems(items.filter((_,i)=>i!==idx))} className="text-red-500 text-sm">✕</button>}
            </div>
          ))}
          <button type="button" onClick={()=>setItems([...items,{skuId:'SKU-001',quantity:1}])}
            className="text-blue-600 text-sm hover:underline">+ Add item</button>
          <p className="mt-3 text-sm font-medium">Total: <span className="text-lg">${total.toFixed(2)}</span></p>
        </div>

        <div className="bg-white rounded-lg border p-6">
          <h2 className="font-semibold mb-4">Shipping Address</h2>
          <div className="grid grid-cols-2 gap-3">
            {(['street','city','state','zip','country'] as const).map(f=>(
              <input key={f} className={`border rounded px-3 py-2 text-sm ${f==='street'?'col-span-2':''}`}
                placeholder={f.charAt(0).toUpperCase()+f.slice(1)} value={addr[f]}
                onChange={e=>setAddr({...addr,[f]:e.target.value})}/>
            ))}
          </div>
        </div>

        <button type="submit" disabled={mutation.isPending}
          className="w-full py-3 bg-blue-600 text-white rounded-md font-medium hover:bg-blue-700 disabled:opacity-50">
          {mutation.isPending ? 'Placing Order...' : `Place Order — $${total.toFixed(2)}`}
        </button>
        {mutation.isError && <p className="text-red-600 text-sm text-center">Failed to place order. Try again.</p>}
      </form>
    </div>
  )
}
