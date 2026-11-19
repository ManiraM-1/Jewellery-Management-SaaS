import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth, OLD_GOLD_ROLES, SELLERS } from '../auth'

const NAV = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/pos', label: 'POS Billing', roles: SELLERS },
  { to: '/invoices', label: 'Invoices' },
  { to: '/rates', label: 'Metal Rates' },
  { to: '/products', label: 'Products' },
  { to: '/inventory', label: 'Stock / Inventory' },
  { to: '/customers', label: 'Customers', roles: SELLERS },
  { to: '/old-gold', label: 'Old Gold Exchange', roles: OLD_GOLD_ROLES },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">◆</span>
          <div>
            <div className="brand-name">JewelERP</div>
            <div className="brand-sub">Showroom management</div>
          </div>
        </div>
        <nav>
          {NAV.filter((n) => !n.roles || n.roles.includes(user.role)).map((n) => (
            <NavLink key={n.to} to={n.to} end={n.end} className={({ isActive }) => (isActive ? 'nav active' : 'nav')}>
              {n.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-user">
          <div className="user-name">{user.name}</div>
          <div className="user-role">{user.role}{user.branch ? ` · ${user.branch.name}` : ''}</div>
          <button
            className="btn ghost small"
            onClick={() => {
              logout()
              navigate('/login')
            }}
          >
            Sign out
          </button>
        </div>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
