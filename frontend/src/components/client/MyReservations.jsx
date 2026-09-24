import { useState, useEffect } from 'react';
import { Calendar, Clock, Users, Phone, User as UserIcon, MessageSquare, Plus, XCircle, Hourglass, CheckCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import reservationService from '../../services/reservationService';
import { RESERVATION_STATUS_LABELS } from '../../utils/constants';
import { formatTime } from '../../utils/dates';
import ConfirmModal from '../common/ConfirmModal';

const MyReservations = () => {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ACTIVAS');
  const [reservationToCancel, setReservationToCancel] = useState(null);
  const [cancelling, setCancelling] = useState(false);
  
  useEffect(() => {
    const loadReservations = async () => {
      try {
        const data = await reservationService.getMine();
        
        // Ordenar por fecha más reciente
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
    loadReservations();
  }, []);
  
  const handleCancel = async () => {
    setCancelling(true);
    try {
      const updated = await reservationService.cancel(reservationToCancel.id);
      setReservations(prev => prev.map(r => (r.id === updated.id ? updated : r)));
    } catch (error) {
      console.error('Error cancelando reserva:', error);
      alert('Error al cancelar la reserva: ' + (error.response?.data?.message || error.message));
    } finally {
      setCancelling(false);
      setReservationToCancel(null);
    }
  };
  
  const canCancel = (reservation) =>
    reservation.status !== 'CANCELLED' && !isPastReservation(reservation);
  
  const getStatusBadge = (status) => {
    const styles = {
      PENDING: 'badge-warning',
      CONFIRMED: 'badge-success',
      CANCELLED: 'badge-danger'
    };
    return `badge ${styles[status]}`;
  };
  
  const getStatusIcon = (status) => {
    switch (status) {
      case 'PENDING':
        return <Hourglass size={24} />;
      case 'CONFIRMED':
        return <CheckCircle size={24} />;
      case 'CANCELLED':
        return <XCircle size={24} />;
      default:
        return <Calendar size={24} />;
    }
  };
  
  const isPastReservation = (reservation) => {
    const reservationDateTime = new Date(reservation.date + 'T' + reservation.time);
    return reservationDateTime < new Date();
  };
  
  const filteredReservations = reservations.filter(r => {
    if (filter === 'ACTIVAS') {
      return !isPastReservation(r) && r.status !== 'CANCELLED';
    } else if (filter === 'PASADAS') {
      return isPastReservation(r) || r.status === 'CANCELLED';
    }
    return true;
  });
  
  if (loading) {
    return (
      <div className="container" style={{ padding: '2rem 0' }}>
        <div className="spinner"></div>
      </div>
    );
  }
  
  return (
    <div className="container" style={{ padding: '2rem 0' }}>
      <div style={{ 
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '2rem',
        flexWrap: 'wrap',
        gap: '1rem'
      }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: 'bold', marginBottom: '0.5rem' }}>
            Mis Reservas
          </h1>
          <p style={{ color: 'var(--text-secondary)' }}>
            {reservations.length === 0 
              ? 'No tienes reservas aún' 
              : `Total: ${reservations.length} reservas`}
          </p>
        </div>
        
        <Link to="/cliente/reservations/new" className="btn btn-primary">
          <Plus size={20} />
          Nueva Reserva
        </Link>
      </div>
      
      {/* Filtros */}
      {reservations.length > 0 && (
        <div style={{ 
          display: 'flex',
          gap: '0.5rem',
          marginBottom: '2rem'
        }}>
          <button
            onClick={() => setFilter('ACTIVAS')}
            className={filter === 'ACTIVAS' ? 'btn btn-primary' : 'btn btn-outline'}
            style={{ fontSize: '0.875rem' }}
          >
            Activas
          </button>
          <button
            onClick={() => setFilter('PASADAS')}
            className={filter === 'PASADAS' ? 'btn btn-primary' : 'btn btn-outline'}
            style={{ fontSize: '0.875rem' }}
          >
            Pasadas
          </button>
          <button
            onClick={() => setFilter('TODAS')}
            className={filter === 'TODAS' ? 'btn btn-primary' : 'btn btn-outline'}
            style={{ fontSize: '0.875rem' }}
          >
            Todas
          </button>
        </div>
      )}
      
      {/* Lista de Reservas */}
      {reservations.length === 0 ? (
        <div className="card" style={{ 
          textAlign: 'center',
          padding: '4rem 2rem'
        }}>
          <Calendar size={64} style={{ 
            margin: '0 auto 1rem',
            opacity: 0.3,
            color: 'var(--text-secondary)'
          }} />
          <h3 style={{ 
            fontSize: '1.25rem',
            fontWeight: 'bold',
            marginBottom: '0.5rem'
          }}>
            No tienes reservas
          </h3>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
            ¡Reserva tu mesa y disfruta de nuestra comida!
          </p>
          <Link to="/cliente/reservations/new" className="btn btn-primary">
            <Plus size={20} />
            Hacer Primera Reserva
          </Link>
        </div>
      ) : filteredReservations.length === 0 ? (
        <div className="card" style={{ 
          textAlign: 'center',
          padding: '3rem 2rem'
        }}>
          <p style={{ color: 'var(--text-secondary)' }}>
            No hay reservas en esta categoría
          </p>
        </div>
      ) : (
        <div className="grid" style={{ gap: '1.5rem' }}>
          {filteredReservations.map(reservation => (
            <div key={reservation.id} className="card">
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
                      {getStatusIcon(reservation.status)} Reserva #{reservation.id}
                    </span>
                    <span className={getStatusBadge(reservation.status)}>
                      {RESERVATION_STATUS_LABELS[reservation.status]}
                    </span>
                  </div>
                </div>
                
                {isPastReservation(reservation) && reservation.status !== 'CANCELLED' && (
                  <span className="badge badge-info" style={{ fontSize: '0.75rem' }}>
                    Completada
                  </span>
                )}
              </div>
              
              {/* Detalles de la Reserva */}
              <div className="grid grid-2" style={{ gap: '1.5rem', marginBottom: '1.5rem' }}>
                <div>
                  <div style={{ 
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                    marginBottom: '0.75rem',
                    fontSize: '1.125rem'
                  }}>
                    <Calendar size={20} color="#2563eb" />
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
                    marginBottom: '0.75rem'
                  }}>
                    <Clock size={18} color="#10b981" />
                    <span style={{ fontSize: '1rem' }}>{formatTime(reservation.time)}</span>
                  </div>
                  
                  <div style={{ 
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem'
                  }}>
                    <Users size={18} color="#f59e0b" />
                    <span>{reservation.pax} {reservation.pax === 1 ? 'persona' : 'personas'}</span>
                  </div>
                </div>
                
                <div>
                  <div style={{ marginBottom: '0.75rem' }}>
                    <div style={{ 
                      fontSize: '0.75rem',
                      color: 'var(--text-secondary)',
                      marginBottom: '0.25rem'
                    }}>
                      Nombre
                    </div>
                    <div style={{ 
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem'
                    }}>
                      <UserIcon size={16} />
                      <strong>{reservation.userName}</strong>
                    </div>
                  </div>
                  
                  <div style={{ marginBottom: '0.75rem' }}>
                    <div style={{ 
                      fontSize: '0.75rem',
                      color: 'var(--text-secondary)',
                      marginBottom: '0.25rem'
                    }}>
                      Teléfono
                    </div>
                    <div style={{ 
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem'
                    }}>
                      <Phone size={16} />
                      <span>{reservation.userPhone}</span>
                    </div>
                  </div>
                  
                  {reservation.comments && (
                    <div>
                      <div style={{ 
                        fontSize: '0.75rem',
                        color: 'var(--text-secondary)',
                        marginBottom: '0.25rem'
                      }}>
                        Comentarios
                      </div>
                      <div style={{ 
                        display: 'flex',
                        alignItems: 'start',
                        gap: '0.5rem',
                        fontSize: '0.875rem',
                        fontStyle: 'italic',
                        color: 'var(--text-secondary)'
                      }}>
                        <MessageSquare size={16} />
                        <span>"{reservation.comments}"</span>
                      </div>
                    </div>
                  )}
                </div>
              </div>
              
              {/* Estado de la reserva */}
              {reservation.status === 'PENDING' && !isPastReservation(reservation) && (
                <div className="card" style={{ 
                  backgroundColor: '#fef3c7',
                  border: '2px solid #f59e0b'
                }}>
                  <div style={{ 
                    fontSize: '0.875rem',
                    color: '#92400e'
                  }}>
                    <strong>Tu reserva está pendiente de confirmación.</strong> Recibirás una notificación cuando sea confirmada.
                  </div>
                </div>
              )}
              
              {reservation.status === 'CONFIRMED' && !isPastReservation(reservation) && (
                <div className="card" style={{ 
                  backgroundColor: '#d1fae5',
                  border: '2px solid #10b981'
                }}>
                  <div style={{ 
                    fontSize: '0.875rem',
                    color: '#065f46'
                  }}>
                    <strong>¡Reserva confirmada!</strong> Te esperamos en la fecha y hora indicada.
                  </div>
                </div>
              )}
              
              {reservation.status === 'CANCELLED' && (
                <div className="card" style={{ 
                  backgroundColor: '#fee2e2',
                  border: '2px solid #ef4444'
                }}>
                  <div style={{ 
                    fontSize: '0.875rem',
                    color: '#991b1b'
                  }}>
                    <strong>Reserva cancelada.</strong>
                  </div>
                </div>
              )}
              
              {canCancel(reservation) && (
                <button
                  onClick={() => setReservationToCancel(reservation)}
                  className="btn btn-outline"
                  style={{ marginTop: '1rem', color: '#991b1b', borderColor: '#ef4444' }}
                  disabled={cancelling}
                >
                  <XCircle size={16} />
                  Cancelar reserva
                </button>
              )}
              
              {isPastReservation(reservation) && reservation.status === 'CONFIRMED' && (
                <div className="card" style={{ 
                  backgroundColor: '#dbeafe',
                  border: '2px solid #2563eb'
                }}>
                  <div style={{ 
                    fontSize: '0.875rem',
                    color: '#1e40af'
                  }}>
                    <strong>Esperamos que hayas disfrutado tu visita.</strong> ¡Gracias por elegirnos!
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
      
      <ConfirmModal
        isOpen={!!reservationToCancel}
        onClose={() => setReservationToCancel(null)}
        type="warning"
        title="Cancelar reserva"
        message={reservationToCancel
          ? `Se cancelará la reserva #${reservationToCancel.id} del ${reservationToCancel.date} a las ${formatTime(reservationToCancel.time)}. Esta acción no se puede deshacer.`
          : ''}
        confirmText="Sí, cancelar"
        showCancel
        cancelText="Volver"
        onConfirm={handleCancel}
      />
    </div>
  );
};

export default MyReservations;