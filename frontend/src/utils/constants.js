export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Accesos rápidos con las credenciales de demo (solo existen con el perfil dev del backend)
export const SHOW_DEMO_LOGIN = import.meta.env.DEV || import.meta.env.VITE_SHOW_DEMO_LOGIN === 'true';

export const USER_ROLES = {
  ADMIN: 'ADMIN',
  CUSTOMER: 'CUSTOMER'
};

export const USER_ROLE_LABELS = {
  ADMIN: 'Admin',
  CUSTOMER: 'Cliente'
};

export const ORDER_STATUS = {
  CREATED: 'CREATED',
  IN_PREPARATION: 'IN_PREPARATION',
  READY: 'READY',
  DELIVERED: 'DELIVERED',
  CANCELLED: 'CANCELLED'
};

export const RESERVATION_STATUS = {
  PENDING: 'PENDING',
  CONFIRMED: 'CONFIRMED',
  CANCELLED: 'CANCELLED'
};

export const PRODUCT_CATEGORIES = {
  ENTRANTES: 'ENTRANTES',
  PRINCIPALES: 'PRINCIPALES',
  POSTRES: 'POSTRES',
  BEBIDAS: 'BEBIDAS'
};

export const PAYMENT_METHOD_LABELS = {
  CARD: 'Tarjeta',
  CASH: 'Efectivo'
};

export const ORDER_STATUS_LABELS = {
  CREATED: 'Creado',
  IN_PREPARATION: 'En Preparación',
  READY: 'Listo',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado'
};

export const RESERVATION_STATUS_LABELS = {
  PENDING: 'Pendiente',
  CONFIRMED: 'Confirmada',
  CANCELLED: 'Cancelada'
};
