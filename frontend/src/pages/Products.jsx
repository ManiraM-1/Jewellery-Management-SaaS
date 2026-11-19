import { useState } from 'react'
import { api } from '../api'
import { useAuth, MANAGERIAL } from '../auth'
import { useApi } from '../hooks'
import { Alert, Card, Empty, Field, Loading, PageHeader } from '../components/ui'
import { label } from '../format'

const EMPTY = {
  sku: '', name: '', categoryId: '', productType: 'Ring', metalType: 'Gold', purity: '22K',
  makingChargeType: 'per_gram', makingChargeValue: '', wastagePct: '', gstRate: '3', description: '',
}

export default function Products() {
  const { can } = useAuth()
  const [q, setQ] = useState('')
  const { data, error, reload } = useApi(`/api/products?q=${encodeURIComponent(q)}`)
  const lookups = useApi('/api/lookups')
  const [form, setForm] = useState(EMPTY)
  const [showForm, setShowForm] = useState(false)
  const [msg, setMsg] = useState({})
  const managerial = can(MANAGERIAL)
  const lk = lookups.data

  function pickCategory(id) {
    const c = lk.categories.find((x) => String(x.id) === id)
    setForm({
      ...form,
      categoryId: id,
      makingChargeType: c?.defaultMakingChargeType || form.makingChargeType,
      makingChargeValue: c?.defaultMakingChargeValue ?? form.makingChargeValue,
      wastagePct: c?.defaultWastagePct ?? form.wastagePct,
    })
  }

  async function submit(e) {
    e.preventDefault()
    try {
      await api('/api/products', {
        method: 'POST',
        body: {
          ...form,
          categoryId: Number(form.categoryId),
          makingChargeValue: Number(form.makingChargeValue),
          wastagePct: Number(form.wastagePct || 0),
          gstRate: Number(form.gstRate || 3),
        },
      })
      setMsg({ ok: `Product ${form.sku} created.` })
      setForm(EMPTY)
      setShowForm(false)
      reload()
    } catch (err) {
      setMsg({ err: err.message })
    }
  }

  async function remove(p) {
    if (!window.confirm(`Delete product ${p.sku}?`)) return
    try {
      await api(`/api/products/${p.id}`, { method: 'DELETE' })
      reload()
    } catch (err) {
      setMsg({ err: err.message })
    }
  }

  return (
    <>
      <PageHeader title="Products" subtitle="Design masters — each physical piece is added under Stock">
        <input className="search" placeholder="Search name or SKU" value={q} onChange={(e) => setQ(e.target.value)} />
        {managerial && <button className="btn primary" onClick={() => setShowForm(!showForm)}>{showForm ? 'Close' : 'New product'}</button>}
      </PageHeader>
      <Alert kind="success" onClose={() => setMsg({})}>{msg.ok}</Alert>
      <Alert onClose={() => setMsg({})}>{msg.err}</Alert>

      {showForm && lk && (
        <Card title="New product">
          <form className="form-grid" onSubmit={submit}>
            <Field label="SKU"><input required value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} /></Field>
            <Field label="Name"><input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
            <Field label="Category">
              <select required value={form.categoryId} onChange={(e) => pickCategory(e.target.value)}>
                <option value="">Choose…</option>
                {lk.categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </Field>
            <Field label="Type">
              <select value={form.productType} onChange={(e) => setForm({ ...form, productType: e.target.value })}>
                {lk.productTypes.map((t) => <option key={t}>{t}</option>)}
              </select>
            </Field>
            <Field label="Metal">
              <select value={form.metalType} onChange={(e) => setForm({ ...form, metalType: e.target.value, purity: lk.purities[e.target.value][0] })}>
                {lk.metalTypes.map((m) => <option key={m}>{m}</option>)}
              </select>
            </Field>
            <Field label="Purity">
              <select value={form.purity} onChange={(e) => setForm({ ...form, purity: e.target.value })}>
                {lk.purities[form.metalType].map((p) => <option key={p}>{p}</option>)}
              </select>
            </Field>
            <Field label="Making charge type">
              <select value={form.makingChargeType} onChange={(e) => setForm({ ...form, makingChargeType: e.target.value })}>
                <option value="per_gram">Per gram (₹/g)</option>
                <option value="percentage">% of metal value</option>
                <option value="fixed">Fixed (₹ per piece)</option>
              </select>
            </Field>
            <Field label={form.makingChargeType === 'percentage' ? 'Making %' : 'Making ₹'}>
              <input type="number" step="0.01" min="0" required value={form.makingChargeValue} onChange={(e) => setForm({ ...form, makingChargeValue: e.target.value })} />
            </Field>
            <Field label="Wastage %"><input type="number" step="0.01" min="0" value={form.wastagePct} onChange={(e) => setForm({ ...form, wastagePct: e.target.value })} /></Field>
            <Field label="GST %"><input type="number" step="0.01" min="0" value={form.gstRate} onChange={(e) => setForm({ ...form, gstRate: e.target.value })} /></Field>
            <Field label="Description" wide><input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
            <div className="wide"><button className="btn primary">Save product</button></div>
          </form>
        </Card>
      )}

      <Card>
        {!data ? <Loading error={error} /> : data.length === 0 ? <Empty /> : (
          <table>
            <thead><tr><th>SKU</th><th>Name</th><th>Category</th><th>Metal</th><th>Making</th><th className="num">Wastage</th><th className="num">In stock</th><th /></tr></thead>
            <tbody>
              {data.map(({ product: p, availableCount }) => (
                <tr key={p.id}>
                  <td className="mono">{p.sku}</td>
                  <td>{p.name}<div className="muted small">{p.productType}</div></td>
                  <td>{p.category?.name}</td>
                  <td>{p.metalType} {p.purity}</td>
                  <td>{p.makingChargeType === 'percentage' ? `${p.makingChargeValue}%` : `₹${p.makingChargeValue}`} <span className="muted small">{label(p.makingChargeType)}</span></td>
                  <td className="num">{p.wastagePct}%</td>
                  <td className="num">{availableCount}</td>
                  <td className="actions">{managerial && <button className="btn small danger" onClick={() => remove(p)}>Delete</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </>
  )
}
