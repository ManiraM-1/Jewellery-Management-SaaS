export function BarChart({ data, format = (v) => v, height = 160 }) {
  const max = Math.max(1, ...data.map((d) => Number(d.value)))
  return (
    <div className="bars" style={{ height }}>
      {data.map((d) => (
        <div className="bar-col" key={d.label} title={`${d.label}: ${format(d.value)}`}>
          <div className="bar-value">{Number(d.value) > 0 ? format(d.value) : ''}</div>
          <div className="bar" style={{ height: `${(Number(d.value) / max) * 100}%` }} />
          <div className="bar-label">{d.label}</div>
        </div>
      ))}
    </div>
  )
}

export function LineChart({ data, format = (v) => v, height = 160 }) {
  if (data.length < 2) return <div className="empty">Not enough rate history yet.</div>
  const values = data.map((d) => Number(d.value))
  const min = Math.min(...values)
  const max = Math.max(...values)
  const span = max - min || 1
  const w = 600
  const h = height
  const pad = 24
  const pts = data.map((d, i) => {
    const x = pad + (i * (w - pad * 2)) / (data.length - 1)
    const y = h - pad - ((Number(d.value) - min) / span) * (h - pad * 2)
    return [x, y]
  })
  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="line-chart" preserveAspectRatio="none">
      <polyline points={pts.map((p) => p.join(',')).join(' ')} fill="none" stroke="var(--gold)" strokeWidth="2.5" />
      {pts.map(([x, y], i) => (
        <g key={i}>
          <circle cx={x} cy={y} r="3.5" fill="var(--gold)" />
          <title>{`${data[i].label}: ${format(data[i].value)}`}</title>
        </g>
      ))}
      <text x={pad} y={h - 4} className="axis">{data[0].label}</text>
      <text x={w - pad} y={h - 4} className="axis" textAnchor="end">{data[data.length - 1].label}</text>
      <text x={w - pad} y={14} className="axis" textAnchor="end">{format(max)}</text>
    </svg>
  )
}
