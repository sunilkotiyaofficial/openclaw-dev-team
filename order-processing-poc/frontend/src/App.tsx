import { Routes, Route, Link, useLocation } from 'react-router-dom'
import { ShoppingCart, LayoutDashboard } from 'lucide-react'
import DashboardPage from './pages/DashboardPage'
import PlaceOrderPage from './pages/PlaceOrderPage'
import OrderStatusPage from './pages/OrderStatusPage'

function NavBar() {
  const loc = useLocation()
  const cls = (p: string) => `flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
    loc.pathname === p ? 'bg-blue-100 text-blue-700' : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'}`
  return (
    <nav className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
      <div className="flex items-center gap-2">
        <ShoppingCart className="h-6 w-6 text-blue-600"/>
        <span className="text-lg font-semibold">Order Processing Dashboard</span>
      </div>
      <div className="flex items-center gap-2">
        <Link to="/" className={cls('/')}><LayoutDashboard className="h-4 w-4"/>Dashboard</Link>
        <Link to="/orders/new" className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md text-sm font-medium hover:bg-blue-700">
          <ShoppingCart className="h-4 w-4"/>Place Order
        </Link>
      </div>
    </nav>
  )
}

export default function App() {
  return (
    <div className="min-h-screen flex flex-col">
      <NavBar/>
      <main className="flex-1 p-6">
        <Routes>
          <Route path="/" element={<DashboardPage/>}/>
          <Route path="/orders/new" element={<PlaceOrderPage/>}/>
          <Route path="/orders/:orderId" element={<OrderStatusPage/>}/>
        </Routes>
      </main>
    </div>
  )
}
