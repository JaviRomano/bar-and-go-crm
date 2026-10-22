import { useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { useAuth } from '../../context/useAuth';
import ConfirmModal from '../../components/common/ConfirmModal';
import { goodsReceipts, purchaseOrders } from '../api';
import { asOptions, itemLabel, unitOptionsFor, useConfirm, useLookups } from '../lookups';
import { formatDate, formatEUR, formatQty, isoDate, toNumber, useForm, useLoad } from '../support';
import { DataTable, ErrorNotice, Field, FormGrid, Input, Notice, Page, Panel, Select, Sheet, Textarea } from '../ui';

const supplierName = (s) => s.tradeName || s.legalName;
const newLine = () => ({ stockItemId: '', quantity: '', unit: '', unitPrice: '', lotCode: '', expiryDate: '' });

const GoodsReceiptPage = () => {
  const { canManage } = useAuth();
  const [done, setDone] = useState(null);
  const [formKey, setFormKey] = useState(0);
  const [detail, setDetail] = useState(null);
  const manager = canManage();
  const list = useLoad(() => (manager ? goodsReceipts.list() : Promise.resolve([])), [manager, formKey]);

  return (
    <Page title="Recepción" subtitle="Registro de albaranes: da entrada al stock, con lote y caducidad">
      {done && <Notice tone="ok">Albarán {done.number} registrado: {done.lines.length} líneas en {done.warehouseName}.</Notice>}
      <ReceiptForm key={formKey} manager={manager} onSaved={(receipt) => { setDone(receipt); setFormKey((k) => k + 1); }} />
      {manager && (
        <Panel title="Albaranes recibidos">
          <DataTable loading={list.loading} error={list.error} rows={list.data} empty="Sin albaranes" onRowClick={setDetail}
                     columns={[
                       { key: 'number', label: 'Albarán', render: (r) => <span className="g-strong">{r.number}</span> },
                       { key: 'receiptDate', label: 'Fecha', render: (r) => formatDate(r.receiptDate) },
                       { key: 'supplierName', label: 'Proveedor' },
                       { key: 'supplierDeliveryNote', label: 'Nº proveedor' },
                       { key: 'warehouseName', label: 'Almacén' },
                       { key: 'totalBase', label: 'Base', align: 'right', render: (r) => formatEUR(r.totalBase) }
                     ]} />
        </Panel>
      )}
      {detail && (
        <Sheet title={`Albarán ${detail.number}`} onClose={() => setDetail(null)} wide>
          <dl className="g-dl">
            <dt>Proveedor</dt><dd>{detail.supplierName}</dd>
            <dt>Almacén</dt><dd>{detail.warehouseName}</dd>
            <dt>Fecha</dt><dd>{formatDate(detail.receiptDate)}</dd>
            {detail.notes && <><dt>Notas</dt><dd>{detail.notes}</dd></>}
          </dl>
          <DataTable rows={detail.lines} columns={[
            { key: 'stockItemName', label: 'Artículo' },
            { key: 'quantity', label: 'Cantidad', align: 'right', render: (l) => formatQty(l.quantity, l.unit) },
            { key: 'stockQuantity', label: 'En inventario', align: 'right', render: (l) => formatQty(l.stockQuantity, l.stockUnit) },
            { key: 'unitPrice', label: 'Precio', align: 'right', render: (l) => formatEUR(l.unitPrice) },
            { key: 'lotCode', label: 'Lote', render: (l) => (l.lotCode ? `${l.lotCode} · ${formatDate(l.expiryDate)}` : '—') },
            { key: 'amount', label: 'Importe', align: 'right', render: (l) => formatEUR(l.amount) }
          ]} />
        </Sheet>
      )}
    </Page>
  );
};

const ReceiptForm = ({ manager, onSaved }) => {
  const lookups = useLookups({ items: true, suppliersList: true });
  const openOrders = useLoad(
    () => (manager ? Promise.all([purchaseOrders.list({ status: 'SENT' }), purchaseOrders.list({ status: 'PARTIALLY_RECEIVED' })])
      .then(([a, b]) => [...a, ...b]) : Promise.resolve([])),
    [manager]
  );
  const confirm = useConfirm();
  const form = useForm({
    supplierId: '', purchaseOrderId: '', warehouseId: '', supplierDeliveryNote: '', receiptDate: isoDate(new Date()),
    notes: '', lines: [newLine()]
  });
  const { values: v, set, errors, setValues } = form;
  const itemsById = Object.fromEntries(lookups.items.map((i) => [i.id, i]));
  const orders = (openOrders.data ?? []).filter((o) => !v.supplierId || String(o.supplierId) === String(v.supplierId));

  const pickOrder = (e) => {
    const order = orders.find((o) => String(o.id) === e.target.value);
    setValues((prev) => ({
      ...prev,
      purchaseOrderId: e.target.value,
      supplierId: order ? order.supplierId : prev.supplierId,
      // Lo pendiente de cada línea, en la unidad del pedido
      lines: order
        ? order.lines.filter((l) => Number(l.quantity) > Number(l.receivedQuantity ?? 0)).map((l) => ({
          ...newLine(), stockItemId: l.stockItemId, unit: l.unit,
          quantity: Number(l.quantity) - Number(l.receivedQuantity ?? 0), unitPrice: l.unitPrice ?? ''
        }))
        : prev.lines
    }));
  };

  const setLine = (index, field, value) => setValues((prev) => ({
    ...prev,
    lines: prev.lines.map((l, i) => {
      if (i !== index) return l;
      const next = { ...l, [field]: value };
      if (field === 'stockItemId') next.unit = itemsById[value]?.purchaseUnit ?? '';
      return next;
    })
  }));

  const submit = (e) => {
    e.preventDefault();
    const warehouse = lookups.warehouses.find((w) => String(w.id) === String(v.warehouseId));
    confirm.ask({
      title: 'Registrar albarán',
      message: `Entrarán ${v.lines.length} línea(s) en ${warehouse?.name ?? 'el almacén elegido'}. Los movimientos de stock no se pueden editar después; un error se corrige con un ajuste.`,
      confirmText: 'Registrar',
      onConfirm: async () => {
        const body = {
          supplierId: toNumber(v.supplierId), purchaseOrderId: toNumber(v.purchaseOrderId), warehouseId: toNumber(v.warehouseId),
          supplierDeliveryNote: v.supplierDeliveryNote || null, receiptDate: v.receiptDate || null, notes: v.notes || null,
          lines: v.lines.map((l) => ({
            stockItemId: toNumber(l.stockItemId), quantity: toNumber(l.quantity), unit: l.unit || null,
            unitPrice: manager ? toNumber(l.unitPrice) : null, lotCode: l.lotCode || null, expiryDate: l.expiryDate || null
          }))
        };
        const saved = await form.submit(() => goodsReceipts.create(body));
        if (saved) onSaved(saved);
      }
    });
  };

  return (
    <Panel title="Nuevo albarán">
      <form onSubmit={submit} noValidate className="g-lines">
        <FormGrid>
          <Select label="Proveedor" placeholder="Elige un proveedor" value={v.supplierId} onChange={set('supplierId')}
                  options={asOptions(lookups.suppliers, supplierName)} error={errors.supplierId} />
          {manager && (
            <Select label="Pedido (opcional)" placeholder="Sin pedido" value={v.purchaseOrderId} onChange={pickOrder}
                    options={asOptions(orders, (o) => `${o.number} · ${o.supplierName}`)} error={errors.purchaseOrderId} />
          )}
          <Select label="Almacén" placeholder="Elige un almacén" value={v.warehouseId} onChange={set('warehouseId')}
                  options={asOptions(lookups.warehouses)} error={errors.warehouseId} />
          <Input label="Nº de albarán del proveedor" value={v.supplierDeliveryNote} onChange={set('supplierDeliveryNote')}
                 error={errors.supplierDeliveryNote} />
          <Input label="Fecha" type="date" value={v.receiptDate} onChange={set('receiptDate')} error={errors.receiptDate} />
          <Textarea label="Notas" value={v.notes} onChange={set('notes')} error={errors.notes} />
        </FormGrid>
        {!manager && <Notice tone="info">El precio lo pone la tarifa del proveedor; si no coincide, avisa al encargado.</Notice>}
        <Field label="Líneas" wide error={errors.lines}>
          <div className="g-lines">
            {v.lines.map((line, i) => {
              const item = itemsById[line.stockItemId];
              return (
                <div className="g-line" key={i}>
                  <Select label="Artículo" placeholder="Elige" value={line.stockItemId} options={asOptions(lookups.items, itemLabel)}
                          onChange={(e) => setLine(i, 'stockItemId', e.target.value)} error={errors[`lines[${i}].stockItemId`]} />
                  <Input label="Cantidad" type="number" min="0" step="any" value={line.quantity}
                         onChange={(e) => setLine(i, 'quantity', e.target.value)} error={errors[`lines[${i}].quantity`]} />
                  <Select label="Unidad" value={line.unit} options={unitOptionsFor(item)}
                          onChange={(e) => setLine(i, 'unit', e.target.value)} error={errors[`lines[${i}].unit`]} />
                  {manager && (
                    <Input label="Precio (vacío = tarifa)" type="number" min="0" step="any" value={line.unitPrice}
                           onChange={(e) => setLine(i, 'unitPrice', e.target.value)} error={errors[`lines[${i}].unitPrice`]} />
                  )}
                  {item?.perishable && (
                    <>
                      <Input label="Lote" value={line.lotCode} onChange={(e) => setLine(i, 'lotCode', e.target.value)}
                             error={errors[`lines[${i}].lotCode`]} />
                      <Input label="Caducidad" type="date" value={line.expiryDate}
                             onChange={(e) => setLine(i, 'expiryDate', e.target.value)} error={errors[`lines[${i}].expiryDate`]} />
                    </>
                  )}
                  <button type="button" className="g-icon-btn" aria-label="Quitar línea"
                          onClick={() => setValues((p) => ({ ...p, lines: p.lines.filter((_, j) => j !== i) }))}>
                    <Trash2 size={18} />
                  </button>
                </div>
              );
            })}
            <button type="button" className="btn btn-outline btn-sm" style={{ alignSelf: 'flex-start' }}
                    onClick={() => setValues((p) => ({ ...p, lines: [...p.lines, newLine()] }))}>
              <Plus size={16} />Añadir línea
            </button>
          </div>
        </Field>
        <ErrorNotice error={form.error} />
        <div className="g-toolbar" style={{ justifyContent: 'flex-end' }}>
          <button type="submit" className="btn btn-primary" disabled={form.saving}>Registrar entrada</button>
        </div>
      </form>
      <ConfirmModal {...confirm.props} />
    </Panel>
  );
};

export default GoodsReceiptPage;
