import { useState } from 'react';
import { reports } from '../../api';
import { asOptions, itemLabel } from '../../lookups';
import { Badge, DataTable, Panel, Select, Toolbar } from '../../components';
import { useLoad } from '../../../hooks/useLoad';
import { formatEUR, formatPct } from '../../../lib/format';
import ReportState from './ReportState';

const SupplierPricesReport = ({ filters, lookups }) => {
  const [stockItemId, setStockItemId] = useState('');
  const [supplierId, setSupplierId] = useState('');
  const report = useLoad(() => reports.supplierPrices({ stockItemId, supplierId, from: filters.from, to: filters.to }),
    [stockItemId, supplierId, filters.from, filters.to]);
  const r = report.data;
  return (
    <>
      <Panel>
        <Toolbar>
          <Select label="Artículo" placeholder="Todos" value={stockItemId} options={asOptions(lookups.items, itemLabel)}
                  onChange={(e) => setStockItemId(e.target.value)} />
          <Select label="Proveedor" placeholder="Todos" value={supplierId}
                  options={asOptions(lookups.suppliers, (s) => s.tradeName || s.legalName)} onChange={(e) => setSupplierId(e.target.value)} />
        </Toolbar>
      </Panel>
      {!r ? <ReportState state={report} /> : (
        <Panel title="Precio pagado por unidad de inventario (mayores subidas primero)">
          <DataTable rowKey={(s) => `${s.stockItemId}-${s.supplierId}`} rows={r.series} empty="Sin compras en el periodo"
                     columns={[
                       { key: 'stockItemName', label: 'Artículo' },
                       { key: 'supplierName', label: 'Proveedor' },
                       { key: 'points', label: 'Compras', align: 'right', render: (s) => s.points.length },
                       { key: 'firstPrice', label: 'Primero', align: 'right', render: (s) => `${formatEUR(s.firstPrice)} / ${s.unit}` },
                       { key: 'lastPrice', label: 'Último', align: 'right', render: (s) => `${formatEUR(s.lastPrice)} / ${s.unit}` },
                       { key: 'range', label: 'Mín. – máx.', align: 'right', render: (s) => `${formatEUR(s.minPrice)} – ${formatEUR(s.maxPrice)}` },
                       { key: 'changePct', label: 'Variación', align: 'right', render: (s) => (
                         s.changePct === null ? '—'
                           : <Badge tone={Number(s.changePct) > 0 ? 'danger' : Number(s.changePct) < 0 ? 'ok' : 'neutral'}>
                               {Number(s.changePct) > 0 ? '+' : ''}{formatPct(s.changePct)}
                             </Badge>
                       ) }
                     ]} />
        </Panel>
      )}
    </>
  );
};

export default SupplierPricesReport;
