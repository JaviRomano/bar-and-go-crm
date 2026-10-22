import { useState } from 'react';
import ConfirmModal from '../../../components/common/ConfirmModal';
import { inventory } from '../../api';
import { asOptions, itemLabel, unitOptionsFor } from '../../lookups';
import { ErrorNotice, FormGrid, Input, Notice, Panel, Select, Textarea } from '../../components';
import { useConfirm } from '../../../hooks/useConfirm';
import { useForm } from '../../../hooks/useForm';
import { formatQty, toNumber } from '../../../lib/format';

const StockOperationForm = ({ kind, lookups }) => {
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

export default StockOperationForm;
