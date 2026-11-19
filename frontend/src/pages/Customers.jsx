import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { useApi } from '../hooks'
import { Alert, Badge, Card, Empty, Field, Loading, PageHeader } from '../components/ui'
import { money } from '../format'

const EMPTY = { name: '', phone: '', email: '', address: '', dob: '', anniversary: '', panNumber: '', groupTier: 'standard', creditLimit: '', preferences: '' }

export default function Customers() {
  const [q, setQ] = useState('')
  const [tier, setTier] = useState('')
  const { data, error, reload } = useApi(`/api/customers?q=${encodeURIComponent(q)}&tier=${tier}`)
  const [form, setForm] = useState(EMPTY)
  const [showForm, setShowForm] = useState(false)
  const [msg, setMsg] = useState({})

  async function submit(e) {
    e.preventDefault()
    try {
      const c = await api('/api/customers', {
        method: 'POST',
        body: {
          ...form,
          email: form.email || null,
          dob: form.dob || null,
          anniversary: form.anniversary || null,
          creditLimit: form.creditLimit ? Number(form.creditLimit) : null,
        },
      })
      setMsg({ ok: `${c.name} added as ${c.customerCode}.` })
      setForm(EMPTY)
      setShowForm(false)
      reload()
    } catch (err) {
      setMsg({ err: err.message })
    }
  }

  return (
    <>
      <PageHeader title="Customers">
        <input className="search" placeholder="Name or phone" value={q} onChange={(e) => setQ(e.target.value)} />
        <select value={tier} onChange={(e) => setTier(e.target.value)}>
          <option value="">All tiers</option>
          {['vip', 'gold', 'silver', 'standard'].map((t) => <option key={t}>{t}</option>)}
        </select>
        <button className="btn primary" onClick={() => setShowForm(!showForm)}>{showForm ? 'Close' : 'New customer'}</button>
      </PageHeader>
      <Alert kind="success" onClose={() => setMsg({})}>{msg.ok}</Alert>
      <Alert onClose={() => setMsg({})}>{msg.err}</Alert>

      {showForm && (
        <Card title="New customer">
          <form className="form-grid" onSubmit={submit}>
            <Field label="Name"><input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
            <Field label="Phone"><input required value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></Field>
            <Field label="Email"><input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
            <Field label="Tier">
              <select value={form.groupTier} onChange={(e) => setForm({ ...form, groupTier: e.target.value })}>
                {['standard', 'silver', 'gold', 'vip'].map((t) => <option key={t}>{t}</option>)}
              </select>
            </Field>
            <Field label="Date of birth"><input type="date" value={form.dob} onChange={(e) => setForm({ ...form, dob: e.target.value })} /></Field>
            <Field label="Anniversary"><input type="date" value={form.anniversary} onChange={(e) => setForm({ ...form, anniversary: e.target.value })} /></Field>
            <Field label="PAN"><input value={form.panNumber} onChange={(e) => setForm({ ...form, panNumber: e.target.value.toUpperCase() })} /></Field>
            <Field label="Credit limit ₹"><input type="number" min="0" value={form.creditLimit} onChange={(e) => setForm({ ...form, creditLimit: e.target.value })} /></Field>
            <Field label="Address" wide><input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} /></Field>
            <Field label="Preferences" wide><input value={form.preferences} onChange={(e) => setForm({ ...form, preferences: e.target.value })} /></Field>
            <div className="wide"><button className="btn primary">Save customer</button></div>
          </form>
        </Card>
      )}

      <Card>
        {!data ? <Loading error={error} /> : data.length === 0 ? <Empty>No customers found.</Empty> : (
          <table>
            <thead><tr><th>Code</th><th>Name</th><th>Phone</th><th>Tier</th><th className="num">Invoices</th><th className="num">Total spent</th><th className="num">Outstanding</th><th className="num">Points</th></tr></thead>
            <tbody>
              {data.map(({ customer: c, invoiceCount, totalSpent }) => (
                <tr key={c.id}>
                  <td className="mono">{c.customerCode}</td>
                  <td><Link to={`/customers/${c.id}`}>{c.name}</Link></td>
                  <td>{c.phone}</td>
                  <td><Badge value={c.groupTier} /></td>
                  <td className="num">{invoiceCount}</td>
                  <td className="num">{money(totalSpent)}</td>
                  <td className="num">{Number(c.outstandingBalance) > 0 ? money(c.outstandingBalance) : '—'}</td>
                  <td className="num">{c.loyaltyPoints}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </>
  )
}
