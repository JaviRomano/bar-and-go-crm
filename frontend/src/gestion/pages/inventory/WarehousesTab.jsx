import { useState } from 'react';
import { Plus } from 'lucide-react';
import { warehouses as warehouseApi } from '../../api';
import { WAREHOUSE_TYPES, toOptions } from '../../labels';
import { Badge, DataTable, ErrorNotice, FormGrid, Input, Panel, Select, Sheet } from '../../components';
import { useForm } from '../../../hooks/useForm';
import { useLoad } from '../../../hooks/useLoad';

const WarehousesTab = () => {
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

export default WarehousesTab;
