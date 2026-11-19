import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'
import { Alert } from '../components/ui'

const DEMO = [
  ['admin@jewelerp.in', 'Admin / Owner'],
  ['manager@jewelerp.in', 'Branch Manager'],
  ['cashier@jewelerp.in', 'Cashier'],
  ['ananya@jewelerp.in', 'Salesperson'],
  ['rahim@jewelerp.in', 'Artisan'],
]

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('admin@jewelerp.in')
  const [password, setPassword] = useState('password')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={submit}>
        <div className="brand center">
          <span className="brand-mark">◆</span>
          <div className="brand-name">JewelERP</div>
        </div>
        <p className="muted center">Sign in to your showroom</p>
        <Alert>{error}</Alert>
        <label className="field">
          <span>Email</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </label>
        <label className="field">
          <span>Password</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <button className="btn primary full" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        <div className="demo-logins">
          <div className="muted small">Demo logins (password: <b>password</b>)</div>
          {DEMO.map(([e, r]) => (
            <button type="button" key={e} className="link small" onClick={() => setEmail(e)}>
              {r}
            </button>
          ))}
        </div>
      </form>
    </div>
  )
}
