import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/useAuth';
import { LogIn, User, Lock } from 'lucide-react';
import { SHOW_DEMO_LOGIN, USER_ROLES } from '../../utils/constants';

const DEMO_CREDENTIALS = {
  ADMIN: { email: 'admin@bar.com', password: 'admin123' },
  CUSTOMER: { email: 'ripleynostromo@example.com', password: 'cliente123' }
};

// Mensaje del backend si lo hay (p. ej. credenciales incorrectas); si no, el de red
const getErrorMessage = (err) =>
  err.response?.data?.message || (err.response ? 'Error al iniciar sesión' : 'No se puede conectar con el servidor');

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const { login } = useAuth();
  const navigate = useNavigate();
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    try {
      const user = await login(email, password);
      redirectByRole(user);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };
  
  const redirectByRole = (user) => {
    navigate(user.role === USER_ROLES.ADMIN ? '/admin' : '/cliente');
  };
  
  // Acceso rápido con las credenciales de demo
  const quickLogin = async (role) => {
    const credentials = DEMO_CREDENTIALS[role];
    setEmail(credentials.email);
    setPassword(credentials.password);
    setError('');
    
    try {
      redirectByRole(await login(credentials.email, credentials.password));
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };
  
  return (
    <div className="login-page" style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    }}>
      <div className="card" style={{ maxWidth: '400px', width: '100%', margin: '1rem' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <h1 style={{ fontSize: '2rem', fontWeight: 'bold', marginBottom: '0.5rem' }}>
            Bar&Go
          </h1>
          <p style={{ color: 'var(--text-secondary)' }}>
            Inicia sesión para continuar
          </p>
        </div>
        
        {error && (
          <div style={{
            padding: '0.75rem',
            backgroundColor: '#fee2e2',
            color: '#991b1b',
            borderRadius: '0.5rem',
            marginBottom: '1rem'
          }}>
            {error}
          </div>
        )}
        
        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <label>
              <User size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="tu@email.com"
              required
            />
          </div>
          
          <div className="input-group">
            <label>
              <Lock size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
              Contraseña
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />
          </div>
          
          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '1rem' }}
            disabled={loading}
          >
            <LogIn size={20} />
            {loading ? 'Iniciando sesión...' : 'Iniciar Sesión'}
          </button>
        </form>
        
        {SHOW_DEMO_LOGIN && (
          <div style={{ marginTop: '2rem', paddingTop: '1.5rem', borderTop: '1px solid var(--border-color)' }}>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
              Acceso rápido (cuentas de demo):
            </p>
            <div className="grid grid-2" style={{ gap: '0.5rem' }}>
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => quickLogin(USER_ROLES.ADMIN)}
                style={{ fontSize: '0.875rem' }}
              >
                Admin
              </button>
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => quickLogin(USER_ROLES.CUSTOMER)}
                style={{ fontSize: '0.875rem' }}
              >
                Cliente
              </button>
            </div>
            <div style={{ marginTop: '1rem', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
              <p><strong>Admin:</strong> admin@bar.com / admin123</p>
              <p><strong>Cliente:</strong> ripleynostromo@example.com / cliente123</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Login;