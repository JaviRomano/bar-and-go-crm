import { useState, useEffect } from 'react';
import { ShoppingCart, Plus, Minus, Trash2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import productService from '../../services/productService';
import { PRODUCT_CATEGORIES } from '../../utils/constants';

const Menu = () => {
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('TODOS');
  const [loading, setLoading] = useState(true);
  const [showCart, setShowCart] = useState(false);
  const navigate = useNavigate();
  
  useEffect(() => {
    loadProducts();
    loadCart();
  }, []);
  
  const loadProducts = async () => {
    try {
      const data = await productService.getAvailable();
      setProducts(data);
    } catch (error) {
      console.error('Error cargando productos:', error);
    } finally {
      setLoading(false);
    }
  };
  
  const loadCart = () => {
    const savedCart = localStorage.getItem('cart');
    if (savedCart) {
      setCart(JSON.parse(savedCart));
    }
  };
  
  const saveCart = (newCart) => {
    localStorage.setItem('cart', JSON.stringify(newCart));
    setCart(newCart);
  };
  
  const addToCart = (product) => {
    const existingItem = cart.find(item => item.id === product.id);
    
    let newCart;
    if (existingItem) {
      newCart = cart.map(item =>
        item.id === product.id
          ? { ...item, quantity: item.quantity + 1 }
          : item
      );
    } else {
      newCart = [...cart, { ...product, quantity: 1 }];
    }
    
    saveCart(newCart);
  };
  
  const updateQuantity = (productId, change) => {
    const newCart = cart.map(item =>
      item.id === productId
        ? { ...item, quantity: Math.max(0, item.quantity + change) }
        : item
    ).filter(item => item.quantity > 0);
    
    saveCart(newCart);
  };
  
  const removeFromCart = (productId) => {
    const newCart = cart.filter(item => item.id !== productId);
    saveCart(newCart);
  };
  
  const getCartTotal = () => {
    return cart.reduce((total, item) => total + (item.price * item.quantity), 0);
  };
  
  const getCartItemsCount = () => {
    return cart.reduce((total, item) => total + item.quantity, 0);
  };
  
  const handleCheckout = () => {
    if (cart.length === 0) {
      alert('El carrito está vacío');
      return;
    }
    setShowCart(false);
    navigate('/cliente/checkout');
  };
  
  const filteredProducts = selectedCategory === 'TODOS'
    ? products
    : products.filter(p => p.category === selectedCategory);
  
  const categories = ['TODOS', ...Object.values(PRODUCT_CATEGORIES)];
  
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
            Nuestro Menú
          </h1>
          <p style={{ color: 'var(--text-secondary)' }}>
            Selecciona los productos que deseas pedir
          </p>
        </div>
        
        <button
          onClick={() => setShowCart(true)}
          className="btn btn-primary"
          style={{
            position: 'relative',
            padding: '0.625rem 1rem',
            fontSize: '0.9rem'
           }}
        >
          <ShoppingCart size={20} />
          <span className="desktop-only">Carrito</span>
          {getCartItemsCount() > 0 && (
            <span style={{
              position: 'absolute',
              top: '-8px',
              right: '-8px',
              backgroundColor: '#ef4444',
              color: 'white',
              borderRadius: '50%',
              width: '24px',
              height: '24px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '0.75rem',
              fontWeight: 'bold'
            }}>
              {getCartItemsCount()}
            </span>
          )}
        </button>
      </div>
      
      {/* Category Filter */}
      <div style={{
        display: 'flex',
        gap: '0.5rem',
        marginBottom: '2rem',
        overflowX: 'auto',
        flexWrap: 'wrap'
      }}>
        {categories.map(category => (
          <button
            key={category}
            onClick={() => setSelectedCategory(category)}
            className={selectedCategory === category ? 'btn btn-primary' : 'btn btn-outline'}
            style={{ fontSize: '0.875rem' }}
          >
            {category}
          </button>
        ))}
      </div>
      
      {/* Products Grid */}
      <div className="grid grid-3">
        {filteredProducts.map(product => {
          const cartItem = cart.find(item => item.id === product.id);
          const inCart = !!cartItem;
          
          return (
            <div 
              key={product.id} 
              className="card"
              style={{
                transition: 'transform 0.2s',
                border: inCart ? '2px solid var(--primary-color)' : '1px solid var(--border-color)'
              }}
              onMouseEnter={(e) => e.currentTarget.style.transform = 'translateY(-4px)'}
              onMouseLeave={(e) => e.currentTarget.style.transform = 'translateY(0)'}
            >
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start',
                marginBottom: '0.75rem'
              }}>
                <h3 style={{ fontWeight: 'bold', fontSize: '1.125rem', flex: 1 }}>
                  {product.name}
                </h3>
                <span style={{
                  fontSize: '1.25rem',
                  fontWeight: 'bold',
                  color: '#10b981'
                }}>
                  €{product.price.toFixed(2)}
                </span>
              </div>
              
              <p style={{ 
                fontSize: '0.875rem', 
                color: 'var(--text-secondary)',
                marginBottom: '0.75rem',
                minHeight: '40px'
              }}>
                {product.description}
              </p>
              
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginTop: '1rem',
                paddingTop: '0.75rem',
                borderTop: '1px solid var(--border-color)'
              }}>
                <span className="badge badge-info" style={{ fontSize: '0.75rem' }}>
                  {product.category}
                </span>
                
                {inCart ? (
                  <div style={{ 
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem'
                  }}>
                    <button
                      onClick={() => updateQuantity(product.id, -1)}
                      className="btn btn-outline"
                      style={{ padding: '0.25rem 0.5rem' }}
                    >
                      <Minus size={16} />
                    </button>
                    <span style={{ 
                      fontWeight: 'bold',
                      minWidth: '30px',
                      textAlign: 'center'
                    }}>
                      {cartItem.quantity}
                    </span>
                    <button
                      onClick={() => updateQuantity(product.id, 1)}
                      className="btn btn-primary"
                      style={{ padding: '0.25rem 0.5rem' }}
                    >
                      <Plus size={16} />
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={() => addToCart(product)}
                    className="btn btn-primary"
                    style={{ fontSize: '0.875rem' }}
                  >
                    <Plus size={16} />
                    Añadir
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
      
      {filteredProducts.length === 0 && (
        <div style={{ 
          textAlign: 'center', 
          padding: '3rem',
          color: 'var(--text-secondary)'
        }}>
          <p>No hay productos disponibles en esta categoría</p>
        </div>
      )}
      
      {/* Cart Modal */}
      {showCart && (
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
              marginBottom: '1.5rem',
              paddingBottom: '1rem',
              borderBottom: '2px solid var(--border-color)'
            }}>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>
                Carrito de Compras
              </h2>
              <button
                onClick={() => setShowCart(false)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  fontSize: '1.5rem',
                  cursor: 'pointer'
                }}
              >
                ×
              </button>
            </div>
            
            {cart.length === 0 ? (
              <div style={{ 
                textAlign: 'center',
                padding: '3rem',
                color: 'var(--text-secondary)'
              }}>
                <ShoppingCart size={64} style={{ margin: '0 auto 1rem', opacity: 0.3 }} />
                <p>El carrito está vacío</p>
              </div>
            ) : (
              <>
                <div style={{ marginBottom: '1.5rem' }}>
                  {cart.map(item => (
                    <div 
                      key={item.id}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        padding: '1rem',
                        borderBottom: '1px solid var(--border-color)'
                      }}
                    >
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 'bold', marginBottom: '0.25rem' }}>
                          {item.name}
                        </div>
                        <div style={{ 
                          fontSize: '0.875rem',
                          color: 'var(--text-secondary)'
                        }}>
                          €{item.price.toFixed(2)} × {item.quantity}
                        </div>
                      </div>
                      
                      <div style={{ 
                        display: 'flex',
                        alignItems: 'center',
                        gap: '1rem'
                      }}>
                        <div style={{ 
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.5rem'
                        }}>
                          <button
                            onClick={() => updateQuantity(item.id, -1)}
                            className="btn btn-outline"
                            style={{ padding: '0.25rem 0.5rem' }}
                          >
                            <Minus size={16} />
                          </button>
                          <span style={{ fontWeight: 'bold', minWidth: '30px', textAlign: 'center' }}>
                            {item.quantity}
                          </span>
                          <button
                            onClick={() => updateQuantity(item.id, 1)}
                            className="btn btn-primary"
                            style={{ padding: '0.25rem 0.5rem' }}
                          >
                            <Plus size={16} />
                          </button>
                        </div>
                        
                        <button
                          onClick={() => removeFromCart(item.id)}
                          className="btn btn-danger"
                          style={{ padding: '0.5rem' }}
                        >
                          <Trash2 size={16} />
                        </button>
                        
                        <div style={{ 
                          fontWeight: 'bold',
                          color: '#10b981',
                          minWidth: '80px',
                          textAlign: 'right'
                        }}>
                          €{(item.price * item.quantity).toFixed(2)}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
                
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '1.5rem',
                  backgroundColor: 'var(--light-bg)',
                  borderRadius: '0.5rem',
                  marginBottom: '1.5rem'
                }}>
                  <span style={{ fontSize: '1.25rem', fontWeight: 'bold' }}>
                    Total:
                  </span>
                  <span style={{ 
                    fontSize: '1.75rem',
                    fontWeight: 'bold',
                    color: '#10b981'
                  }}>
                    €{getCartTotal().toFixed(2)}
                  </span>
                </div>
                
                <button
                  onClick={handleCheckout}
                  className="btn btn-primary"
                  style={{ width: '100%' }}
                >
                  <ShoppingCart size={20} />
                  Proceder al Pedido
                </button>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default Menu;