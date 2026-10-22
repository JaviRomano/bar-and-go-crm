import { useState } from 'react';
import { asOptions, useLookups } from '../../lookups';
import { Input, Page, Panel, Select, Tabs, Toolbar } from '../../components';
import { daysAgo, isoDate } from '../../../lib/format';
import FoodCostReport from './FoodCostReport';
import StockValueReport from './StockValueReport';
import WasteReport from './WasteReport';
import RotationReport from './RotationReport';
import SupplierPricesReport from './SupplierPricesReport';
import MenuEngineeringReport from './MenuEngineeringReport';

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
      {tab === 'food-cost' && <FoodCostReport filters={filters} />}
      {tab === 'stock' && <StockValueReport filters={filters} />}
      {tab === 'waste' && <WasteReport filters={filters} />}
      {tab === 'rotation' && <RotationReport filters={filters} />}
      {tab === 'prices' && <SupplierPricesReport filters={filters} lookups={lookups} />}
      {tab === 'menu' && <MenuEngineeringReport filters={filters} />}
    </Page>
  );
};

export default ReportsPage;
