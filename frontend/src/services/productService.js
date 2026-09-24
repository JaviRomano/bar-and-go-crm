import api from './api';

const productService = {
  getAll: async () => {
    const response = await api.get('/products');
    return response.data;
  },
  
  getAvailable: async () => {
    const response = await api.get('/products/available');
    return response.data;
  },
  
  getByCategory: async (category) => {
    const response = await api.get(`/products/category/${category}`);
    return response.data;
  },
  
  getById: async (id) => {
    const response = await api.get(`/products/${id}`);
    return response.data;
  },
  
  create: async (productData) => {
    const response = await api.post('/products', productData);
    return response.data;
  },
  
  update: async (id, productData) => {
    const response = await api.put(`/products/${id}`, productData);
    return response.data;
  },
  
  toggleAvailability: async (id) => {
    const response = await api.patch(`/products/${id}/toggle-availability`);
    return response.data;
  },
  
  delete: async (id) => {
    await api.delete(`/products/${id}`);
  }
};

export default productService;