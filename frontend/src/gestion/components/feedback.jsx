import { errorMessage } from '../../lib/apiErrors';

// Estados: etiquetas, carga, avisos y vacío.

export const Badge = ({ tone = 'neutral', children }) => <span className={`g-badge g-badge--${tone}`}>{children}</span>;

export const Spinner = () => <div className="spinner" role="status" aria-label="Cargando" />;

export const Notice = ({ tone = 'danger', children }) => (
  <div className={`g-notice g-notice--${tone}`} role={tone === 'danger' ? 'alert' : 'status'}>
    {children}
  </div>
);

export const ErrorNotice = ({ error }) => (error ? <Notice>{errorMessage(error)}</Notice> : null);

export const Empty = ({ children }) => <p className="g-empty">{children}</p>;
