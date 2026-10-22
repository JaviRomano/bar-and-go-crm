// Bar&Go — tokens para lógica JS (gráficas, estados, breakpoints). Versión JS del tokens.ts del
// sistema de diseño. Los colores se leen como var(--x) para que respeten el tema activo; la fuente de
// verdad visual es styles/tokens.css.

export const cssVar = (name) => `var(--${name})`;

export const breakpoints = {
  tablet: 640,
  desktop: 1024
};

export const media = {
  mobile: `(max-width: ${breakpoints.tablet - 1}px)`,
  tablet: `(min-width: ${breakpoints.tablet}px) and (max-width: ${breakpoints.desktop - 1}px)`,
  desktop: `(min-width: ${breakpoints.desktop}px)`
};

// Claves alineadas con el enum OrderStatus del backend (el tokens.ts original usaba claves en español)
export const orderStatusTone = {
  CREATED: 'info',
  IN_PREPARATION: 'warn',
  READY: 'ok',
  DELIVERED: 'neutral',
  CANCELLED: 'danger'
};

export const toneVars = {
  info: { fg: cssVar('info'), bg: cssVar('info-soft') },
  warn: { fg: cssVar('warn'), bg: cssVar('warn-soft') },
  ok: { fg: cssVar('ok'), bg: cssVar('ok-soft') },
  neutral: { fg: cssVar('muted'), bg: cssVar('surface-2') },
  danger: { fg: cssVar('danger'), bg: cssVar('danger-soft') }
};

// Gráficas: serie actual = accent. La comparativa del tokens.ts es surface-2, pero sobre --surface no se
// distingue (1,1:1), así que aquí la comparativa es una línea en --muted.
export const chart = {
  current: cssVar('accent'),
  previous: cssVar('muted'),
  grid: cssVar('border'),
  label: cssVar('muted')
};

// useGrouping 'always' para obtener "1.284,50 €" (es-ES no agrupa 4 cifras por defecto)
const eur = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR', useGrouping: 'always' });

export const formatEUR = (value) => (value === null || value === undefined ? '—' : eur.format(Number(value)));
