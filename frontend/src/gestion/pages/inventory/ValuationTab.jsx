import { inventory } from '../../api';
import { DataTable, Kpi, Panel } from '../../components';
import { useLoad } from '../../../hooks/useLoad';
import { formatEUR, formatQty } from '../../../lib/format';

const ValuationTab = () => {
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

export default ValuationTab;
