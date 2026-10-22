import { reports } from '../../api';
import { BarList, DataTable, Kpi, Panel } from '../../components';
import { useLoad } from '../../../hooks/useLoad';
import { formatDate, formatEUR, formatPct, formatQty } from '../../../lib/format';
import ReportState from './ReportState';

const StockValueReport = ({ filters }) => {
  const report = useLoad(() => reports.stockValue({ at: filters.to, warehouseId: filters.warehouseId }),
    [filters.to, filters.warehouseId]);
  const r = report.data;
  if (!r) return <ReportState state={report} />;
  return (
    <>
      <div className="g-kpis">
        <Kpi label="Valor del stock" value={formatEUR(r.totalValue)} detail={`A ${formatDate(r.at)}`} />
        <Kpi label="Artículos con stock" value={r.lines.length} />
      </div>
      <div className="g-columns">
        <Panel title="Por categoría">
          <BarList title="Valor por categoría" rows={r.byCategory.map((g) => ({
            key: g.name, label: g.name, value: g.value, display: `${formatEUR(g.value)} · ${formatPct(g.sharePct)}`
          }))} />
        </Panel>
        <Panel title="Por almacén">
          <BarList title="Valor por almacén" rows={r.byWarehouse.map((g) => ({
            key: g.id, label: g.name, value: g.value, display: `${formatEUR(g.value)} · ${formatPct(g.sharePct)}`
          }))} />
        </Panel>
      </div>
      <Panel title="Por artículo">
        <DataTable rowKey="stockItemId" rows={r.lines} empty="Sin stock" columns={[
          { key: 'stockItemName', label: 'Artículo' },
          { key: 'category', label: 'Categoría' },
          { key: 'quantity', label: 'Cantidad', align: 'right', render: (l) => formatQty(l.quantity, l.unit) },
          { key: 'averageCost', label: 'Coste medio', align: 'right', render: (l) => formatEUR(l.averageCost) },
          { key: 'value', label: 'Valor', align: 'right', render: (l) => formatEUR(l.value) }
        ]} />
      </Panel>
    </>
  );
};

export default StockValueReport;
