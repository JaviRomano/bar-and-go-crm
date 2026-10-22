import api from '../services/api';

// Clientes de la API de back-office. Cada función devuelve directamente el cuerpo de la respuesta.
const get = (url, params) => api.get(url, { params: clean(params) }).then((r) => r.data);
const post = (url, body) => api.post(url, body).then((r) => r.data);
const put = (url, body) => api.put(url, body).then((r) => r.data);
const del = (url) => api.delete(url).then((r) => r.data);

// Los filtros vacíos no se envían: el backend interpreta "ausente" como "sin filtro"
function clean(params) {
  if (!params) return undefined;
  return Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v !== null && v !== undefined));
}

export const masterData = {
  units: () => get('/units'),
  vatRates: () => get('/vat-rates'),
  itemCategories: () => get('/item-categories'),
  productCategories: () => get('/product-categories'),
  products: () => get('/products')
};

export const suppliers = {
  list: (active) => get('/suppliers', { active }),
  create: (body) => post('/suppliers', body),
  update: (id, body) => put(`/suppliers/${id}`, body)
};

export const stockItems = {
  list: (filters) => get('/stock-items', filters),
  get: (id) => get(`/stock-items/${id}`),
  create: (body) => post('/stock-items', body),
  update: (id, body) => put(`/stock-items/${id}`, body),
  suppliers: (id) => get(`/stock-items/${id}/suppliers`),
  setPrice: (id, supplierId, body) => put(`/stock-items/${id}/suppliers/${supplierId}`, body),
  removeSupplier: (id, supplierId) => del(`/stock-items/${id}/suppliers/${supplierId}`),
  priceHistory: (id) => get(`/stock-items/${id}/price-history`)
};

export const purchaseOrders = {
  list: (filters) => get('/purchase-orders', filters),
  get: (id) => get(`/purchase-orders/${id}`),
  create: (body) => post('/purchase-orders', body),
  update: (id, body) => put(`/purchase-orders/${id}`, body),
  send: (id) => post(`/purchase-orders/${id}/send`),
  cancel: (id) => post(`/purchase-orders/${id}/cancel`),
  close: (id) => post(`/purchase-orders/${id}/close`),
  remove: (id) => del(`/purchase-orders/${id}`)
};

export const warehouses = {
  list: (includeInactive) => get('/warehouses', { includeInactive }),
  create: (body) => post('/warehouses', body),
  update: (id, body) => put(`/warehouses/${id}`, body)
};

export const inventory = {
  stock: (filters) => get('/inventory/stock', filters),
  valuation: () => get('/inventory/valuation'),
  movements: (filters) => get('/inventory/movements', filters),
  adjust: (body) => post('/inventory/adjustments', body),
  transfer: (body) => post('/inventory/transfers', body),
  expiringLots: (days) => get('/inventory/lots/expiring', { days }),
  replenishment: () => get('/inventory/replenishment'),
  replenishmentOrders: (supplierIds) => post('/inventory/replenishment/orders', { supplierIds })
};

export const goodsReceipts = {
  list: (filters) => get('/goods-receipts', filters),
  get: (id) => get(`/goods-receipts/${id}`),
  create: (body) => post('/goods-receipts', body)
};

export const stockCounts = {
  list: (warehouseId) => get('/stock-counts', { warehouseId }),
  get: (id) => get(`/stock-counts/${id}`),
  open: (body) => post('/stock-counts', body),
  record: (id, entries) => put(`/stock-counts/${id}/lines`, { entries }),
  close: (id) => post(`/stock-counts/${id}/close`)
};

export const recipes = {
  list: (type) => get('/recipes', { type }),
  get: (id) => get(`/recipes/${id}`),
  create: (body) => post('/recipes', body),
  update: (id, body) => put(`/recipes/${id}`, body),
  costHistory: (id) => get(`/recipes/${id}/cost-history`),
  produce: (id, body) => post(`/recipes/${id}/production`, body)
};

export const waste = {
  list: (filters) => get('/waste', filters),
  register: (body) => post('/waste', body),
  consumption: (fromCountId, toCountId) => get('/cost-control/consumption', { fromCountId, toCountId })
};

export const reports = {
  stockValue: (filters) => get('/reports/stock-value', filters),
  foodCost: (filters) => get('/reports/food-cost', filters),
  waste: (filters) => get('/reports/waste', filters),
  rotation: (filters) => get('/reports/rotation', filters),
  supplierPrices: (filters) => get('/reports/supplier-prices', filters),
  menuEngineering: (filters) => get('/reports/menu-engineering', filters)
};
