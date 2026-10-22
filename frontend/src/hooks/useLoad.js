import { useCallback, useEffect, useState } from 'react';

/**
 * Ejecuta loader al montar y cada vez que cambian deps. Devuelve { data, loading, error, reload }.
 * Si la respuesta llega después de cambiar los filtros, se descarta.
 */
export function useLoad(loader, deps) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const [version, setVersion] = useState(0);

  // deps las indica el llamante: son los filtros que usa loader
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
