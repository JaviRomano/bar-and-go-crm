import axios from 'axios';
import { API_BASE_URL } from '../utils/constants';
import { clearSession, getSession } from './session';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

api.interceptors.request.use((config) => {
  const session = getSession();
  if (session) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});

// Token caducado o inválido: se cierra la sesión. Un 401 del propio login
// (credenciales incorrectas) se deja pasar para mostrar el mensaje en el formulario.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginRequest = error.config?.url?.endsWith('/auth/login');
    if (error.response?.status === 401 && !isLoginRequest) {
      clearSession();
      window.location.href = '/login';
      // La página se va a recargar: no se propaga el error para evitar avisos intermedios
      return new Promise(() => {});
    }
    return Promise.reject(error);
  }
);

export default api;
