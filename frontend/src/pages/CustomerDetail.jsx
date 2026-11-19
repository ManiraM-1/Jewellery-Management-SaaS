import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import { useAuth, MANAGERIAL } from '../auth'
import { useApi } from '../hooks'
import { Alert, Badge, Card, Empty, Field, Loading, PageHeader, Stat } from '../components/ui'
import { date, dateTime, grams, money } from '../format'

export default function CustomerDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { can } = useAuth()
  const { data, error, reload } = useApi(`/api/customers/${id}`)
  const [loyalty, setLoyalty] = useState({ type: 'redeem', points: '', description: '' })
  const [msg, setMsg] = useState({})

  if (!data) return <Loading error={error} />
  const c = data.customer
  const spent = data.invoices.filter((i) => !['cancelled', 'estimate'].includes(i.status)).reduce((s, i) => s + Number(i.grandTotal), 0)

  async function adjust(e) {
    e.preventDefault()
    try {
      await api(`/api/customers/${id}/loyalty`, { method: 'POST', body: { ...loyalty, points: Number(loyalty.points) } })
      setLoyalty({ type: 'redeem', points: '', description: '' })
      setMsg({ ok: 'Loyalty points updated.' })
      reload()
    } catch (err) {
      setMsg({ err: err.message })
    }
  }

  async function remove() {
    if (!window.confirm(`Delete ${c.name}?`)) return
    try {
      await api(`/api/customers/${id}`, { method: 'DELETE' })
      navigate('/customers')
    } catch (err) {
      setMsg({ err: err.message })
    }
  }

  return (
    <>
      <PageHeader title={c.name} subtitle={`${c.customerCode} · ${c.phone}${c.email ? ' · ' + c.email : ''}`}>
        <Badge value={c.groupTier} />
        {can(MANAGERIAL) && <button className="btn danger" onClick={remove}>Delete</button>}
      </PageHeader>
      <Alert kind="success" onClose={() => setMsg({})}>{msg.ok}</Alert>
      <Alert onClose={() => setMsg({})}>{msg.err}</Alert>

      <div className="stats">
        <Stat label="Total spent" value={money(spent)} hint={`${data.invoices.length} invoices`} />
        <Stat label="Outstanding" value={money(c.outstandingBalance)} hint={`Credit limit ${money(c.creditLimit)}`} />
        <Stat label="Loyalty points" value={c.loyaltyPoints} hint="1 point per ₹500 billed" />
        <Stat label="Occasions" value={c.dob ? date(c.dob) : '—'} hint={c.anniversary ? `Anniversary ${date(c.anniversary)}` : 'Birthday'} />
      </div>
      {c.preferences && <Card title="Preferences"><p>{c.preferences}</p></Card>}

      <div className="grid two">
        <Card title="Invoices">
          {data.invoices.length === 0 ? <Empty /> : (
            <table>
              <thead><tr><th>Invoice</th><th>Date</th><th>Status</th><th className="num">Total</th></tr></thead>
              <tbody>
                {data.invoices.map((i) => (
                  <tr key={i.id}>
                    <td><Link to={`/invoices/${i.id}`} className="mono">{i.invoiceNo}</Link></td>
                    <td>{dateTime(i.billedAt)}</td>
                    <td><Badge value={i.status} /></td>
                    <td className="num">{money(i.grandTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
        <Card title="Old gold brought in">
          {data.oldGold.length === 0 ? <Empty /> : (
            <table>
              <thead><tr><th>Receipt</th><th>Item</th><th className="num">Net</th><th className="num">Value</th><th>Status</th></tr></thead>
              <tbody>
                {data.oldGold.map((o) => (
                  <tr key={o.id}>
                    <td className="mono">{o.receiptNo}</td>
                    <td>{o.itemDescription}</td>
                    <td className="num">{grams(o.netWeight)}</td>
                    <td className="num">{money(o.valuationAmount)}</td>
                    <td><Badge value={o.invoiceId ? 'exchanged' : o.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>

      <div className="grid two">
        <Card title="Loyalty history">
          {data.loyalty.length === 0 ? <Empty /> : (
            <table>
              <thead><tr><th>Date</th><th>Type</th><th className="num">Points</th><th>Note</th></tr></thead>
              <tbody>
                {data.loyalty.map((t) => (
                  <tr key={t.id}><td>{dateTime(t.createdAt)}</td><td>{t.type}</td><td className="num">{t.type === 'redeem' ? '−' : '+'}{t.points}</td><td>{t.description}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
        {can(['admin', 'manager', 'cashier']) && (
          <Card title="Adjust loyalty points">
            <form className="form-grid" onSubmit={adjust}>
              <Field label="Action">
                <select value={loyalty.type} onChange={(e) => setLoyalty({ ...loyalty, type: e.target.value })}>
                  <option value="redeem">Redeem</option>
                  <option value="earn">Add (earn)</option>
                  <option value="adjust">Manual adjust (+)</option>
                </select>
              </Field>
              <Field label="Points"><input type="number" min="1" required value={loyalty.points} onChange={(e) => setLoyalty({ ...loyalty, points: e.target.value })} /></Field>
              <Field label="Reason" wide><input required value={loyalty.description} onChange={(e) => setLoyalty({ ...loyalty, description: e.target.value })} /></Field>
              <div className="wide"><button className="btn primary">Save</button></div>
            </form>
          </Card>
        )}
      </div>
    </>
  )
}
