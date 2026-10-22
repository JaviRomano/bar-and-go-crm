// Formato de importes, cantidades y fechas en es-ES.

export { formatEUR } from '../design/tokens';

const qty = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 4, useGrouping: 'always' });
const pct = new Intl.NumberFormat('es-ES', { minimumFractionDigits: 1, maximumFractionDigits: 2 });

export const formatQty = (value, unit) =>
  value === null || value === undefined ? '—' : `${qty.format(Number(value))}${unit ? ` ${unit}` : ''}`;

export const formatPct = (value) => (value === null || value === undefined ? '—' : `${pct.format(Number(value))} %`);

export const formatDate = (value) => (value ? new Date(value).toLocaleDateString('es-ES') : '—');

export const formatDateTime = (value) =>
  value ? new Date(value).toLocaleString('es-ES', { dateStyle: 'short', timeStyle: 'short' }) : '—';

/** Fecha local en formato yyyy-MM-dd (lo que espera un input date y el backend). */
export const isoDate = (date) => {
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

export const daysAgo = (days) => {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return isoDate(d);
};

/** Número de un input o null si está vacío. */
export const toNumber = (value) => (value === '' || value === null || value === undefined ? null : Number(value));
