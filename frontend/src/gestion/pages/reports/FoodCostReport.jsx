import { reports } from '../../api';
import { ColumnLineChart, DataTable, Kpi, Notice, Panel } from '../../components';
import { useLoad } from '../../../hooks/useLoad';
import { formatEUR, formatPct } from '../../../lib/format';
import ReportState from './ReportState';
import { shortDate, pctAxis } from './chartFormat';

const FoodCostReport = ({ filters }) => {
  const report = useLoad(() => reports.foodCost(filters), [filters.from, filters.to, filters.warehouseId]);
  const r = report.data;
  if (!r) return <ReportState state={report} />;
  const deviationTone = r.deviationPts > 0 ? 'danger' : undefined;
  return (
    <>
      <div className="g-kpis">
        <Kpi label="Ventas sin IVA" value={formatEUR(r.netRevenue)} detail={`${formatEUR(r.grossRevenue)} con IVA`} />
        <Kpi label="Food cost teórico" value={formatPct(r.theoreticalPct)} detail={formatEUR(r.theoreticalCost)} />
        <Kpi label="Food cost real" value={formatPct(r.realPct)} detail={formatEUR(r.realCost)} />
        <Kpi label="Desviación" value={r.deviationPts === null ? '—' : `${formatPct(r.deviationPts).replace(' %', '')} pts`}
             tone={deviationTone} detail="Real menos teórico" />
      </div>
      {Number(r.revenueWithoutRecipe) > 0 && (
        <Notice tone="warn">
          {formatEUR(r.revenueWithoutRecipe)} de ventas son de productos sin escandallo: cuentan como venta pero no como coste
          teórico, así que el food cost teórico sale más bajo de lo que es.
        </Notice>
      )}
      <Panel title={`Food cost ${r.bucket === 'DAY' ? 'por día' : 'por semana'}`}>
        <ColumnLineChart
          title="Food cost real frente a teórico"
          points={r.series.map((p) => ({ label: shortDate(p.start), bar: p.realPct, line: p.theoreticalPct }))}
          barLabel="Real" lineLabel="Teórico" format={pctAxis}
        />
      </Panel>
      <Panel title="De dónde sale el coste real">
        <DataTable rowKey="concept" rows={[
          { concept: 'Ventas según escandallo (teórico)', value: r.theoreticalCost },
          { concept: 'Mermas registradas', value: r.wasteCost },
          { concept: 'Ajustes y diferencias de recuento', value: r.adjustmentCost },
          { concept: 'Producción (consumo menos alta)', value: r.productionVariance }
        ]} columns={[
          { key: 'concept', label: 'Concepto' },
          { key: 'value', label: 'Importe', align: 'right', render: (row) => formatEUR(row.value) },
          { key: 'pct', label: 'Sobre ventas', align: 'right',
            render: (row) => (Number(r.netRevenue) > 0 ? formatPct((Number(row.value) * 100) / Number(r.netRevenue)) : '—') }
        ]} footer={(
          <tr><td>Coste real</td><td className="g-num">{formatEUR(r.realCost)}</td><td className="g-num">{formatPct(r.realPct)}</td></tr>
        )} />
        <p className="g-muted" style={{ fontSize: 'var(--fs-sm)' }}>
          El coste real equivale a stock inicial + compras − stock final. Es fiable cuando hay recuentos cerrados al principio
          y al final del periodo.
        </p>
      </Panel>
    </>
  );
};

export default FoodCostReport;
