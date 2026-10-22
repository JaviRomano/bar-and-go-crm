import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/useAuth';
import { homePathFor } from '../../utils/constants';

const ProtectedRoute = ({ children, allowedRoles }) => {
  const { user } = useAuth();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    // Sin permiso para esta ruta: a la página de inicio de su rol
    return <Navigate to={homePathFor(user.role)} replace />;
  }

  return children;
};

export default ProtectedRoute;
