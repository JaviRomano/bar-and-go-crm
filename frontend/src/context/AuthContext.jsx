import { useState } from 'react';
import authService from '../services/authService';
import { AuthContext } from './auth-context';
import { USER_ROLES } from '../utils/constants';

export const AuthProvider = ({ children }) => {
  // Lectura síncrona de la sesión guardada: evita un render intermedio sin usuario
  const [user, setUser] = useState(() => authService.getCurrentUser());
  
  const login = async (email, password) => {
    const userData = await authService.login(email, password);
    setUser(userData);
    return userData;
  };
  
  const logout = () => {
    authService.logout();
    setUser(null);
  };
  
  const isAdmin = () => user?.role === USER_ROLES.ADMIN;
  
  const isCustomer = () => user?.role === USER_ROLES.CUSTOMER;
  
  const value = {
    user,
    login,
    logout,
    isAdmin,
    isCustomer
  };
  
  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
