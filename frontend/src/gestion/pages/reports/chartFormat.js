import { formatPct } from '../../../lib/format';

// Etiquetas de las gráficas de informes.

export const shortDate = (value) => new Date(value).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });

/** Porcentaje: redondeado en el eje, con decimales en la lectura. */
export const pctAxis = (v, axis) => (v === null || v === undefined ? '—' : axis ? `${Math.round(v)} %` : formatPct(v));
