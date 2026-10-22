// Errores del backend: { status, message, validationErrors: { campo: mensaje } }. Las claves de las
// líneas llegan con índice: "lines[0].quantity".

export const fieldErrors = (error) => error?.response?.data?.validationErrors ?? {};

export const errorMessage = (error) => {
  const data = error?.response?.data;
  if (data?.validationErrors && Object.keys(data.validationErrors).length > 0) {
    return 'Revisa los campos marcados';
  }
  return data?.message || error?.message || 'Error inesperado';
};
