import { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { ShoppingCart, Calendar, Clock, CreditCard, Banknote, ArrowLeft } from 'lucide-react';
import orderService from '../../services/orderService';
import ConfirmModal from '../common/ConfirmModal';
import { parseLocalDate, todayString } from '../../utils/dates';
import { PAYMENT_METHOD_LABELS } from '../../utils/constants';

const readCart = () => {
  try {
    return JSON.parse(localStorage.getItem('cart')) || [];
  } catch {
    return [];
  }
};

const Checkout = () => {
  const [cart] = useState(readCart);
  const [orderDate, setOrderDate] = useState('');
  const [orderTime, setOrderTime] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});
  const [orderDetails, setOrderDetails] = useState(null);
  const lunchSlots = ['12:00', '12:30', '13:00', '13:30', '14:00', '14:30', '15:00', '15:30', '16:00'];
  const dinnerSlots = ['20:00', '20:30', '21:00', '21:30', '22:00', '22:30', '23:00'];
  const navigate = useNavigate();

  const getCartTotal = () => {
    return cart.reduce((total, item) => total + (item.price * item.quantity), 0);
  };

  const handleTimeSelect = (time) => {
    setOrderTime(time);
    if (errors.orderTime) {
      setErrors(prev => ({ ...prev, orderTime: undefined }));
    }
  };

  const validate = () => {
    const newErrors = {};

    if (!orderDate) {
      newErrors.orderDate = 'La fecha es obligatoria';
    } else {
      const selectedDate = parseLocalDate(orderDate);
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      if (selectedDate < today) {
        newErrors.orderDate = 'La fecha debe ser futura';
      }
    }

    if (!orderTime) {
      newErrors.orderTime = 'La hora es obligatoria';
    } else if (orderDate) {
      const selectedDateTime = new Date(`${orderDate}T${orderTime}`);
      const oneHourFromNow = new Date(Date.now() + 60 * 60 * 1000);

      if (selectedDateTime < oneHourFromNow) {
        newErrors.orderTime = 'El pedido debe ser al menos 1 hora después de ahora';
      }
    }

    if (!paymentMethod) {
      newErrors.paymentMethod = 'Selecciona un método de pago';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validate()) return;

    setLoading(true);

    try {
      // Solo producto y cantidad: nombre y precio los fija el backend a partir de la carta
      const orderData = {
        timeTakeAway: `${orderDate}T${orderTime}:00`,
        paymentMethod,
        items: cart.map(item => ({
          productId: item.id,
          quantity: item.quantity
        }))
      };

      const response = await orderService.create(orderData);

      setOrderDetails({
        id: response.id,
        total: response.total,
        paymentMethod: response.paymentMethod,
        itemCount: response.items.reduce((sum, item) => sum + item.quantity, 0),
        pickupDate: new Date(response.timeTakeAway).toLocaleString('es-ES', {
          weekday: 'long',
          year: 'numeric',
          month: 'long',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        })
      });

      localStorage.removeItem('cart');
    } catch (error) {
      console.error('Error creando pedido:', error);

      if (error.response?.data?.validationErrors && Object.keys(error.response.data.validationErrors).length > 0) {
        setErrors(error.response.data.validationErrors);
      } else {
        alert('Error al crear pedido: ' + (error.response?.data?.message || error.message));
      }
    } finally {
      setLoading(false);
    }
  };

  if (cart.length === 0 && !orderDetails) {
    return <Navigate to="/cliente" replace />;
  }

  return (
    <div className="container" style={{ padding: '2rem 0' }}>
      <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '1rem',
          marginBottom: '2rem'
        }}>
          <button
            onClick={() => navigate('/cliente')}
            className="btn btn-outline"
          >
            <ArrowLeft size={20} />
            Volver al Menú
          </button>

          <div>
            <h1 style={{ fontSize: '2rem', fontWeight: 'bold', marginBottom: '0.5rem' }}>
              Finalizar Pedido
            </h1>
            <p style={{ color: 'var(--text-secondary)' }}>
              Revisa tu pedido y selecciona la hora de recogida
            </p>
          </div>
        </div>

        <div className="grid grid-2" style={{ gap: '2rem', alignItems: 'start' }}>
          {/* Resumen del Pedido */}
          <div className="card">
            <h2 style={{
              fontSize: '1.5rem',
              fontWeight: 'bold',
              marginBottom: '1.5rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem'
            }}>
              <ShoppingCart size={24} color="#2563eb" />
              Resumen del Pedido
            </h2>

            <div style={{ marginBottom: '1.5rem' }}>
              {cart.map((item, index) => (
                <div
                  key={item.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '0.75rem 0',
                    borderBottom: index < cart.length - 1 ? '1px solid var(--border-color)' : 'none'
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 'bold' }}>{item.name}</div>
                    <div style={{
                      fontSize: '0.875rem',
                      color: 'var(--text-secondary)'
                    }}>
                      €{item.price.toFixed(2)} × {item.quantity}
                    </div>
                  </div>
                  <div style={{
                    fontWeight: 'bold',
                    color: '#10b981'
                  }}>
                    €{(item.price * item.quantity).toFixed(2)}
                  </div>
                </div>
              ))}
            </div>

            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              padding: '1.5rem',
              backgroundColor: '#dbeafe',
              borderRadius: '0.5rem',
              marginTop: '1rem'
            }}>
              <span style={{ fontSize: '1.25rem', fontWeight: 'bold', color: '#1e40af' }}>
                Total a Pagar:
              </span>
              <span style={{
                fontSize: '2rem',
                fontWeight: 'bold',
                color: '#1e40af'
              }}>
                €{getCartTotal().toFixed(2)}
              </span>
            </div>

            <div className="card" style={{
              marginTop: '1.5rem',
              backgroundColor: '#fef3c7',
              border: '2px solid #f59e0b'
            }}>
              <div style={{
                fontSize: '0.875rem',
                color: '#92400e'
              }}>
                <strong>Nota:</strong> Los pedidos son para recoger en el local.
                No realizamos envíos a domicilio.
              </div>
            </div>
          </div>

          {/* Formulario de Recogida */}
          <div className="card">
            <h2 style={{
              fontSize: '1.5rem',
              fontWeight: 'bold',
              marginBottom: '1.5rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem'
            }}>
              <Clock size={24} color="#10b981" />
              Hora de Recogida
            </h2>

            <div className="card" style={{
              backgroundColor: '#d1fae5',
              border: '2px solid #10b981',
              marginBottom: '1.5rem'
            }}>
              <div style={{
                fontSize: '0.875rem',
                color: '#065f46'
              }}>
                <strong>Importante:</strong> El pedido debe ser al menos 1 hora después de ahora.
                Horarios disponibles: 12:00-16:00 y 20:00-23:00 (Martes cerrado).
              </div>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="input-group">
                <label>
                  <Calendar size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                  Fecha de Recogida *
                </label>
                <input
                  type="date"
                  value={orderDate}
                  onChange={(e) => {
                    setOrderDate(e.target.value);
                    if (errors.orderDate) {
                      setErrors(prev => ({ ...prev, orderDate: undefined }));
                    }
                  }}
                  min={todayString()}
                  required
                />
                {errors.orderDate && <span className="error-message">{errors.orderDate}</span>}
              </div>

              <div className="input-group">
                <label>
                  <Clock size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                  Hora de Recogida *
                </label>

                <div style={{ marginBottom: '1rem' }}>
                  <div style={{ fontWeight: 'bold', marginBottom: '0.75rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                    ALMUERZO (12:00 - 16:00)
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(90px, 1fr))', gap: '0.5rem' }}>
                    {lunchSlots.map(slot => (
                      <button
                        key={slot}
                        type="button"
                        onClick={() => handleTimeSelect(slot)}
                        className={orderTime === slot ? 'btn btn-primary' : 'btn btn-outline'}
                        style={{ padding: '0.75rem', fontSize: '1rem', fontWeight: orderTime === slot ? 'bold' : 'normal' }}
                      >
                        {slot}
                      </button>
                    ))}
                  </div>
                </div>

                <div style={{ marginBottom: '0.5rem' }}>
                  <div style={{ fontWeight: 'bold', marginBottom: '0.75rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                    CENA (20:00 - 23:00)
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(90px, 1fr))', gap: '0.5rem' }}>
                    {dinnerSlots.map(slot => (
                      <button
                        key={slot}
                        type="button"
                        onClick={() => handleTimeSelect(slot)}
                        className={orderTime === slot ? 'btn btn-primary' : 'btn btn-outline'}
                        style={{ padding: '0.75rem', fontSize: '1rem', fontWeight: orderTime === slot ? 'bold' : 'normal' }}
                      >
                        {slot}
                      </button>
                    ))}
                  </div>
                </div>

                {orderTime && (
                  <div style={{
                    marginTop: '0.75rem',
                    padding: '0.75rem',
                    backgroundColor: '#dbeafe',
                    borderRadius: '0.5rem',
                    color: '#1e40af',
                    fontWeight: 'bold',
                    textAlign: 'center'
                  }}>
                    Hora seleccionada: {orderTime}
                  </div>
                )}

                {errors.orderTime && <span className="error-message">{errors.orderTime}</span>}
              </div>

              <div className="input-group">
                <label>
                  <CreditCard size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                  Método de Pago *
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginTop: '0.25rem' }}>
                  <button
                    type="button"
                    onClick={() => { setPaymentMethod('CARD'); setErrors(prev => ({ ...prev, paymentMethod: undefined })); }}
                    className={paymentMethod === 'CARD' ? 'btn btn-primary' : 'btn btn-outline'}
                    style={{ padding: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', fontSize: '1rem', fontWeight: paymentMethod === 'CARD' ? 'bold' : 'normal' }}
                  >
                    <CreditCard size={20} />
                    Tarjeta
                  </button>
                  <button
                    type="button"
                    onClick={() => { setPaymentMethod('CASH'); setErrors(prev => ({ ...prev, paymentMethod: undefined })); }}
                    className={paymentMethod === 'CASH' ? 'btn btn-primary' : 'btn btn-outline'}
                    style={{ padding: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', fontSize: '1rem', fontWeight: paymentMethod === 'CASH' ? 'bold' : 'normal' }}
                  >
                    <Banknote size={20} />
                    Efectivo
                  </button>
                </div>
                {errors.paymentMethod && <span className="error-message">{errors.paymentMethod}</span>}
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                style={{ width: '100%' }}
                disabled={loading}
              >
                <ShoppingCart size={20} />
                {loading ? 'Procesando...' : 'Confirmar Pedido'}
              </button>
            </form>
          </div>
        </div>
      </div>

      {orderDetails && (
        <ConfirmModal
          isOpen
          onClose={() => navigate('/cliente/orders')}
          type="success"
          title="Pedido realizado"
          message={`Pedido #${orderDetails.id}: ${orderDetails.itemCount} ${orderDetails.itemCount === 1 ? 'producto' : 'productos'}.
Total: €${Number(orderDetails.total).toFixed(2)} (${PAYMENT_METHOD_LABELS[orderDetails.paymentMethod]})
Recogida: ${orderDetails.pickupDate}`}
          confirmText="Ver Mis Pedidos"
        />
      )}
    </div>
  );
};

export default Checkout;