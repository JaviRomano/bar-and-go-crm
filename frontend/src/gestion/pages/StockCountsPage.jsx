import { useState } from 'react';
import { Plus } from 'lucide-react';
import { useAuth } from '../../context/useAuth';
import ConfirmModal from '../../components/common/ConfirmModal';
import { stockCounts, waste } from '../api';
import { COUNT_STATUS, COUNT_TONES } from '../labels';
import { asOptions, itemLabel, useConfirm, useLookups } from '../lookups';
import { formatDateTime, formatEUR, formatPct, formatQty, toNumber, useForm, useLoad } from '../support';
import {
  Badge, DataTable, ErrorNotice, FormGrid, Input, Kpi, Notice, Page, Panel, Select, Sheet, Tabs, Textarea, Toolbar
} from '../ui';

const StockCountsPage = () => {
  const { canManage } = useAuth();
  const manager = canManage();
  const [tab, setTab] = useState('counts');
  return (
    <Page title="Recuentos" subtitle="Inventario físico: lo contado corrige el stock al cerrar">
      {manager && (
        <Tabs active={tab} onChange={setTab} tabs={[
          { id: 'counts', label: 'Recuentos' }, { id: 'consumption', label: 'Consumo teórico frente a real' }
        ]} />
      )}
      {tab === 'counts' ? <Counts manager={manager} /> : <Consumption />}
    </Page>
  );
};

const Counts = ({ manager }) => {
  const lookups = useLookups();
  const [warehouseId, setWarehouseId] = useState('');
  const [openId, setOpenId] = useState(null);
  const [creating, setCreating] = useState(false);
  const list = useLoad(() => stockCounts.list(warehouseId), [warehouseId]);

  return (
    <Panel actions={manager && (
      <button type="button" className="btn btn-primary" onClick={() => setCreating(true)}><Plus size={18} />Abrir recuento</button>
    )}>
      <Toolbar>
        <Select label="Almacén" placeholder="Todos" value={warehouseId} options={asOptions(lookups.warehouses)}
                onChange={(e) => setWarehouseId(e.target.value)} />
      </Toolbar>
      <DataTable loading={list.loading} error={list.error} rows={list.data} empty="No hay recuentos"
                 onRowClick={(c) => setOpenId(c.id)}
                 columns={[
                   { key: 'id', label: 'Nº', render: (c) => <span className="g-strong">#{c.id}</span> },
                   { key: 'warehouseName', label: 'Almacén' },
                   { key: 'status', label: 'Estado', render: (c) => <Badge tone={COUNT_TONES[c.status]}>{COUNT_STATUS[c.status]}</Badge> },
                   { key: 'openedAt', label: 'Abierto', render: (c) => formatDateTime(c.openedAt) },
                   { key: 'closedAt', label: 'Cerrado', render: (c) => formatDateTime(c.closedAt) },
                   { key: 'lines', label: 'Contados', align: 'right',
                     render: (c) => `${c.lines.filter((l) => l.countedQuantity != null).length} / ${c.lines.length}` },
                   ...(manager ? [{ key: 'totalDifferenceValue', label: 'Diferencia', align: 'right', render: (c) => formatEUR(c.totalDifferenceValue) }] : [])
                 ]} />
      {creating && <OpenCountSheet lookups={lookups} onClose={(created) => {
        setCreating(false);
        if (created) { list.reload(); setOpenId(created.id); }
      }} />}
      {openId && <CountSheet id={openId} manager={manager} onClose={() => { setOpenId(null); list.reload(); }} />}
    </Panel>
  );
};

const OpenCountSheet = ({ lookups, onClose }) => {
  const form = useForm({ warehouseId: '', notes: '' });
  const { values: v, set, errors } = form;
  const save = async (e) => {
    e.preventDefault();
    const created = await form.submit(() => stockCounts.open({ warehouseId: toNumber(v.warehouseId), notes: v.notes || null }));
    if (created) onClose(created);
  };
  return (
    <Sheet title="Abrir recuento" onClose={() => onClose(null)} footer={(
      <>
        <button type="button" className="btn btn-outline" onClick={() => onClose(null)}>Cancelar</button>
        <button type="submit" form="open-count" className="btn btn-primary" disabled={form.saving}>Abrir</button>
      </>
    )}>
      <Notice tone="info">Cuenta con el almacén parado: los movimientos entre el conteo y el cierre desvían el resultado.</Notice>
      <form id="open-count" onSubmit={save} noValidate>
        <FormGrid>
          <Select label="Almacén" placeholder="Elige un almacén" value={v.warehouseId} onChange={set('warehouseId')}
                  options={asOptions(lookups.warehouses)} error={errors.warehouseId} />
          <Textarea label="Notas" value={v.notes} onChange={set('notes')} error={errors.notes} />
        </FormGrid>
      </form>
      <ErrorNotice error={form.error} />
    </Sheet>
  );
};

const CountSheet = ({ id, manager, onClose }) => {
  const count = useLoad(() => stockCounts.get(id), [id]);
  const { items } = useLookups({ items: true });
  const [entries, setEntries] = useState({});
  const [extra, setExtra] = useState({ stockItemId: '', quantity: '' });
  const form = useForm({});
  const confirm = useConfirm();
  const data = count.data;
  const closed = data?.status === 'CLOSED';

  const value = (line) => entries[line.stockItemId] ?? (line.countedQuantity ?? '');
  const dirty = Object.keys(entries).length > 0;

  const save = async () => {
    const list = Object.entries(entries).filter(([, q]) => q !== '')
      .map(([stockItemId, q]) => ({ stockItemId: Number(stockItemId), countedQuantity: Number(q) }));
    if (list.length === 0) return;
    const saved = await form.submit(() => stockCounts.record(id, list));
    if (saved) { setEntries({}); count.reload(); }
  };

  // Un artículo que no estaba en la hoja (no había tenido stock en este almacén)
  const addExtra = async () => {
    const saved = await form.submit(() => stockCounts.record(id, [
      { stockItemId: Number(extra.stockItemId), countedQuantity: Number(extra.quantity) }
    ]));
    if (saved) { setExtra({ stockItemId: '', quantity: '' }); count.reload(); }
  };
  const listed = new Set((data?.lines ?? []).map((l) => l.stockItemId));

  const close = () => confirm.ask({
    title: 'Cerrar recuento',
    message: 'Cada diferencia entre lo contado y el stock del sistema genera un ajuste. Las líneas sin contar no se ajustan. No se puede deshacer.',
    confirmText: 'Cerrar y ajustar',
    onConfirm: async () => {
      if (dirty) await save();
      const closedCount = await form.submit(() => stockCounts.close(id));
      if (closedCount) count.reload();
    }
  });

  return (
    <Sheet
      title={data ? `Recuento #${data.id} · ${data.warehouseName}` : 'Recuento'}
      onClose={onClose}
      wide
      footer={!closed && data && (
        <>
          <button type="button" className="btn btn-outline" disabled={!dirty || form.saving} onClick={save}>Guardar conteo</button>
          {manager && <button type="button" className="btn btn-primary" disabled={form.saving} onClick={close}>Cerrar recuento</button>}
        </>
      )}
    >
      {data && (
        <dl className="g-dl">
          <dt>Estado</dt><dd><Badge tone={COUNT_TONES[data.status]}>{COUNT_STATUS[data.status]}</Badge></dd>
          <dt>Abierto</dt><dd>{formatDateTime(data.openedAt)}</dd>
          {closed && <><dt>Cerrado</dt><dd>{formatDateTime(data.closedAt)}</dd></>}
          {closed && manager && <><dt>Diferencia valorada</dt><dd className="g-strong">{formatEUR(data.totalDifferenceValue)}</dd></>}
          {data.notes && <><dt>Notas</dt><dd>{data.notes}</dd></>}
        </dl>
      )}
      <ErrorNotice error={form.error} />
      {data && !closed && (
        <Toolbar>
          <Select label="Artículo no listado" placeholder="Elige" value={extra.stockItemId}
                  options={asOptions(items.filter((i) => !listed.has(i.id)), itemLabel)}
                  onChange={(e) => setExtra({ ...extra, stockItemId: e.target.value })} />
          <Input label="Contado (unidad de inventario)" type="number" min="0" step="any" value={extra.quantity}
                 onChange={(e) => setExtra({ ...extra, quantity: e.target.value })} />
          <button type="button" className="btn btn-outline" disabled={!extra.stockItemId || extra.quantity === '' || form.saving}
                  onClick={addExtra}>Añadir</button>
        </Toolbar>
      )}
      <DataTable loading={count.loading} error={count.error} rows={data?.lines} empty="Sin líneas"
                 columns={[
                   { key: 'stockItemName', label: 'Artículo', render: (l) => (
                     <><div className="g-strong">{l.stockItemName}</div><div className="g-code">{l.stockItemCode}</div></>
                   ) },
                   { key: 'countedQuantity', label: `Contado`, align: 'right', render: (l) => (closed
                     ? formatQty(l.countedQuantity, l.unit)
                     : (
                       <input className="g-input" style={{ maxWidth: 140 }} type="number" min="0" step="any"
                              aria-label={`Cantidad contada de ${l.stockItemName} en ${l.unit}`}
                              value={value(l)} onChange={(e) => setEntries({ ...entries, [l.stockItemId]: e.target.value })} />
                     )) },
                   { key: 'unit', label: 'Unidad' },
                   ...(closed && manager ? [
                     { key: 'theoreticalQuantity', label: 'Teórico', align: 'right', render: (l) => formatQty(l.theoreticalQuantity) },
                     { key: 'difference', label: 'Diferencia', align: 'right', render: (l) => (
                       l.difference == null ? '—'
                         : <Badge tone={Number(l.difference) < 0 ? 'danger' : Number(l.difference) > 0 ? 'ok' : 'neutral'}>{formatQty(l.difference)}</Badge>
                     ) },
                     { key: 'differenceValue', label: 'Valor', align: 'right', render: (l) => formatEUR(l.differenceValue) }
                   ] : [])
                 ]} />
      <ConfirmModal {...confirm.props} />
    </Sheet>
  );
};

const Consumption = () => {
  const closedCounts = useLoad(() => stockCounts.list().then((list) => list.filter((c) => c.status === 'CLOSED')), []);
  const [selection, setSelection] = useState({ from: '', to: '' });
  const report = useLoad(
    () => (selection.from && selection.to ? waste.consumption(selection.from, selection.to) : Promise.resolve(null)),
    [selection.from, selection.to]
  );
  const options = asOptions(closedCounts.data ?? [], (c) => `#${c.id} · ${c.warehouseName} · ${formatDateTime(c.closedAt)}`);
  const r = report.data;

  return (
    <>
      <Panel>
        <Notice tone="info">
          Real = contado inicial + entradas − traspasos de salida − contado final. Teórico = lo que dicen los escandallos de
          lo vendido y producido. Lo que no explican las mermas registradas queda como «sin explicar».
        </Notice>
        <Toolbar>
          <Select label="Recuento inicial" placeholder="Elige" value={selection.from} options={options}
                  onChange={(e) => setSelection({ ...selection, from: e.target.value })} />
          <Select label="Recuento final" placeholder="Elige" value={selection.to} options={options}
                  onChange={(e) => setSelection({ ...selection, to: e.target.value })} />
        </Toolbar>
        {report.error && <ErrorNotice error={report.error} />}
      </Panel>
      {r && (
        <>
          <div className="g-kpis">
            <Kpi label="Consumo teórico" value={formatEUR(r.totalTheoreticalValue)} />
            <Kpi label="Consumo real" value={formatEUR(r.totalRealValue)} />
            <Kpi label="Desviación" value={formatEUR(r.totalDeviationValue)} />
            <Kpi label="Sin explicar" value={formatEUR(r.totalUnexplainedValue)}
                 tone={Number(r.totalUnexplainedValue) > 0 ? 'danger' : undefined} />
          </div>
          <Panel title="Por artículo (lo que más dinero se va sin explicar, primero)">
            <DataTable rowKey="stockItemId" rows={r.lines} empty="Ningún artículo contado en los dos recuentos"
                       columns={[
                         { key: 'stockItemName', label: 'Artículo' },
                         { key: 'realConsumption', label: 'Real', align: 'right', render: (l) => formatQty(l.realConsumption, l.unit) },
                         { key: 'theoreticalConsumption', label: 'Teórico', align: 'right', render: (l) => formatQty(l.theoreticalConsumption) },
                         { key: 'deviationPct', label: 'Desv.', align: 'right', render: (l) => formatPct(l.deviationPct) },
                         { key: 'recordedWaste', label: 'Mermas', align: 'right', render: (l) => formatQty(l.recordedWaste) },
                         { key: 'unexplained', label: 'Sin explicar', align: 'right', render: (l) => formatQty(l.unexplained) },
                         { key: 'unexplainedValue', label: 'Valor', align: 'right', render: (l) => (
                           <span className={Number(l.unexplainedValue) > 0 ? 'g-strong' : undefined}>{formatEUR(l.unexplainedValue)}</span>
                         ) }
                       ]} />
          </Panel>
        </>
      )}
    </>
  );
};

export default StockCountsPage;
