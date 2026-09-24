import { useState, useEffect } from 'react';
import { Calendar, Clock, Users, Phone, CheckCircle, XCircle, Trash2 } from 'lucide-react';
import reservationService from '../../services/reservationService';
import { RESERVATION_STATUS_LABELS } from '../../utils/constants';
import { formatTime } from '../../utils/dates';

const ReservationList = () => {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('TODOS');
  
  useEffect(() => {
    loadReservations();
  }, []);
  
  const loadReservations = async () => {
    try {
      const data = await reservationService.getAll();
      // Ordenar por fecha y hora
      data.sort((a, b) => {
        const dateA = new Date(a.date + 'T' + a.time);
        const dateB = new Date(b.date + 'T' + b.time);
        return dateB - dateA;
      });
      setReservations(data);
    } catch (error) {
      console.error('Error cargando reservas:', error);
      alert('Error al cargar reservas');
    } finally {
      setLoading(false);
    }
  };
  
  const handleUpdateStatus = async (id, newStatus) => {
    try {
      await reservationService.update(id, { status: newStatus });
      await loadReservations();
      alert('Estado actualizado correctamente');
    } catch (error) {
      console.error('Error actualizando estado:', error);
      alert('Error al actualizar estado');
    }
  };
  
  const handleCancel = async (id) => {
    if (!window.confirm('¿Cancelar esta reserva?')) return;
    
    try {
      await reservationService.cancel(id);
      await loadReservations();
      alert('Reserva cancelada');
    } catch (error) {
      console.error('Error cancelando reserva:', error);
      alert('Error al cancelar reserva');
    }
  };
  
  const handleDelete = async (id) => {
    if (!window.confirm('¿Estás seguro de eliminar esta reserva? Esta acción no se puede deshacer.')) return;
    
    try {
      await reservationService.delete(id);
      await loadReservations();
      alert('Reserva eliminada');
    } catch (error) {
      console.error('Error eliminando reserva:', error);
      alert('Error al eliminar reserva');
    }
  };
  
  const getStatusBadge = (status) => {
    const styles = {
      PENDING: 'badge-warning',
      CONFIRMED: 'badge-success',
      CANCELLED: 'badge-danger'
    };
    return `badge ${styles[status]}`;
  };
  
  const filteredReservations = filter === 'TODOS'
    ? reservations
    : reservations.filter(r => r.status === filter);
  
  const statusFilters = ['TODOS', 'PENDING', 'CONFIRMED', 'CANCELLED'];
  
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
          Gestión de Reservas
        </h1>
        <p style={{ color: 'var(--text-secondary)' }}>
          Total: {reservations.length} reservas
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
            {status === 'TODOS' ? 'Todas' : RESERVATION_STATUS_LABELS[status]}
          </button>
        ))}
      </div>
      
      {/* Lista de reservas */}
      <div className="grid" style={{ gap: '1rem' }}>
        {filteredReservations.map(reservation => (
          <div key={reservation.id} className="card">
            <div style={{ 
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-start'
            }}>
              <div style={{ flex: 1 }}>
                <div style={{ 
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                  marginBottom: '1rem'
                }}>
                  <span style={{ 
                    fontSize: '1.5rem',
                    fontWeight: 'bold',
                    color: 'var(--primary-color)'
                  }}>
                    #{reservation.id}
                  </span>
                  <span className={getStatusBadge(reservation.status)}>
                    {RESERVATION_STATUS_LABELS[reservation.status]}
                  </span>
                </div>
                
                <div className="grid grid-2" style={{ gap: '1rem' }}>
                  <div>
                    <div style={{ 
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                      marginBottom: '0.5rem'
                    }}>
                      <Calendar size={16} color="#2563eb" />
                      <span style={{ fontWeight: 'bold' }}>
                        {new Date(reservation.date).toLocaleDateString('es-ES', {
                          weekday: 'long',
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric'
                        })}
                      </span>
                    </div>
                    
                    <div style={{ 
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                      marginBottom: '0.5rem'
                    }}>
                      <Clock size={16} color="#10b981" />
                      <span>{formatTime(reservation.time)}</span>
                    </div>
                    
                    <div style={{ 
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem'
                    }}>
                      <Users size={16} color="#f59e0b" />
                      <span>{reservation.pax} {reservation.pax === 1 ? 'persona' : 'personas'}</span>
                    </div>
                  </div>
                  
                  <div>
                    <div style={{ marginBottom: '0.5rem' }}>
                      <strong>Cliente:</strong> {reservation.userName}
                    </div>
                    <div style={{ 
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                      marginBottom: '0.5rem'
                    }}>
                      <Phone size={16} />
                      <span>{reservation.userPhone}</span>
                    </div>
                    {reservation.comments && (
                      <div style={{ 
                        fontSize: '0.875rem',
                        color: 'var(--text-secondary)',
                        fontStyle: 'italic'
                      }}>
                        "{reservation.comments}"
                      </div>
                    )}
                  </div>
                </div>
              </div>
              
              <div style={{ 
                display: 'flex',
                flexDirection: 'column',
                gap: '0.5rem',
                marginLeft: '1rem'
              }}>
                {reservation.status === 'PENDING' && (
                  <>
                    <button
                      onClick={() => handleUpdateStatus(reservation.id, 'CONFIRMED')}
                      className="btn btn-secondary"
                      style={{ padding: '0.5rem 1rem', fontSize: '0.875rem' }}
                    >
                      <CheckCircle size={16} />
                      Confirmar
                    </button>
                    <button
                      onClick={() => handleCancel(reservation.id)}
                      className="btn btn-outline"
                      style={{ padding: '0.5rem 1rem', fontSize: '0.875rem' }}
                    >
                      <XCircle size={16} />
                      Cancelar
                    </button>
                  </>
                )}
                
                {reservation.status === 'CONFIRMED' && (
                  <button
                    onClick={() => handleCancel(reservation.id)}
                    className="btn btn-outline"
                    style={{ padding: '0.5rem 1rem', fontSize: '0.875rem' }}
                  >
                    <XCircle size={16} />
                    Cancelar
                  </button>
                )}
                
                <button
                  onClick={() => handleDelete(reservation.id)}
                  className="btn btn-danger"
                  style={{ padding: '0.5rem 1rem', fontSize: '0.875rem' }}
                >
                  <Trash2 size={16} />
                  Eliminar
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
      
      {filteredReservations.length === 0 && (
        <div style={{ 
          textAlign: 'center',
          padding: '3rem',
          color: 'var(--text-secondary)'
        }}>
          No hay reservas con este estado
        </div>
      )}
    </div>
  );
};

export default ReservationList;