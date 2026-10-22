import { useId } from 'react';

/** Campo de formulario con etiqueta y error del backend debajo. */
export const Field = ({ label, error, hint, children, wide = false }) => {
  const id = useId();
  const control = typeof children === 'function' ? children(id) : children;
  return (
    <div className={`g-field${wide ? ' g-field--wide' : ''}${error ? ' g-field--error' : ''}`}>
      <label htmlFor={id} className="g-field__label">{label}</label>
      {control}
      {error ? <p className="g-field__error">{error}</p> : hint && <p className="g-field__hint">{hint}</p>}
    </div>
  );
};

export const Input = ({ label, error, hint, wide, ...props }) => (
  <Field label={label} error={error} hint={hint} wide={wide}>
    {(id) => <input id={id} className="g-input" aria-invalid={!!error} {...props} />}
  </Field>
);

export const Select = ({ label, error, hint, wide, options, placeholder, ...props }) => (
  <Field label={label} error={error} hint={hint} wide={wide}>
    {(id) => (
      <select id={id} className="g-input" aria-invalid={!!error} {...props}>
        {placeholder !== undefined && <option value="">{placeholder}</option>}
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    )}
  </Field>
);

export const Textarea = ({ label, error, wide = true, ...props }) => (
  <Field label={label} error={error} wide={wide}>
    {(id) => <textarea id={id} className="g-input" rows={2} aria-invalid={!!error} {...props} />}
  </Field>
);

export const FormGrid = ({ children }) => <div className="g-form-grid">{children}</div>;
