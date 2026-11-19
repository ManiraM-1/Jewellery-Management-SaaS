import { useEffect, useState } from 'react'
import { api } from '../api'
import { useAuth, MANAGERIAL } from '../auth'
import { useApi } from '../hooks'
import { Alert, Badge, Card, Empty, Field, Loading, PageHeader } from '../components/ui'
import { date, grams, money } from '../format'

const PURITY_PCT = { '24K': 99.9, '22K': 91.6, '20K': 83.3, '18K': 75.0, '14K': 58.3, '9K': 37.5 }

export default function OldGold() {
  const { user, can } = useAuth()
  const { data, error, reload } = useApi('/api/old-gold')
  const lookups = useApi('/api/lookups')
  const customers = useApi('/api/customers')
  const rates = useApi('/api/rates')
  const [form, setForm] = useState({
    branchId: user.branch?.id || '', customerId: '', itemDescription: '', grossWeight: '', stoneWeight: '',
    testedPurity: '22K', purityPct: '91.6', appliedRate: '', deductionPct: '2', meltingLossPct: '1',
    settlementType: 'exchange', customerDeclaration: false,
  })
  const [valuation, setValuation] = useState(null)
  const [msg, setMsg] = useState({})
  const managerial = can(MANAGERIAL)

  useEffect(() => {
    if (rates.data && !form.appliedRate) setForm((f) => ({ ...f, appliedRate: rates.data.current['Gold 24K'] }))
  }, [rates.data])

  const body = () => ({
    ...form,
    branchId: Number(form.branchId),
    customerId: form.customerId ? Number(form.customerId) : null,
    grossWeight: Number(form.grossWeight),
    stoneWeight: Number(form.stoneWeight || 0),
    purityPct: Number(form.purityPct),
    appliedRate: Number(form.appliedRate),
    deductionPct: Number(form.deductionPct || 0),
    meltingLossPct: Number(form.meltingLossPct || 0),
  })

  useEffect(() => {
    if (!form.grossWeight || !form.appliedRate || !form.branchId || !form.itemDescription) {
      setValuation(null)
      return
    }
    const t = setTimeout(() => {
      api('/api/old-gold/calculate', { method: 'POST', body: body() }).then(setValuation).catch(() => setValuation(null))
    }, 250)
    return () => clearTimeout(t)
  }, [form])

  async function submit(e) {
    e.preventDefault()
    try {
      const r = await api('/api/old-gold', { method: 'POST', body: body() })
      setMsg({ ok: `Receipt ${r.receiptNo} created — ${money(r.valuationAmount)}${r.status === 'approved' ? '' : ' (waiting for manager approval)'}.` })
      setForm({ ...form, itemDescription: '', grossWeight: '', stoneWeight: '', customerDeclaration: false })
      reload()
    } catch (err) {
      setMsg({ err: err.message })
    }
  }

  async function act(path, method = 'POST', bodyData) {
    try {
      await api(path, { method, body: bodyData })
      reload()
    } catch (err) {
      setMsg({ err: err.message })
    }
  }

  return (
    <>
      <PageHeader title="Old Gold Exchange" subtitle="Value customer gold by fine content, then adjust it in a bill" />
      <Alert kind="success" onClose={() => setMsg({})}>{msg.ok}</Alert>
      <Alert onClose={() => setMsg({})}>{msg.err}</Alert>

      <div className="grid two">
        <Card title="Weigh & test">
          {!lookups.data ? <Loading error={lookups.error} /> : (
            <form className="form-grid" onSubmit={submit}>
              <Field label="Branch">
                <select required value={form.branchId} onChange={(e) => setForm({ ...form, branchId: e.target.value })}>
                  <option value="">Choose…</option>
                  {lookups.data.branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
                </select>
              </Field>
              <Field label="Customer">
                <select value={form.customerId} onChange={(e) => setForm({ ...form, customerId: e.target.value })}>
                  <option value="">Walk-in</option>
                  {(customers.data || []).map(({ customer: c }) => <option key={c.id} value={c.id}>{c.name} · {c.phone}</option>)}
                </select>
              </Field>
              <Field label="Item description" wide><input required value={form.itemDescription} onChange={(e) => setForm({ ...form, itemDescription: e.target.value })} /></Field>
              <Field label="Gross weight (g)"><input type="number" step="0.001" min="0.01" required value={form.grossWeight} onChange={(e) => setForm({ ...form, grossWeight: e.target.value })} /></Field>
              <Field label="Stone weight (g)"><input type="number" step="0.001" min="0" value={form.stoneWeight} onChange={(e) => setForm({ ...form, stoneWeight: e.target.value })} /></Field>
              <Field label="Tested purity">
                <select value={form.testedPurity} onChange={(e) => setForm({ ...form, testedPurity: e.target.value, purityPct: PURITY_PCT[e.target.value] })}>
                  {Object.keys(PURITY_PCT).map((p) => <option key={p}>{p}</option>)}
                </select>
              </Field>
              <Field label="Purity % (from tester)"><input type="number" step="0.01" min="1" max="100" required value={form.purityPct} onChange={(e) => setForm({ ...form, purityPct: e.target.value })} /></Field>
              <Field label="24K rate ₹/g"><input type="number" step="0.01" min="1" required value={form.appliedRate} onChange={(e) => setForm({ ...form, appliedRate: e.target.value })} /></Field>
              <Field label="Deduction %"><input type="number" step="0.01" min="0" max="50" value={form.deductionPct} onChange={(e) => setForm({ ...form, deductionPct: e.target.value })} /></Field>
              <Field label="Melting loss %"><input type="number" step="0.01" min="0" max="25" value={form.meltingLossPct} onChange={(e) => setForm({ ...form, meltingLossPct: e.target.value })} /></Field>
              <Field label="Settlement">
                <select value={form.settlementType} onChange={(e) => setForm({ ...form, settlementType: e.target.value })}>
                  <option value="exchange">Exchange against new jewellery</option>
                  <option value="cash">Cash purchase</option>
                  <option value="bank">Bank transfer</option>
                </select>
              </Field>
              <label className="check wide">
                <input type="checkbox" checked={form.customerDeclaration} onChange={(e) => setForm({ ...form, customerDeclaration: e.target.checked })} />
                Customer has signed the ownership declaration and KYC is verified
              </label>
              <div className="wide"><button className="btn primary" disabled={!form.customerDeclaration}>Create receipt</button></div>
            </form>
          )}
        </Card>
        <Card title="Valuation">
          {!valuation ? <Empty>Enter the weight and purity to see the value.</Empty> : (
            <>
              <dl className="totals">
                <dt>Net weight</dt><dd>{grams(valuation.netWeight)}</dd>
                <dt>Fine gold ({valuation.purityPct}%)</dt><dd>{grams(valuation.fineGoldWeight)}</dd>
                <dt>After {Number(valuation.deductionPct) + Number(valuation.meltingLossPct)}% deduction + melting loss</dt><dd>{grams(valuation.effectiveWeight)}</dd>
                <dt>× 24K rate</dt><dd>{money(valuation.appliedRate)}</dd>
              </dl>
              <div className="grand"><span>Value</span><span>{money(valuation.valuationAmount)}</span></div>
            </>
          )}
        </Card>
      </div>

      <Card title="Receipts">
        {!data ? <Loading error={error} /> : data.length === 0 ? <Empty /> : (
          <table>
            <thead><tr><th>Receipt</th><th>Date</th><th>Customer</th><th>Item</th><th className="num">Net</th><th className="num">Fine</th><th className="num">Value</th><th>Status</th><th /></tr></thead>
            <tbody>
              {data.map((o) => (
                <tr key={o.id}>
                  <td className="mono">{o.receiptNo}</td>
                  <td>{date(o.createdAt)}</td>
                  <td>{o.customer?.name || 'Walk-in'}</td>
                  <td>{o.itemDescription}<div className="muted small">{o.testedPurity} · {o.settlementType}</div></td>
                  <td className="num">{grams(o.netWeight)}</td>
                  <td className="num">{grams(o.fineGoldWeight)}</td>
                  <td className="num">{money(o.valuationAmount)}</td>
                  <td><Badge value={o.invoiceId ? 'exchanged' : o.status} />{o.refineryName && <div className="muted small">{o.refineryName}</div>}</td>
                  <td className="actions">
                    {managerial && o.status === 'pending_approval' && <button className="btn small" onClick={() => act(`/api/old-gold/${o.id}/approve`)}>Approve</button>}
                    {managerial && o.status === 'approved' && (
                      <button className="btn small" onClick={() => {
                        const name = window.prompt('Refinery name?')
                        if (name) act(`/api/old-gold/${o.id}/refinery`, 'POST', { refineryName: name })
                      }}>To refinery</button>
                    )}
                    {managerial && !o.invoiceId && <button className="btn small danger" onClick={() => window.confirm('Delete receipt?') && act(`/api/old-gold/${o.id}`, 'DELETE')}>Delete</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </>
  )
}
