/**
 * Edición de una lista de líneas dentro de un formulario de useForm (pedidos, albaranes, escandallos,
 * conversiones). derive(next, field, value) permite completar otros campos al cambiar uno, por ejemplo
 * la unidad por defecto al elegir un artículo.
 */
export function useLines(form, key, { create, derive } = {}) {
  const lines = form.values[key];
  const replace = (next) => form.setValues((prev) => ({ ...prev, [key]: next(prev[key]) }));

  return {
    lines,
    add: () => replace((list) => [...list, create()]),
    remove: (index) => replace((list) => list.filter((_, i) => i !== index)),
    update: (index, field, value) => replace((list) => list.map((line, i) => {
      if (i !== index) return line;
      const next = { ...line, [field]: value };
      return derive ? derive(next, field, value) : next;
    })),
    error: (index, field) => form.errors[`${key}[${index}].${field}`]
  };
}
