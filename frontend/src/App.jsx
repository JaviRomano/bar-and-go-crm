import { lazy, Suspense } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/auth/ProtectedRoute';
import Navbar from './components/common/Navbar';
import Footer from './components/common/Footer';
import Login from './components/auth/Login';
import HomePage from './pages/HomePage';
import AdminDashboard from './pages/AdminDashboard';
import NotFound from './pages/NotFound';
import { BACK_OFFICE_ROLES, MANAGEMENT_ROLES, USER_ROLES } from './utils/constants';

// Componentes Admin
import ProductList from './components/admin/ProductList';
import ReservationList from './components/admin/ReservationList';
import OrderList from './components/admin/OrderList';

// Componentes Cliente
import Menu from './components/client/Menu';
import ReservationForm from './components/client/ReservationForm';
import MyReservations from './components/client/MyReservations';
import MyOrders from './components/client/MyOrders';
import Checkout from './components/client/Checkout';

// Gestión (back-office ERP): se carga al entrar, no forma parte del bundle inicial
const SuppliersPage = lazy(() => import('./gestion/pages/SuppliersPage'));
const StockItemsPage = lazy(() => import('./gestion/pages/StockItemsPage'));
const PurchaseOrdersPage = lazy(() => import('./gestion/pages/PurchaseOrdersPage'));
const GoodsReceiptPage = lazy(() => import('./gestion/pages/GoodsReceiptPage'));
const InventoryPage = lazy(() => import('./gestion/pages/InventoryPage'));
const StockCountsPage = lazy(() => import('./gestion/pages/StockCountsPage'));
const RecipesPage = lazy(() => import('./gestion/pages/RecipesPage'));
const WastePage = lazy(() => import('./gestion/pages/WastePage'));
const ReportsPage = lazy(() => import('./gestion/pages/ReportsPage'));

const GESTION_ROUTES = [
  { path: 'proveedores', element: <SuppliersPage />, roles: MANAGEMENT_ROLES },
  { path: 'articulos', element: <StockItemsPage />, roles: BACK_OFFICE_ROLES },
  { path: 'compras', element: <PurchaseOrdersPage />, roles: MANAGEMENT_ROLES },
  { path: 'recepcion', element: <GoodsReceiptPage />, roles: BACK_OFFICE_ROLES },
  { path: 'inventario', element: <InventoryPage />, roles: BACK_OFFICE_ROLES },
  { path: 'recuentos', element: <StockCountsPage />, roles: BACK_OFFICE_ROLES },
  { path: 'escandallos', element: <RecipesPage />, roles: BACK_OFFICE_ROLES },
  { path: 'mermas', element: <WastePage />, roles: BACK_OFFICE_ROLES },
  { path: 'informes', element: <ReportsPage />, roles: MANAGEMENT_ROLES }
];

function App() {
  return (
    <Router>
      <AuthProvider>
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          minHeight: '100vh'
        }}>
          <Navbar />

          <main style={{
            flex: 1,
            backgroundColor: 'var(--bg)'
          }}>
            <Suspense fallback={<div className="spinner" role="status" aria-label="Cargando" />}>
            <Routes>
              {/* Ruta pública */}
              <Route path="/login" element={<Login />} />

              {/* Ruta home */}
              <Route path="/" element={<HomePage />} />

              {/* Rutas Admin */}
              <Route
                path="/admin"
                element={
                  <ProtectedRoute allowedRoles={BACK_OFFICE_ROLES}>
                    <AdminDashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/products"
                element={
                  <ProtectedRoute allowedRoles={MANAGEMENT_ROLES}>
                    <ProductList />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/reservations"
                element={
                  <ProtectedRoute allowedRoles={BACK_OFFICE_ROLES}>
                    <ReservationList />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/orders"
                element={
                  <ProtectedRoute allowedRoles={BACK_OFFICE_ROLES}>
                    <OrderList />
                  </ProtectedRoute>
                }
              />

              {GESTION_ROUTES.map(({ path, element, roles }) => (
                <Route
                  key={path}
                  path={`/admin/gestion/${path}`}
                  element={<ProtectedRoute allowedRoles={roles}>{element}</ProtectedRoute>}
                />
              ))}

              {/* Rutas Cliente */}
              <Route
                path="/cliente"
                element={
                  <ProtectedRoute allowedRoles={[USER_ROLES.CUSTOMER]}>
                    <Menu />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/cliente/reservations/new"
                element={
                  <ProtectedRoute allowedRoles={[USER_ROLES.CUSTOMER]}>
                    <ReservationForm />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/cliente/reservations"
                element={
                  <ProtectedRoute allowedRoles={[USER_ROLES.CUSTOMER]}>
                    <MyReservations />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/cliente/orders"
                element={
                  <ProtectedRoute allowedRoles={[USER_ROLES.CUSTOMER]}>
                    <MyOrders />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/cliente/checkout"
                element={
                  <ProtectedRoute allowedRoles={[USER_ROLES.CUSTOMER]}>
                    <Checkout />
                  </ProtectedRoute>
                }
              />

              {/* 404 */}
              <Route path="*" element={<NotFound />} />
            </Routes>
            </Suspense>
          </main>

          <Footer />
        </div>
      </AuthProvider>
    </Router>
  );
}

export default App;