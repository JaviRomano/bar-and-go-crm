import { useCallback } from 'react';
import { masterData, stockItems, suppliers, warehouses } from './api';
import { useLoad } from '../hooks/useLoad';

// Catálogos que usan casi todas las pantallas de Gestión, cargados en paralelo. Los fallos de uno no
// bloquean los demás: queda una lista vacía.
export function useLookups({ items = false, suppliersList = false, units = false } = {}) {
  const loader = useCallback(async () => {
    const safe = (promise) => promise.catch(() => []);
    const [wh, it, sp, un] = await Promise.all([
      safe(warehouses.list(false)),
      items ? safe(stockItems.list({ active: true })) : [],
      suppliersList ? safe(suppliers.list(true)) : [],
      units ? safe(masterData.units()) : []
    ]);
    return { warehouses: wh, items: it, suppliers: sp, units: un };
  }, [items, suppliersList, units]);
  const { data } = useLoad(loader, [loader]);
  return data ?? { warehouses: [], items: [], suppliers: [], units: [] };
}

export const asOptions = (list, label = (x) => x.name, value = (x) => x.id) =>
  list.map((x) => ({ value: value(x), label: label(x) }));

export const itemLabel = (item) => `${item.name} (${item.code})`;

/** Unidades en que se puede indicar una cantidad de un artículo. */
export const unitOptionsFor = (item) => {
  if (!item) return [];
  const codes = new Set([item.stockUnit, item.purchaseUnit, item.usageUnit]);
  (item.conversions ?? []).forEach((c) => {
    codes.add(c.fromUnit);
    codes.add(c.toUnit);
  });
  return [...codes].filter(Boolean).map((code) => ({ value: code, label: code }));
};
