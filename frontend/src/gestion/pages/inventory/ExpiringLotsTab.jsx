import { useState } from 'react';
import { inventory } from '../../api';
import { Badge, DataTable, Input, Panel, Toolbar } from '../../components';
import { useLoad } from '../../../hooks/useLoad';
import { formatDate, formatQty } from '../../../lib/format';

const ExpiringLotsTab = () => {
  const [days, setDays] = useState('3');
  const lots = useLoad(() => inventory.expiringLots(days || 0), [days]);
  return (
    <Panel>
      <Toolbar>
        <Input label="Próximos días" type="number" min="0" value={days} onChange={(e) => setDays(e.target.value)} />
      </Toolbar>
      <DataTable rowKey="lotId" loading={lots.loading} error={lots.error} rows={lots.data} empty="Nada caduca en ese plazo"
                 columns={[
                   { key: 'stockItemName', label: 'Artículo' },
                   { key: 'warehouseName', label: 'Almacén' },
                   { key: 'lotCode', label: 'Lote', render: (l) => <span className="g-code">{l.lotCode ?? '—'}</span> },
                   { key: 'expiryDate', label: 'Caduca', render: (l) => formatDate(l.expiryDate) },
                   { key: 'daysLeft', label: 'Días', align: 'right', render: (l) => (
                     <Badge tone={l.daysLeft < 0 ? 'danger' : l.daysLeft <= 1 ? 'warn' : 'neutral'}>
                       {l.daysLeft < 0 ? 'Caducado' : `${l.daysLeft} d`}
                     </Badge>
                   ) },
                   { key: 'remainingQuantity', label: 'Queda', align: 'right', render: (l) => formatQty(l.remainingQuantity, l.unit) }
                 ]} />
    </Panel>
  );
};

export default ExpiringLotsTab;
