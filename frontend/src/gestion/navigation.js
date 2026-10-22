import { lazy } from 'react';
import { BarChart3, Boxes, ChefHat, ClipboardList, ListChecks, PackageCheck, Trash2, Truck, Warehouse } from 'lucide-react';
import { BACK_OFFICE_ROLES, MANAGEMENT_ROLES } from '../utils/constants';

// Única definición de la sección Gestión: la usan las rutas (App) y el menú lateral (Navbar), así que
// una pantalla y su permiso no pueden quedar desalineados. Cada pantalla se carga al entrar.
export const GESTION_BASE = '/admin/gestion';

export const GESTION_SCREENS = [
  { path: 'proveedores', label: 'Proveedores', icon: Truck, roles: MANAGEMENT_ROLES,
    page: lazy(() => import('./pages/SuppliersPage')) },
  { path: 'articulos', label: 'Artículos', icon: Boxes, roles: BACK_OFFICE_ROLES,
    page: lazy(() => import('./pages/StockItemsPage')) },
  { path: 'compras', label: 'Compras', icon: ClipboardList, roles: MANAGEMENT_ROLES,
    page: lazy(() => import('./pages/PurchaseOrdersPage')) },
  { path: 'recepcion', label: 'Recepción', icon: PackageCheck, roles: BACK_OFFICE_ROLES,
    page: lazy(() => import('./pages/GoodsReceiptPage')) },
  { path: 'inventario', label: 'Inventario', icon: Warehouse, roles: BACK_OFFICE_ROLES,
    page: lazy(() => import('./pages/inventory/InventoryPage')) },
  { path: 'recuentos', label: 'Recuentos', icon: ListChecks, roles: BACK_OFFICE_ROLES,
    page: lazy(() => import('./pages/StockCountsPage')) },
  { path: 'escandallos', label: 'Escandallos', icon: ChefHat, roles: BACK_OFFICE_ROLES,
    page: lazy(() => import('./pages/RecipesPage')) },
  { path: 'mermas', label: 'Mermas', icon: Trash2, roles: BACK_OFFICE_ROLES,
    page: lazy(() => import('./pages/WastePage')) },
  { path: 'informes', label: 'Informes', icon: BarChart3, roles: MANAGEMENT_ROLES,
    page: lazy(() => import('./pages/reports/ReportsPage')) }
];

export const gestionScreensFor = (role) => GESTION_SCREENS.filter((screen) => screen.roles.includes(role));
