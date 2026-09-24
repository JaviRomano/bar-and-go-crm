import api from './api';
import { clearSession, getSession, saveSession } from './session';

const authService = {
  login: async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    return saveSession(response.data).user;
  },

  logout: () => {
    clearSession();
  },

  getCurrentUser: () => getSession()?.user ?? null
};

export default authService;
