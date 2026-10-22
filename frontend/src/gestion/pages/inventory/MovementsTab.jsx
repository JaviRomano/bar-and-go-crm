import { useState } from 'react';
import { inventory } from '../../api';
import { MOVEMENT_TONES, MOVEMENT_TYPES } from '../../labels';
import { asOptions, itemLabel } from '../../lookups';
import { Badge, DataTable, Input, Panel, Select, Toolbar } from '../../components';
import { useLoad } from '../../../hooks/useLoad';
import { formatDateTime, formatEUR, formatQty } from '../../../lib/format';

const MovementsTab = ({ lookups }) => {
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

export default MovementsTab;
