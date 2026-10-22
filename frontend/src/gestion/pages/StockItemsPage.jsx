import { useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { useAuth } from '../../context/useAuth';
import { masterData, stockItems } from '../api';
import { ALLERGENS } from '../labels';
import { asOptions, useLookups } from '../lookups';
import { formatDate, formatEUR, formatQty, toNumber, useForm, useLoad } from '../support';
import {
  Badge, DataTable, ErrorNotice, Field, FormGrid, Input, Page, Panel, Select, Sheet, Tabs, Toolbar
} from '../ui';

const EMPTY = {
  code: '', name: '', itemCategoryId: '', purchaseUnit: '', stockUnit: '', usageUnit: '', conversions: [],
  preferredSupplierId: '', vatRateId: '', minStock: '', maxStock: '', perishable: false, allergens: [], active: true
};

const StockItemsPage = () => {
  const { canManage } = useAuth();
  const [filters, setFilters] = useState({ categoryId: '', active: 'true', search: '' });
  const [editing, setEditing] = useState(null);
  const categories = useLoad(() => masterData.itemCategories(), []);
  const list = useLoad(() => stockItems.list({ categoryId: filters.categoryId, active: filters.active }),
    [filters.categoryId, filters.active]);

  const search = filters.search.trim().toLowerCase();
  const rows = (list.data ?? []).filter((i) => !search || `${i.code} ${i.name}`.toLowerCase().includes(search));

  const columns = [
    { key: 'name', label: 'Artículo', render: (i) => (
      <>
        <div className="g-strong">{i.name}</div>
        <div className="g-code">{i.code}</div>
      </>
    ) },
    { key: 'itemCategory', label: 'Categoría' },
    { key: 'units', label: 'Compra / inventario / uso', render: (i) => `${i.purchaseUnit} / ${i.stockUnit} / ${i.usageUnit}` },
    { key: 'minStock', label: 'Mín. / máx.', align: 'right',
      render: (i) => (i.minStock != null || i.maxStock != null ? `${formatQty(i.minStock)} / ${formatQty(i.maxStock)}` : '—') },
    { key: 'allergens', label: 'Alérgenos', render: (i) => (i.allergens?.length ? i.allergens.map((a) => ALLERGENS[a]).join(', ') : '—') },
    { key: 'active', label: 'Estado', render: (i) => (
      <>
        {i.active ? <Badge tone="ok">Activo</Badge> : <Badge>Inactivo</Badge>}
        {i.perishable && <> <Badge tone="warn">Perecedero</Badge></>}
      </>
    ) }
  ];

  return (
    <Page
      title="Artículos"
      subtitle="Materias primas y productos de compra, con sus unidades y alérgenos"
      actions={canManage() && (
        <button type="button" className="btn btn-primary" onClick={() => setEditing(EMPTY)}><Plus size={18} />Nuevo artículo</button>
      )}
    >
      <Panel>
        <Toolbar>
          <Input label="Buscar" placeholder="Código o nombre" value={filters.search}
                 onChange={(e) => setFilters({ ...filters, search: e.target.value })} />
          <Select label="Categoría" placeholder="Todas" value={filters.categoryId}
                  options={asOptions(categories.data ?? [])}
                  onChange={(e) => setFilters({ ...filters, categoryId: e.target.value })} />
          <Select label="Estado" placeholder="Todos" value={filters.active}
                  options={[{ value: 'true', label: 'Activos' }, { value: 'false', label: 'Inactivos' }]}
                  onChange={(e) => setFilters({ ...filters, active: e.target.value })} />
        </Toolbar>
        <DataTable columns={columns} rows={rows} loading={list.loading} error={list.error} empty="No hay artículos"
                   onRowClick={canManage() ? (i) => setEditing(i) : undefined} />
      </Panel>
      {editing && (
        <StockItemSheet item={editing} categories={categories.data ?? []}
                        onClose={(saved) => { setEditing(null); if (saved) list.reload(); }} />
      )}
    </Page>
  );
};

const StockItemSheet = ({ item, categories, onClose }) => {
  const isNew = !item.id;
  const [tab, setTab] = useState('data');
  return (
    <Sheet title={isNew ? 'Nuevo artículo' : item.name} onClose={() => onClose(false)} wide>
      {!isNew && (
        <Tabs active={tab} onChange={setTab} tabs={[
          { id: 'data', label: 'Datos' }, { id: 'prices', label: 'Proveedores y precios' }, { id: 'history', label: 'Compras' }
        ]} />
      )}
      {tab === 'data' && <StockItemForm item={item} categories={categories} onClose={onClose} />}
      {tab === 'prices' && <SupplierPrices item={item} />}
      {tab === 'history' && <PriceHistory item={item} />}
    </Sheet>
  );
};

const StockItemForm = ({ item, categories, onClose }) => {
  const isNew = !item.id;
  const { suppliers, units } = useLookups({ suppliersList: true, units: true });
  const vatRates = useLoad(() => masterData.vatRates(), []);
  const form = useForm({
    ...EMPTY, ...item,
    itemCategoryId: item.itemCategoryId ?? '', preferredSupplierId: item.preferredSupplierId ?? '',
    vatRateId: item.vatRateId ?? '', minStock: item.minStock ?? '', maxStock: item.maxStock ?? '',
    conversions: item.conversions ?? [], allergens: item.allergens ?? []
  });
  const { values: v, set, errors, setValues } = form;
  const unitOptions = units.map((u) => ({ value: u.code, label: `${u.code} · ${u.name}` }));

  const toggleAllergen = (code) => setValues((prev) => ({
    ...prev,
    allergens: prev.allergens.includes(code) ? prev.allergens.filter((a) => a !== code) : [...prev.allergens, code]
  }));
  const setConversion = (index, field, value) => setValues((prev) => ({
    ...prev, conversions: prev.conversions.map((c, i) => (i === index ? { ...c, [field]: value } : c))
  }));

  const save = async (e) => {
    e.preventDefault();
    const body = {
      ...v,
      itemCategoryId: toNumber(v.itemCategoryId), preferredSupplierId: toNumber(v.preferredSupplierId),
      vatRateId: toNumber(v.vatRateId), minStock: toNumber(v.minStock), maxStock: toNumber(v.maxStock),
      conversions: v.conversions.map((c) => ({ ...c, factor: toNumber(c.factor) }))
    };
    const saved = await form.submit(() => (isNew ? stockItems.create(body) : stockItems.update(item.id, body)));
    if (saved) onClose(true);
  };

  return (
    <form onSubmit={save} noValidate className="g-lines">
      <FormGrid>
        <Input label="Código" value={v.code} onChange={set('code')} error={errors.code} />
        <Input label="Nombre" value={v.name} onChange={set('name')} error={errors.name} />
        <Select label="Categoría" placeholder="Elige una categoría" value={v.itemCategoryId} onChange={set('itemCategoryId')}
                options={asOptions(categories)} error={errors.itemCategoryId} />
        <Select label="IVA de compra" placeholder="Elige un tipo" value={v.vatRateId} onChange={set('vatRateId')}
                options={asOptions(vatRates.data ?? [], (r) => `${r.name} (${r.rate} %)`)} error={errors.vatRateId} />
        <Select label="Unidad de compra" placeholder="—" value={v.purchaseUnit} onChange={set('purchaseUnit')}
                options={unitOptions} error={errors.purchaseUnit} />
        <Select label="Unidad de inventario" placeholder="—" value={v.stockUnit} onChange={set('stockUnit')}
                options={unitOptions} error={errors.stockUnit} />
        <Select label="Unidad de uso" placeholder="—" value={v.usageUnit} onChange={set('usageUnit')}
                options={unitOptions} error={errors.usageUnit} />
        <Select label="Proveedor habitual" placeholder="Ninguno" value={v.preferredSupplierId}
                onChange={set('preferredSupplierId')} options={asOptions(suppliers, (s) => s.tradeName || s.legalName)}
                error={errors.preferredSupplierId} />
        <Input label={`Stock mínimo${v.stockUnit ? ` (${v.stockUnit})` : ''}`} type="number" min="0" step="any"
               value={v.minStock} onChange={set('minStock')} error={errors.minStock} />
        <Input label={`Stock máximo${v.stockUnit ? ` (${v.stockUnit})` : ''}`} type="number" min="0" step="any"
               value={v.maxStock} onChange={set('maxStock')} error={errors.maxStock} />
        <label className="g-checkbox">
          <input type="checkbox" checked={!!v.perishable} onChange={set('perishable')} /> Perecedero (lote y caducidad)
        </label>
        {!isNew && (
          <label className="g-checkbox">
            <input type="checkbox" checked={!!v.active} onChange={set('active')} /> Activo
          </label>
        )}
      </FormGrid>

      <Field label="Conversiones propias" hint="Por ejemplo, 1 caja = 24 ud" wide error={errors.conversions}>
        <div className="g-lines">
          {v.conversions.map((c, i) => (
            <div className="g-line" key={i}>
              <Select label="De" placeholder="—" value={c.fromUnit ?? ''} options={unitOptions}
                      onChange={(e) => setConversion(i, 'fromUnit', e.target.value)} error={errors[`conversions[${i}].fromUnit`]} />
              <Input label="Factor" type="number" min="0" step="any" value={c.factor ?? ''}
                     onChange={(e) => setConversion(i, 'factor', e.target.value)} error={errors[`conversions[${i}].factor`]} />
              <Select label="A" placeholder="—" value={c.toUnit ?? ''} options={unitOptions}
                      onChange={(e) => setConversion(i, 'toUnit', e.target.value)} error={errors[`conversions[${i}].toUnit`]} />
              <button type="button" className="g-icon-btn" aria-label="Quitar conversión"
                      onClick={() => setValues((p) => ({ ...p, conversions: p.conversions.filter((_, j) => j !== i) }))}>
                <Trash2 size={18} />
              </button>
            </div>
          ))}
          <button type="button" className="btn btn-outline btn-sm" style={{ alignSelf: 'flex-start' }}
                  onClick={() => setValues((p) => ({ ...p, conversions: [...p.conversions, { fromUnit: '', toUnit: '', factor: '' }] }))}>
            <Plus size={16} />Añadir conversión
          </button>
        </div>
      </Field>

      <Field label="Alérgenos" wide>
        <div className="g-toolbar">
          {Object.entries(ALLERGENS).map(([code, label]) => (
            <label key={code} className="g-checkbox">
              <input type="checkbox" checked={v.allergens.includes(code)} onChange={() => toggleAllergen(code)} /> {label}
            </label>
          ))}
        </div>
      </Field>

      <ErrorNotice error={form.error} />
      <div className="g-toolbar" style={{ justifyContent: 'flex-end' }}>
        <button type="button" className="btn btn-outline" onClick={() => onClose(false)}>Cancelar</button>
        <button type="submit" className="btn btn-primary" disabled={form.saving}>Guardar</button>
      </div>
    </form>
  );
};

const SupplierPrices = ({ item }) => {
  const { suppliers } = useLookups({ suppliersList: true });
  const prices = useLoad(() => stockItems.suppliers(item.id), [item.id]);
  const form = useForm({ supplierId: '', purchaseUnit: item.purchaseUnit, unitPrice: '', supplierItemCode: '' });
  const { values: v, set, errors } = form;

  const save = async (e) => {
    e.preventDefault();
    const body = { purchaseUnit: v.purchaseUnit, unitPrice: toNumber(v.unitPrice), supplierItemCode: v.supplierItemCode || null };
    const saved = await form.submit(() => stockItems.setPrice(item.id, v.supplierId, body));
    if (saved) {
      form.setValues({ ...v, unitPrice: '', supplierItemCode: '' });
      prices.reload();
    }
  };
  const remove = async (supplierId) => {
    await form.submit(() => stockItems.removeSupplier(item.id, supplierId));
    prices.reload();
  };

  return (
    <div className="g-lines">
      <DataTable rowKey="supplierId" loading={prices.loading} error={prices.error} rows={prices.data}
                 empty="Sin precios de proveedor"
                 columns={[
                   { key: 'supplierName', label: 'Proveedor' },
                   { key: 'supplierItemCode', label: 'Ref. proveedor' },
                   { key: 'unitPrice', label: 'Precio', align: 'right', render: (p) => `${formatEUR(p.unitPrice)} / ${p.purchaseUnit}` },
                   { key: 'updatedAt', label: 'Actualizado', render: (p) => formatDate(p.updatedAt) },
                   { key: 'actions', label: '', render: (p) => (
                     <button type="button" className="g-icon-btn" aria-label="Quitar proveedor" onClick={() => remove(p.supplierId)}>
                       <Trash2 size={16} />
                     </button>
                   ) }
                 ]} />
      <Panel title="Fijar precio">
        <form onSubmit={save} noValidate>
          <FormGrid>
            <Select label="Proveedor" placeholder="Elige un proveedor" value={v.supplierId} onChange={set('supplierId')}
                    options={asOptions(suppliers, (s) => s.tradeName || s.legalName)} error={errors.supplierId} />
            <Input label="Unidad de compra" value={v.purchaseUnit} onChange={set('purchaseUnit')} error={errors.purchaseUnit} />
            <Input label="Precio sin IVA" type="number" min="0" step="any" value={v.unitPrice} onChange={set('unitPrice')}
                   error={errors.unitPrice} />
            <Input label="Ref. proveedor" value={v.supplierItemCode} onChange={set('supplierItemCode')} error={errors.supplierItemCode} />
          </FormGrid>
          <div className="g-toolbar" style={{ justifyContent: 'flex-end', marginTop: 'var(--sp-4)' }}>
            <button type="submit" className="btn btn-primary btn-sm" disabled={form.saving || !v.supplierId}>Guardar precio</button>
          </div>
        </form>
        <ErrorNotice error={form.error} />
      </Panel>
    </div>
  );
};

const PriceHistory = ({ item }) => {
  const history = useLoad(() => stockItems.priceHistory(item.id), [item.id]);
  return (
    <DataTable rowKey={(p, i) => `${p.purchasedOn}-${i}`} loading={history.loading} error={history.error} rows={history.data}
               empty="Aún no se ha comprado"
               columns={[
                 { key: 'purchasedOn', label: 'Fecha', render: (p) => formatDate(p.purchasedOn) },
                 { key: 'supplierName', label: 'Proveedor' },
                 { key: 'unitPrice', label: 'Precio', align: 'right', render: (p) => `${formatEUR(p.unitPrice)} / ${p.unit}` },
                 { key: 'reference', label: 'Documento' }
               ]} />
  );
};

export default StockItemsPage;
