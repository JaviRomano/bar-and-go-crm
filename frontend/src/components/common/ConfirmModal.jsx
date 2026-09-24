import { CheckCircle, XCircle, AlertCircle } from 'lucide-react';

const ConfirmModal = ({ 
  isOpen, 
  onClose, 
  title, 
  message, 
  type = 'success', // 'success', 'error', 'warning'
  confirmText = 'Aceptar',
  showCancel = false,
  cancelText = 'Cancelar',
  onConfirm
}) => {
  if (!isOpen) return null;
  
  const getIcon = () => {
    switch (type) {
      case 'success':
        return <CheckCircle size={64} color="#10b981" />;
      case 'error':
        return <XCircle size={64} color="#ef4444" />;
      case 'warning':
        return <AlertCircle size={64} color="#f59e0b" />;
      default:
        return <CheckCircle size={64} color="#2563eb" />;
    }
  };
  
  const getBackgroundColor = () => {
    switch (type) {
      case 'success':
        return '#d1fae5';
      case 'error':
        return '#fee2e2';
      case 'warning':
        return '#fef3c7';
      default:
        return '#dbeafe';
    }
  };
  
  const getBorderColor = () => {
    switch (type) {
      case 'success':
        return '#10b981';
      case 'error':
        return '#ef4444';
      case 'warning':
        return '#f59e0b';
      default:
        return '#2563eb';
    }
  };
  
  const handleConfirm = () => {
    if (onConfirm) {
      onConfirm();
    }
    onClose();
  };
  
  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(0,0,0,0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 9999,
      padding: '1rem',
      animation: 'fadeIn 0.2s ease-in-out'
    }}>
      <div 
        className="card"
        style={{ 
          maxWidth: '500px',
          width: '100%',
          textAlign: 'center',
          animation: 'slideUp 0.3s ease-out'
        }}
      >
        {/* Icon */}
        <div style={{ marginBottom: '1.5rem' }}>
          {getIcon()}
        </div>
        
        {/* Title */}
        <h2 style={{ 
          fontSize: '1.5rem',
          fontWeight: 'bold',
          marginBottom: '1rem',
          color: 'var(--text-primary)'
        }}>
          {title}
        </h2>
        
        {/* Message */}
        <div 
          className="card"
          style={{
            backgroundColor: getBackgroundColor(),
            border: `2px solid ${getBorderColor()}`,
            marginBottom: '2rem',
            padding: '1rem'
          }}
        >
          <p style={{ 
            fontSize: '1rem',
            color: 'var(--text-primary)',
            lineHeight: '1.6',
            whiteSpace: 'pre-line'
          }}>
            {message}
          </p>
        </div>
        
        {/* Buttons */}
        <div style={{
          display: 'flex',
          gap: '1rem',
          justifyContent: 'center'
        }}>
          {showCancel && (
            <button
              onClick={onClose}
              className="btn btn-outline"
              style={{ minWidth: '120px' }}
            >
              {cancelText}
            </button>
          )}
          <button
            onClick={handleConfirm}
            className="btn btn-primary"
            style={{ minWidth: '120px' }}
          >
            {confirmText}
          </button>
        </div>
      </div>
      
      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        
        @keyframes slideUp {
          from { 
            opacity: 0;
            transform: translateY(20px); 
          }
          to { 
            opacity: 1;
            transform: translateY(0); 
          }
        }
      `}</style>
    </div>
  );
};

export default ConfirmModal;