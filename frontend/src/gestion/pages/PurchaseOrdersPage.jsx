import { useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import ConfirmModal from '../../components/common/ConfirmModal';
import { inventory, purchaseOrders } from '../api';
import { PURCHASE_ORDER_STATUS, PURCHASE_ORDER_TONES, toOptions } from '../labels';
import { asOptions, itemLabel, unitOptionsFor, useConfirm, useLookups } from '../lookups';
import { formatDate, formatEUR, formatQty, toNumber, useForm, useLoad } from '../support';
import {
  Badge, DataTable, ErrorNotice, Field, FormGrid, Input, Notice, Page, Panel, Select, Sheet, Tabs, Textarea, Toolbar
} from '../ui';

const supplierName = (s) => s.tradeName || s.legalName;

const PurchaseOrdersPage = () => {
  const [tab, setTab] = useState('orders');
  return (
    <Page title="Compras" subtitle="Pedidos a proveedor y sugerencia de reposición">
      <Tabs active={tab} onChange={setTab} tabs={[{ id: 'orders', label: 'Pedidos' }, { id: 'replenishment', label: 'Reposición' }]} />
      {tab === 'orders' ? <Orders /> : <Replenishment onCreated={() => setTab('orders')} />}
    </Page>
  );
};

const Orders = () => {
  const [filters, setFilters] = useState({ status: '', supplierId: '' });
  const [open, setOpen] = useState(null);
  const lookups = useLookups({ items: true, suppliersList: true });
  const list = useLoad(() => purchaseOrders.list(filters), [filters.status, filters.supplierId]);

  return (
    <Panel
      actions={<button type="button" className="btn btn-primary" onClick={() => setOpen({ lines: [] })}><Plus size={18} />Nuevo pedido</button>}
    >
      <Toolbar>
        <Select label="Estado" placeholder="Todos" value={filters.status} options={toOptions(PURCHASE_ORDER_STATUS)}
                onChange={(e) => setFilters({ ...filters, status: e.target.value })} />
        <Select label="Proveedor" placeholder="Todos" value={filters.supplierId}
                options={asOptions(lookups.suppliers, supplierName)}
                onChange={(e) => setFilters({ ...filters, supplierId: e.target.value })} />
      </Toolbar>
      <DataTable loading={list.loading} error={list.error} rows={list.data} empty="No hay pedidos"
                 onRowClick={(o) => setOpen(o)}
                 columns={[
                   { key: 'number', label: 'Pedido', render: (o) => <span className="g-strong">{o.number}</span> },
                   { key: 'supplierName', label: 'Proveedor' },
                   { key: 'orderDate', label: 'Fecha', render: (o) => formatDate(o.orderDate) },
                   { key: 'expectedDate', label: 'Entrega prevista', render: (o) => formatDate(o.expectedDate) },
                   { key: 'status', label: 'Estado', render: (o) => <Badge tone={PURCHASE_ORDER_TONES[o.status]}>{PURCHASE_ORDER_STATUS[o.status]}</Badge> },
                   { key: 'total', label: 'Total', align: 'right', render: (o) => formatEUR(o.total) }
                 ]} />
      {open && (
        <OrderSheet order={open} lookups={lookups} onClose={(changed) => { setOpen(null); if (changed) list.reload(); }} />
      )}
    </Panel>
  );
};

const OrderSheet = ({ order, lookups, onClose }) => {
  const editable = !order.id || order.status === 'DRAFT';
  return editable
    ? <OrderForm order={order} lookups={lookups} onClose={onClose} />
    : <OrderDetail order={order} onClose={onClose} />;
};

const newLine = () => ({ stockItemId: '', quantity: '', unit: '', unitPrice: '' });

const OrderForm = ({ order, lookups, onClose }) => {
  const isNew = !order.id;
  const confirm = useConfirm();
  const form = useForm({
    supplierId: order.supplierId ?? '', expectedDate: order.expectedDate ?? '', notes: order.notes ?? '',
    lines: order.lines?.length
      ? order.lines.map((l) => ({ stockItemId: l.stockItemId, quantity: l.quantity, unit: l.unit, unitPrice: l.unitPrice ?? '' }))
      : [newLine()]
  });
  const { values: v, set, errors, setValues } = form;
  const itemsById = Object.fromEntries(lookups.items.map((i) => [i.id, i]));

  const setLine = (index, field, value) => setValues((prev) => ({
    ...prev,
    lines: prev.lines.map((l, i) => {
      if (i !== index) return l;
      const next = { ...l, [field]: value };
      if (field === 'stockItemId') next.unit = itemsById[value]?.purchaseUnit ?? '';
      return next;
    })
  }));

  const body = () => ({
    supplierId: toNumber(v.supplierId), expectedDate: v.expectedDate || null, notes: v.notes || null,
    lines: v.lines.map((l) => ({
      stockItemId: toNumber(l.stockItemId), quantity: toNumber(l.quantity), unit: l.unit || null, unitPrice: toNumber(l.unitPrice)
    }))
  });

  const save = async (e) => {
    e.preventDefault();
    const saved = await form.submit(() => (isNew ? purchaseOrders.create(body()) : purchaseOrders.update(order.id, body())));
    if (saved) onClose(true);
  };
  const saveAndSend = async () => {
    const saved = await form.submit(async () => {
      const draft = isNew ? await purchaseOrders.create(body()) : await purchaseOrders.update(order.id, body());
      return purchaseOrders.send(draft.id);
    });
    if (saved) onClose(true);
  };
  const remove = () => confirm.ask({
    title: 'Borrar borrador', message: `Se borrará el pedido ${order.number}.`, type: 'error', confirmText: 'Borrar',
    onConfirm: async () => { if (await form.submit(() => purchaseOrders.remove(order.id)) !== null) onClose(true); }
  });

  return (
    <Sheet
      title={isNew ? 'Nuevo pedido a proveedor' : `Pedido ${order.number}`}
      onClose={() => onClose(false)}
      wide
      footer={(
        <>
          {!isNew && <button type="button" className="btn btn-outline" onClick={remove}>Borrar</button>}
          <button type="submit" form="po-form" className="btn btn-outline" disabled={form.saving}>Guardar borrador</button>
          <button type="button" className="btn btn-primary" disabled={form.saving} onClick={saveAndSend}>Guardar y enviar</button>
        </>
      )}
    >
      <form id="po-form" onSubmit={save} noValidate className="g-lines">
        <FormGrid>
          <Select label="Proveedor" placeholder="Elige un proveedor" value={v.supplierId} onChange={set('supplierId')}
                  options={asOptions(lookups.suppliers, supplierName)} error={errors.supplierId} />
          <Input label="Entrega prevista" type="date" value={v.expectedDate} onChange={set('expectedDate')} error={errors.expectedDate} />
          <Textarea label="Notas" value={v.notes} onChange={set('notes')} error={errors.notes} />
        </FormGrid>
        <Field label="Líneas" wide error={errors.lines}>
          <div className="g-lines">
            {v.lines.map((line, i) => (
              <div className="g-line" key={i}>
                <Select label="Artículo" placeholder="Elige" value={line.stockItemId}
                        options={asOptions(lookups.items, itemLabel)}
                        onChange={(e) => setLine(i, 'stockItemId', e.target.value)} error={errors[`lines[${i}].stockItemId`]} />
                <Input label="Cantidad" type="number" min="0" step="any" value={line.quantity}
                       onChange={(e) => setLine(i, 'quantity', e.target.value)} error={errors[`lines[${i}].quantity`]} />
                <Select label="Unidad" value={line.unit} options={unitOptionsFor(itemsById[line.stockItemId])}
                        onChange={(e) => setLine(i, 'unit', e.target.value)} error={errors[`lines[${i}].unit`]} />
                <Input label="Precio (vacío = tarifa)" type="number" min="0" step="any" value={line.unitPrice}
                       onChange={(e) => setLine(i, 'unitPrice', e.target.value)} error={errors[`lines[${i}].unitPrice`]} />
                <button type="button" className="g-icon-btn" aria-label="Quitar línea"
                        onClick={() => setValues((p) => ({ ...p, lines: p.lines.filter((_, j) => j !== i) }))}>
                  <Trash2 size={18} />
                </button>
              </div>
            ))}
            <button type="button" className="btn btn-outline btn-sm" style={{ alignSelf: 'flex-start' }}
                    onClick={() => setValues((p) => ({ ...p, lines: [...p.lines, newLine()] }))}>
              <Plus size={16} />Añadir línea
            </button>
          </div>
        </Field>
      </form>
      <ErrorNotice error={form.error} />
      <ConfirmModal {...confirm.props} />
    </Sheet>
  );
};

const OrderDetail = ({ order, onClose }) => {
  const form = useForm({});
  const confirm = useConfirm();
  const act = (title, message, action) => confirm.ask({
    title, message,
    onConfirm: async () => { if (await form.submit(action) !== null) onClose(true); }
  });
  const canCancel = order.status === 'SENT';
  const canClose = order.status === 'PARTIALLY_RECEIVED';

  return (
    <Sheet
      title={`Pedido ${order.number}`}
      onClose={() => onClose(false)}
      wide
      footer={(
        <>
          {canCancel && (
            <button type="button" className="btn btn-outline" disabled={form.saving}
                    onClick={() => act('Cancelar pedido', `El pedido ${order.number} quedará cancelado.`, () => purchaseOrders.cancel(order.id))}>
              Cancelar pedido
            </button>
          )}
          {canClose && (
            <button type="button" className="btn btn-primary" disabled={form.saving}
                    onClick={() => act('Cerrar con faltas', 'Lo pendiente no se recibirá y el pedido pasa a recibido.', () => purchaseOrders.close(order.id))}>
              Cerrar con faltas
            </button>
          )}
        </>
      )}
    >
      <dl className="g-dl">
        <dt>Proveedor</dt><dd>{order.supplierName}</dd>
        <dt>Estado</dt><dd><Badge tone={PURCHASE_ORDER_TONES[order.status]}>{PURCHASE_ORDER_STATUS[order.status]}</Badge></dd>
        <dt>Fecha</dt><dd>{formatDate(order.orderDate)}</dd>
        <dt>Entrega prevista</dt><dd>{formatDate(order.expectedDate)}</dd>
        {order.notes && <><dt>Notas</dt><dd>{order.notes}</dd></>}
      </dl>
      <DataTable rows={order.lines} columns={[
        { key: 'stockItemName', label: 'Artículo' },
        { key: 'quantity', label: 'Pedido', align: 'right', render: (l) => formatQty(l.quantity, l.unit) },
        { key: 'receivedQuantity', label: 'Recibido', align: 'right', render: (l) => formatQty(l.receivedQuantity, l.unit) },
        { key: 'unitPrice', label: 'Precio', align: 'right', render: (l) => formatEUR(l.unitPrice) },
        { key: 'vatRate', label: 'IVA', align: 'right', render: (l) => `${l.vatRate} %` },
        { key: 'amount', label: 'Importe', align: 'right', render: (l) => formatEUR(l.amount) }
      ]} footer={(
        <>
          {order.vatBreakdown.map((b) => (
            <tr key={b.vatRate}>
              <td colSpan={5} className="g-num">Base al {b.vatRate} % · IVA</td>
              <td className="g-num">{formatEUR(b.base)} · {formatEUR(b.vat)}</td>
            </tr>
          ))}
          <tr><td colSpan={5} className="g-num">Total</td><td className="g-num">{formatEUR(order.total)}</td></tr>
        </>
      )} />
      <ErrorNotice error={form.error} />
      <ConfirmModal {...confirm.props} />
    </Sheet>
  );
};

const Replenishment = ({ onCreated }) => {
  const suggestions = useLoad(() => inventory.replenishment(), []);
  const [selected, setSelected] = useState([]);
  const form = useForm({});
  const groups = suggestions.data ?? [];

  const create = async () => {
    const result = await form.submit(() => inventory.replenishmentOrders(selected));
    if (result !== null) onCreated();
  };

  return (
    <Panel
      title="Artículos por debajo del mínimo"
      actions={(
        <button type="button" className="btn btn-primary" disabled={!selected.length || form.saving} onClick={create}>
          Crear {selected.length || ''} borrador{selected.length === 1 ? '' : 'es'}
        </button>
      )}
    >
      <Notice tone="info">
        Se pide hasta el stock máximo, descontando lo que ya está pedido y sin recibir. Los pedidos se crean en borrador.
      </Notice>
      <ErrorNotice error={form.error} />
      {suggestions.loading || suggestions.error || groups.length === 0
        ? <DataTable loading={suggestions.loading} error={suggestions.error} rows={groups} columns={[]} empty="Nada que reponer" />
        : groups.map((group) => (
          <Panel key={group.supplierId ?? 'none'} title={(
            <label className="g-checkbox">
              <input type="checkbox" disabled={!group.supplierId} checked={selected.includes(group.supplierId)}
                     onChange={(e) => setSelected(e.target.checked
                       ? [...selected, group.supplierId] : selected.filter((id) => id !== group.supplierId))} />
              {group.supplierName ?? 'Sin proveedor habitual'}
            </label>
          )}>
            <DataTable rowKey="stockItemId" rows={group.lines} columns={[
              { key: 'stockItemName', label: 'Artículo' },
              { key: 'onHand', label: 'Stock', align: 'right', render: (l) => formatQty(l.onHand, l.stockUnit) },
              { key: 'onOrder', label: 'Pedido', align: 'right', render: (l) => formatQty(l.onOrder, l.stockUnit) },
              { key: 'minStock', label: 'Mín. / máx.', align: 'right', render: (l) => `${formatQty(l.minStock)} / ${formatQty(l.maxStock)}` },
              { key: 'suggestedPurchaseQuantity', label: 'Pedir', align: 'right',
                render: (l) => <span className="g-strong">{formatQty(l.suggestedPurchaseQuantity, l.purchaseUnit)}</span> },
              { key: 'unitPrice', label: 'Precio', align: 'right', render: (l) => formatEUR(l.unitPrice) }
            ]} />
          </Panel>
        ))}
    </Panel>
  );
};

export default PurchaseOrdersPage;
