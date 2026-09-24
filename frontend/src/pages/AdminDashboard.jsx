import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  Package, 
  Calendar, 
  ShoppingCart, 
  TrendingUp,
  Users,
  DollarSign
} from 'lucide-react';
import productService from '../services/productService';
import reservationService from '../services/reservationService';
import orderService from '../services/orderService';
import { todayString } from '../utils/dates';

const AdminDashboard = () => {
  const [stats, setStats] = useState({
    totalProducts: 0,
    totalReservations: 0,
    totalOrders: 0,
    todayReservations: 0
  });
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    loadStats();
  }, []);
  
  const loadStats = async () => {
    try {
      const [products, reservations, orders] = await Promise.all([
        productService.getAll(),
        reservationService.getAll(),
        orderService.getAll()
      ]);
      
      const today = todayString();
      const todayReservations = reservations.filter(r => 
        r.date === today && r.status !== 'CANCELLED'
      ).length;
      
      setStats({
        totalProducts: products.length,
        totalReservations: reservations.length,
        totalOrders: orders.length,
        todayReservations
      });
    } catch (error) {
      console.error('Error cargando estadísticas:', error);
    } finally {
      setLoading(false);
    }
  };
  
  if (loading) {
    return (
      <div className="container" style={{ padding: '2rem 0' }}>
        <div className="spinner"></div>
      </div>
    );
  }
  
  const statCards = [
    {
      title: 'Total Productos',
      value: stats.totalProducts,
      icon: <Package size={32} />,
      color: '#2563eb',
      link: '/admin/products'
    },
    {
      title: 'Reservas Hoy',
      value: stats.todayReservations,
      icon: <Calendar size={32} />,
      color: '#10b981',
      link: '/admin/reservations'
    },
    {
      title: 'Total Pedidos',
      value: stats.totalOrders,
      icon: <ShoppingCart size={32} />,
      color: '#f59e0b',
      link: '/admin/orders'
    },
    {
      title: 'Total Reservas',
      value: stats.totalReservations,
      icon: <Users size={32} />,
      color: '#8b5cf6',
      link: '/admin/reservations'
    }
  ];
  
  return (
    <div className="container" style={{ padding: '2rem 0' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: 'bold', marginBottom: '0.5rem' }}>
          Panel de Administración
        </h1>
        <p style={{ color: 'var(--text-secondary)' }}>
          Resumen general del sistema Bar&Go
        </p>
      </div>
      
      <div className="grid grid-4" style={{ marginBottom: '3rem' }}>
        {statCards.map((stat, index) => (
          <Link
            key={index}
            to={stat.link}
            style={{ textDecoration: 'none' }}
          >
            <div 
              className="card"
              style={{
                cursor: 'pointer',
                transition: 'transform 0.2s, box-shadow 0.2s',
                borderLeft: `4px solid ${stat.color}`
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-4px)';
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.1)';
              }}
            >
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start'
              }}>
                <div>
                  <p style={{ 
                    color: 'var(--text-secondary)', 
                    fontSize: '0.875rem',
                    marginBottom: '0.5rem'
                  }}>
                    {stat.title}
                  </p>
                  <p style={{ 
                    fontSize: '2rem', 
                    fontWeight: 'bold',
                    color: stat.color
                  }}>
                    {stat.value}
                  </p>
                </div>
                <div style={{ color: stat.color, opacity: 0.5 }}>
                  {stat.icon}
                </div>
              </div>
            </div>
          </Link>
        ))}
      </div>
      
      <div className="grid grid-2">
        <div className="card">
          <h2 style={{ 
            fontSize: '1.25rem', 
            fontWeight: 'bold',
            marginBottom: '1.5rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem'
          }}>
            <TrendingUp size={24} color="#2563eb" />
            Acciones Rápidas
          </h2>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <Link to="/admin/products" className="btn btn-primary" style={{ width: '100%' }}>
              <Package size={20} />
              Gestionar Productos
            </Link>
            <Link to="/admin/reservations" className="btn btn-secondary" style={{ width: '100%' }}>
              <Calendar size={20} />
              Ver Reservas
            </Link>
            <Link to="/admin/orders" className="btn btn-outline" style={{ width: '100%' }}>
              <ShoppingCart size={20} />
              Ver Pedidos
            </Link>
          </div>
        </div>
        
        <div className="card">
          <h2 style={{ 
            fontSize: '1.25rem', 
            fontWeight: 'bold',
            marginBottom: '1.5rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem'
          }}>
            <DollarSign size={24} color="#10b981" />
            Información del Sistema
          </h2>
          
          <div style={{ 
            display: 'flex', 
            flexDirection: 'column', 
            gap: '1rem',
            fontSize: '0.875rem'
          }}>
            <div style={{ 
              display: 'flex', 
              justifyContent: 'space-between',
              paddingBottom: '0.75rem',
              borderBottom: '1px solid var(--border-color)'
            }}>
              <span style={{ color: 'var(--text-secondary)' }}>Productos Activos:</span>
              <span style={{ fontWeight: 'bold' }}>{stats.totalProducts}</span>
            </div>
            
            <div style={{ 
              display: 'flex', 
              justifyContent: 'space-between',
              paddingBottom: '0.75rem',
              borderBottom: '1px solid var(--border-color)'
            }}>
              <span style={{ color: 'var(--text-secondary)' }}>Reservas Totales:</span>
              <span style={{ fontWeight: 'bold' }}>{stats.totalReservations}</span>
            </div>
            
            <div style={{ 
              display: 'flex', 
              justifyContent: 'space-between',
              paddingBottom: '0.75rem',
              borderBottom: '1px solid var(--border-color)'
            }}>
              <span style={{ color: 'var(--text-secondary)' }}>Pedidos Totales:</span>
              <span style={{ fontWeight: 'bold' }}>{stats.totalOrders}</span>
            </div>
            
            <div style={{ 
              display: 'flex', 
              justifyContent: 'space-between'
            }}>
              <span style={{ color: 'var(--text-secondary)' }}>Reservas Hoy:</span>
              <span style={{ 
                fontWeight: 'bold',
                color: stats.todayReservations > 0 ? '#10b981' : '#6b7280'
              }}>
                {stats.todayReservations}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;