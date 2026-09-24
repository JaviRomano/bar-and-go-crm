import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/useAuth';
import { USER_ROLE_LABELS } from '../../utils/constants';
import { 
  LogOut, 
  User, 
  ShoppingCart, 
  Calendar, 
  LayoutDashboard,
  UtensilsCrossed,
  Package,
  Menu,
  X
} from 'lucide-react';

const Navbar = () => {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  
  const handleLogout = () => {
    logout();
    navigate('/login');
  };
  
  const closeSidebar = () => {
    setSidebarOpen(false);
  };
  
  if (!user) return null;
  
  // Menú según rol
  const menuItems = isAdmin() ? [
    { path: '/admin', icon: <LayoutDashboard size={20} />, label: 'Dashboard' },
    { path: '/admin/products', icon: <Package size={20} />, label: 'Productos' },
    { path: '/admin/reservations', icon: <Calendar size={20} />, label: 'Reservas' },
    { path: '/admin/orders', icon: <ShoppingCart size={20} />, label: 'Pedidos' }
  ] : [
    { path: '/cliente', icon: <UtensilsCrossed size={20} />, label: 'Menú' },
    { path: '/cliente/reservations/new', icon: <Calendar size={20} />, label: 'Reservar' },
    { path: '/cliente/reservations', icon: <Calendar size={20} />, label: 'Mis Reservas' },
    { path: '/cliente/orders', icon: <ShoppingCart size={20} />, label: 'Mis Pedidos' }
  ];
  
  return (
    <>
      {/* Top Bar */}
      <nav style={{
        backgroundColor: 'var(--dark-bg)',
        color: 'white',
        padding: '1rem 0',
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
        position: 'sticky',
        top: 0,
        zIndex: 100
      }}>
        <div className="container" style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          {/* Hamburger Button */}
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'white',
              cursor: 'pointer',
              padding: '0.5rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              fontSize: '1rem'
            }}
          >
            <Menu size={24} />
            <span style={{ fontWeight: '500' }}>Menú</span>
          </button>
          
          {/* Logo */}
          <Link 
            to={isAdmin() ? '/admin' : '/cliente'} 
            style={{ 
              textDecoration: 'none', 
              color: 'white',
              fontSize: '1.5rem',
              fontWeight: 'bold',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem'
            }}
          >
            <UtensilsCrossed size={28} />
            Bar&Go
          </Link>
          
          {/* User Info */}
          <div style={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: '0.5rem' 
          }}>
            <User size={20} />
            <span style={{ 
              fontSize: '0.875rem',
              display: 'none' 
            }} className="user-name-desktop">
              {user.name}
            </span>
            <span style={{ 
              padding: '0.125rem 0.5rem',
              backgroundColor: isAdmin() ? '#ef4444' : '#10b981',
              borderRadius: '9999px',
              fontSize: '0.75rem',
              fontWeight: 'bold'
            }}>
              {USER_ROLE_LABELS[user.role] ?? user.role}
            </span>
          </div>
        </div>
      </nav>
      
      {/* Overlay */}
      {sidebarOpen && (
        <div
          onClick={closeSidebar}
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            zIndex: 998,
            animation: 'fadeIn 0.2s ease-in-out'
          }}
        />
      )}
      
      {/* Sidebar */}
      <aside style={{
        position: 'fixed',
        top: 0,
        left: sidebarOpen ? 0 : '-280px',
        width: '280px',
        height: '100vh',
        backgroundColor: 'var(--dark-bg)',
        color: 'white',
        zIndex: 999,
        transition: 'left 0.3s ease-in-out',
        display: 'flex',
        flexDirection: 'column',
        boxShadow: '2px 0 8px rgba(0,0,0,0.3)'
      }}>
        {/* Sidebar Header */}
        <div style={{
          padding: '1.5rem',
          borderBottom: '1px solid rgba(255,255,255,0.1)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem'
          }}>
            <UtensilsCrossed size={32} />
            <div>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 'bold' }}>Bar&Go</h2>
              <p style={{ fontSize: '0.75rem', opacity: 0.7 }}>
                {isAdmin() ? 'Panel Admin' : 'Portal Cliente'}
              </p>
            </div>
          </div>
          
          <button
            onClick={closeSidebar}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'white',
              cursor: 'pointer',
              padding: '0.5rem'
            }}
          >
            <X size={24} />
          </button>
        </div>
        
        {/* User Info */}
        <div style={{
          padding: '1.5rem',
          borderBottom: '1px solid rgba(255,255,255,0.1)',
          backgroundColor: 'rgba(255,255,255,0.05)'
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem'
          }}>
            <div style={{
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              backgroundColor: isAdmin() ? '#ef4444' : '#10b981',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '1.5rem',
              fontWeight: 'bold'
            }}>
              {user.name.charAt(0).toUpperCase()}
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 'bold', fontSize: '1rem' }}>
                {user.name}
              </div>
              <div style={{ fontSize: '0.875rem', opacity: 0.7 }}>
                {user.email}
              </div>
            </div>
          </div>
        </div>
        
        {/* Navigation Links */}
        <nav style={{
          flex: 1,
          padding: '1rem 0',
          overflowY: 'auto'
        }}>
          {menuItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              onClick={closeSidebar}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '0.875rem 1.5rem',
                color: 'white',
                textDecoration: 'none',
                transition: 'all 0.2s',
                borderLeft: '3px solid transparent'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.1)';
                e.currentTarget.style.borderLeftColor = isAdmin() ? '#ef4444' : '#10b981';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'transparent';
                e.currentTarget.style.borderLeftColor = 'transparent';
              }}
            >
              {item.icon}
              <span style={{ fontSize: '0.95rem', fontWeight: '500' }}>
                {item.label}
              </span>
            </Link>
          ))}
        </nav>
        
        {/* Logout Button */}
        <div style={{
          padding: '1rem',
          borderTop: '1px solid rgba(255,255,255,0.1)'
        }}>
          <button
            onClick={() => {
              closeSidebar();
              handleLogout();
            }}
            style={{
              width: '100%',
              padding: '0.875rem',
              background: 'rgba(239, 68, 68, 0.2)',
              border: '1px solid #ef4444',
              color: 'white',
              borderRadius: '0.5rem',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
              fontSize: '0.95rem',
              fontWeight: '500',
              transition: 'all 0.2s'
            }}
            onMouseEnter={(e) => {
              e.target.style.backgroundColor = '#ef4444';
            }}
            onMouseLeave={(e) => {
              e.target.style.backgroundColor = 'rgba(239, 68, 68, 0.2)';
            }}
          >
            <LogOut size={20} />
            Cerrar Sesión
          </button>
        </div>
      </aside>
      
      {/* CSS para animaciones */}
      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        
        @media (min-width: 768px) {
          .user-name-desktop {
            display: inline !important;
          }
        }
        
        /* Scrollbar personalizado para sidebar */
        aside nav::-webkit-scrollbar {
          width: 6px;
        }
        
        aside nav::-webkit-scrollbar-track {
          background: rgba(255,255,255,0.05);
        }
        
        aside nav::-webkit-scrollbar-thumb {
          background: rgba(255,255,255,0.2);
          border-radius: 3px;
        }
        
        aside nav::-webkit-scrollbar-thumb:hover {
          background: rgba(255,255,255,0.3);
        }
      `}</style>
    </>
  );
};

export default Navbar;