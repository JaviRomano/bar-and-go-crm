import { useEffect } from 'react';
import { CheckCircle, XCircle, AlertCircle, Info } from 'lucide-react';

const TONES = {
  success: { icon: CheckCircle, fg: 'var(--ok)', bg: 'var(--ok-soft)' },
  error: { icon: XCircle, fg: 'var(--danger)', bg: 'var(--danger-soft)' },
  warning: { icon: AlertCircle, fg: 'var(--warn)', bg: 'var(--warn-soft)' },
  info: { icon: Info, fg: 'var(--info)', bg: 'var(--info-soft)' }
};

const ConfirmModal = ({
  isOpen,
  onClose,
  title,
  message,
  type = 'success', // 'success', 'error', 'warning', 'info'
  confirmText = 'Aceptar',
  showCancel = false,
  cancelText = 'Cancelar',
  onConfirm
}) => {
  useEffect(() => {
    if (!isOpen) return undefined;
    const onKey = (e) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const tone = TONES[type] ?? TONES.info;
  const Icon = tone.icon;

  const handleConfirm = () => {
    if (onConfirm) {
      onConfirm();
    }
    onClose();
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'var(--scrim)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 'var(--z-modal)',
        padding: 'var(--sp-4)',
        animation: 'fadeIn var(--dur) var(--ease)'
      }}
    >
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-modal-title"
        style={{
          maxWidth: '480px',
          width: '100%',
          background: 'var(--surface)',
          borderRadius: 'var(--r-lg)',
          boxShadow: 'var(--shadow-2)',
          padding: 'var(--sp-6)',
          display: 'flex',
          flexDirection: 'column',
          gap: 'var(--sp-4)'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--sp-3)' }}>
          <span
            style={{
              display: 'inline-flex',
              padding: 'var(--sp-2)',
              borderRadius: 'var(--r-full)',
              background: tone.bg,
              color: tone.fg
            }}
          >
            <Icon size={24} aria-hidden="true" />
          </span>
          <h2 id="confirm-modal-title" style={{ fontSize: 'var(--fs-h3)', lineHeight: 'var(--lh-h3)' }}>
            {title}
          </h2>
        </div>

        <p style={{ color: 'var(--muted)', whiteSpace: 'pre-line' }}>{message}</p>

        <div style={{ display: 'flex', gap: 'var(--sp-3)', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
          {showCancel && (
            <button type="button" onClick={onClose} className="btn btn-outline">
              {cancelText}
            </button>
          )}
          <button type="button" onClick={handleConfirm} className="btn btn-primary" autoFocus>
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmModal;
