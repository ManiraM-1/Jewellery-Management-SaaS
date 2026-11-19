import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth, MANAGERIAL, OLD_GOLD_ROLES, SELLERS } from './auth'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Rates from './pages/Rates'
import Products from './pages/Products'
import Inventory from './pages/Inventory'
import Pos from './pages/Pos'
import Invoices from './pages/Invoices'
import InvoiceView from './pages/InvoiceView'
import Customers from './pages/Customers'
import CustomerDetail from './pages/CustomerDetail'
import OldGold from './pages/OldGold'

function Guard({ roles, children }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}

export default function App() {
  const { user } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
      <Route path="/invoices/:id/print" element={<Guard><InvoiceView printOnly /></Guard>} />
      <Route element={<Guard><Layout /></Guard>}>
        <Route index element={<Dashboard />} />
        <Route path="rates" element={<Rates />} />
        <Route path="products" element={<Products />} />
        <Route path="inventory" element={<Inventory />} />
        <Route path="pos" element={<Guard roles={SELLERS}><Pos /></Guard>} />
        <Route path="invoices" element={<Invoices />} />
        <Route path="invoices/:id" element={<InvoiceView />} />
        <Route path="customers" element={<Guard roles={SELLERS}><Customers /></Guard>} />
        <Route path="customers/:id" element={<Guard roles={SELLERS}><CustomerDetail /></Guard>} />
        <Route path="old-gold" element={<Guard roles={OLD_GOLD_ROLES}><OldGold /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export { MANAGERIAL }
