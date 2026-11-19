import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { useAuth, MANAGERIAL } from '../auth'
import { useApi } from '../hooks'
import { Alert, Badge, Loading, PageHeader } from '../components/ui'
import { amountInWords, dateTime, money } from '../format'

function A4({ inv }) {
  const b = inv.branch
  return (
    <div className="invoice-a4">
      <div className="inv-head">
        <div>
          <div className="inv-shop">{b.name}</div>
          <div className="small">{[b.address, b.city, b.state].filter(Boolean).join(', ')}</div>
          {b.gstin && <div className="small">GSTIN: {b.gstin}</div>}
        </div>
        <div className="right">
          <div className="inv-title">{inv.status === 'estimate' ? 'ESTIMATE' : 'TAX INVOICE'}</div>
          <div className="mono">{inv.invoiceNo}</div>
          <div className="small">{dateTime(inv.billedAt)}</div>
        </div>
      </div>
      <div className="inv-party">
        <div>
          <div className="muted small">Billed to</div>
          <b>{inv.customer?.name || 'Walk-in customer'}</b>
          {inv.customer && <div className="small">{inv.customer.phone}{inv.customer.panNumber ? ` · PAN ${inv.customer.panNumber}` : ''}</div>}
        </div>
        <div className="right small">
          <div>Salesperson: {inv.salesperson?.name}</div>
          <div>Cashier: {inv.cashier?.name}</div>
          <div>{inv.gstType === 'intra' ? 'Intra-state supply' : 'Inter-state supply'}</div>
        </div>
      </div>
      <table className="inv-table">
        <thead>
          <tr><th>#</th><th>Description</th><th>HUID</th><th className="num">Gross g</th><th className="num">Net g</th><th className="num">Rate/g</th><th className="num">Metal</th><th className="num">Wastage</th><th className="num">Making</th><th className="num">Stone/HM</th><th className="num">Taxable</th></tr>
        </thead>
        <tbody>
          {inv.items.map((it, i) => (
            <tr key={it.id}>
              <td>{i + 1}</td>
              <td>{it.productName}<div className="small muted">{it.metalType} {it.purity}</div></td>
              <td className="mono">{it.huid || '—'}</td>
              <td className="num">{Number(it.grossWeight).toFixed(3)}</td>
              <td className="num">{Number(it.netWeight).toFixed(3)}</td>
              <td className="num">{money(it.metalRate)}</td>
              <td className="num">{money(it.metalValue)}</td>
              <td className="num">{money(it.wastageValue)}<div className="small muted">{it.wastagePct}%</div></td>
              <td className="num">{money(it.makingChargeAmount)}</td>
              <td className="num">{money(Number(it.stoneCharge) + Number(it.diamondCharge) + Number(it.hallmarkCharge) + Number(it.otherCharge))}</td>
              <td className="num">{money(it.taxableValue)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="inv-foot">
        <div className="small">
          <div><b>Amount in words:</b> {amountInWords(inv.grandTotal)}</div>
          <div className="muted" style={{ marginTop: 8 }}>Prices include hallmarking charges. Goods once sold can be exchanged as per store policy. HSN 7113.</div>
          {inv.payments.map((p) => (
            <div key={p.id}>Paid {money(p.amount)} by {p.method.toUpperCase()}{p.referenceNo ? ` (${p.referenceNo})` : ''}</div>
          ))}
        </div>
        <dl className="totals">
          <dt>Taxable value</dt><dd>{money(inv.subtotal)}</dd>
          {Number(inv.discountAmount) > 0 && <><dt>Discount</dt><dd>− {money(inv.discountAmount)}</dd></>}
          {inv.gstType === 'intra' ? (
            <><dt>CGST 1.5%</dt><dd>{money(inv.cgstAmount)}</dd><dt>SGST 1.5%</dt><dd>{money(inv.sgstAmount)}</dd></>
          ) : (
            <><dt>IGST 3%</dt><dd>{money(inv.igstAmount)}</dd></>
          )}
          <dt>Round off</dt><dd>{money(inv.roundOff)}</dd>
          {Number(inv.oldGoldDeduction) > 0 && <><dt>Old gold exchange</dt><dd>− {money(inv.oldGoldDeduction)}</dd></>}
          {Number(inv.schemeAdjusted) > 0 && <><dt>Scheme adjusted</dt><dd>− {money(inv.schemeAdjusted)}</dd></>}
          <dt className="grand-dt">Grand total</dt><dd className="grand-dd">{money(inv.grandTotal)}</dd>
          <dt>Paid</dt><dd>{money(inv.paidAmount)}</dd>
          <dt>Balance due</dt><dd>{money(inv.balanceDue)}</dd>
        </dl>
      </div>
      <div className="inv-sign">
        <div>Customer signature</div>
        <div>For {b.name}</div>
      </div>
    </div>
  )
}

function Thermal({ inv }) {
  return (
    <div className="invoice-thermal">
      <div className="center"><b>{inv.branch.name}</b></div>
      {inv.branch.gstin && <div className="center">GSTIN {inv.branch.gstin}</div>}
      <div className="center">{inv.status === 'estimate' ? 'ESTIMATE' : 'TAX INVOICE'}</div>
      <hr />
      <div>{inv.invoiceNo}</div>
      <div>{dateTime(inv.billedAt)}</div>
      <div>{inv.customer?.name || 'Walk-in'}</div>
      <hr />
      {inv.items.map((it) => (
        <div key={it.id} className="t-line">
          <div>{it.productName} {it.purity}</div>
          <div className="t-row"><span>{Number(it.netWeight).toFixed(3)}g @ {money(it.metalRate)}</span><span>{money(it.taxableValue)}</span></div>
        </div>
      ))}
      <hr />
      {inv.gstType === 'intra' ? (
        <>
          <div className="t-row"><span>CGST</span><span>{money(inv.cgstAmount)}</span></div>
          <div className="t-row"><span>SGST</span><span>{money(inv.sgstAmount)}</span></div>
        </>
      ) : <div className="t-row"><span>IGST</span><span>{money(inv.igstAmount)}</span></div>}
      {Number(inv.oldGoldDeduction) > 0 && <div className="t-row"><span>Old gold</span><span>-{money(inv.oldGoldDeduction)}</span></div>}
      <div className="t-row"><b>TOTAL</b><b>{money(inv.grandTotal)}</b></div>
      <div className="t-row"><span>Paid</span><span>{money(inv.paidAmount)}</span></div>
      <hr />
      <div className="center">Thank you! Visit again.</div>
    </div>
  )
}

function PrintPage({ inv, format }) {
  useEffect(() => {
    const t = setTimeout(() => window.print(), 300)
    return () => clearTimeout(t)
  }, [])
  return <div className={`print-only ${format}`}>{format === 'thermal' ? <Thermal inv={inv} /> : <A4 inv={inv} />}</div>
}

export default function InvoiceView({ printOnly }) {
  const { id } = useParams()
  const [params] = useSearchParams()
  const { can } = useAuth()
  const { data: inv, error, reload } = useApi(`/api/invoices/${id}`)
  const [format, setFormat] = useState(params.get('format') || 'a4')
  const [msg, setMsg] = useState('')

  if (!inv) return <Loading error={error} />

  if (printOnly) return <PrintPage inv={inv} format={format} />

  async function cancel() {
    const reason = window.prompt('Reason for cancelling this invoice?')
    if (reason === null) return
    try {
      await api(`/api/invoices/${inv.id}/cancel`, { method: 'POST', body: { reason } })
      reload()
    } catch (e) {
      setMsg(e.message)
    }
  }

  return (
    <>
      <PageHeader title={inv.invoiceNo} subtitle={<Badge value={inv.status} />}>
        <div className="segmented">
          <button className={format === 'a4' ? 'on' : ''} onClick={() => setFormat('a4')}>A4</button>
          <button className={format === 'thermal' ? 'on' : ''} onClick={() => setFormat('thermal')}>80mm</button>
        </div>
        <button className="btn primary" onClick={() => window.open(`/invoices/${inv.id}/print?format=${format}`, '_blank')}>Print</button>
        {can(MANAGERIAL) && inv.status !== 'cancelled' && <button className="btn danger" onClick={cancel}>Cancel invoice</button>}
        <Link to="/pos" className="btn">New bill</Link>
      </PageHeader>
      <Alert onClose={() => setMsg('')}>{msg}</Alert>
      <div className="invoice-preview">{format === 'thermal' ? <Thermal inv={inv} /> : <A4 inv={inv} />}</div>
    </>
  )
}
