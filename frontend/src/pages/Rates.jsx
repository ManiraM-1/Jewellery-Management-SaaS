import { useState } from 'react'
import { api } from '../api'
import { useAuth, MANAGERIAL } from '../auth'
import { useApi } from '../hooks'
import { Alert, Badge, Card, Field, Loading, PageHeader } from '../components/ui'
import { dateTime, money } from '../format'

const PURITIES = { Gold: ['24K', '22K', '20K', '18K', '14K', '9K'], Silver: ['999', '925'], Platinum: ['950'] }

export default function Rates() {
  const { can } = useAuth()
  const { data, error, reload } = useApi('/api/rates')
  const [form, setForm] = useState({ metalType: 'Gold', purity: '24K', ratePerGram: '', autoCalculatePurities: true, notes: '' })
  const [msg, setMsg] = useState({})
  const [busy, setBusy] = useState(false)
  const managerial = can(MANAGERIAL)

  async function run(fn, success) {
    setBusy(true)
    setMsg({})
    try {
      const res = await fn()
      setMsg({ ok: typeof success === 'function' ? success(res) : success })
      reload()
    } catch (e) {
      setMsg({ err: e.message })
    } finally {
      setBusy(false)
    }
  }

  function submit(e) {
    e.preventDefault()
    run(
      () => api('/api/rates', { method: 'POST', body: { ...form, ratePerGram: Number(form.ratePerGram) } }),
      managerial ? 'Rate published.' : 'Rate submitted for manager approval.',
    )
    setForm({ ...form, ratePerGram: '', notes: '' })
  }

  if (!data) return <Loading error={error} />

  return (
    <>
      <PageHeader title="Metal Rates" subtitle="Daily bullion rates used for billing and stock valuation">
        {managerial && (
          <button className="btn" disabled={busy} onClick={() => run(() => api('/api/rates/sync', { method: 'POST', body: {} }),
            (r) => `Live rates synced: 24K ${money(r.gold24k)}/g, 22K ${money(r.gold22k)}/g, Silver ${money(r.silver999)}/g`)}>
            Fetch live rates
          </button>
        )}
      </PageHeader>
      <Alert kind="success" onClose={() => setMsg({})}>{msg.ok}</Alert>
      <Alert onClose={() => setMsg({})}>{msg.err}</Alert>

      <div className="grid two">
        <Card title="Current board">
          <div className="rate-board">
            {Object.entries(data.current).map(([k, v]) => (
              <div key={k} className="rate-tile">
                <div className="muted small">{k}</div>
                <div className="rate-value">{Number(v) > 0 ? money(v) : '—'}</div>
              </div>
            ))}
          </div>
        </Card>
        <Card title="Set a rate">
          <form className="form-grid" onSubmit={submit}>
            <Field label="Metal">
              <select value={form.metalType} onChange={(e) => setForm({ ...form, metalType: e.target.value, purity: PURITIES[e.target.value][0] })}>
                {Object.keys(PURITIES).map((m) => <option key={m}>{m}</option>)}
              </select>
            </Field>
            <Field label="Purity">
              <select value={form.purity} onChange={(e) => setForm({ ...form, purity: e.target.value })}>
                {PURITIES[form.metalType].map((p) => <option key={p}>{p}</option>)}
              </select>
            </Field>
            <Field label="Rate per gram (₹)">
              <input type="number" step="0.01" min="1" required value={form.ratePerGram} onChange={(e) => setForm({ ...form, ratePerGram: e.target.value })} />
            </Field>
            <Field label="Notes">
              <input value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
            </Field>
            {form.metalType === 'Gold' && form.purity === '24K' && (
              <label className="check wide">
                <input type="checkbox" checked={form.autoCalculatePurities} onChange={(e) => setForm({ ...form, autoCalculatePurities: e.target.checked })} />
                Also calculate 22K, 20K, 18K, 14K and 9K from this 24K rate
              </label>
            )}
            <div className="wide"><button className="btn primary" disabled={busy}>{managerial ? 'Publish rate' : 'Submit for approval'}</button></div>
          </form>
        </Card>
      </div>

      <Card title="Rate history">
        <table>
          <thead><tr><th>Effective</th><th>Metal</th><th>Purity</th><th className="num">Rate / g</th><th>Status</th><th>By</th><th>Notes</th><th /></tr></thead>
          <tbody>
            {data.history.map((r) => (
              <tr key={r.id}>
                <td>{dateTime(r.effectiveAt)}</td>
                <td>{r.metalType}</td>
                <td>{r.purity}</td>
                <td className="num">{money(r.ratePerGram)}</td>
                <td><Badge value={r.status} /></td>
                <td>{r.createdBy?.name}</td>
                <td className="muted small">{r.notes}</td>
                <td className="actions">
                  {managerial && r.status === 'pending' && (
                    <button className="btn small" onClick={() => run(() => api(`/api/rates/${r.id}/approve`, { method: 'POST' }), 'Rate approved.')}>Approve</button>
                  )}
                  {managerial && (
                    <button className="btn small danger" onClick={() => window.confirm('Delete this rate entry?') && run(() => api(`/api/rates/${r.id}`, { method: 'DELETE' }), 'Rate deleted.')}>Delete</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </>
  )
}
