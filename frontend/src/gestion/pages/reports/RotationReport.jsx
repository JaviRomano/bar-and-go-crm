import { reports } from '../../api';
import { Badge, DataTable, Kpi, Panel } from '../../components';
import { useLoad } from '../../../hooks/useLoad';
import { formatEUR } from '../../../lib/format';
import ReportState from './ReportState';

const RotationReport = ({ filters }) => {
  const report = useLoad(() => reports.rotation(filters), [filters.from, filters.to, filters.warehouseId]);
  const r = report.data;
  if (!r) return <ReportState state={report} />;
  return (
    <>
      <div className="g-kpis">
        <Kpi label="Consumo del periodo" value={formatEUR(r.totalConsumption)} detail={`${r.days} días`} />
        <Kpi label="Stock medio" value={formatEUR(r.totalAverageValue)} />
        <Kpi label="Rotación" value={r.rotation === null ? '—' : `${String(r.rotation).replace('.', ',')}×`} />
        <Kpi label="Cobertura" value={r.coverageDays === null ? '—' : `${String(r.coverageDays).replace('.', ',')} días`}
             detail="Días que dura el stock medio" />
      </div>
      <Panel title="Por artículo (primero lo que no se mueve)">
        <DataTable rowKey="stockItemId" rows={r.lines} empty="Sin stock ni consumo" columns={[
          { key: 'stockItemName', label: 'Artículo' },
          { key: 'category', label: 'Categoría' },
          { key: 'averageValue', label: 'Stock medio', align: 'right', render: (l) => formatEUR(l.averageValue) },
          { key: 'consumptionValue', label: 'Consumo', align: 'right', render: (l) => formatEUR(l.consumptionValue) },
          { key: 'rotation', label: 'Rotación', align: 'right', render: (l) => (l.rotation === null ? <Badge tone="warn">Sin consumo</Badge> : `${l.rotation}×`) },
          { key: 'coverageDays', label: 'Cobertura', align: 'right', render: (l) => (l.coverageDays === null ? '—' : `${l.coverageDays} d`) }
        ]} />
      </Panel>
    </>
  );
};

export default RotationReport;
