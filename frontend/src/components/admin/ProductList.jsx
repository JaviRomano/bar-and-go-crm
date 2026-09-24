import { useState, useEffect } from 'react';
import { Plus, Edit, Trash2, Power, PowerOff } from 'lucide-react';
import productService from '../../services/productService';
import ProductForm from './ProductForm';

const ProductList = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [filter, setFilter] = useState('TODOS');
  
  useEffect(() => {
    loadProducts();
  }, []);
  
  const loadProducts = async () => {
    try {
      const data = await productService.getAll();
      setProducts(data);
    } catch (error) {
      console.error('Error cargando productos:', error);
      alert('Error al cargar productos');
    } finally {
      setLoading(false);
    }
  };
  
  const handleCreate = () => {
    setEditingProduct(null);
    setShowForm(true);
  };
  
  const handleEdit = (product) => {
    setEditingProduct(product);
    setShowForm(true);
  };
  
  const handleDelete = async (id) => {
    if (!window.confirm('¿Estás seguro de eliminar este producto?')) return;
    
    try {
      await productService.delete(id);
      await loadProducts();
      alert('Producto eliminado correctamente');
    } catch (error) {
      console.error('Error eliminando producto:', error);
      alert('Error al eliminar producto');
    }
  };
  
  const handleToggleAvailability = async (id) => {
    try {
      await productService.toggleAvailability(id);
      await loadProducts();
    } catch (error) {
      console.error('Error cambiando disponibilidad:', error);
      alert('Error al cambiar disponibilidad');
    }
  };
  
  const handleFormClose = async (reload) => {
    setShowForm(false);
    setEditingProduct(null);
    if (reload) {
      await loadProducts();
    }
  };
  
  const filteredProducts = filter === 'TODOS'
    ? products
    : products.filter(p => p.category === filter);
  
  const categories = ['TODOS', 'ENTRANTES', 'PRINCIPALES', 'POSTRES', 'BEBIDAS'];
  
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
        marginBottom: '2rem'
      }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: 'bold', marginBottom: '0.5rem' }}>
            Gestión de Productos
          </h1>
          <p style={{ color: 'var(--text-secondary)' }}>
            Total: {products.length} productos
          </p>
        </div>
        
        <button onClick={handleCreate} className="btn btn-primary">
          <Plus size={20} />
          Nuevo Producto
        </button>
      </div>
      
      {/* Filtro de categorías */}
      <div style={{ 
        display: 'flex', 
        gap: '0.5rem',
        marginBottom: '2rem',
        flexWrap: 'wrap'
      }}>
        {categories.map(cat => (
          <button
            key={cat}
            onClick={() => setFilter(cat)}
            className={filter === cat ? 'btn btn-primary' : 'btn btn-outline'}
            style={{ fontSize: '0.875rem' }}
          >
            {cat}
          </button>
        ))}
      </div>
      
      {/* Tabla de productos */}
      <div className="card table-wrapper" style={{ padding: 0, overflow: 'hidden' }}>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre</th>
              <th>Categoría</th>
              <th>Precio</th>
              <th>Disponible</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {filteredProducts.map(product => (
              <tr key={product.id}>
                <td>#{product.id}</td>
                <td>
                  <div>
                    <div style={{ fontWeight: 'bold' }}>{product.name}</div>
                    <div style={{ 
                      fontSize: '0.875rem', 
                      color: 'var(--text-secondary)',
                      marginTop: '0.25rem'
                    }}>
                      {product.description}
                    </div>
                  </div>
                </td>
                <td>
                  <span className="badge badge-info">
                    {product.category}
                  </span>
                </td>
                <td style={{ fontWeight: 'bold', color: '#10b981' }}>
                  €{product.price.toFixed(2)}
                </td>
                <td>
                  <button
                    onClick={() => handleToggleAvailability(product.id)}
                    className={product.available ? 'badge badge-success' : 'badge badge-danger'}
                    style={{ 
                      cursor: 'pointer',
                      border: 'none',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.25rem'
                    }}
                  >
                    {product.available ? <Power size={14} /> : <PowerOff size={14} />}
                    {product.available ? 'Disponible' : 'No disponible'}
                  </button>
                </td>
                <td>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button
                      onClick={() => handleEdit(product)}
                      className="btn btn-outline"
                      style={{ padding: '0.5rem' }}
                    >
                      <Edit size={16} />
                    </button>
                    <button
                      onClick={() => handleDelete(product.id)}
                      className="btn btn-danger"
                      style={{ padding: '0.5rem' }}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        
        {filteredProducts.length === 0 && (
          <div style={{ 
            padding: '3rem',
            textAlign: 'center',
            color: 'var(--text-secondary)'
          }}>
            No hay productos en esta categoría
          </div>
        )}
      </div>
      
      {/* Modal de formulario */}
      {showForm && (
        <ProductForm
          product={editingProduct}
          onClose={handleFormClose}
        />
      )}
    </div>
  );
};

export default ProductList;