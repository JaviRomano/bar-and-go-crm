import { useState } from 'react';
import { useAuth } from '../../../context/useAuth';
import { useLookups } from '../../lookups';
import { Page, Tabs } from '../../components';
import StockTab from './StockTab';
import StockOperationForm from './StockOperationForm';
import ExpiringLotsTab from './ExpiringLotsTab';
import MovementsTab from './MovementsTab';
import ValuationTab from './ValuationTab';
import WarehousesTab from './WarehousesTab';

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
      {tab === 'stock' && <StockTab lookups={lookups} />}
      {tab === 'transfer' && <StockOperationForm kind="transfer" lookups={lookups} />}
      {tab === 'expiring' && <ExpiringLotsTab />}
      {tab === 'movements' && <MovementsTab lookups={lookups} />}
      {tab === 'valuation' && <ValuationTab />}
      {tab === 'adjust' && <StockOperationForm kind="adjust" lookups={lookups} />}
      {tab === 'warehouses' && <WarehousesTab />}
    </Page>
  );
};

export default InventoryPage;
