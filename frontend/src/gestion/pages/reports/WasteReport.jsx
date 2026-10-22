import { reports } from '../../api';
import { WASTE_REASONS } from '../../labels';
import { Badge, BarList, DataTable, Kpi, Panel } from '../../components';
import { useLoad } from '../../../hooks/useLoad';
import { formatEUR, formatPct, formatQty } from '../../../lib/format';
import ReportState from './ReportState';

const WasteReport = ({ filters }) => {
  const report = useLoad(() => reports.waste(filters), [filters.from, filters.to, filters.warehouseId]);
  const r = report.data;
  if (!r) return <ReportState state={report} />;
  return (
    <>
      <div className="g-kpis">
        <Kpi label="Mermas del periodo" value={formatEUR(r.totalValue)} />
        <Kpi label="Registros" value={r.byReason.reduce((n, g) => n + g.count, 0)} />
      </div>
      <div className="g-columns">
        <Panel title="Por motivo">
          {r.byReason.length === 0 ? <p className="g-empty">Sin mermas</p> : (
            <BarList title="Mermas por motivo" rows={r.byReason.map((g) => ({
              key: g.reason, label: WASTE_REASONS[g.reason], value: g.value, display: `${formatEUR(g.value)} · ${formatPct(g.sharePct)}`
            }))} />
          )}
        </Panel>
        <Panel title="Artículos que más se pierden">
          {r.byItem.length === 0 ? <p className="g-empty">Sin mermas</p> : (
            <BarList title="Mermas por artículo" rows={r.byItem.slice(0, 10).map((g) => ({
              key: g.stockItemId, label: g.stockItemName, value: g.value, display: `${formatEUR(g.value)} · ${formatQty(g.quantity, g.unit)}`
            }))} />
          )}
        </Panel>
      </div>
      <Panel title="Lo registrado">
        <DataTable rowKey={(g) => `${g.kind}-${g.id}-${g.unit}`} rows={r.bySubject} empty="Sin mermas" columns={[
          { key: 'name', label: 'Qué', render: (g) => <>{g.name} {g.kind === 'PRODUCT' && <Badge>Plato</Badge>}</> },
          { key: 'count', label: 'Veces', align: 'right' },
          { key: 'quantity', label: 'Cantidad', align: 'right', render: (g) => formatQty(g.quantity, g.unit) },
          { key: 'value', label: 'Coste', align: 'right', render: (g) => formatEUR(g.value) }
        ]} />
      </Panel>
    </>
  );
};

export default WasteReport;
