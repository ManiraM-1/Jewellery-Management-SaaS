import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useApi } from '../hooks'
import { Badge, Card, Empty, Loading, PageHeader } from '../components/ui'
import { dateTime, money } from '../format'

export default function Invoices() {
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')
  const { data, error } = useApi(`/api/invoices?q=${encodeURIComponent(q)}&status=${status}`)

  return (
    <>
      <PageHeader title="Invoices">
        <input className="search" placeholder="Invoice number" value={q} onChange={(e) => setQ(e.target.value)} />
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {['paid', 'partial', 'credit', 'estimate', 'cancelled'].map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </PageHeader>
      <Card>
        {!data ? <Loading error={error} /> : data.length === 0 ? <Empty>No invoices.</Empty> : (
          <table>
            <thead><tr><th>Invoice</th><th>Date</th><th>Customer</th><th>Branch</th><th>Salesperson</th><th>Status</th><th className="num">Total</th><th className="num">Balance</th></tr></thead>
            <tbody>
              {data.map((i) => (
                <tr key={i.id}>
                  <td><Link to={`/invoices/${i.id}`} className="mono">{i.invoiceNo}</Link></td>
                  <td>{dateTime(i.billedAt)}</td>
                  <td>{i.customer?.name || 'Walk-in'}</td>
                  <td>{i.branch.name}</td>
                  <td>{i.salesperson?.name}</td>
                  <td><Badge value={i.status} /></td>
                  <td className="num">{money(i.grandTotal)}</td>
                  <td className="num">{Number(i.balanceDue) > 0 ? money(i.balanceDue) : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </>
  )
}
