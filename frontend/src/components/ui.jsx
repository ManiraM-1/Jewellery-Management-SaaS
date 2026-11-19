export function PageHeader({ title, subtitle, children }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      <div className="row gap">{children}</div>
    </div>
  )
}

export function Card({ title, actions, children, className = '' }) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <div className="card-head">
          <h2>{title}</h2>
          <div className="row gap">{actions}</div>
        </div>
      )}
      {children}
    </section>
  )
}

export function Stat({ label, value, hint }) {
  return (
    <div className="stat">
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </div>
  )
}

export function Field({ label, children, wide }) {
  return (
    <label className={wide ? 'field wide' : 'field'}>
      <span>{label}</span>
      {children}
    </label>
  )
}

export function Alert({ kind = 'error', children, onClose }) {
  if (!children) return null
  return (
    <div className={`alert ${kind}`}>
      <span>{children}</span>
      {onClose && <button className="link" onClick={onClose}>✕</button>}
    </div>
  )
}

const BADGE = {
  paid: 'green', approved: 'green', available: 'green', partial: 'amber', pending: 'amber',
  pending_approval: 'amber', credit: 'red', cancelled: 'grey', sold: 'grey', estimate: 'blue',
  refinery_transferred: 'blue', exchanged: 'blue', vip: 'gold', gold: 'gold', silver: 'grey', standard: 'grey',
}

export function Badge({ value }) {
  return <span className={`badge ${BADGE[value] || 'grey'}`}>{(value || '').replace(/_/g, ' ')}</span>
}

export function Empty({ children = 'Nothing here yet.' }) {
  return <div className="empty">{children}</div>
}

export function Loading({ error }) {
  if (error) return <Alert>{error}</Alert>
  return <div className="empty">Loading…</div>
}
