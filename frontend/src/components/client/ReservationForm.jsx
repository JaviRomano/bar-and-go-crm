import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, Clock, Users, Phone, User, MessageSquare, Save } from 'lucide-react';
import reservationService from '../../services/reservationService';
import { useAuth } from '../../context/useAuth';
import ConfirmModal from '../common/ConfirmModal';
import { parseLocalDate, tomorrowString } from '../../utils/dates';

const ReservationForm = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  // Nombre y teléfono se precargan del usuario: /api/reservations/me asocia las reservas por teléfono
  const emptyForm = () => ({
    date: '',
    time: '',
    pax: 2,
    userName: user?.name ?? '',
    userPhone: user?.phone ?? '',
    comments: ''
  });

  const [formData, setFormData] = useState(emptyForm);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [reservationDetails, setReservationDetails] = useState(null);

  // Horarios disponibles
  const lunchSlots = ['12:00', '12:30', '13:00', '13:30', '14:00', '14:30', '15:00', '15:30', '16:00'];
  const dinnerSlots = ['20:00', '20:30', '21:00', '21:30', '22:00', '22:30', '23:00'];

  const validate = () => {
    const newErrors = {};

    if (!formData.date) {
      newErrors.date = 'La fecha es obligatoria';
    } else {
      const selectedDate = parseLocalDate(formData.date);
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      if (selectedDate < today) {
        newErrors.date = 'La fecha debe ser futura';
      }

      const dayOfWeek = selectedDate.getDay();
      if (dayOfWeek === 2) {
        newErrors.date = 'Los martes el bar está cerrado';
      }
    }

    if (!formData.time) {
      newErrors.time = 'La hora es obligatoria';
    }

    if (!formData.pax || formData.pax < 1) {
      newErrors.pax = 'Debe haber al menos 1 persona';
    } else if (formData.pax > 50) {
      newErrors.pax = 'No se permiten reservas para más de 50 personas';
    }

    if (!formData.userName.trim()) {
      newErrors.userName = 'El nombre es obligatorio';
    } else if (formData.userName.trim().length < 2) {
      newErrors.userName = 'El nombre debe tener al menos 2 caracteres';
    }

    if (!formData.userPhone.trim()) {
      newErrors.userPhone = 'El teléfono es obligatorio';
    } else {
      const cleanPhone = formData.userPhone.replace(/[^\d+]/g, '');
      if (cleanPhone.length < 9 || cleanPhone.length > 20) {
        newErrors.userPhone = 'El teléfono debe tener entre 9 y 20 dígitos';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validate()) {
      return;
    }

    setLoading(true);

    try {
      const reservationData = {
        date: formData.date,
        time: formData.time,
        pax: parseInt(formData.pax),
        userName: formData.userName.trim(),
        userPhone: formData.userPhone.trim(),
        comments: formData.comments.trim() || null
      };

      const response = await reservationService.create(reservationData);

      // Guardar detalles para el modal
      setReservationDetails({
        id: response.id,
        date: parseLocalDate(formData.date).toLocaleDateString('es-ES', {
          weekday: 'long',
          year: 'numeric',
          month: 'long',
          day: 'numeric'
        }),
        time: formData.time,
        pax: formData.pax,
        userName: formData.userName
      });

      setFormData(emptyForm());
      setErrors({});

    } catch (error) {
      console.error('Error creando reserva:', error);

      const validationErrors = error.response?.data?.validationErrors;
      if (validationErrors && Object.keys(validationErrors).length > 0) {
        setErrors(validationErrors);
      } else if (error.response?.data?.message) {
        alert('Error: ' + error.response.data.message);
      } else {
        alert('Error al crear reserva: ' + error.message);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value, type } = e.target;

    setFormData(prev => ({
      ...prev,
      [name]: type === 'number' ? parseInt(value) || '' : value
    }));

    if (errors[name]) {
      setErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[name];
        return newErrors;
      });
    }
  };

  const handleTimeSelect = (time) => {
    setFormData(prev => ({ ...prev, time }));
    if (errors.time) {
      setErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors.time;
        return newErrors;
      });
    }
  };

  return (
    <div className="container" style={{ padding: '2rem 0' }}>
      <div style={{ maxWidth: '900px', margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <h1 style={{ fontSize: '2rem', fontWeight: 'bold', marginBottom: '0.5rem' }}>
            Reservar Mesa
          </h1>
          <p style={{ color: 'var(--text-secondary)' }}>
            Completa el formulario para reservar tu mesa en Bar&Go
          </p>
        </div>

        {/* Info importante */}
        <div className="card" style={{
          backgroundColor: '#dbeafe',
          border: '2px solid #2563eb',
          marginBottom: '2rem'
        }}>
          <h3 style={{
            fontWeight: 'bold',
            marginBottom: '0.75rem',
            color: '#1e40af'
          }}>
            Horarios de Reserva
          </h3>
          <ul style={{
            marginLeft: '1.5rem',
            color: '#1e40af',
            fontSize: '0.875rem'
          }}>
            <li>Almuerzo: 12:00 - 16:00</li>
            <li>Cena: 20:00 - 23:00</li>
            <li>Martes: Cerrado</li>
          </ul>
        </div>

        {Object.keys(errors).length > 0 && (
          <div className="card" style={{
            backgroundColor: '#fee2e2',
            border: '2px solid #ef4444',
            marginBottom: '1.5rem'
          }}>
            <div style={{
              color: '#991b1b',
              fontWeight: 'bold',
              marginBottom: '0.5rem'
            }}>
              Por favor, corrige los siguientes errores:
            </div>
            <ul style={{
              marginLeft: '1.5rem',
              fontSize: '0.875rem',
              color: '#991b1b'
            }}>
              {Object.entries(errors).map(([field, message]) => (
                <li key={field}>{message}</li>
              ))}
            </ul>
          </div>
        )}

        <div className="card">
          <form onSubmit={handleSubmit}>
            {/* Fecha y Personas */}
            <div className="grid grid-2">
              <div className="input-group">
                <label>
                  <Calendar size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                  Fecha *
                </label>
                <input
                  type="date"
                  name="date"
                  value={formData.date}
                  onChange={handleChange}
                  min={tomorrowString()}
                  required
                />
                {errors.date && <span className="error-message">{errors.date}</span>}
              </div>

              <div className="input-group">
                <label>
                  <Users size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                  Número de Personas *
                </label>
                <input
                  type="number"
                  name="pax"
                  value={formData.pax}
                  onChange={handleChange}
                  min="1"
                  max="50"
                  required
                />
                {errors.pax && <span className="error-message">{errors.pax}</span>}
              </div>
            </div>

            {/* Selector de Hora Visual */}
            <div className="input-group">
              <label>
                <Clock size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                Selecciona la Hora *
              </label>

              {/* Almuerzo */}
              <div style={{ marginBottom: '1.5rem' }}>
                <div style={{
                  fontWeight: 'bold',
                  marginBottom: '0.75rem',
                  color: 'var(--text-secondary)',
                  fontSize: '0.875rem'
                }}>
                  ALMUERZO (12:00 - 16:00)
                </div>
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(100px, 1fr))',
                  gap: '0.5rem'
                }}>
                  {lunchSlots.map(slot => (
                    <button
                      key={slot}
                      type="button"
                      onClick={() => handleTimeSelect(slot)}
                      className={formData.time === slot ? 'btn btn-primary' : 'btn btn-outline'}
                      style={{
                        padding: '0.75rem',
                        fontSize: '1rem',
                        fontWeight: formData.time === slot ? 'bold' : 'normal'
                      }}
                    >
                      {slot}
                    </button>
                  ))}
                </div>
              </div>

              {/* Cena */}
              <div>
                <div style={{
                  fontWeight: 'bold',
                  marginBottom: '0.75rem',
                  color: 'var(--text-secondary)',
                  fontSize: '0.875rem'
                }}>
                  CENA (20:00 - 23:00)
                </div>
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(100px, 1fr))',
                  gap: '0.5rem'
                }}>
                  {dinnerSlots.map(slot => (
                    <button
                      key={slot}
                      type="button"
                      onClick={() => handleTimeSelect(slot)}
                      className={formData.time === slot ? 'btn btn-primary' : 'btn btn-outline'}
                      style={{
                        padding: '0.75rem',
                        fontSize: '1rem',
                        fontWeight: formData.time === slot ? 'bold' : 'normal'
                      }}
                    >
                      {slot}
                    </button>
                  ))}
                </div>
              </div>

              {formData.time && (
                <div style={{
                  marginTop: '0.75rem',
                  padding: '0.75rem',
                  backgroundColor: '#dbeafe',
                  borderRadius: '0.5rem',
                  color: '#1e40af',
                  fontWeight: 'bold',
                  textAlign: 'center'
                }}>
                  Hora seleccionada: {formData.time}
                </div>
              )}

              {errors.time && <span className="error-message">{errors.time}</span>}
            </div>

            {/* Datos de Contacto */}
            <div className="grid grid-2">
              <div className="input-group">
                <label>
                  <User size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                  Nombre Completo *
                </label>
                <input
                  type="text"
                  name="userName"
                  value={formData.userName}
                  onChange={handleChange}
                  placeholder="Tu nombre"
                  minLength="2"
                  maxLength="100"
                  required
                />
                {errors.userName && <span className="error-message">{errors.userName}</span>}
              </div>

              <div className="input-group">
                <label>
                  <Phone size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                  Teléfono *
                </label>
                <input
                  type="tel"
                  name="userPhone"
                  value={formData.userPhone}
                  onChange={handleChange}
                  placeholder="+34600000000"
                  required
                />
                {errors.userPhone && <span className="error-message">{errors.userPhone}</span>}
                <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                  Formato: +34XXXXXXXXX
                </span>
              </div>
            </div>

            {/* Comentarios */}
            <div className="input-group">
              <label>
                <MessageSquare size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                Comentarios (opcional)
              </label>
              <textarea
                name="comments"
                value={formData.comments}
                onChange={handleChange}
                placeholder="Alergias, preferencias de mesa, ocasión especial..."
                rows="3"
                maxLength="500"
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', marginTop: '1rem' }}
              disabled={loading}
            >
              <Save size={20} />
              {loading ? 'Procesando...' : 'Confirmar Reserva'}
            </button>
          </form>
        </div>

        <div className="card" style={{ marginTop: '2rem', backgroundColor: 'var(--light-bg)' }}>
          <h3 style={{ fontWeight: 'bold', marginBottom: '0.75rem' }}>
            Información Adicional
          </h3>
          <ul style={{
            marginLeft: '1.5rem',
            fontSize: '0.875rem',
            color: 'var(--text-secondary)'
          }}>
            <li>Las reservas están sujetas a disponibilidad</li>
            <li>Recibirás una confirmación en las próximas horas</li>
            <li>Puedes ver el estado de tu reserva en "Mis Reservas"</li>
            <li>Para grupos mayores a 10 personas, contacta directamente al bar</li>
          </ul>
        </div>
      </div>
      {/* Modal de Confirmación */}
      {reservationDetails && (
        <ConfirmModal
          isOpen
          onClose={() => setReservationDetails(null)}
          type="success"
          title="Reserva registrada"
          message={`Reserva #${reservationDetails.id} para ${reservationDetails.pax} ${reservationDetails.pax === 1 ? 'persona' : 'personas'}.
Fecha: ${reservationDetails.date}
Hora: ${reservationDetails.time}
A nombre de: ${reservationDetails.userName}

Queda pendiente de confirmación por el local.`}
          confirmText="Ver Mis Reservas"
          onConfirm={() => navigate('/cliente/reservations')}
        />
      )}
    </div>
  );
};

export default ReservationForm;