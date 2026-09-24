import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/useAuth';

const ProtectedRoute = ({ children, requiredRole }) => {
  const { user } = useAuth();
  
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  
  if (requiredRole && user.role !== requiredRole) {
    // Si el usuario no tiene el rol requerido, redirigir a su dashboard
    return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/cliente'} replace />;
  }
  
  return children;
};

export default ProtectedRoute;