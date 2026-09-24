import { useState, useEffect } from 'react';
import { X, Save } from 'lucide-react';
import productService from '../../services/productService';

const ProductForm = ({ product, onClose }) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    price: '',
    category: 'ENTRANTES',
    available: true,
    imageUrl: ''
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    if (product) {
      setFormData({
        name: product.name || '',
        description: product.description || '',
        price: product.price || '',
        category: product.category || 'ENTRANTES',
        available: product.available ?? true,
        imageUrl: product.imageUrl || ''
      });
    }
  }, [product]);
  
  const validate = () => {
    const newErrors = {};
    
    if (!formData.name.trim()) {
      newErrors.name = 'El nombre es obligatorio';
    }
    
    if (!formData.price || formData.price <= 0) {
      newErrors.price = 'El precio debe ser mayor que 0';
    }
    
    if (!formData.category) {
      newErrors.category = 'La categoría es obligatoria';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validate()) return;
    
    setLoading(true);
    
    try {
      const data = {
        ...formData,
        price: parseFloat(formData.price)
      };
      
      if (product) {
        await productService.update(product.id, data);
      } else {
        await productService.create(data);
      }
      
      alert(product ? 'Producto actualizado correctamente' : 'Producto creado correctamente');
      onClose(true);
    } catch (error) {
      console.error('Error guardando producto:', error);
      
      const validationErrors = error.response?.data?.validationErrors;
      if (validationErrors && Object.keys(validationErrors).length > 0) {
        setErrors(validationErrors);
      } else {
        alert('Error al guardar producto: ' + (error.response?.data?.message || error.message));
      }
    } finally {
      setLoading(false);
    }
  };
  
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
    
    // Limpiar error del campo
    if (errors[name]) {
      setErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[name];
        return newErrors;
      });
    }
  };
  
  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(0,0,0,0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
      padding: '1rem'
    }}>
      <div className="card" style={{ 
        maxWidth: '600px',
        width: '100%',
        maxHeight: '90vh',
        overflow: 'auto'
      }}>
        <div style={{ 
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '1.5rem'
        }}>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>
            {product ? 'Editar Producto' : 'Nuevo Producto'}
          </h2>
          <button
            onClick={() => onClose(false)}
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              padding: '0.5rem'
            }}
          >
            <X size={24} />
          </button>
        </div>
        
        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <label>Nombre *</label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              placeholder="Ej: Hamburguesa Clásica"
              required
            />
            {errors.name && <span className="error-message">{errors.name}</span>}
          </div>
          
          <div className="input-group">
            <label>Descripción</label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleChange}
              placeholder="Descripción del producto"
              rows="3"
            />
            {errors.description && <span className="error-message">{errors.description}</span>}
          </div>
          
          <div className="grid grid-2">
            <div className="input-group">
              <label>Precio (€) *</label>
              <input
                type="number"
                name="price"
                value={formData.price}
                onChange={handleChange}
                placeholder="0.00"
                step="0.01"
                min="0"
                required
              />
              {errors.price && <span className="error-message">{errors.price}</span>}
            </div>
            
            <div className="input-group">
              <label>Categoría *</label>
              <select
                name="category"
                value={formData.category}
                onChange={handleChange}
                required
              >
                <option value="ENTRANTES">Entrantes</option>
                <option value="PRINCIPALES">Principales</option>
                <option value="POSTRES">Postres</option>
                <option value="BEBIDAS">Bebidas</option>
              </select>
              {errors.category && <span className="error-message">{errors.category}</span>}
            </div>
          </div>
          
          <div className="input-group">
            <label>URL de Imagen (opcional)</label>
            <input
              type="url"
              name="imageUrl"
              value={formData.imageUrl}
              onChange={handleChange}
              placeholder="https://ejemplo.com/imagen.jpg"
            />
          </div>
          
          <div className="input-group">
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
              <input
                type="checkbox"
                name="available"
                checked={formData.available}
                onChange={handleChange}
                style={{ width: 'auto', cursor: 'pointer' }}
              />
              Producto disponible
            </label>
          </div>
          
          <div style={{ 
            display: 'flex',
            gap: '1rem',
            marginTop: '1.5rem',
            paddingTop: '1.5rem',
            borderTop: '1px solid var(--border-color)'
          }}>
            <button
              type="button"
              onClick={() => onClose(false)}
              className="btn btn-outline"
              style={{ flex: 1 }}
              disabled={loading}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              style={{ flex: 1 }}
              disabled={loading}
            >
              <Save size={20} />
              {loading ? 'Guardando...' : 'Guardar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ProductForm;