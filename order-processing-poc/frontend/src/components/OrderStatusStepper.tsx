import { Check, X, ShoppingCart, CreditCard, Package, Truck, CheckCircle } from 'lucide-react'
import type { OrderStatus } from '../types/order'

const STEPS = [
  { key: 'PENDING' as OrderStatus, label: 'Order Placed', Icon: ShoppingCart },
  { key: 'PAYMENT_PROCESSING' as OrderStatus, label: 'Payment', Icon: CreditCard },
  { key: 'INVENTORY_RESERVING' as OrderStatus, label: 'Inventory', Icon: Package },
  { key: 'SHIPPING_SCHEDULED' as OrderStatus, label: 'Shipping', Icon: Truck },
  { key: 'COMPLETED' as OrderStatus, label: 'Delivered', Icon: CheckCircle },
]
const ORDER: OrderStatus[] = ['PENDING','PAYMENT_PROCESSING','INVENTORY_RESERVING','SHIPPING_SCHEDULED','COMPLETED']

function stepState(stepKey: OrderStatus, current: OrderStatus): 'complete'|'active'|'pending' {
  const si = ORDER.indexOf(stepKey), ci = ORDER.indexOf(current === 'CANCELLED' ? 'PENDING' : current)
  if (si < ci || current === 'COMPLETED') return 'complete'
  if (si === ci && !['CANCELLED','PAYMENT_FAILED','INVENTORY_FAILED','SHIPPING_FAILED'].includes(current)) return 'active'
  return 'pending'
}

export default function OrderStatusStepper({ currentStatus }: { currentStatus: OrderStatus }) {
  const failed = ['CANCELLED','PAYMENT_FAILED','INVENTORY_FAILED','SHIPPING_FAILED'].includes(currentStatus)
  return (
    <div className="w-full py-4">
      <div className="flex items-center">
        {STEPS.map(({ key, label, Icon }, idx) => {
          const state = stepState(key, currentStatus)
          const isLast = idx === STEPS.length - 1
          return (
            <div key={key} className="flex items-center flex-1 min-w-0">
              <div className="flex flex-col items-center">
                <div className={`w-12 h-12 rounded-full flex items-center justify-center border-2 transition-all
                  ${state==='complete' ? 'bg-green-500 border-green-500 text-white' : ''}
                  ${state==='active' && !failed ? 'bg-blue-500 border-blue-500 text-white animate-pulse' : ''}
                  ${state==='active' && failed ? 'bg-red-500 border-red-500 text-white' : ''}
                  ${state==='pending' ? 'bg-white border-gray-300 text-gray-400' : ''}`}>
                  {state==='complete' && <Check className="w-6 h-6"/>}
                  {state==='active' && failed && <X className="w-6 h-6"/>}
                  {state==='active' && !failed && <Icon className="w-6 h-6"/>}
                  {state==='pending' && <Icon className="w-6 h-6"/>}
                </div>
                <span className={`mt-2 text-xs font-medium text-center
                  ${state==='complete' ? 'text-green-600' : ''}
                  ${state==='active' && !failed ? 'text-blue-600' : ''}
                  ${state==='active' && failed ? 'text-red-600' : ''}
                  ${state==='pending' ? 'text-gray-400' : ''}`}>{label}</span>
              </div>
              {!isLast && <div className={`flex-1 h-1 mx-2 rounded ${state==='complete' ? 'bg-green-400' : 'bg-gray-200'}`}/>}
            </div>
          )
        })}
      </div>
      {failed && (
        <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-md">
          <p className="text-red-700 text-sm font-medium">Order {currentStatus.replace(/_/g,' ')}</p>
        </div>
      )}
    </div>
  )
}
