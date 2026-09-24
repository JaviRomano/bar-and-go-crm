import api from './api';

const reservationService = {
  getAll: async () => {
    const response = await api.get('/reservations');
    return response.data;
  },
  
  // Reservas del usuario autenticado (el backend las asocia por teléfono)
  getMine: async () => {
    const response = await api.get('/reservations/me');
    return response.data;
  },
  
  getById: async (id) => {
    const response = await api.get(`/reservations/${id}`);
    return response.data;
  },
  
  create: async (reservationData) => {
    const response = await api.post('/reservations', reservationData);
    return response.data;
  },
  
  update: async (id, reservationData) => {
    const response = await api.put(`/reservations/${id}`, reservationData);
    return response.data;
  },
  
  cancel: async (id) => {
    const response = await api.patch(`/reservations/${id}/cancel`);
    return response.data;
  },
  
  delete: async (id) => {
    await api.delete(`/reservations/${id}`);
  },
  
  getByDate: async (date) => {
    const response = await api.get(`/reservations/date/${date}`);
    return response.data;
  },
  
  getByStatus: async (status) => {
    const response = await api.get(`/reservations/status/${status}`);
    return response.data;
  }
};

export default reservationService;