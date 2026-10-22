import { useState } from 'react';
import { inventory } from '../../api';
import { asOptions } from '../../lookups';
import { Badge, DataTable, Input, Notice, Panel, Select, Toolbar } from '../../components';
import { useLoad } from '../../../hooks/useLoad';
import { formatQty } from '../../../lib/format';

const StockTab = ({ lookups }) => {
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

export default StockTab;
