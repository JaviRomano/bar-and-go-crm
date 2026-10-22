import { useCallback, useEffect, useState } from 'react';

export { formatEUR } from '../design/tokens';

// ---- Errores del backend ----
// Forma: { status, message, validationErrors: { campo: mensaje } }. Las claves de líneas llegan como
// "lines[0].quantity".

export const fieldErrors = (error) => error?.response?.data?.validationErrors ?? {};

export const errorMessage = (error) => {
  const data = error?.response?.data;
  if (data?.validationErrors && Object.keys(data.validationErrors).length > 0) {
    return 'Revisa los campos marcados';
  }
  return data?.message || error?.message || 'Error inesperado';
};

// ---- Formato ----

const qty = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 4, useGrouping: 'always' });
const pct = new Intl.NumberFormat('es-ES', { minimumFractionDigits: 1, maximumFractionDigits: 2 });

export const formatQty = (value, unit) =>
  value === null || value === undefined ? '—' : `${qty.format(Number(value))}${unit ? ` ${unit}` : ''}`;

export const formatPct = (value) => (value === null || value === undefined ? '—' : `${pct.format(Number(value))} %`);

export const formatDate = (value) => (value ? new Date(value).toLocaleDateString('es-ES') : '—');

export const formatDateTime = (value) =>
  value ? new Date(value).toLocaleString('es-ES', { dateStyle: 'short', timeStyle: 'short' }) : '—';

/** Fecha local en formato yyyy-MM-dd (lo que espera un input date y el backend). */
export const isoDate = (date) => {
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

export const daysAgo = (days) => {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return isoDate(d);
};

/** Número de un input o null si está vacío. */
export const toNumber = (value) => (value === '' || value === null || value === undefined ? null : Number(value));

// ---- Formularios ----

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

// ---- Carga de datos ----

/**
 * Ejecuta loader al montar y cada vez que cambian deps. Devuelve { data, loading, error, reload }.
 * Si la respuesta llega después de cambiar los filtros, se descarta.
 */
export function useLoad(loader, deps) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const [version, setVersion] = useState(0);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const load = useCallback(loader, deps);

  useEffect(() => {
    let current = true;
    setState((prev) => ({ ...prev, loading: true, error: null }));
    load()
      .then((data) => current && setState({ data, loading: false, error: null }))
      .catch((error) => current && setState({ data: null, loading: false, error }));
    return () => {
      current = false;
    };
  }, [load, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  return { ...state, reload };
}
