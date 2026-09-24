import { Link } from 'react-router-dom';
import { Home } from 'lucide-react';

const NotFound = () => {
  return (
    <div className="container" style={{ 
      padding: '4rem 0',
      textAlign: 'center',
      minHeight: '60vh',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      alignItems: 'center'
    }}>
      <h1 style={{ 
        fontSize: '6rem', 
        fontWeight: 'bold',
        color: 'var(--primary-color)',
        marginBottom: '1rem'
      }}>
        404
      </h1>
      
      <h2 style={{ 
        fontSize: '2rem', 
        fontWeight: 'bold',
        marginBottom: '1rem'
      }}>
        Página No Encontrada
      </h2>
      
      <p style={{ 
        color: 'var(--text-secondary)',
        marginBottom: '2rem',
        fontSize: '1.125rem'
      }}>
        Lo sentimos, la página que buscas no existe.
      </p>
      
      <Link to="/" className="btn btn-primary">
        <Home size={20} />
        Volver al Inicio
      </Link>
    </div>
  );
};

export default NotFound;