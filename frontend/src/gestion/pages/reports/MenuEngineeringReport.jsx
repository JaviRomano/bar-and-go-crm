import { reports } from '../../api';
import { MENU_CLASSES } from '../../labels';
import { Badge, DataTable, Kpi, Notice, Panel } from '../../components';
import { useLoad } from '../../../hooks/useLoad';
import { formatEUR, formatPct } from '../../../lib/format';
import ReportState from './ReportState';

const QUADRANTS = ['PUZZLE', 'STAR', 'DOG', 'PLOWHORSE'];

const MenuEngineeringReport = ({ filters }) => {
  const report = useLoad(() => reports.menuEngineering({ from: filters.from, to: filters.to }), [filters.from, filters.to]);
  const r = report.data;
  if (!r) return <ReportState state={report} />;
  return (
    <>
      <div className="g-kpis">
        <Kpi label="Platos vendidos" value={r.totalSold} />
        <Kpi label="Umbral de popularidad" value={formatPct(r.popularityThresholdPct)} detail="70 % de la cuota media" />
        <Kpi label="Margen medio" value={formatEUR(r.marginThreshold)} detail="Ponderado por ventas" />
      </div>
      {r.productsWithoutRecipe.length > 0 && (
        <Notice tone="warn">Vendidos sin escandallo (fuera de la matriz): {r.productsWithoutRecipe.join(', ')}.</Notice>
      )}
      <Panel title="Matriz popularidad × margen">
        <p className="g-muted" style={{ fontSize: 'var(--fs-sm)' }}>Arriba, más margen; a la derecha, más vendidos.</p>
        <div className="g-matrix">
          {QUADRANTS.map((cls) => {
            const items = r.items.filter((i) => i.classification === cls);
            return (
              <div key={cls} className={`g-quadrant${cls === 'STAR' ? ' g-quadrant--star' : ''}`}>
                <span className="g-quadrant__title">{MENU_CLASSES[cls].label} ({items.length})</span>
                <span className="g-quadrant__hint">{MENU_CLASSES[cls].hint}</span>
                <ul>{items.map((i) => <li key={i.productId}><Badge tone={cls === 'STAR' ? 'accent' : 'neutral'}>{i.productName}</Badge></li>)}</ul>
              </div>
            );
          })}
        </div>
      </Panel>
      <Panel title="Detalle">
        <DataTable rowKey="productId" rows={r.items} empty="No hay platos con escandallo" columns={[
          { key: 'productName', label: 'Plato', render: (i) => (
            <>{i.productName} {i.costComplete === false && <Badge tone="warn">Coste incompleto</Badge>}</>
          ) },
          { key: 'classification', label: 'Clase', render: (i) => MENU_CLASSES[i.classification].label },
          { key: 'sold', label: 'Vendidos', align: 'right' },
          { key: 'mixPct', label: 'Cuota', align: 'right', render: (i) => formatPct(i.mixPct) },
          { key: 'netPrice', label: 'PVP sin IVA', align: 'right', render: (i) => formatEUR(i.netPrice) },
          { key: 'costPerPortion', label: 'Coste', align: 'right', render: (i) => formatEUR(i.costPerPortion) },
          { key: 'foodCostPct', label: 'Food cost', align: 'right', render: (i) => formatPct(i.foodCostPct) },
          { key: 'unitMargin', label: 'Margen', align: 'right', render: (i) => formatEUR(i.unitMargin) },
          { key: 'totalMargin', label: 'Margen total', align: 'right', render: (i) => formatEUR(i.totalMargin) }
        ]} />
      </Panel>
    </>
  );
};

export default MenuEngineeringReport;
