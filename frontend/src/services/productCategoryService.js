import api from './api';

const productCategoryService = {
  // Categorías activas en orden de carta
  getAll: async () => {
    const response = await api.get('/product-categories');
    return response.data;
  }
};

export default productCategoryService;
