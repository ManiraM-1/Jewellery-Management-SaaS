import { useState } from 'react'
import { api } from '../api'
import { useAuth, MANAGERIAL } from '../auth'
import { useApi } from '../hooks'
import { Alert, Badge, Card, Empty, Field, Loading, PageHeader } from '../components/ui'
import { grams, money } from '../format'

const EMPTY = {
  productId: '', branchId: '', counterId: '', grossWeight: '', stoneWeight: '', diamondWeight: '', purity: '',
  huid: '', hallmarkCenter: '', hallmarkCharge: '45', purchaseCost: '', stoneCharge: '', diamondCharge: '', notes: '',
}

const num = (v) => (v === '' || v === null || v === undefined ? null : Number(v))

export default function Inventory() {
  const { can } = useAuth()
  const [filters, setFilters] = useState({ q: '', status: 'available', metal: '', branchId: '' })
  const qs = new URLSearchParams(Object.entries(filters).filter(([, v]) => v)).toString()
  const { data, error, reload } = useApi(`/api/inventory?${qs}`)
  const lookups = useApi('/api/lookups')
  const products = useApi('/api/products')
  const [form, setForm] = useState(EMPTY)
  const [showForm, setShowForm] = useState(false)
  const [msg, setMsg] = useState({})
  const managerial = can(MANAGERIAL)
  const lk = lookups.data

  function pickProduct(id) {
    const row = products.data?.find((r) => String(r.product.id) === id)
    setForm({ ...form, productId: id, purity: row?.product.purity || '' })
  }

  async function submit(e) {
    e.preventDefault()
    try {
      const item = await api('/api/inventory', {
        method: 'POST',
        body: {
          ...form,
          productId: Number(form.productId),
          branchId: Number(form.branchId),
          counterId: num(form.counterId),
          grossWeight: num(form.grossWeight),
          stoneWeight: num(form.stoneWeight),
          diamondWeight: num(form.diamondWeight),
          hallmarkCharge: num(form.hallmarkCharge),
          purchaseCost: num(form.purchaseCost),
          stoneCharge: num(form.stoneCharge),
          diamondCharge: num(form.diamondCharge),
          huid: form.huid || null,
        },
      })
      setMsg({ ok: `Added ${item.itemCode} (barcode ${item.barcode}).` })
      setForm({ ...EMPTY, branchId: form.branchId })
      reload()
    } catch (err) {
      setMsg({ err: err.message })
    }
  }

  async function remove(item) {
    if (!window.confirm(`Delete ${item.itemCode} from stock?`)) return
    try {
      await api(`/api/inventory/${item.id}`, { method: 'DELETE' })
      reload()
    } catch (err) {
      setMsg({ err: err.message })
    }
  }

  const counters = lk?.counters.filter((c) => !form.branchId || String(c.branch.id) === form.branchId) || []
  const net = Math.max(0, Number(form.grossWeight || 0) - Number(form.stoneWeight || 0))

  return (
    <>
      <PageHeader title="Stock / Inventory" subtitle="Every physical piece with its own weight, HUID and barcode">
        {managerial && <button className="btn primary" onClick={() => setShowForm(!showForm)}>{showForm ? 'Close' : 'Add piece'}</button>}
      </PageHeader>
      <Alert kind="success" onClose={() => setMsg({})}>{msg.ok}</Alert>
      <Alert onClose={() => setMsg({})}>{msg.err}</Alert>

      {showForm && lk && products.data && (
        <Card title="Add a piece to stock">
          <form className="form-grid" onSubmit={submit}>
            <Field label="Product">
              <select required value={form.productId} onChange={(e) => pickProduct(e.target.value)}>
                <option value="">Choose…</option>
                {products.data.map(({ product: p }) => <option key={p.id} value={p.id}>{p.sku} — {p.name}</option>)}
              </select>
            </Field>
            <Field label="Branch">
              <select required value={form.branchId} onChange={(e) => setForm({ ...form, branchId: e.target.value, counterId: '' })}>
                <option value="">Choose…</option>
                {lk.branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
            </Field>
            <Field label="Counter">
              <select value={form.counterId} onChange={(e) => setForm({ ...form, counterId: e.target.value })}>
                <option value="">—</option>
                {counters.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </Field>
            <Field label="Purity"><input required value={form.purity} onChange={(e) => setForm({ ...form, purity: e.target.value })} /></Field>
            <Field label="Gross weight (g)"><input type="number" step="0.001" min="0.001" required value={form.grossWeight} onChange={(e) => setForm({ ...form, grossWeight: e.target.value })} /></Field>
            <Field label="Stone weight (g)"><input type="number" step="0.001" min="0" value={form.stoneWeight} onChange={(e) => setForm({ ...form, stoneWeight: e.target.value })} /></Field>
            <Field label="Net weight (auto)"><input disabled value={net.toFixed(3)} /></Field>
            <Field label="Diamond (carats)"><input type="number" step="0.001" min="0" value={form.diamondWeight} onChange={(e) => setForm({ ...form, diamondWeight: e.target.value })} /></Field>
            <Field label="HUID (6 characters)"><input maxLength={6} value={form.huid} onChange={(e) => setForm({ ...form, huid: e.target.value.toUpperCase() })} /></Field>
            <Field label="Hallmark centre"><input value={form.hallmarkCenter} onChange={(e) => setForm({ ...form, hallmarkCenter: e.target.value })} /></Field>
            <Field label="Hallmark charge ₹"><input type="number" step="0.01" min="0" value={form.hallmarkCharge} onChange={(e) => setForm({ ...form, hallmarkCharge: e.target.value })} /></Field>
            <Field label="Stone charge ₹"><input type="number" step="0.01" min="0" value={form.stoneCharge} onChange={(e) => setForm({ ...form, stoneCharge: e.target.value })} /></Field>
            <Field label="Diamond charge ₹"><input type="number" step="0.01" min="0" value={form.diamondCharge} onChange={(e) => setForm({ ...form, diamondCharge: e.target.value })} /></Field>
            <Field label="Purchase cost ₹"><input type="number" step="0.01" min="0" value={form.purchaseCost} onChange={(e) => setForm({ ...form, purchaseCost: e.target.value })} /></Field>
            <Field label="Notes" wide><input value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></Field>
            <div className="wide"><button className="btn primary">Add to stock</button></div>
          </form>
        </Card>
      )}

      <Card>
        <div className="filters">
          <input className="search" placeholder="Item code, barcode, HUID or name" value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
          <select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
            <option value="">All statuses</option>
            <option value="available">Available</option>
            <option value="sold">Sold</option>
          </select>
          <select value={filters.metal} onChange={(e) => setFilters({ ...filters, metal: e.target.value })}>
            <option value="">All metals</option>
            {(lk?.metalTypes || []).map((m) => <option key={m}>{m}</option>)}
          </select>
          <select value={filters.branchId} onChange={(e) => setFilters({ ...filters, branchId: e.target.value })}>
            <option value="">All branches</option>
            {(lk?.branches || []).map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
          </select>
        </div>
        {!data ? <Loading error={error} /> : data.length === 0 ? <Empty>No pieces match.</Empty> : (
          <table>
            <thead><tr><th>Item</th><th>Product</th><th>Purity</th><th className="num">Gross</th><th className="num">Net</th><th>HUID</th><th>Branch / Counter</th><th className="num">Stone ₹</th><th>Status</th><th /></tr></thead>
            <tbody>
              {data.map((i) => (
                <tr key={i.id}>
                  <td className="mono">{i.itemCode}<div className="muted small">{i.barcode}</div></td>
                  <td>{i.product.name}</td>
                  <td>{i.product.metalType} {i.purity}</td>
                  <td className="num">{grams(i.grossWeight)}</td>
                  <td className="num">{grams(i.netWeight)}</td>
                  <td className="mono">{i.huid || '—'}</td>
                  <td>{i.branch.name}<div className="muted small">{i.counter?.name}</div></td>
                  <td className="num">{money(Number(i.stoneCharge) + Number(i.diamondCharge))}</td>
                  <td><Badge value={i.status} /></td>
                  <td className="actions">{managerial && i.status !== 'sold' && <button className="btn small danger" onClick={() => remove(i)}>Delete</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </>
  )
}
