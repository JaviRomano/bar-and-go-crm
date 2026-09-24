const Footer = () => {
  return (
    <footer style={{
      backgroundColor: 'var(--dark-bg)',
      color: 'white',
      padding: '2rem 0',
      marginTop: 'auto'
    }}>
      <div className="container">
        <div className="grid grid-3" style={{ marginBottom: '2rem' }}>
          <div>
            <h3 style={{ marginBottom: '1rem' }}>Bar&Go</h3>
            <p style={{ color: 'rgba(255,255,255,0.7)', fontSize: '0.875rem' }}>
              Reserva tu mesa o pide para recoger en tu bar favorito.
            </p>
          </div>
          
          <div>
            <h4 style={{ marginBottom: '1rem' }}>Horario</h4>
            <p style={{ color: 'rgba(255,255,255,0.7)', fontSize: '0.875rem' }}>
              Lunes - Sábado: 12:00 - 16:00 y 20:00 - 23:00<br />
              Martes: Cerrado
            </p>
          </div>
          
          <div>
            <h4 style={{ marginBottom: '1rem' }}>Contacto</h4>
            <p style={{ color: 'rgba(255,255,255,0.7)', fontSize: '0.875rem' }}>
              Email: info@bargo.com<br />
              Tel: +34 952 345 68<br />
              Calle 13 de la carpa, Sevilla
            </p>
          </div>
        </div>
        
        <div style={{
          paddingTop: '1.5rem',
          borderTop: '1px solid rgba(255,255,255,0.2)',
          textAlign: 'center',
          color: 'rgba(255,255,255,0.7)',
          fontSize: '0.875rem'
        }}>
          <p>&copy; 2026 Bar&Go. Todos los derechos reservados.</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;