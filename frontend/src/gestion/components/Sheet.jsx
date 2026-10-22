import { useEffect, useRef } from 'react';
import { X } from 'lucide-react';

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
