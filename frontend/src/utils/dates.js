// Fechas en hora local con formato yyyy-MM-dd. toISOString() usa UTC y
// cerca de medianoche devuelve el día anterior o el siguiente.

export const toLocalDateString = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

export const todayString = () => toLocalDateString(new Date());

export const tomorrowString = () => {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return toLocalDateString(tomorrow);
};

// La API serializa LocalTime como HH:mm:ss; en pantalla basta HH:mm
export const formatTime = (value) => (value ? value.slice(0, 5) : '');

// new Date('yyyy-MM-dd') se interpreta en UTC; así se construye en hora local
export const parseLocalDate = (value) => {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
};
