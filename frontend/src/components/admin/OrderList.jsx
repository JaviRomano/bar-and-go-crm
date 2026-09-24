import { useState, useEffect } from 'react';
import { Clock, User, Package, DollarSign } from 'lucide-react';
import orderService from '../../services/orderService';
import { ORDER_STATUS, ORDER_STATUS_LABELS, PAYMENT_METHOD_LABELS } from '../../utils/constants';

const OrderList = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('TODOS');
  
  useEffect(() => {
    loadOrders();
  }, []);
  
  const loadOrders = async () => {
    try {
      const data = await orderService.getAll();
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
  
  const handleUpdateStatus = async (id, newStatus) => {
    try {
      await orderService.updateStatus(id, newStatus);
      await loadOrders();
    } catch (error) {
      console.error('Error actualizando estado:', error);
      alert('Error al actualizar estado');
    }
  };
  
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
  
  const getNextStatus = (currentStatus) => {
    const flow = {
      CREATED: ORDER_STATUS.IN_PREPARATION,
      IN_PREPARATION: ORDER_STATUS.READY,
      READY: ORDER_STATUS.DELIVERED
    };
    return flow[currentStatus];
  };
  
  const getNextStatusLabel = (currentStatus) => {
    const nextStatus = getNextStatus(currentStatus);
    return nextStatus ? ORDER_STATUS_LABELS[nextStatus] : null;
  };
  
  const filteredOrders = filter === 'TODOS'
    ? orders
    : orders.filter(o => o.status === filter);
  
  const statusFilters = ['TODOS', ...Object.keys(ORDER_STATUS)];
  
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
          Gestión de Pedidos
        </h1>
        <p style={{ color: 'var(--text-secondary)' }}>
          Total: {orders.length} pedidos
        </p>
      </div>
      
      {/* Filtros */}
      <div style={{ 
        display: 'flex',
        gap: '0.5rem',
        marginBottom: '2rem',
        flexWrap: 'wrap'
      }}>
        {statusFilters.map(status => (
          <button
            key={status}
            onClick={() => setFilter(status)}
            className={filter === status ? 'btn btn-primary' : 'btn btn-outline'}
            style={{ fontSize: '0.875rem' }}
          >
            {status === 'TODOS' ? 'Todos' : ORDER_STATUS_LABELS[status]}
          </button>
        ))}
      </div>
      
      {/* Lista de pedidos */}
      <div className="grid" style={{ gap: '1rem' }}>
        {filteredOrders.map(order => (
          <div key={order.id} className="card">
            <div style={{ 
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-start',
              marginBottom: '1rem'
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
                    color: 'var(--primary-color)'
                  }}>
                    Pedido #{order.id}
                  </span>
                  <span className={getStatusBadge(order.status)}>
                    {ORDER_STATUS_LABELS[order.status]}
                  </span>
                </div>
                
                <div style={{ 
                  display: 'flex',
                  gap: '1.5rem',
                  fontSize: '0.875rem',
                  color: 'var(--text-secondary)'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <User size={16} />
                    {order.userName}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Clock size={16} />
                    {new Date(order.dateOrder).toLocaleString('es-ES')}
                  </div>
                </div>
              </div>
              
              <div style={{ 
                fontSize: '1.5rem',
                fontWeight: 'bold',
                color: '#10b981'
              }}>
                €{order.total.toFixed(2)}
              </div>
            </div>
            
            {/* Items del pedido */}
            <div style={{ 
              backgroundColor: 'var(--light-bg)',
              borderRadius: '0.5rem',
              padding: '1rem',
              marginBottom: '1rem'
            }}>
              <div style={{ 
                fontWeight: 'bold',
                marginBottom: '0.75rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem'
              }}>
                <Package size={16} />
                Productos:
              </div>
              
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
                    {item.quantity}x {item.productName}
                  </span>
                  <span style={{ fontWeight: 'bold' }}>
                    €{(item.quantity * item.unitPrice).toFixed(2)}
                  </span>
                </div>
              ))}
            </div>
            
            {/* Hora de recogida */}
            <div style={{ 
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              marginBottom: '1rem',
              padding: '0.75rem',
              backgroundColor: '#dbeafe',
              borderRadius: '0.5rem'
            }}>
              <Clock size={20} color="#2563eb" />
              <span>
                <strong>Hora de recogida:</strong>{' '}
                {new Date(order.timeTakeAway).toLocaleString('es-ES')}
                {' · '}
                <strong>Pago:</strong> {PAYMENT_METHOD_LABELS[order.paymentMethod]}
              </span>
            </div>
            
            {/* Acciones */}
            {order.status !== 'DELIVERED' && order.status !== 'CANCELLED' && (
              <div style={{ 
                display: 'flex',
                gap: '0.5rem',
                paddingTop: '1rem',
                borderTop: '1px solid var(--border-color)'
              }}>
                {getNextStatus(order.status) && (
                  <button
                    onClick={() => handleUpdateStatus(order.id, getNextStatus(order.status))}
                    className="btn btn-primary"
                    style={{ flex: 1 }}
                  >
                    Marcar como {getNextStatusLabel(order.status)}
                  </button>
                )}
                
                <button
                  onClick={() => handleUpdateStatus(order.id, ORDER_STATUS.CANCELLED)}
                  className="btn btn-danger"
                  style={{ flex: 1 }}
                >
                  Cancelar Pedido
                </button>
              </div>
            )}
          </div>
        ))}
      </div>
      
      {filteredOrders.length === 0 && (
        <div style={{ 
          textAlign: 'center',
          padding: '3rem',
          color: 'var(--text-secondary)'
        }}>
          No hay pedidos con este estado
        </div>
      )}
    </div>
  );
};

export default OrderList;