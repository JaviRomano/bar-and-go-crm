import { useCallback, useState } from 'react';

/**
 * Confirmación previa a las acciones que mueven stock. Uso: const confirm = useConfirm();
 * confirm.ask({ title, message, onConfirm }) y <ConfirmModal {...confirm.props} />.
 */
export function useConfirm() {
  const [state, setState] = useState(null);
  const close = useCallback(() => setState(null), []);
  return {
    ask: (options) => setState(options),
    props: {
      isOpen: !!state,
      onClose: close,
      title: state?.title,
      message: state?.message,
      type: state?.type ?? 'warning',
      confirmText: state?.confirmText ?? 'Confirmar',
      showCancel: true,
      onConfirm: state?.onConfirm
    }
  };
}
