// Estructura de las pantallas: página, paneles, barra de filtros, pestañas y KPI.

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

export const Kpi = ({ label, value, detail, tone }) => (
  <div className="g-kpi">
    <span className="g-kpi__label">{label}</span>
    <span className={`g-kpi__value${tone ? ` g-kpi__value--${tone}` : ''}`}>{value}</span>
    {detail && <span className="g-kpi__detail">{detail}</span>}
  </div>
);
