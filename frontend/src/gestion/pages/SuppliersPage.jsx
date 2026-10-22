import { useState } from 'react';
import { Plus } from 'lucide-react';
import { suppliers } from '../api';
import { useForm, useLoad, toNumber } from '../support';
import { Badge, DataTable, ErrorNotice, FormGrid, Input, Page, Panel, Sheet, Textarea, Toolbar } from '../ui';

const EMPTY = {
  taxId: '', legalName: '', tradeName: '', contactName: '', email: '', phone: '', paymentTerms: '',
  leadTimeDays: '', notes: '', active: true
};

const SuppliersPage = () => {
  const [showInactive, setShowInactive] = useState(false);
  const [editing, setEditing] = useState(null);
  const list = useLoad(() => suppliers.list(showInactive ? undefined : true), [showInactive]);

  const columns = [
    { key: 'legalName', label: 'Proveedor', render: (s) => (
      <>
        <div className="g-strong">{s.tradeName || s.legalName}</div>
        {s.tradeName && <div className="g-muted">{s.legalName}</div>}
      </>
    ) },
    { key: 'taxId', label: 'CIF/NIF', render: (s) => <span className="g-code">{s.taxId}</span> },
    { key: 'contactName', label: 'Contacto', render: (s) => [s.contactName, s.phone, s.email].filter(Boolean).join(' · ') || '—' },
    { key: 'leadTimeDays', label: 'Plazo', align: 'right', render: (s) => (s.leadTimeDays != null ? `${s.leadTimeDays} d` : '—') },
    { key: 'active', label: 'Estado', render: (s) => (s.active ? <Badge tone="ok">Activo</Badge> : <Badge>Inactivo</Badge>) }
  ];

  return (
    <Page
      title="Proveedores"
      subtitle="Datos fiscales, contacto y plazo de entrega"
      actions={<button type="button" className="btn btn-primary" onClick={() => setEditing(EMPTY)}><Plus size={18} />Nuevo proveedor</button>}
    >
      <Panel>
        <Toolbar>
          <label className="g-checkbox">
            <input type="checkbox" checked={showInactive} onChange={(e) => setShowInactive(e.target.checked)} />
            Mostrar inactivos
          </label>
        </Toolbar>
        <DataTable columns={columns} rows={list.data} loading={list.loading} error={list.error}
                   empty="No hay proveedores" onRowClick={(s) => setEditing(s)} />
      </Panel>
      {editing && (
        <SupplierSheet supplier={editing} onClose={(saved) => { setEditing(null); if (saved) list.reload(); }} />
      )}
    </Page>
  );
};

const SupplierSheet = ({ supplier, onClose }) => {
  const isNew = !supplier.id;
  const form = useForm({ ...EMPTY, ...supplier, leadTimeDays: supplier.leadTimeDays ?? '' });
  const { values: v, set, errors } = form;

  const save = async (e) => {
    e.preventDefault();
    const body = { ...v, leadTimeDays: toNumber(v.leadTimeDays) };
    const saved = await form.submit(() => (isNew ? suppliers.create(body) : suppliers.update(supplier.id, body)));
    if (saved) onClose(true);
  };

  return (
    <Sheet
      title={isNew ? 'Nuevo proveedor' : v.legalName}
      onClose={() => onClose(false)}
      footer={(
        <>
          <button type="button" className="btn btn-outline" onClick={() => onClose(false)}>Cancelar</button>
          <button type="submit" form="supplier-form" className="btn btn-primary" disabled={form.saving}>Guardar</button>
        </>
      )}
    >
      <form id="supplier-form" onSubmit={save} noValidate>
        <FormGrid>
          <Input label="CIF/NIF" value={v.taxId} onChange={set('taxId')} error={errors.taxId} required />
          <Input label="Razón social" value={v.legalName} onChange={set('legalName')} error={errors.legalName} required />
          <Input label="Nombre comercial" value={v.tradeName ?? ''} onChange={set('tradeName')} error={errors.tradeName} />
          <Input label="Persona de contacto" value={v.contactName ?? ''} onChange={set('contactName')} error={errors.contactName} />
          <Input label="Teléfono" value={v.phone ?? ''} onChange={set('phone')} error={errors.phone} />
          <Input label="Email" type="email" value={v.email ?? ''} onChange={set('email')} error={errors.email} />
          <Input label="Condiciones de pago" value={v.paymentTerms ?? ''} onChange={set('paymentTerms')} error={errors.paymentTerms} />
          <Input label="Plazo de entrega (días)" type="number" min="0" value={v.leadTimeDays} onChange={set('leadTimeDays')}
                 error={errors.leadTimeDays} />
          <Textarea label="Notas" value={v.notes ?? ''} onChange={set('notes')} error={errors.notes} />
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

export default SuppliersPage;
