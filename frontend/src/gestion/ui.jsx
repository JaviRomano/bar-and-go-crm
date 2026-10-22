import { useEffect, useId, useRef } from 'react';
import { X } from 'lucide-react';
import { errorMessage } from './support';
import './gestion.css';

// Piezas de interfaz de las pantallas de Gestión. Estilos en gestion.css, sobre los tokens.

export const Page = ({ title, subtitle, actions, children }) => (
  <div className="g-page">
    <header className="g-page__header">
      <div>
        <h1 className="g-page__title">{title}</h1>
        {subtitle && <p className="g-page__subtitle">{subtitle}</p>}
      </div>
      {actions && <div className="g-page__actions">{actions}</div>}
    </header>
    {children}
  </div>
);

export const Panel = ({ title, actions, children, flush = false }) => (
  <section className={`g-panel${flush ? ' g-panel--flush' : ''}`}>
    {(title || actions) && (
      <div className="g-panel__header">
        {title && <h2 className="g-panel__title">{title}</h2>}
        {actions && <div className="g-panel__actions">{actions}</div>}
      </div>
    )}
    {children}
  </section>
);

export const Toolbar = ({ children }) => <div className="g-toolbar">{children}</div>;

export const Tabs = ({ tabs, active, onChange }) => (
  <div className="g-tabs" role="tablist">
    {tabs.map((tab) => (
      <button
        key={tab.id}
        type="button"
        role="tab"
        aria-selected={active === tab.id}
        className={`g-tab${active === tab.id ? ' g-tab--active' : ''}`}
        onClick={() => onChange(tab.id)}
      >
        {tab.label}
      </button>
    ))}
  </div>
);

export const Badge = ({ tone = 'neutral', children }) => <span className={`g-badge g-badge--${tone}`}>{children}</span>;

export const Spinner = () => <div className="spinner" role="status" aria-label="Cargando" />;

export const Notice = ({ tone = 'danger', children }) => (
  <div className={`g-notice g-notice--${tone}`} role={tone === 'danger' ? 'alert' : 'status'}>
    {children}
  </div>
);

export const ErrorNotice = ({ error }) => (error ? <Notice>{errorMessage(error)}</Notice> : null);

export const Empty = ({ children }) => <p className="g-empty">{children}</p>;

/**
 * Tabla con columnas declarativas: { key, label, render?, align? }. Con loading muestra el spinner y
 * con error, el mensaje del backend.
 */
export const DataTable = ({ columns, rows, rowKey = 'id', loading, error, empty = 'Sin datos', onRowClick, footer }) => {
  if (loading) return <Spinner />;
  if (error) return <ErrorNotice error={error} />;
  if (!rows || rows.length === 0) return <Empty>{empty}</Empty>;
  return (
    <div className="g-table-wrap">
      <table className="g-table">
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key} className={col.align === 'right' ? 'g-num' : undefined}>
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr
              key={typeof rowKey === 'function' ? rowKey(row, index) : row[rowKey]}
              className={onRowClick ? 'g-row--clickable' : undefined}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
            >
              {columns.map((col) => (
                <td key={col.key} className={col.align === 'right' ? 'g-num' : undefined}>
                  {col.render ? col.render(row) : row[col.key] ?? '—'}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
        {footer && <tfoot>{footer}</tfoot>}
      </table>
    </div>
  );
};

/** Campo de formulario con etiqueta y error del backend debajo. */
export const Field = ({ label, error, hint, children, wide = false }) => {
  const id = useId();
  const control = typeof children === 'function' ? children(id) : children;
  return (
    <div className={`g-field${wide ? ' g-field--wide' : ''}${error ? ' g-field--error' : ''}`}>
      <label htmlFor={id} className="g-field__label">{label}</label>
      {control}
      {error ? <p className="g-field__error">{error}</p> : hint && <p className="g-field__hint">{hint}</p>}
    </div>
  );
};

export const Input = ({ label, error, hint, wide, ...props }) => (
  <Field label={label} error={error} hint={hint} wide={wide}>
    {(id) => <input id={id} className="g-input" aria-invalid={!!error} {...props} />}
  </Field>
);

export const Select = ({ label, error, hint, wide, options, placeholder, ...props }) => (
  <Field label={label} error={error} hint={hint} wide={wide}>
    {(id) => (
      <select id={id} className="g-input" aria-invalid={!!error} {...props}>
        {placeholder !== undefined && <option value="">{placeholder}</option>}
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    )}
  </Field>
);

export const Textarea = ({ label, error, wide = true, ...props }) => (
  <Field label={label} error={error} wide={wide}>
    {(id) => <textarea id={id} className="g-input" rows={2} aria-invalid={!!error} {...props} />}
  </Field>
);

export const FormGrid = ({ children }) => <div className="g-form-grid">{children}</div>;

/** Diálogo lateral (hoja) para formularios. Cierra con Escape. */
export const Sheet = ({ title, onClose, children, footer, wide = false }) => {
  const ref = useRef(null);
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    ref.current?.focus();
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);
  return (
    <div className="g-scrim" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`g-sheet${wide ? ' g-sheet--wide' : ''}`} role="dialog" aria-modal="true" aria-label={title}
           tabIndex={-1} ref={ref}>
        <div className="g-sheet__header">
          <h2 className="g-panel__title">{title}</h2>
          <button type="button" className="g-icon-btn" onClick={onClose} aria-label="Cerrar"><X size={20} /></button>
        </div>
        <div className="g-sheet__body">{children}</div>
        {footer && <div className="g-sheet__footer">{footer}</div>}
      </div>
    </div>
  );
};

export const Kpi = ({ label, value, detail, tone }) => (
  <div className="g-kpi">
    <span className="g-kpi__label">{label}</span>
    <span className={`g-kpi__value${tone ? ` g-kpi__value--${tone}` : ''}`}>{value}</span>
    {detail && <span className="g-kpi__detail">{detail}</span>}
  </div>
);
