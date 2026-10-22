export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Accesos rápidos con las credenciales de demo (solo existen con el perfil dev del backend)
export const SHOW_DEMO_LOGIN = import.meta.env.DEV || import.meta.env.VITE_SHOW_DEMO_LOGIN === 'true';

export const USER_ROLES = {
  ADMIN: 'ADMIN',
  MANAGER: 'MANAGER',
  STAFF: 'STAFF',
  CUSTOMER: 'CUSTOMER'
};

export const USER_ROLE_LABELS = {
  ADMIN: 'Admin',
  MANAGER: 'Encargado',
  STAFF: 'Personal',
  CUSTOMER: 'Cliente'
};

// Personal del local: entra al panel de gestión
export const BACK_OFFICE_ROLES = [USER_ROLES.ADMIN, USER_ROLES.MANAGER, USER_ROLES.STAFF];

// Mantienen la carta y pueden borrar pedidos y reservas
export const MANAGEMENT_ROLES = [USER_ROLES.ADMIN, USER_ROLES.MANAGER];

export const homePathFor = (role) => (BACK_OFFICE_ROLES.includes(role) ? '/admin' : '/cliente');

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
