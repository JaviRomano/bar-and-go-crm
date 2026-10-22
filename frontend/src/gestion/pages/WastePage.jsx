import { useState } from 'react';
import { useAuth } from '../../context/useAuth';
import ConfirmModal from '../../components/common/ConfirmModal';
import { masterData, waste } from '../api';
import { WASTE_REASONS, toOptions } from '../labels';
import { asOptions, itemLabel, unitOptionsFor, useConfirm, useLookups } from '../lookups';
import { daysAgo, formatDateTime, formatEUR, formatQty, isoDate, toNumber, useForm, useLoad } from '../support';
import { Badge, DataTable, ErrorNotice, FormGrid, Input, Notice, Page, Panel, Select, Tabs, Textarea, Toolbar } from '../ui';

const WastePage = () => {
  const { canManage } = useAuth();
  const manager = canManage();
  const lookups = useLookups({ items: true });
  const [version, setVersion] = useState(0);
  return (
    <Page title="Mermas" subtitle="Lo que se tira, se rompe o se invita: sale del stock valorado al coste medio">
      <WasteForm lookups={lookups} manager={manager} onSaved={() => setVersion((n) => n + 1)} />
      {manager && <WasteList lookups={lookups} version={version} />}
    </Page>
  );
};

const EMPTY = { mode: 'item', warehouseId: '', stockItemId: '', productId: '', quantity: '', unit: '', reason: '', notes: '' };

const WasteForm = ({ lookups, manager, onSaved }) => {
  const products = useLoad(() => masterData.products(), []);
  const confirm = useConfirm();
  const [done, setDone] = useState(null);
  const form = useForm(EMPTY);
  const { values: v, set, errors, setValues } = form;
  const item = lookups.items.find((i) => String(i.id) === String(v.stockItemId));
  const product = (products.data ?? []).find((p) => String(p.id) === String(v.productId));
  const byItem = v.mode === 'item';

  const submit = (e) => {
    e.preventDefault();
    const what = byItem
      ? `${formatQty(v.quantity, v.unit || item?.stockUnit)} de ${item?.name ?? 'artículo'}`
      : `${formatQty(v.quantity)} ración(es) de ${product?.name ?? 'producto'} (se descuenta su escandallo)`;
    confirm.ask({
      title: 'Registrar merma',
      message: `${what}. Motivo: ${WASTE_REASONS[v.reason] ?? '—'}. La merma no se edita después; un error se corrige con un ajuste.`,
      confirmText: 'Registrar',
      onConfirm: async () => {
        const body = {
          warehouseId: toNumber(v.warehouseId), stockItemId: byItem ? toNumber(v.stockItemId) : null,
          productId: byItem ? null : toNumber(v.productId), quantity: toNumber(v.quantity), unit: byItem ? v.unit || null : null,
          reason: v.reason || null, notes: v.notes || null
        };
        const saved = await form.submit(() => waste.register(body));
        if (saved) {
          setDone(saved);
          setValues({ ...EMPTY, mode: v.mode, warehouseId: v.warehouseId });
          onSaved();
        }
      }
    });
  };

  return (
    <Panel title="Registrar merma">
      {done && (
        <Notice tone="ok">
          Merma registrada: {done.stockItemName ?? done.productName}
          {manager && done.valuedCost != null && ` · ${formatEUR(done.valuedCost)}`}
        </Notice>
      )}
      <Tabs active={v.mode} onChange={(mode) => setValues({ ...v, mode, stockItemId: '', productId: '', unit: '' })}
            tabs={[{ id: 'item', label: 'Un artículo' }, { id: 'product', label: 'Un plato de la carta' }]} />
      <form onSubmit={submit} noValidate>
        <FormGrid>
          <Select label="Almacén" placeholder="Elige un almacén" value={v.warehouseId} onChange={set('warehouseId')}
                  options={asOptions(lookups.warehouses)} error={errors.warehouseId} />
          {byItem ? (
            <>
              <Select label="Artículo" placeholder="Elige un artículo" value={v.stockItemId}
                      onChange={(e) => setValues({ ...v, stockItemId: e.target.value, unit: '' })}
                      options={asOptions(lookups.items, itemLabel)} error={errors.stockItemId} />
              <Input label="Cantidad" type="number" min="0" step="any" value={v.quantity} onChange={set('quantity')} error={errors.quantity} />
              <Select label="Unidad" placeholder={item ? `${item.stockUnit} (inventario)` : '—'} value={v.unit} onChange={set('unit')}
                      options={unitOptionsFor(item)} error={errors.unit} />
            </>
          ) : (
            <>
              <Select label="Plato" placeholder="Elige un plato" value={v.productId} onChange={set('productId')}
                      options={asOptions(products.data ?? [])} error={errors.productId} />
              <Input label="Raciones" type="number" min="0" step="any" value={v.quantity} onChange={set('quantity')} error={errors.quantity} />
            </>
          )}
          <Select label="Motivo" placeholder="Elige un motivo" value={v.reason} onChange={set('reason')}
                  options={toOptions(WASTE_REASONS)} error={errors.reason} />
          <Textarea label="Observaciones" value={v.notes} onChange={set('notes')} error={errors.notes} />
        </FormGrid>
        <ErrorNotice error={form.error} />
        <div className="g-toolbar" style={{ justifyContent: 'flex-end', marginTop: 'var(--sp-4)' }}>
          <button type="submit" className="btn btn-primary" disabled={form.saving}>Registrar merma</button>
        </div>
      </form>
      <ConfirmModal {...confirm.props} />
    </Panel>
  );
};

const WasteList = ({ lookups, version }) => {
  const [filters, setFilters] = useState({ from: daysAgo(29), to: isoDate(new Date()), reason: '', warehouseId: '' });
  const list = useLoad(() => waste.list({
    from: filters.from ? new Date(`${filters.from}T00:00:00`).toISOString() : '',
    to: filters.to ? new Date(`${filters.to}T23:59:59`).toISOString() : '',
    reason: filters.reason, warehouseId: filters.warehouseId
  }), [filters.from, filters.to, filters.reason, filters.warehouseId, version]);
  const set = (name) => (e) => setFilters({ ...filters, [name]: e.target.value });
  const total = (list.data ?? []).reduce((sum, w) => sum + Number(w.valuedCost ?? 0), 0);
  const warehouseName = (id) => lookups.warehouses.find((w) => w.id === id)?.name ?? '—';

  return (
    <Panel title="Mermas registradas" actions={list.data && <span className="g-strong">Total {formatEUR(total)}</span>}>
      <Toolbar>
        <Input label="Desde" type="date" value={filters.from} onChange={set('from')} />
        <Input label="Hasta" type="date" value={filters.to} onChange={set('to')} />
        <Select label="Motivo" placeholder="Todos" value={filters.reason} options={toOptions(WASTE_REASONS)} onChange={set('reason')} />
        <Select label="Almacén" placeholder="Todos" value={filters.warehouseId} options={asOptions(lookups.warehouses)} onChange={set('warehouseId')} />
      </Toolbar>
      <DataTable loading={list.loading} error={list.error} rows={list.data} empty="Sin mermas en el periodo"
                 columns={[
                   { key: 'occurredAt', label: 'Fecha', render: (w) => formatDateTime(w.occurredAt) },
                   { key: 'subject', label: 'Qué', render: (w) => (
                     <>{w.stockItemName ?? w.productName} {w.productId && <Badge>Plato</Badge>}</>
                   ) },
                   { key: 'quantity', label: 'Cantidad', align: 'right', render: (w) => formatQty(w.quantity, w.unit) },
                   { key: 'reason', label: 'Motivo', render: (w) => WASTE_REASONS[w.reason] },
                   { key: 'warehouseId', label: 'Almacén', render: (w) => warehouseName(w.warehouseId) },
                   { key: 'valuedCost', label: 'Coste', align: 'right', render: (w) => formatEUR(w.valuedCost) },
                   { key: 'notes', label: 'Observaciones', render: (w) => <span className="g-muted">{w.notes ?? ''}</span> }
                 ]} />
    </Panel>
  );
};

export default WastePage;
