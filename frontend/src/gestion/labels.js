// Etiquetas visibles de los enums del backend (que van en inglés).

export const ALLERGENS = {
  GLUTEN: 'Gluten', CRUSTACEANS: 'Crustáceos', EGGS: 'Huevos', FISH: 'Pescado', PEANUTS: 'Cacahuetes',
  SOYBEANS: 'Soja', MILK: 'Lácteos', TREE_NUTS: 'Frutos de cáscara', CELERY: 'Apio', MUSTARD: 'Mostaza',
  SESAME: 'Sésamo', SULPHITES: 'Sulfitos', LUPIN: 'Altramuces', MOLLUSCS: 'Moluscos'
};

export const WAREHOUSE_TYPES = {
  COLD_ROOM: 'Cámara', DRY_STORE: 'Almacén de secos', BAR: 'Barra', FREEZER: 'Congelador', KITCHEN: 'Cocina'
};

export const MOVEMENT_TYPES = {
  PURCHASE_RECEIPT: 'Compra', SALE: 'Venta', WASTE: 'Merma', ADJUSTMENT: 'Ajuste', TRANSFER: 'Traspaso',
  PRODUCTION_CONSUMPTION: 'Consumo producción', PRODUCTION_OUTPUT: 'Alta producción'
};

export const MOVEMENT_TONES = {
  PURCHASE_RECEIPT: 'ok', SALE: 'info', WASTE: 'danger', ADJUSTMENT: 'warn', TRANSFER: 'neutral',
  PRODUCTION_CONSUMPTION: 'neutral', PRODUCTION_OUTPUT: 'accent'
};

export const WASTE_REASONS = {
  EXPIRED: 'Caducidad', BREAKAGE: 'Rotura', PREPARATION: 'Elaboración', CUSTOMER_RETURN: 'Devolución cliente',
  COMPLIMENTARY: 'Invitación', STAFF_MEAL: 'Consumo personal', ORDER_ERROR: 'Error de comanda', OTHER: 'Otro'
};

export const PURCHASE_ORDER_STATUS = {
  DRAFT: 'Borrador', SENT: 'Enviado', PARTIALLY_RECEIVED: 'Recibido en parte', RECEIVED: 'Recibido',
  CANCELLED: 'Cancelado'
};

export const PURCHASE_ORDER_TONES = {
  DRAFT: 'neutral', SENT: 'info', PARTIALLY_RECEIVED: 'warn', RECEIVED: 'ok', CANCELLED: 'danger'
};

export const COUNT_STATUS = { OPEN: 'Abierto', IN_PROGRESS: 'En curso', CLOSED: 'Cerrado' };
export const COUNT_TONES = { OPEN: 'info', IN_PROGRESS: 'warn', CLOSED: 'neutral' };

export const RECIPE_TYPES = { DISH: 'Plato', PREPARATION: 'Elaboración' };

export const MENU_CLASSES = {
  STAR: { label: 'Estrella', hint: 'Popular y rentable: mantener' },
  PLOWHORSE: { label: 'Vaca', hint: 'Popular, poco margen: revisar precio o escandallo' },
  PUZZLE: { label: 'Puzzle', hint: 'Buen margen, poco vendido: darle visibilidad' },
  DOG: { label: 'Perro', hint: 'Ni lo uno ni lo otro: candidato a salir de la carta' }
};

export const toOptions = (labels) => Object.entries(labels).map(([value, label]) => ({ value, label }));
