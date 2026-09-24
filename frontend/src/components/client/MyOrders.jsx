import { useState, useEffect } from 'react';
import { Package, Clock, DollarSign, Calendar, FileText, ChefHat, CheckCircle, PackageCheck, XCircle } from 'lucide-react';
import orderService from '../../services/orderService';
import { ORDER_STATUS_LABELS, PAYMENT_METHOD_LABELS } from '../../utils/constants';

const MyOrders = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    const loadOrders = async () => {
      try {
        const data = await orderService.getMine();
        // Ordenar por fecha más reciente
        data.sort((a, b) => new Date(b.dateOrder) - new Date(a.dateOrder));
        setOrders(data);
      } catch (error) {
        console.error('Error cargando pedidos:', error);
        alert('Error al cargar pedidos');
      } finally {
        setLoading(false);
      }
    };
    loadOrders();
  }, []);
  
  const getStatusBadge = (status) => {
    const styles = {
      CREATED: 'badge-info',
      IN_PREPARATION: 'badge-warning',
      READY: 'badge-success',
      DELIVERED: 'badge-success',
      CANCELLED: 'badge-danger'
    };
    return `badge ${styles[status]}`;
  };
  
  const getStatusIcon = (status) => {
    switch (status) {
      case 'CREATED':
        return <FileText size={24} />;
      case 'IN_PREPARATION':
        return <ChefHat size={24} />;
      case 'READY':
        return <CheckCircle size={24} />;
      case 'DELIVERED':
        return <PackageCheck size={24} />;
      case 'CANCELLED':
        return <XCircle size={24} />;
      default:
        return <Package size={24} />;
    }
  };
  
  if (loading) {
    return (
      <div className="container" style={{ padding: '2rem 0' }}>
        <div className="spinner"></div>
      </div>
    );
  }
  
  return (
    <div className="container" style={{ padding: '2rem 0' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: 'bold', marginBottom: '0.5rem' }}>
          Mis Pedidos
        </h1>
        <p style={{ color: 'var(--text-secondary)' }}>
          {orders.length === 0 ? 'No tienes pedidos aún' : `Total: ${orders.length} pedidos`}
        </p>
      </div>
      
      {orders.length === 0 ? (
        <div className="card" style={{ 
          textAlign: 'center',
          padding: '4rem 2rem'
        }}>
          <Package size={64} style={{ 
            margin: '0 auto 1rem',
            opacity: 0.3,
            color: 'var(--text-secondary)'
          }} />
          <h3 style={{ 
            fontSize: '1.25rem',
            fontWeight: 'bold',
            marginBottom: '0.5rem'
          }}>
            No tienes pedidos
          </h3>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
            ¡Explora nuestro menú y haz tu primer pedido!
          </p>
          <a href="/cliente" className="btn btn-primary">
            Ver Menú
          </a>
        </div>
      ) : (
        <div className="grid" style={{ gap: '1.5rem' }}>
          {orders.map(order => (
            <div key={order.id} className="card">
              {/* Header */}
              <div style={{ 
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start',
                marginBottom: '1.5rem',
                paddingBottom: '1rem',
                borderBottom: '2px solid var(--border-color)'
              }}>
                <div>
                  <div style={{ 
                    display: 'flex',
                    alignItems: 'center',
                    gap: '1rem',
                    marginBottom: '0.5rem'
                  }}>
                    <span style={{ 
                      fontSize: '1.5rem',
                      fontWeight: 'bold',
                      color: 'var(--primary-color)',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.5rem'
                    }}>
                      {getStatusIcon(order.status)} Pedido #{order.id}
                    </span>
                    <span className={getStatusBadge(order.status)}>
                      {ORDER_STATUS_LABELS[order.status]}
                    </span>
                  </div>
                  
                  <div style={{ 
                    fontSize: '0.875rem',
                    color: 'var(--text-secondary)',
                    display: 'flex',
                    gap: '1rem'
                  }}>
                    <span>
                      <Clock size={14} style={{ display: 'inline', marginRight: '0.25rem' }} />
                      {new Date(order.dateOrder).toLocaleString('es-ES')}
                    </span>
                  </div>
                </div>
                
                <div style={{ 
                  fontSize: '2rem',
                  fontWeight: 'bold',
                  color: '#10b981'
                }}>
                  €{order.total.toFixed(2)}
                </div>
              </div>
              
              {/* Productos */}
              <div style={{ marginBottom: '1.5rem' }}>
                <h3 style={{ 
                  fontWeight: 'bold',
                  marginBottom: '1rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem'
                }}>
                  <Package size={20} />
                  Productos:
                </h3>
                
                <div style={{ 
                  backgroundColor: 'var(--light-bg)',
                  borderRadius: '0.5rem',
                  padding: '1rem'
                }}>
                  {order.items.map((item, index) => (
                    <div 
                      key={index}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        padding: '0.5rem 0',
                        borderBottom: index < order.items.length - 1 ? '1px solid var(--border-color)' : 'none'
                      }}
                    >
                      <span>
                        <strong>{item.quantity}x</strong> {item.productName}
                      </span>
                      <span style={{ fontWeight: 'bold' }}>
                        €{(item.quantity * item.unitPrice).toFixed(2)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
              
              {/* Hora de recogida */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '1rem'
              }}>
                <div className="card" style={{ 
                  backgroundColor: '#dbeafe',
                  padding: '1rem'
                }}>
                  <div style={{ 
                    fontSize: '0.75rem',
                    color: '#1e40af',
                    marginBottom: '0.25rem',
                    fontWeight: 'bold'
                  }}>
                    HORA DE RECOGIDA
                  </div>
                  <div style={{ 
                    fontSize: '1.125rem',
                    fontWeight: 'bold',
                    color: '#1e40af',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem'
                  }}>
                    <Calendar size={20} />
                    {new Date(order.timeTakeAway).toLocaleString('es-ES', {
                      day: '2-digit',
                      month: '2-digit',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    })}
                  </div>
                </div>
                
                <div className="card" style={{ 
                  backgroundColor: '#d1fae5',
                  padding: '1rem'
                }}>
                  <div style={{ 
                    fontSize: '0.75rem',
                    color: '#065f46',
                    marginBottom: '0.25rem',
                    fontWeight: 'bold'
                  }}>
                    TOTAL
                  </div>
                  <div style={{ 
                    fontSize: '1.5rem',
                    fontWeight: 'bold',
                    color: '#065f46',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem'
                  }}>
                    <DollarSign size={20} />
                    €{order.total.toFixed(2)}
                  </div>
                  <div style={{ fontSize: '0.875rem', color: '#065f46', marginTop: '0.25rem' }}>
                    Pago en recogida: {PAYMENT_METHOD_LABELS[order.paymentMethod]}
                  </div>
                </div>
              </div>
              
              {/* Información de estado */}
              {order.status === 'CREATED' && (
                <div className="card" style={{ 
                  marginTop: '1rem',
                  backgroundColor: '#fef3c7',
                  border: '2px solid #f59e0b'
                }}>
                  <div style={{ 
                    fontSize: '0.875rem',
                    color: '#92400e'
                  }}>
                    <strong>Tu pedido está siendo procesado.</strong> Te avisaremos cuando esté listo.
                  </div>
                </div>
              )}
              
              {order.status === 'IN_PREPARATION' && (
                <div className="card" style={{ 
                  marginTop: '1rem',
                  backgroundColor: '#fef3c7',
                  border: '2px solid #f59e0b'
                }}>
                  <div style={{ 
                    fontSize: '0.875rem',
                    color: '#92400e'
                  }}>
                    <strong>Tu pedido se está preparando.</strong> Pronto estará listo para recoger.
                  </div>
                </div>
              )}
              
              {order.status === 'READY' && (
                <div className="card" style={{ 
                  marginTop: '1rem',
                  backgroundColor: '#d1fae5',
                  border: '2px solid #10b981'
                }}>
                  <div style={{ 
                    fontSize: '0.875rem',
                    color: '#065f46'
                  }}>
                    <strong>¡Tu pedido está listo!</strong> Puedes pasar a recogerlo.
                  </div>
                </div>
              )}
              
              {order.status === 'DELIVERED' && (
                <div className="card" style={{ 
                  marginTop: '1rem',
                  backgroundColor: '#d1fae5',
                  border: '2px solid #10b981'
                }}>
                  <div style={{ 
                    fontSize: '0.875rem',
                    color: '#065f46'
                  }}>
                    <strong>Pedido completado.</strong> ¡Gracias por tu compra!
                  </div>
                </div>
              )}
              
              {order.status === 'CANCELLED' && (
                <div className="card" style={{ 
                  marginTop: '1rem',
                  backgroundColor: '#fee2e2',
                  border: '2px solid #ef4444'
                }}>
                  <div style={{ 
                    fontSize: '0.875rem',
                    color: '#991b1b'
                  }}>
                    <strong>Pedido cancelado.</strong>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default MyOrders;