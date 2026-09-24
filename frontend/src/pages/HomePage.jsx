import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

const HomePage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  
  useEffect(() => {
    // Redirigir automáticamente según el rol
    if (user) {
      if (user.role === 'ADMIN') {
        navigate('/admin');
      } else {
        navigate('/cliente');
      }
    } else {
      navigate('/login');
    }
  }, [user, navigate]);
  
  return (
    <div className="container" style={{ textAlign: 'center', padding: '3rem 0' }}>
      <div className="spinner"></div>
      <p style={{ marginTop: '1rem', color: 'var(--text-secondary)' }}>
        Redirigiendo...
      </p>
    </div>
  );
};

export default HomePage;