import { useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { useAuth } from '../../context/useAuth';
import ConfirmModal from '../../components/common/ConfirmModal';
import { masterData, recipes } from '../api';
import { ALLERGENS, RECIPE_TYPES, toOptions } from '../labels';
import { asOptions, itemLabel, unitOptionsFor, useLookups } from '../lookups';
import { Badge, DataTable, ErrorNotice, Field, FormGrid, Input, Notice, Page, Panel, Select, Sheet, Tabs, Textarea, Toolbar } from '../components';
import { useConfirm } from '../../hooks/useConfirm';
import { useForm } from '../../hooks/useForm';
import { useLoad } from '../../hooks/useLoad';
import { formatDateTime, formatEUR, formatPct, formatQty, toNumber } from '../../lib/format';
import { useLines } from '../../hooks/useLines';

const RecipesPage = () => {
  const { canManage } = useAuth();
  const manager = canManage();
  const [type, setType] = useState('');
  const [open, setOpen] = useState(null);
  const [editing, setEditing] = useState(null);
  const list = useLoad(() => recipes.list(type), [type]);

  const columns = [
    { key: 'name', label: 'Escandallo', render: (r) => (
      <>
        <div className="g-strong">{r.name}</div>
        <div className="g-muted">{r.productName ?? r.stockItemCode ?? ''}</div>
      </>
    ) },
    { key: 'type', label: 'Tipo', render: (r) => (
      <>
        <Badge tone={r.type === 'DISH' ? 'info' : 'neutral'}>{RECIPE_TYPES[r.type]}</Badge>
        {!r.active && <> <Badge>Inactivo</Badge></>}
      </>
    ) },
    ...(manager ? [
      { key: 'cost', label: 'Coste', align: 'right', render: (r) => (
        <>
          {r.type === 'DISH' ? `${formatEUR(r.costPerPortion)} / ración` : `${formatEUR(r.costPerYieldUnit)} / ${r.yieldUnit}`}
          {r.costComplete === false && <div><Badge tone="warn">Incompleto</Badge></div>}
        </>
      ) },
      { key: 'productPrice', label: 'PVP', align: 'right', render: (r) => formatEUR(r.productPrice) },
      { key: 'foodCostPct', label: 'Food cost', align: 'right', render: (r) => formatPct(r.foodCostPct) },
      { key: 'grossMargin', label: 'Margen', align: 'right', render: (r) => formatEUR(r.grossMargin) },
      { key: 'suggestedPrice', label: 'PVP sugerido', align: 'right', render: (r) => formatEUR(r.suggestedPrice) }
    ] : []),
    { key: 'allergens', label: 'Alérgenos', render: (r) => (r.allergens?.length ? r.allergens.map((a) => ALLERGENS[a]).join(', ') : '—') }
  ];

  return (
    <Page title="Escandallos" subtitle="Coste de cada plato y elaboración, alérgenos y producción"
          actions={manager && (
            <button type="button" className="btn btn-primary" onClick={() => setEditing({})}><Plus size={18} />Nuevo escandallo</button>
          )}>
      <Panel>
        <Toolbar>
          <Select label="Tipo" placeholder="Todos" value={type} options={toOptions(RECIPE_TYPES)} onChange={(e) => setType(e.target.value)} />
        </Toolbar>
        <DataTable loading={list.loading} error={list.error} rows={list.data} empty="No hay escandallos" columns={columns}
                   onRowClick={setOpen} />
      </Panel>
      {open && (
        <RecipeSheet recipe={open} manager={manager}
                     onEdit={() => { setEditing(open); setOpen(null); }}
                     onClose={() => { setOpen(null); list.reload(); }} />
      )}
      {editing && (
        <RecipeForm recipe={editing} recipes={list.data ?? []}
                    onClose={(saved) => { setEditing(null); if (saved) list.reload(); }} />
      )}
    </Page>
  );
};

const RecipeSheet = ({ recipe, manager, onEdit, onClose }) => {
  const [tab, setTab] = useState('lines');
  const canProduce = recipe.type === 'PREPARATION' && recipe.stockItemId && recipe.active;
  const tabs = [
    { id: 'lines', label: 'Ingredientes' },
    ...(canProduce ? [{ id: 'production', label: 'Producir' }] : []),
    ...(manager ? [{ id: 'history', label: 'Histórico de coste' }] : [])
  ];
  return (
    <Sheet title={recipe.name} onClose={onClose} wide
           footer={manager && <button type="button" className="btn btn-primary" onClick={onEdit}>Editar</button>}>
      <dl className="g-dl">
        <dt>Tipo</dt><dd>{RECIPE_TYPES[recipe.type]}</dd>
        {recipe.type === 'DISH'
          ? <><dt>Producto</dt><dd>{recipe.productName} · {recipe.portions} ración(es)</dd></>
          : <><dt>Rinde</dt><dd>{formatQty(recipe.yieldQuantity, recipe.yieldUnit)}{recipe.stockItemCode && ` · se guarda como ${recipe.stockItemCode}`}</dd></>}
        {manager && recipe.costComplete === false && (
          <><dt>Coste</dt><dd><Badge tone="warn">Incompleto: sin precio {recipe.missingCostItems.join(', ')}</Badge></dd></>
        )}
        {manager && recipe.totalCost != null && <><dt>Coste total</dt><dd className="g-strong">{formatEUR(recipe.totalCost)}</dd></>}
        {recipe.notes && <><dt>Notas</dt><dd>{recipe.notes}</dd></>}
      </dl>
      <Tabs tabs={tabs} active={tab} onChange={setTab} />
      {tab === 'lines' && (
        <DataTable rowKey="lineNumber" rows={recipe.lines} columns={[
          { key: 'name', label: 'Ingrediente', render: (l) => l.stockItemName ?? <>{l.subRecipeName} <Badge>Elaboración</Badge></> },
          { key: 'grossQuantity', label: 'Bruto', align: 'right', render: (l) => formatQty(l.grossQuantity, l.unit) },
          { key: 'yieldPct', label: 'Aprov.', align: 'right', render: (l) => formatPct(l.yieldPct) },
          { key: 'netQuantity', label: 'Neto', align: 'right', render: (l) => formatQty(l.netQuantity, l.unit) },
          ...(manager ? [{ key: 'lineCost', label: 'Coste', align: 'right', render: (l) => formatEUR(l.lineCost) }] : [])
        ]} />
      )}
      {tab === 'production' && <Production recipe={recipe} />}
      {tab === 'history' && <CostHistory recipe={recipe} />}
    </Sheet>
  );
};

const Production = ({ recipe }) => {
  const { warehouses } = useLookups();
  const confirm = useConfirm();
  const [done, setDone] = useState(null);
  const form = useForm({ warehouseId: '', quantity: '', notes: '' });
  const { values: v, set, errors } = form;
  const submit = (e) => {
    e.preventDefault();
    confirm.ask({
      title: 'Registrar producción',
      message: `Se descontarán los ingredientes de ${formatQty(v.quantity, recipe.yieldUnit)} de ${recipe.name} y se dará de alta el resultado en el almacén.`,
      confirmText: 'Producir',
      onConfirm: async () => {
        const result = await form.submit(() => recipes.produce(recipe.id, {
          warehouseId: toNumber(v.warehouseId), quantity: toNumber(v.quantity), notes: v.notes || null
        }));
        if (result) setDone(result);
      }
    });
  };
  return (
    <form onSubmit={submit} noValidate className="g-lines">
      {done && <Notice tone="ok">Producidos {formatQty(done.quantity, done.unit)} ({done.movements.length} movimientos).</Notice>}
      <FormGrid>
        <Select label="Almacén" placeholder="Elige un almacén" value={v.warehouseId} onChange={set('warehouseId')}
                options={asOptions(warehouses)} error={errors.warehouseId} />
        <Input label={`Cantidad (${recipe.yieldUnit})`} type="number" min="0" step="any" value={v.quantity} onChange={set('quantity')}
               error={errors.quantity} />
        <Textarea label="Notas" value={v.notes} onChange={set('notes')} error={errors.notes} />
      </FormGrid>
      <ErrorNotice error={form.error} />
      <div className="g-toolbar" style={{ justifyContent: 'flex-end' }}>
        <button type="submit" className="btn btn-primary" disabled={form.saving}>Producir</button>
      </div>
      <ConfirmModal {...confirm.props} />
    </form>
  );
};

const HISTORY_REASONS = { CREATED: 'Alta', UPDATED: 'Edición', PRICE_CHANGE: 'Cambio de precio', RECALCULATION: 'Recálculo' };

const CostHistory = ({ recipe }) => {
  const history = useLoad(() => recipes.costHistory(recipe.id), [recipe.id]);
  return (
    <DataTable rowKey="recordedAt" loading={history.loading} error={history.error} rows={history.data} empty="Sin histórico"
               columns={[
                 { key: 'recordedAt', label: 'Fecha', render: (h) => formatDateTime(h.recordedAt) },
                 { key: 'reason', label: 'Motivo', render: (h) => HISTORY_REASONS[h.reason] ?? h.reason },
                 { key: 'totalCost', label: 'Coste total', align: 'right', render: (h) => formatEUR(h.totalCost) },
                 { key: 'costPerUnit', label: 'Por unidad', align: 'right', render: (h) => formatEUR(h.costPerUnit) },
                 { key: 'complete', label: '', render: (h) => (h.complete ? '' : <Badge tone="warn">Incompleto</Badge>) }
               ]} />
  );
};

const newLine = () => ({ kind: 'item', stockItemId: '', subRecipeId: '', grossQuantity: '', unit: '', yieldPct: '' });

const RecipeForm = ({ recipe, recipes: allRecipes, onClose }) => {
  const isNew = !recipe.id;
  const { items, units } = useLookups({ items: true, units: true });
  const products = useLoad(() => masterData.products(), []);
  const form = useForm({
    name: recipe.name ?? '', type: recipe.type ?? 'DISH', productId: recipe.productId ?? '', stockItemId: recipe.stockItemId ?? '',
    portions: recipe.portions ?? '1', yieldQuantity: recipe.yieldQuantity ?? '', yieldUnit: recipe.yieldUnit ?? '',
    targetFoodCostPct: recipe.targetFoodCostPct ?? '', notes: recipe.notes ?? '', active: recipe.active ?? true,
    lines: recipe.lines?.length
      ? recipe.lines.map((l) => ({
        kind: l.subRecipeId ? 'sub' : 'item', stockItemId: l.stockItemId ?? '', subRecipeId: l.subRecipeId ?? '',
        grossQuantity: l.grossQuantity, unit: l.unit, yieldPct: l.yieldPct ?? ''
      }))
      : [newLine()]
  });
  const { values: v, set, errors } = form;
  const itemsById = Object.fromEntries(items.map((i) => [i.id, i]));
  const preparations = allRecipes.filter((r) => r.type === 'PREPARATION' && r.id !== recipe.id);
  const unitCodes = units.map((u) => ({ value: u.code, label: u.code }));

  // Al elegir ingrediente, su unidad de uso o la de rendimiento de la elaboración
  const lines = useLines(form, 'lines', {
    create: newLine,
    derive: (line, field, value) => {
      if (field === 'stockItemId') return { ...line, unit: itemsById[value]?.usageUnit ?? '' };
      if (field === 'subRecipeId') return { ...line, unit: preparations.find((r) => String(r.id) === String(value))?.yieldUnit ?? '' };
      return line;
    }
  });

  const save = async (e) => {
    e.preventDefault();
    const dish = v.type === 'DISH';
    const body = {
      name: v.name, type: v.type, productId: dish ? toNumber(v.productId) : null, stockItemId: dish ? null : toNumber(v.stockItemId),
      portions: dish ? toNumber(v.portions) : null, yieldQuantity: dish ? null : toNumber(v.yieldQuantity),
      yieldUnit: dish ? null : v.yieldUnit || null, targetFoodCostPct: toNumber(v.targetFoodCostPct), notes: v.notes || null,
      active: v.active,
      lines: v.lines.map((l) => ({
        stockItemId: l.kind === 'item' ? toNumber(l.stockItemId) : null, subRecipeId: l.kind === 'sub' ? toNumber(l.subRecipeId) : null,
        grossQuantity: toNumber(l.grossQuantity), unit: l.unit || null, yieldPct: toNumber(l.yieldPct)
      }))
    };
    const saved = await form.submit(() => (isNew ? recipes.create(body) : recipes.update(recipe.id, body)));
    if (saved) onClose(true);
  };

  return (
    <Sheet title={isNew ? 'Nuevo escandallo' : `Editar ${recipe.name}`} onClose={() => onClose(false)} wide footer={(
      <>
        <button type="button" className="btn btn-outline" onClick={() => onClose(false)}>Cancelar</button>
        <button type="submit" form="recipe-form" className="btn btn-primary" disabled={form.saving}>Guardar</button>
      </>
    )}>
      <form id="recipe-form" onSubmit={save} noValidate className="g-lines">
        <FormGrid>
          <Input label="Nombre" value={v.name} onChange={set('name')} error={errors.name} />
          <Select label="Tipo" value={v.type} onChange={set('type')} options={toOptions(RECIPE_TYPES)} error={errors.type} />
          {v.type === 'DISH' ? (
            <>
              <Select label="Producto de la carta" placeholder="Elige un producto" value={v.productId} onChange={set('productId')}
                      options={asOptions(products.data ?? [])} error={errors.productId} />
              <Input label="Raciones" type="number" min="0" step="any" value={v.portions} onChange={set('portions')} error={errors.portions} />
              <Input label="Food cost objetivo (%)" type="number" min="0" max="99.99" step="any" value={v.targetFoodCostPct}
                     onChange={set('targetFoodCostPct')} error={errors.targetFoodCostPct} hint="Vacío: el general del local" />
            </>
          ) : (
            <>
              <Input label="Rinde" type="number" min="0" step="any" value={v.yieldQuantity} onChange={set('yieldQuantity')}
                     error={errors.yieldQuantity} />
              <Select label="Unidad de rendimiento" placeholder="—" value={v.yieldUnit} onChange={set('yieldUnit')} options={unitCodes}
                      error={errors.yieldUnit} />
              <Select label="Se guarda como artículo" placeholder="No se guarda (se hace al momento)" value={v.stockItemId}
                      onChange={set('stockItemId')} options={asOptions(items, itemLabel)} error={errors.stockItemId} />
            </>
          )}
          <Textarea label="Notas" value={v.notes} onChange={set('notes')} error={errors.notes} />
          {!isNew && (
            <label className="g-checkbox"><input type="checkbox" checked={!!v.active} onChange={set('active')} /> Activo</label>
          )}
        </FormGrid>
        <Field label="Ingredientes (cantidad bruta: lo que se compra)" wide error={errors.lines}>
          <div className="g-lines">
            {v.lines.map((line, i) => (
              <div className="g-line" key={i}>
                <Select label="Tipo" value={line.kind} onChange={(e) => lines.update(i, 'kind', e.target.value)}
                        options={[{ value: 'item', label: 'Artículo' }, { value: 'sub', label: 'Elaboración' }]} />
                {line.kind === 'item' ? (
                  <Select label="Artículo" placeholder="Elige" value={line.stockItemId} options={asOptions(items, itemLabel)}
                          onChange={(e) => lines.update(i, 'stockItemId', e.target.value)} error={lines.error(i, 'stockItemId')} />
                ) : (
                  <Select label="Elaboración" placeholder="Elige" value={line.subRecipeId} options={asOptions(preparations)}
                          onChange={(e) => lines.update(i, 'subRecipeId', e.target.value)} error={lines.error(i, 'subRecipeId')} />
                )}
                <Input label="Cantidad" type="number" min="0" step="any" value={line.grossQuantity}
                       onChange={(e) => lines.update(i, 'grossQuantity', e.target.value)} error={lines.error(i, 'grossQuantity')} />
                <Select label="Unidad" value={line.unit}
                        options={line.kind === 'item' ? unitOptionsFor(itemsById[line.stockItemId]) : unitCodes}
                        onChange={(e) => lines.update(i, 'unit', e.target.value)} error={lines.error(i, 'unit')} />
                <Input label="Aprovechamiento %" type="number" min="0" max="100" step="any" value={line.yieldPct}
                       placeholder="100" onChange={(e) => lines.update(i, 'yieldPct', e.target.value)} error={lines.error(i, 'yieldPct')} />
                <button type="button" className="g-icon-btn" aria-label="Quitar ingrediente"
                        onClick={() => lines.remove(i)}>
                  <Trash2 size={18} />
                </button>
              </div>
            ))}
            <button type="button" className="btn btn-outline btn-sm" style={{ alignSelf: 'flex-start' }}
                    onClick={lines.add}>
              <Plus size={16} />Añadir ingrediente
            </button>
          </div>
        </Field>
      </form>
      <ErrorNotice error={form.error} />
    </Sheet>
  );
};

export default RecipesPage;
