import { createContext, useContext, useState } from 'react'
import { api, clearSession, setSession, storedUser } from './api'

const AuthContext = createContext(null)

export const MANAGERIAL = ['admin', 'manager']
export const SELLERS = ['admin', 'manager', 'cashier', 'salesperson']
export const OLD_GOLD_ROLES = ['admin', 'manager', 'cashier']

export function AuthProvider({ children }) {
  const [user, setUser] = useState(storedUser())

  async function login(email, password) {
    const res = await api('/api/auth/login', { method: 'POST', body: { email, password } })
    setSession(res.token, res.user)
    setUser(res.user)
  }

  function logout() {
    clearSession()
    setUser(null)
  }

  const can = (roles) => !!user && roles.includes(user.role)

  return <AuthContext.Provider value={{ user, login, logout, can }}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
