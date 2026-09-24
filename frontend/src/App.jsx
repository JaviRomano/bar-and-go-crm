import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/auth/ProtectedRoute';
import Navbar from './components/common/Navbar';
import Footer from './components/common/Footer';
import Login from './components/auth/Login';
import HomePage from './pages/HomePage';
import AdminDashboard from './pages/AdminDashboard';
import NotFound from './pages/NotFound';

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
            backgroundColor: 'var(--light-bg)'
          }}>
            <Routes>
              {/* Ruta pública */}
              <Route path="/login" element={<Login />} />

              {/* Ruta home */}
              <Route path="/" element={<HomePage />} />

              {/* Rutas Admin */}
              <Route
                path="/admin"
                element={
                  <ProtectedRoute requiredRole="ADMIN">
                    <AdminDashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/products"
                element={
                  <ProtectedRoute requiredRole="ADMIN">
                    <ProductList />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/reservations"
                element={
                  <ProtectedRoute requiredRole="ADMIN">
                    <ReservationList />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/orders"
                element={
                  <ProtectedRoute requiredRole="ADMIN">
                    <OrderList />
                  </ProtectedRoute>
                }
              />

              {/* Rutas Cliente */}
              <Route
                path="/cliente"
                element={
                  <ProtectedRoute requiredRole="CUSTOMER">
                    <Menu />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/cliente/reservations/new"
                element={
                  <ProtectedRoute requiredRole="CUSTOMER">
                    <ReservationForm />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/cliente/reservations"
                element={
                  <ProtectedRoute requiredRole="CUSTOMER">
                    <MyReservations />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/cliente/orders"
                element={
                  <ProtectedRoute requiredRole="CUSTOMER">
                    <MyOrders />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/cliente/checkout"
                element={
                  <ProtectedRoute requiredRole="CUSTOMER">
                    <Checkout />
                  </ProtectedRoute>
                }
              />

              {/* 404 */}
              <Route path="*" element={<NotFound />} />
            </Routes>
          </main>

          <Footer />
        </div>
      </AuthProvider>
    </Router>
  );
}

export default App;