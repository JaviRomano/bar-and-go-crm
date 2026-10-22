import { useState } from 'react';
import { fieldErrors } from '../lib/apiErrors';

/**
 * Estado de un formulario con los errores por campo del backend. submit(action) devuelve el resultado
 * de action o null si falló; en ese caso deja errors (por campo) y error (general) rellenos.
 */
export function useForm(initial) {
  const [values, setValues] = useState(initial);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const set = (name) => (eventOrValue) => {
    const target = eventOrValue?.target;
    const value = target ? (target.type === 'checkbox' ? target.checked : target.value) : eventOrValue;
    setValues((prev) => ({ ...prev, [name]: value }));
    setErrors((prev) => {
      if (!prev[name]) return prev;
      const next = { ...prev };
      delete next[name];
      return next;
    });
  };

  const submit = async (action) => {
    setSaving(true);
    setErrors({});
    setError(null);
    try {
      return await action();
    } catch (e) {
      setErrors(fieldErrors(e));
      setError(e);
      return null;
    } finally {
      setSaving(false);
    }
  };

  return { values, setValues, set, errors, error, saving, submit };
}
