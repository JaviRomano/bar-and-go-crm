import { useState } from 'react';
import { Plus } from 'lucide-react';
import { useAuth } from '../../context/useAuth';
import ConfirmModal from '../../components/common/ConfirmModal';
import { inventory, warehouses as warehouseApi } from '../api';
import { MOVEMENT_TONES, MOVEMENT_TYPES, WAREHOUSE_TYPES, toOptions } from '../labels';
import { asOptions, itemLabel, unitOptionsFor, useConfirm, useLookups } from '../lookups';
import { formatDate, formatDateTime, formatEUR, formatQty, toNumber, useForm, useLoad } from '../support';
import {
  Badge, DataTable, ErrorNotice, FormGrid, Input, Kpi, Notice, Page, Panel, Select, Sheet, Tabs, Textarea, Toolbar
} from '../ui';

const InventoryPage = () => {
  const { canManage } = useAuth();
  const manager = canManage();
  const [tab, setTab] = useState('stock');
  const lookups = useLookups({ items: true });
  const tabs = [
    { id: 'stock', label: 'Stock' },
    { id: 'transfer', label: 'Traspaso' },
    { id: 'expiring', label: 'Caducidades' },
    ...(manager ? [
      { id: 'movements', label: 'Movimientos' },
      { id: 'valuation', label: 'Valoración' },
      { id: 'adjust', label: 'Ajuste' },
      { id: 'warehouses', label: 'Almacenes' }
    ] : [])
  ];

  return (
    <Page title="Inventario" subtitle="Existencias por almacén. Todo cambio de stock queda como movimiento">
      <Tabs tabs={tabs} active={tab} onChange={setTab} />
      {tab === 'stock' && <Stock lookups={lookups} />}
      {tab === 'transfer' && <StockOperation kind="transfer" lookups={lookups} />}
      {tab === 'expiring' && <Expiring />}
      {tab === 'movements' && <Movements lookups={lookups} />}
      {tab === 'valuation' && <Valuation />}
      {tab === 'adjust' && <StockOperation kind="adjust" lookups={lookups} />}
      {tab === 'warehouses' && <Warehouses />}
    </Page>
  );
};

const Stock = ({ lookups }) => {
  const [filters, setFilters] = useState({ warehouseId: '', search: '' });
  const stock = useLoad(() => inventory.stock({ warehouseId: filters.warehouseId }), [filters.warehouseId]);
  const search = filters.search.trim().toLowerCase();
  const rows = (stock.data ?? []).filter((r) => !search || `${r.stockItemCode} ${r.stockItemName}`.toLowerCase().includes(search));
  return (
    <Panel>
      <Toolbar>
        <Input label="Buscar" placeholder="Código o nombre" value={filters.search}
               onChange={(e) => setFilters({ ...filters, search: e.target.value })} />
        <Select label="Almacén" placeholder="Todos" value={filters.warehouseId} options={asOptions(lookups.warehouses)}
                onChange={(e) => setFilters({ ...filters, warehouseId: e.target.value })} />
      </Toolbar>
      <DataTable rowKey={(r) => `${r.stockItemId}-${r.warehouseId}`} loading={stock.loading} error={stock.error} rows={rows}
                 empty="Sin existencias"
                 columns={[
                   { key: 'stockItemName', label: 'Artículo', render: (r) => (
                     <><div className="g-strong">{r.stockItemName}</div><div className="g-code">{r.stockItemCode}</div></>
                   ) },
                   { key: 'warehouseName', label: 'Almacén' },
                   { key: 'quantity', label: 'Cantidad', align: 'right', render: (r) => (
                     Number(r.quantity) < 0
                       ? <Badge tone="danger">{formatQty(r.quantity, r.unit)}</Badge>
                       : formatQty(r.quantity, r.unit)
                   ) }
                 ]} />
      {rows.some((r) => Number(r.quantity) < 0) && (
        <Notice tone="warn">
          Stock negativo: las ventas han descontado más de lo que había según el sistema. Suele ser una entrada sin
          registrar o un escandallo desajustado; un recuento lo corrige.
        </Notice>
      )}
    </Panel>
  );
};

/** Traspaso (personal y encargado) o ajuste manual (encargado). Los dos mueven stock: se confirman. */
const StockOperation = ({ kind, lookups }) => {
  const transfer = kind === 'transfer';
  const confirm = useConfirm();
  const [done, setDone] = useState(null);
  const empty = transfer
    ? { fromWarehouseId: '', toWarehouseId: '', stockItemId: '', quantity: '', unit: '', notes: '' }
    : { warehouseId: '', stockItemId: '', quantity: '', unit: '', unitCost: '', reason: '' };
  const form = useForm(empty);
  const { values: v, set, errors, setValues } = form;
  const item = lookups.items.find((i) => String(i.id) === String(v.stockItemId));
  const warehouseName = (id) => lookups.warehouses.find((w) => String(w.id) === String(id))?.name ?? '?';

  const submit = (e) => {
    e.preventDefault();
    const amount = formatQty(v.quantity, v.unit || item?.stockUnit);
    confirm.ask({
      title: transfer ? 'Confirmar traspaso' : 'Confirmar ajuste',
      message: transfer
        ? `${amount} de ${item?.name ?? 'artículo'}: ${warehouseName(v.fromWarehouseId)} → ${warehouseName(v.toWarehouseId)}.`
        : `${Number(v.quantity) >= 0 ? 'Entrada' : 'Salida'} de ${amount} de ${item?.name ?? 'artículo'} en ${warehouseName(v.warehouseId)}.`,
      onConfirm: async () => {
        const body = transfer
          ? { ...v, fromWarehouseId: toNumber(v.fromWarehouseId), toWarehouseId: toNumber(v.toWarehouseId),
            stockItemId: toNumber(v.stockItemId), quantity: toNumber(v.quantity), unit: v.unit || null, notes: v.notes || null }
          : { ...v, warehouseId: toNumber(v.warehouseId), stockItemId: toNumber(v.stockItemId), quantity: toNumber(v.quantity),
            unit: v.unit || null, unitCost: toNumber(v.unitCost) };
        const result = await form.submit(() => (transfer ? inventory.transfer(body) : inventory.adjust(body)));
        if (result) {
          setDone(`${transfer ? 'Traspaso' : 'Ajuste'} registrado (${result.length} movimientos).`);
          setValues(empty);
        }
      }
    });
  };

  return (
    <Panel title={transfer ? 'Traspaso entre almacenes' : 'Ajuste manual'}>
      {!transfer && (
        <Notice tone="info">
          Cantidad con signo: positiva para entradas, negativa para salidas. Para diferencias de inventario, mejor un recuento;
          para mermas, la pantalla de mermas.
        </Notice>
      )}
      {done && <Notice tone="ok">{done}</Notice>}
      <form onSubmit={submit} noValidate>
        <FormGrid>
          {transfer ? (
            <>
              <Select label="Desde" placeholder="Almacén de origen" value={v.fromWarehouseId} onChange={set('fromWarehouseId')}
                      options={asOptions(lookups.warehouses)} error={errors.fromWarehouseId} />
              <Select label="Hacia" placeholder="Almacén de destino" value={v.toWarehouseId} onChange={set('toWarehouseId')}
                      options={asOptions(lookups.warehouses)} error={errors.toWarehouseId} />
            </>
          ) : (
            <Select label="Almacén" placeholder="Elige un almacén" value={v.warehouseId} onChange={set('warehouseId')}
                    options={asOptions(lookups.warehouses)} error={errors.warehouseId} />
          )}
          <Select label="Artículo" placeholder="Elige un artículo" value={v.stockItemId}
                  onChange={(e) => setValues({ ...v, stockItemId: e.target.value, unit: '' })}
                  options={asOptions(lookups.items, itemLabel)} error={errors.stockItemId} />
          <Input label="Cantidad" type="number" step="any" value={v.quantity} onChange={set('quantity')} error={errors.quantity} />
          <Select label="Unidad" placeholder={item ? `${item.stockUnit} (inventario)` : '—'} value={v.unit} onChange={set('unit')}
                  options={unitOptionsFor(item)} error={errors.unit} />
          {transfer ? (
            <Textarea label="Notas" value={v.notes} onChange={set('notes')} error={errors.notes} />
          ) : (
            <>
              <Input label="Coste unitario (solo entradas)" type="number" min="0" step="any" value={v.unitCost}
                     onChange={set('unitCost')} error={errors.unitCost} hint="Vacío: se valora al PMP" />
              <Textarea label="Motivo" value={v.reason} onChange={set('reason')} error={errors.reason} />
            </>
          )}
        </FormGrid>
        <ErrorNotice error={form.error} />
        <div className="g-toolbar" style={{ justifyContent: 'flex-end', marginTop: 'var(--sp-4)' }}>
          <button type="submit" className="btn btn-primary" disabled={form.saving}>{transfer ? 'Traspasar' : 'Ajustar'}</button>
        </div>
      </form>
      <ConfirmModal {...confirm.props} />
    </Panel>
  );
};

const Expiring = () => {
  const [days, setDays] = useState('3');
  const lots = useLoad(() => inventory.expiringLots(days || 0), [days]);
  return (
    <Panel>
      <Toolbar>
        <Input label="Próximos días" type="number" min="0" value={days} onChange={(e) => setDays(e.target.value)} />
      </Toolbar>
      <DataTable rowKey="lotId" loading={lots.loading} error={lots.error} rows={lots.data} empty="Nada caduca en ese plazo"
                 columns={[
                   { key: 'stockItemName', label: 'Artículo' },
                   { key: 'warehouseName', label: 'Almacén' },
                   { key: 'lotCode', label: 'Lote', render: (l) => <span className="g-code">{l.lotCode ?? '—'}</span> },
                   { key: 'expiryDate', label: 'Caduca', render: (l) => formatDate(l.expiryDate) },
                   { key: 'daysLeft', label: 'Días', align: 'right', render: (l) => (
                     <Badge tone={l.daysLeft < 0 ? 'danger' : l.daysLeft <= 1 ? 'warn' : 'neutral'}>
                       {l.daysLeft < 0 ? 'Caducado' : `${l.daysLeft} d`}
                     </Badge>
                   ) },
                   { key: 'remainingQuantity', label: 'Queda', align: 'right', render: (l) => formatQty(l.remainingQuantity, l.unit) }
                 ]} />
    </Panel>
  );
};

const Movements = ({ lookups }) => {
  const [filters, setFilters] = useState({ warehouseId: '', stockItemId: '', from: '', to: '' });
  const toInstant = (date, endOfDay) => (date ? new Date(`${date}T${endOfDay ? '23:59:59' : '00:00:00'}`).toISOString() : '');
  const movements = useLoad(() => inventory.movements({
    warehouseId: filters.warehouseId, stockItemId: filters.stockItemId,
    from: toInstant(filters.from, false), to: toInstant(filters.to, true)
  }), [filters.warehouseId, filters.stockItemId, filters.from, filters.to]);
  const set = (name) => (e) => setFilters({ ...filters, [name]: e.target.value });

  return (
    <Panel>
      <Toolbar>
        <Select label="Almacén" placeholder="Todos" value={filters.warehouseId} options={asOptions(lookups.warehouses)} onChange={set('warehouseId')} />
        <Select label="Artículo" placeholder="Todos" value={filters.stockItemId} options={asOptions(lookups.items, itemLabel)} onChange={set('stockItemId')} />
        <Input label="Desde" type="date" value={filters.from} onChange={set('from')} />
        <Input label="Hasta" type="date" value={filters.to} onChange={set('to')} />
      </Toolbar>
      <DataTable loading={movements.loading} error={movements.error} rows={movements.data} empty="Sin movimientos"
                 columns={[
                   { key: 'occurredAt', label: 'Fecha', render: (m) => formatDateTime(m.occurredAt) },
                   { key: 'type', label: 'Tipo', render: (m) => <Badge tone={MOVEMENT_TONES[m.type]}>{MOVEMENT_TYPES[m.type]}</Badge> },
                   { key: 'stockItemName', label: 'Artículo' },
                   { key: 'warehouseName', label: 'Almacén' },
                   { key: 'quantity', label: 'Cantidad', align: 'right', render: (m) => formatQty(m.quantity, m.unit) },
                   { key: 'unitCost', label: 'Coste unit.', align: 'right', render: (m) => formatEUR(m.unitCost) },
                   { key: 'totalCost', label: 'Valor', align: 'right', render: (m) => formatEUR(m.totalCost) },
                   { key: 'notes', label: 'Origen', render: (m) => <span className="g-muted">{m.notes ?? m.sourceType}</span> }
                 ]} />
    </Panel>
  );
};

const Valuation = () => {
  const valuation = useLoad(() => inventory.valuation(), []);
  return (
    <>
      {valuation.data && <div className="g-kpis"><Kpi label="Valor del stock (PMP)" value={formatEUR(valuation.data.totalValue)} /></div>}
      <Panel>
        <DataTable rowKey={(l) => `${l.stockItemId}-${l.siteId}`} loading={valuation.loading} error={valuation.error}
                   rows={valuation.data?.lines} empty="Sin stock valorado"
                   columns={[
                     { key: 'stockItemName', label: 'Artículo' },
                     { key: 'quantity', label: 'Cantidad', align: 'right', render: (l) => formatQty(l.quantity, l.unit) },
                     { key: 'averageCost', label: 'PMP', align: 'right', render: (l) => formatEUR(l.averageCost) },
                     { key: 'value', label: 'Valor', align: 'right', render: (l) => formatEUR(l.value) }
                   ]} />
      </Panel>
    </>
  );
};

const Warehouses = () => {
  const list = useLoad(() => warehouseApi.list(true), []);
  const [editing, setEditing] = useState(null);
  return (
    <Panel actions={(
      <button type="button" className="btn btn-primary" onClick={() => setEditing({ name: '', type: 'DRY_STORE', active: true, defaultForSales: false })}>
        <Plus size={18} />Nuevo almacén
      </button>
    )}>
      <DataTable loading={list.loading} error={list.error} rows={list.data} empty="Sin almacenes" onRowClick={setEditing}
                 columns={[
                   { key: 'name', label: 'Almacén', render: (w) => <span className="g-strong">{w.name}</span> },
                   { key: 'type', label: 'Tipo', render: (w) => WAREHOUSE_TYPES[w.type] },
                   { key: 'defaultForSales', label: 'Ventas', render: (w) => (w.defaultForSales ? <Badge tone="accent">Descuenta las ventas</Badge> : '—') },
                   { key: 'active', label: 'Estado', render: (w) => (w.active ? <Badge tone="ok">Activo</Badge> : <Badge>Inactivo</Badge>) }
                 ]} />
      {editing && <WarehouseSheet warehouse={editing} onClose={(saved) => { setEditing(null); if (saved) list.reload(); }} />}
    </Panel>
  );
};

const WarehouseSheet = ({ warehouse, onClose }) => {
  const isNew = !warehouse.id;
  const form = useForm(warehouse);
  const { values: v, set, errors } = form;
  const save = async (e) => {
    e.preventDefault();
    const saved = await form.submit(() => (isNew ? warehouseApi.create(v) : warehouseApi.update(warehouse.id, v)));
    if (saved) onClose(true);
  };
  return (
    <Sheet title={isNew ? 'Nuevo almacén' : warehouse.name} onClose={() => onClose(false)} footer={(
      <>
        <button type="button" className="btn btn-outline" onClick={() => onClose(false)}>Cancelar</button>
        <button type="submit" form="warehouse-form" className="btn btn-primary" disabled={form.saving}>Guardar</button>
      </>
    )}>
      <form id="warehouse-form" onSubmit={save} noValidate>
        <FormGrid>
          <Input label="Nombre" value={v.name} onChange={set('name')} error={errors.name} />
          <Select label="Tipo" value={v.type} onChange={set('type')} options={toOptions(WAREHOUSE_TYPES)} error={errors.type} />
          <label className="g-checkbox">
            <input type="checkbox" checked={!!v.defaultForSales} onChange={set('defaultForSales')} />
            Las ventas descuentan de este almacén
          </label>
          {!isNew && (
            <label className="g-checkbox">
              <input type="checkbox" checked={!!v.active} onChange={set('active')} /> Activo
            </label>
          )}
        </FormGrid>
      </form>
      <ErrorNotice error={form.error} />
    </Sheet>
  );
};

export default InventoryPage;
