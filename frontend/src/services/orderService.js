import api from './api';

const orderService = {
  getAll: async () => {
    const response = await api.get('/orders');
    return response.data;
  },
  
  getById: async (id) => {
    const response = await api.get(`/orders/${id}`);
    return response.data;
  },
  
  // Pedidos del usuario autenticado; status es opcional
  getMine: async (status) => {
    const response = await api.get('/orders/me', { params: status ? { status } : {} });
    return response.data;
  },
  
  getByStatus: async (status) => {
    const response = await api.get(`/orders/status/${status}`);
    return response.data;
  },
  
  // orderData: { timeTakeAway, items: [{ productId, quantity }] }
  create: async (orderData) => {
    const response = await api.post('/orders', orderData);
    return response.data;
  },
  
  updateStatus: async (id, status) => {
    const response = await api.patch(`/orders/${id}/status`, { status });
    return response.data;
  },
  
  delete: async (id) => {
    await api.delete(`/orders/${id}`);
  }
};

export default orderService;