// Sesión persistida en localStorage: token de acceso, caducidad y datos del usuario.
const SESSION_KEY = 'session';
const CART_KEY = 'cart';

export const saveSession = ({ accessToken, expiresIn, user }) => {
  const session = {
    token: accessToken,
    expiresAt: Date.now() + expiresIn * 1000,
    user
  };
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
};

export const getSession = () => {
  try {
    const session = JSON.parse(localStorage.getItem(SESSION_KEY));
    if (!session?.token || session.expiresAt <= Date.now()) {
      return null;
    }
    return session;
  } catch {
    return null;
  }
};

// Al cerrar sesión se descarta también el carrito, para que no pase al siguiente usuario
export const clearSession = () => {
  localStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(CART_KEY);
};
