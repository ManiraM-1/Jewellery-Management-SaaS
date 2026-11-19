import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth'
import { useApi } from '../hooks'
import { Alert, Card, Empty, Field, Loading, PageHeader } from '../components/ui'
import { grams, money } from '../format'

function toLine(p) {
  return {
    key: `${p.id}`,
    inventoryItemId: p.id,
    productName: p.name,
    itemCode: p.itemCode,
    huid: p.huid,
    metalType: p.metalType,
    purity: p.purity,
    grossWeight: p.grossWeight,
    stoneWeight: p.stoneWeight,
    netWeight: p.netWeight,
    metalRate: p.metalRate,
    wastagePct: p.wastagePct,
    makingChargeType: p.makingChargeType,
    makingChargeRate: p.makingChargeRate,
    stoneCharge: p.stoneCharge,
    diamondCharge: p.diamondCharge,
    hallmarkCharge: p.hallmarkCharge,
    otherCharge: p.otherCharge,
    discount: 0,
    gstRate: p.gstRate,
  }
}

const numeric = ['metalRate', 'wastagePct', 'makingChargeRate', 'stoneCharge', 'diamondCharge', 'hallmarkCharge', 'otherCharge', 'discount', 'gstRate']

function payload(lines) {
  return lines.map((l) => {
    const out = { ...l }
    delete out.key
    delete out.itemCode
    numeric.forEach((k) => (out[k] = Number(out[k] || 0)))
    return out
  })
}

export default function Pos() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const catalog = useApi('/api/pos/catalog')
  const lookups = useApi('/api/lookups')
  const customers = useApi('/api/customers')
  const oldGold = useApi('/api/pos/old-gold')

  const [search, setSearch] = useState('')
  const [lines, setLines] = useState([])
  const [opts, setOpts] = useState({
    branchId: user.branch?.id || '',
    customerId: '',
    salespersonId: '',
    invoiceType: 'sale',
    gstType: 'intra',
    roundingMode: 'nearest_rupee',
    discountAmount: '',
    oldGoldIds: [],
    schemeAdjusted: '',
    paymentMethod: 'cash',
    paymentReference: '',
    paidAmount: '',
    notes: '',
  })
  const [calc, setCalc] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const inCart = new Set(lines.map((l) => l.inventoryItemId))
  const matches = useMemo(() => {
    const q = search.trim().toLowerCase()
    return (catalog.data || [])
      .filter((p) => !inCart.has(p.id))
      .filter((p) => !opts.branchId || String(p.branchId) === String(opts.branchId))
      .filter((p) => !q || [p.itemCode, p.barcode, p.huid, p.name].some((v) => (v || '').toLowerCase().includes(q)))
      .slice(0, 30)
  }, [catalog.data, search, lines, opts.branchId])

  useEffect(() => {
    if (lines.length === 0) {
      setCalc(null)
      return
    }
    const t = setTimeout(async () => {
      try {
        const res = await api('/api/pos/calculate', {
          method: 'POST',
          body: {
            items: payload(lines),
            discountAmount: Number(opts.discountAmount || 0),
            gstType: opts.gstType,
            roundingMode: opts.roundingMode,
            oldGoldIds: opts.oldGoldIds,
            schemeAdjusted: Number(opts.schemeAdjusted || 0),
          },
        })
        setCalc(res)
        setError('')
      } catch (e) {
        setError(e.message)
      }
    }, 250)
    return () => clearTimeout(t)
  }, [lines, opts.discountAmount, opts.gstType, opts.roundingMode, opts.oldGoldIds, opts.schemeAdjusted])

  function addPiece(p) {
    setLines([...lines, toLine(p)])
    setSearch('')
  }

  function onSearchKey(e) {
    if (e.key === 'Enter' && matches.length > 0) {
      e.preventDefault()
      const exact = matches.find((p) => p.barcode === search.trim() || p.itemCode.toLowerCase() === search.trim().toLowerCase())
      addPiece(exact || matches[0])
    }
  }

  const update = (key, field, value) => setLines(lines.map((l) => (l.key === key ? { ...l, [field]: value } : l)))
  const removeLine = (key) => setLines(lines.filter((l) => l.key !== key))

  function toggleOldGold(id) {
    const ids = opts.oldGoldIds.includes(id) ? opts.oldGoldIds.filter((x) => x !== id) : [...opts.oldGoldIds, id]
    setOpts({ ...opts, oldGoldIds: ids })
  }

  async function checkout() {
    if (!opts.branchId) {
      setError('Choose a branch first')
      return
    }
    setBusy(true)
    setError('')
    try {
      const inv = await api('/api/pos/checkout', {
        method: 'POST',
        body: {
          ...opts,
          branchId: Number(opts.branchId),
          customerId: opts.customerId ? Number(opts.customerId) : null,
          salespersonId: opts.salespersonId ? Number(opts.salespersonId) : null,
          discountAmount: Number(opts.discountAmount || 0),
          schemeAdjusted: Number(opts.schemeAdjusted || 0),
          paidAmount: Number(opts.paidAmount === '' ? calc?.grandTotal || 0 : opts.paidAmount),
          items: payload(lines),
        },
      })
      navigate(`/invoices/${inv.id}`)
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  if (!catalog.data || !lookups.data) return <Loading error={catalog.error || lookups.error} />
  const lk = lookups.data
  const customerOldGold = (oldGold.data || []).filter((o) => !opts.customerId || !o.customer || String(o.customer.id) === String(opts.customerId))

  return (
    <>
      <PageHeader title="POS Billing" subtitle="Scan a barcode or search, adjust making/wastage, and bill">
        <select value={opts.branchId} onChange={(e) => setOpts({ ...opts, branchId: e.target.value })}>
          <option value="">Choose branch…</option>
          {lk.branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </select>
      </PageHeader>
      <Alert onClose={() => setError('')}>{error}</Alert>

      <div className="pos">
        <div className="pos-left">
          <Card title="Add pieces">
            <input
              className="search big"
              placeholder="Scan barcode, or type item code / HUID / name and press Enter"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              onKeyDown={onSearchKey}
              autoFocus
            />
            <div className="catalog">
              {matches.length === 0 ? <Empty>No available pieces match.</Empty> : matches.map((p) => (
                <button key={p.id} className="catalog-item" onClick={() => addPiece(p)}>
                  <div><b>{p.name}</b></div>
                  <div className="muted small">{p.itemCode} · {p.metalType} {p.purity} · {grams(p.netWeight)} net{p.huid ? ` · HUID ${p.huid}` : ''}</div>
                  <div className="small">{money(p.metalRate)}/g</div>
                </button>
              ))}
            </div>
          </Card>

          <Card title={`Cart (${lines.length})`}>
            {lines.length === 0 ? <Empty>Cart is empty.</Empty> : lines.map((l, idx) => {
              const c = calc?.items?.[idx]
              return (
                <div className="cart-line" key={l.key}>
                  <div className="cart-head">
                    <div>
                      <b>{l.productName}</b>
                      <div className="muted small">{l.itemCode} · {l.metalType} {l.purity} · gross {grams(l.grossWeight)} · net {grams(l.netWeight)}</div>
                    </div>
                    <button className="link" onClick={() => removeLine(l.key)}>Remove</button>
                  </div>
                  <div className="cart-fields">
                    <Field label="Rate ₹/g"><input type="number" step="0.01" value={l.metalRate} onChange={(e) => update(l.key, 'metalRate', e.target.value)} /></Field>
                    <Field label="Wastage %"><input type="number" step="0.01" value={l.wastagePct} onChange={(e) => update(l.key, 'wastagePct', e.target.value)} /></Field>
                    <Field label="Making">
                      <select value={l.makingChargeType} onChange={(e) => update(l.key, 'makingChargeType', e.target.value)}>
                        <option value="per_gram">₹ / gram</option>
                        <option value="percentage">% of metal</option>
                        <option value="fixed">Fixed ₹</option>
                      </select>
                    </Field>
                    <Field label="Making value"><input type="number" step="0.01" value={l.makingChargeRate} onChange={(e) => update(l.key, 'makingChargeRate', e.target.value)} /></Field>
                    <Field label="Stone ₹"><input type="number" step="0.01" value={l.stoneCharge} onChange={(e) => update(l.key, 'stoneCharge', e.target.value)} /></Field>
                    <Field label="Discount ₹"><input type="number" step="0.01" value={l.discount} onChange={(e) => update(l.key, 'discount', e.target.value)} /></Field>
                  </div>
                  {c && (
                    <div className="breakdown">
                      <span>Metal {money(c.metalValue)}</span>
                      <span>Wastage {money(c.wastageValue)}</span>
                      <span>Making {money(c.makingChargeAmount)}</span>
                      <span>Stone/Dia {money(Number(c.stoneCharge) + Number(c.diamondCharge))}</span>
                      <span>Hallmark {money(c.hallmarkCharge)}</span>
                      <span>GST {money(c.gstAmount)}</span>
                      <b>{money(c.lineTotal)}</b>
                    </div>
                  )}
                </div>
              )
            })}
          </Card>
        </div>

        <div className="pos-right">
          <Card title="Bill details">
            <div className="form-grid one">
              <Field label="Customer">
                <select value={opts.customerId} onChange={(e) => setOpts({ ...opts, customerId: e.target.value, oldGoldIds: [] })}>
                  <option value="">Walk-in customer</option>
                  {(customers.data || []).map(({ customer: c }) => <option key={c.id} value={c.id}>{c.name} · {c.phone}</option>)}
                </select>
              </Field>
              <Field label="Salesperson">
                <select value={opts.salespersonId} onChange={(e) => setOpts({ ...opts, salespersonId: e.target.value })}>
                  <option value="">Me ({user.name})</option>
                  {lk.salespersons.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </Field>
              <div className="row gap">
                <Field label="Bill type">
                  <select value={opts.invoiceType} onChange={(e) => setOpts({ ...opts, invoiceType: e.target.value })}>
                    <option value="sale">Tax invoice</option>
                    <option value="estimate">Estimate (no stock change)</option>
                  </select>
                </Field>
                <Field label="GST">
                  <select value={opts.gstType} onChange={(e) => setOpts({ ...opts, gstType: e.target.value })}>
                    <option value="intra">Same state (CGST+SGST)</option>
                    <option value="inter">Other state (IGST)</option>
                  </select>
                </Field>
              </div>
              <div className="row gap">
                <Field label="Bill discount ₹"><input type="number" step="0.01" min="0" value={opts.discountAmount} onChange={(e) => setOpts({ ...opts, discountAmount: e.target.value })} /></Field>
                <Field label="Rounding">
                  <select value={opts.roundingMode} onChange={(e) => setOpts({ ...opts, roundingMode: e.target.value })}>
                    <option value="nearest_rupee">Nearest ₹1</option>
                    <option value="nearest_5">Nearest ₹5</option>
                    <option value="nearest_10">Nearest ₹10</option>
                    <option value="none">No rounding</option>
                  </select>
                </Field>
              </div>
              <Field label="Scheme maturity adjusted ₹"><input type="number" step="0.01" min="0" value={opts.schemeAdjusted} onChange={(e) => setOpts({ ...opts, schemeAdjusted: e.target.value })} /></Field>
              {customerOldGold.length > 0 && (
                <div className="field">
                  <span>Old gold exchange</span>
                  {customerOldGold.map((o) => (
                    <label className="check" key={o.id}>
                      <input type="checkbox" checked={opts.oldGoldIds.includes(o.id)} onChange={() => toggleOldGold(o.id)} />
                      {o.receiptNo} · {o.itemDescription} · {money(o.valuationAmount)}
                    </label>
                  ))}
                </div>
              )}
            </div>
          </Card>

          <Card title="Total" className="totals-card">
            {!calc ? <Empty>Add a piece to see the bill.</Empty> : (
              <>
                <dl className="totals">
                  <dt>Taxable value</dt><dd>{money(calc.subtotal)}</dd>
                  <dt>Making charges (incl.)</dt><dd>{money(calc.makingTotal)}</dd>
                  {Number(calc.discountAmount) > 0 && <><dt>Bill discount</dt><dd>− {money(calc.discountAmount)}</dd></>}
                  {opts.gstType === 'intra' ? (
                    <><dt>CGST 1.5%</dt><dd>{money(calc.cgstAmount)}</dd><dt>SGST 1.5%</dt><dd>{money(calc.sgstAmount)}</dd></>
                  ) : (
                    <><dt>IGST 3%</dt><dd>{money(calc.igstAmount)}</dd></>
                  )}
                  <dt>Round off</dt><dd>{money(calc.roundOff)}</dd>
                  {Number(calc.oldGoldDeduction) > 0 && <><dt>Old gold</dt><dd>− {money(calc.oldGoldDeduction)}</dd></>}
                  {Number(calc.schemeAdjusted) > 0 && <><dt>Scheme</dt><dd>− {money(calc.schemeAdjusted)}</dd></>}
                </dl>
                <div className="grand">
                  <span>Payable</span>
                  <span>{money(calc.grandTotal)}</span>
                </div>
                <div className="row gap">
                  <Field label="Payment">
                    <select value={opts.paymentMethod} onChange={(e) => setOpts({ ...opts, paymentMethod: e.target.value })}>
                      {lk.paymentMethods.map((m) => <option key={m} value={m}>{m.replace('_', ' ').toUpperCase()}</option>)}
                    </select>
                  </Field>
                  <Field label="Amount received ₹">
                    <input type="number" step="0.01" min="0" placeholder={calc.grandTotal} value={opts.paidAmount} onChange={(e) => setOpts({ ...opts, paidAmount: e.target.value })} />
                  </Field>
                </div>
                {opts.paymentMethod !== 'cash' && (
                  <Field label="Reference (UPI / card txn)"><input value={opts.paymentReference} onChange={(e) => setOpts({ ...opts, paymentReference: e.target.value })} /></Field>
                )}
                <button className="btn primary full big" disabled={busy || lines.length === 0} onClick={checkout}>
                  {busy ? 'Saving…' : opts.invoiceType === 'estimate' ? 'Save estimate' : 'Complete sale'}
                </button>
              </>
            )}
          </Card>
        </div>
      </div>
    </>
  )
}
