import { useState } from 'react';
import { reports } from '../api';
import { BarList, ColumnLineChart } from '../charts';
import { MENU_CLASSES, WASTE_REASONS } from '../labels';
import { asOptions, itemLabel, useLookups } from '../lookups';
import { daysAgo, formatDate, formatEUR, formatPct, formatQty, isoDate, useLoad } from '../support';
import { Badge, DataTable, ErrorNotice, Input, Kpi, Notice, Page, Panel, Select, Spinner, Tabs, Toolbar } from '../ui';

const TABS = [
  { id: 'food-cost', label: 'Food cost' },
  { id: 'stock', label: 'Valor de stock' },
  { id: 'waste', label: 'Mermas' },
  { id: 'rotation', label: 'Rotación' },
  { id: 'prices', label: 'Precios de compra' },
  { id: 'menu', label: 'Ingeniería de menú' }
];

const ReportsPage = () => {
  const lookups = useLookups({ items: true, suppliersList: true });
  const [tab, setTab] = useState('food-cost');
  const [filters, setFilters] = useState({ from: daysAgo(29), to: isoDate(new Date()), warehouseId: '' });
  const set = (name) => (e) => setFilters({ ...filters, [name]: e.target.value });
  const usesWarehouse = !['prices', 'menu'].includes(tab);

  return (
    <Page title="Informes" subtitle="Coste, stock y carta. Importes sin IVA">
      <Panel>
        <Toolbar>
          <Input label={tab === 'stock' ? 'Valor a fecha' : 'Desde'} type="date" value={tab === 'stock' ? filters.to : filters.from}
                 onChange={set(tab === 'stock' ? 'to' : 'from')} />
          {tab !== 'stock' && <Input label="Hasta" type="date" value={filters.to} onChange={set('to')} />}
          {usesWarehouse && (
            <Select label="Almacén" placeholder="Todos" value={filters.warehouseId} options={asOptions(lookups.warehouses)}
                    onChange={set('warehouseId')} />
          )}
        </Toolbar>
      </Panel>
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'food-cost' && <FoodCost filters={filters} />}
      {tab === 'stock' && <StockValue filters={filters} />}
      {tab === 'waste' && <Waste filters={filters} />}
      {tab === 'rotation' && <Rotation filters={filters} />}
      {tab === 'prices' && <Prices filters={filters} lookups={lookups} />}
      {tab === 'menu' && <MenuEngineering filters={filters} />}
    </Page>
  );
};

const Loading = ({ state }) => (state.loading ? <Spinner /> : <ErrorNotice error={state.error} />);

const shortDate = (value) => new Date(value).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
const pctAxis = (v, axis) => (v === null || v === undefined ? '—' : axis ? `${Math.round(v)} %` : formatPct(v));

const FoodCost = ({ filters }) => {
  const report = useLoad(() => reports.foodCost(filters), [filters.from, filters.to, filters.warehouseId]);
  const r = report.data;
  if (!r) return <Loading state={report} />;
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

const StockValue = ({ filters }) => {
  const report = useLoad(() => reports.stockValue({ at: filters.to, warehouseId: filters.warehouseId }),
    [filters.to, filters.warehouseId]);
  const r = report.data;
  if (!r) return <Loading state={report} />;
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

const Waste = ({ filters }) => {
  const report = useLoad(() => reports.waste(filters), [filters.from, filters.to, filters.warehouseId]);
  const r = report.data;
  if (!r) return <Loading state={report} />;
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

const Rotation = ({ filters }) => {
  const report = useLoad(() => reports.rotation(filters), [filters.from, filters.to, filters.warehouseId]);
  const r = report.data;
  if (!r) return <Loading state={report} />;
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

const Prices = ({ filters, lookups }) => {
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
      {!r ? <Loading state={report} /> : (
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

const QUADRANTS = ['PUZZLE', 'STAR', 'DOG', 'PLOWHORSE'];

const MenuEngineering = ({ filters }) => {
  const report = useLoad(() => reports.menuEngineering({ from: filters.from, to: filters.to }), [filters.from, filters.to]);
  const r = report.data;
  if (!r) return <Loading state={report} />;
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

export default ReportsPage;
