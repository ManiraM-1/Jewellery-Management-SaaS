import { Link } from 'react-router-dom'
import { useApi } from '../hooks'
import { Badge, Card, Empty, Loading, PageHeader, Stat } from '../components/ui'
import { BarChart, LineChart } from '../components/Charts'
import { date, grams, money, money0 } from '../format'

export default function Dashboard() {
  const { data, error } = useApi('/api/dashboard')
  if (!data) return <Loading error={error} />

  const sales = data.salesLast7Days.map((d) => ({
    label: new Date(d.date).toLocaleDateString('en-IN', { weekday: 'short' }),
    value: d.value,
  }))
  const trend = data.gold24Trend.map((d) => ({ label: date(d.date).slice(0, 6), value: d.value }))

  return (
    <>
      <PageHeader title="Dashboard" subtitle={new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })} />
      <div className="stats">
        <Stat label="Today's sales" value={money0(data.todaySales)} hint={`${grams(data.todayGrossWeightSold)} sold today`} />
        <Stat label="Stock valuation" value={money0(data.stockValuation)} hint={`${data.stockPieces} pieces at today's rates`} />
        <Stat label="Gold in stock" value={grams(data.goldWeightInStock)} hint={`Silver ${grams(data.silverWeightInStock)}`} />
        <Stat label="Customer outstanding" value={money0(data.customerOutstanding)} hint={`${data.pendingOldGoldApprovals} old-gold approvals pending`} />
      </div>

      <div className="grid two">
        <Card title="Today's rates (per gram)">
          <div className="rate-board">
            {Object.entries(data.rates).map(([k, v]) => (
              <div key={k} className="rate-tile">
                <div className="muted small">{k}</div>
                <div className="rate-value">{Number(v) > 0 ? money(v) : '—'}</div>
              </div>
            ))}
          </div>
        </Card>
        <Card title="Gold 24K trend">
          <LineChart data={trend} format={money0} />
        </Card>
      </div>

      <div className="grid two">
        <Card title="Sales — last 7 days">
          <BarChart data={sales} format={(v) => `₹${Math.round(Number(v) / 1000)}k`} />
        </Card>
        <Card title="Staff leaderboard">
          {data.staffLeaderboard.length === 0 ? <Empty /> : (
            <table>
              <thead><tr><th>Salesperson</th><th className="num">Invoices</th><th className="num">Sales</th></tr></thead>
              <tbody>
                {data.staffLeaderboard.map((s) => (
                  <tr key={s.name}><td>{s.name}</td><td className="num">{s.invoices}</td><td className="num">{money0(s.total)}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>

      <div className="grid two">
        <Card title="Recent invoices" actions={<Link to="/invoices" className="link">View all</Link>}>
          {data.recentInvoices.length === 0 ? <Empty /> : (
            <table>
              <thead><tr><th>Invoice</th><th>Customer</th><th>Status</th><th className="num">Total</th></tr></thead>
              <tbody>
                {data.recentInvoices.map((i) => (
                  <tr key={i.id}>
                    <td><Link to={`/invoices/${i.id}`}>{i.invoiceNo}</Link></td>
                    <td>{i.customer?.name || 'Walk-in'}</td>
                    <td><Badge value={i.status} /></td>
                    <td className="num">{money(i.grandTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
        <Card title="Birthdays & anniversaries (next 7 days)">
          {data.upcomingEvents.length === 0 ? <Empty>No customer occasions this week.</Empty> : (
            <ul className="events">
              {data.upcomingEvents.map((e) => (
                <li key={`${e.customer.id}-${e.type}`}>
                  <span>{e.type === 'birthday' ? '🎂' : '💍'} <b>{e.customer.name}</b> · {e.customer.phone}</span>
                  <span className="muted">{e.daysUntil === 0 ? 'Today' : e.daysUntil === 1 ? 'Tomorrow' : `in ${e.daysUntil} days`}</span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  )
}
