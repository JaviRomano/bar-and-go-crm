import { useState } from 'react';

// Gráficas sin librería. Serie principal en --accent, comparativa como línea en --muted (ver
// design/tokens.js). El ámbar no llega a 3:1 sobre la superficie, así que cada gráfica va con su tabla.

const niceMax = (value) => {
  if (value <= 0) return 1;
  const magnitude = 10 ** Math.floor(Math.log10(value));
  const step = [1, 2, 2.5, 5, 10].find((s) => s * magnitude >= value / 4) * magnitude;
  return Math.ceil(value / step) * step;
};

/**
 * Columnas (serie actual) con una línea superpuesta (comparativa), mismo eje. points: [{ label, bar,
 * line }]. Los valores null se omiten (un día sin ventas no tiene food cost).
 */
export const ColumnLineChart = ({ points, barLabel, lineLabel, format, height = 220, title }) => {
  const [active, setActive] = useState(null);
  const width = 720;
  const pad = { top: 12, right: 8, bottom: 28, left: 44 };
  const plotW = width - pad.left - pad.right;
  const plotH = height - pad.top - pad.bottom;
  const values = points.flatMap((p) => [p.bar, p.line]).filter((v) => v !== null && v !== undefined).map(Number);
  const max = niceMax(Math.max(0, ...values));
  const ticks = [0, 0.25, 0.5, 0.75, 1].map((t) => t * max);
  const band = plotW / Math.max(points.length, 1);
  const barW = Math.min(24, band * 0.6);
  const x = (i) => pad.left + band * i + band / 2;
  const y = (v) => pad.top + plotH - (Number(v) / max) * plotH;
  const labelEvery = Math.ceil(points.length / 10);

  const linePath = points
    .map((p, i) => (p.line === null || p.line === undefined ? null : `${x(i)},${y(p.line)}`))
    .reduce((acc, pt) => {
      if (!pt) return [...acc, []];
      acc[acc.length - 1].push(pt);
      return acc;
    }, [[]])
    .filter((seg) => seg.length > 0)
    .map((seg) => `M${seg.join('L')}`)
    .join(' ');

  const current = active !== null ? points[active] : null;

  return (
    <figure style={{ display: 'flex', flexDirection: 'column', gap: 'var(--sp-2)' }}>
      <div className="g-legend" aria-hidden="true">
        <span><span className="g-legend__key g-legend__key--bar" />{barLabel}</span>
        <span><span className="g-legend__key g-legend__key--line" />{lineLabel}</span>
      </div>
      <div className="g-chart-readout" aria-live="polite">
        {current
          ? `${current.label}: ${barLabel} ${format(current.bar)} · ${lineLabel} ${format(current.line)}`
          : 'Pasa el ratón por la gráfica para ver cada periodo'}
      </div>
      <svg className="g-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label={title}
           onMouseLeave={() => setActive(null)}>
        {ticks.map((t) => (
          <g key={t}>
            <line className="g-chart__grid" x1={pad.left} x2={width - pad.right} y1={y(t)} y2={y(t)} />
            <text x={pad.left - 6} y={y(t) + 4} textAnchor="end">{format(t, true)}</text>
          </g>
        ))}
        {points.map((p, i) => {
          if (p.bar === null || p.bar === undefined || Number(p.bar) <= 0) return null;
          const top = y(p.bar);
          const h = pad.top + plotH - top;
          const r = Math.min(4, h, barW / 2);
          const x0 = x(i) - barW / 2;
          // Extremo de datos redondeado, base recta
          const d = `M${x0},${top + h} V${top + r} Q${x0},${top} ${x0 + r},${top} H${x0 + barW - r} Q${x0 + barW},${top} ${x0 + barW},${top + r} V${top + h} Z`;
          return <path key={i} d={d} className={`g-chart__bar${active === i ? ' g-chart__bar--active' : ''}`} />;
        })}
        {linePath && <path d={linePath} className="g-chart__line" />}
        {points.map((p, i) => (p.line === null || p.line === undefined ? null
          : <circle key={i} cx={x(i)} cy={y(p.line)} r={active === i ? 5 : 4} className="g-chart__dot" />))}
        {points.map((p, i) => (i % labelEvery === 0
          ? <text key={i} x={x(i)} y={height - 8} textAnchor="middle">{p.label}</text> : null))}
        {/* Zonas de hover más anchas que la marca */}
        {points.map((p, i) => (
          <rect key={i} x={pad.left + band * i} y={pad.top} width={band} height={plotH} fill="transparent"
                onMouseEnter={() => setActive(i)} />
        ))}
      </svg>
    </figure>
  );
};

/** Barras horizontales de una sola serie, con el valor en la punta. rows: [{ key, label, value, display }]. */
export const BarList = ({ rows, title }) => {
  const max = Math.max(0, ...rows.map((r) => Number(r.value)));
  return (
    <div className="g-barlist" role="list" aria-label={title}>
      {rows.map((row) => (
        <div key={row.key} className="g-barlist__row" role="listitem" title={`${row.label}: ${row.display}`}>
          <span className="g-barlist__label">{row.label}</span>
          <span className="g-barlist__track">
            <span className="g-barlist__bar" style={{ width: `${max > 0 ? Math.max(0, (Number(row.value) / max) * 100) : 0}%` }} />
          </span>
          <span className="g-num g-strong">{row.display}</span>
        </div>
      ))}
    </div>
  );
};
