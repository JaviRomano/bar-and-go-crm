import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/useAuth';
import { USER_ROLE_LABELS, homePathFor } from '../../utils/constants';
import {
  LogOut,
  ShoppingCart,
  Calendar,
  LayoutDashboard,
  UtensilsCrossed,
  Package,
  Menu,
  X,
  Truck,
  Boxes,
  ClipboardList,
  PackageCheck,
  Warehouse,
  ListChecks,
  ChefHat,
  Trash2,
  BarChart3
} from 'lucide-react';
import BrandMark from './BrandMark';
import './Navbar.css';

const Navbar = () => {
  const { user, logout, isBackOffice, canManage } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  if (!user) return null;

  const backOffice = isBackOffice();
  const manager = canManage();
  const closeSidebar = () => setSidebarOpen(false);

  const handleLogout = () => {
    closeSidebar();
    logout();
    navigate('/login');
  };

  // Menú según rol: la carta, las compras y los informes son de admin y encargado
  const sections = backOffice ? [
    {
      title: 'Sala',
      items: [
        { path: '/admin', icon: LayoutDashboard, label: 'Dashboard', end: true },
        ...(manager ? [{ path: '/admin/products', icon: Package, label: 'Productos' }] : []),
        { path: '/admin/reservations', icon: Calendar, label: 'Reservas' },
        { path: '/admin/orders', icon: ShoppingCart, label: 'Pedidos' }
      ]
    },
    {
      title: 'Gestión',
      items: [
        ...(manager ? [{ path: '/admin/gestion/proveedores', icon: Truck, label: 'Proveedores' }] : []),
        { path: '/admin/gestion/articulos', icon: Boxes, label: 'Artículos' },
        ...(manager ? [{ path: '/admin/gestion/compras', icon: ClipboardList, label: 'Compras' }] : []),
        { path: '/admin/gestion/recepcion', icon: PackageCheck, label: 'Recepción' },
        { path: '/admin/gestion/inventario', icon: Warehouse, label: 'Inventario' },
        { path: '/admin/gestion/recuentos', icon: ListChecks, label: 'Recuentos' },
        { path: '/admin/gestion/escandallos', icon: ChefHat, label: 'Escandallos' },
        { path: '/admin/gestion/mermas', icon: Trash2, label: 'Mermas' },
        ...(manager ? [{ path: '/admin/gestion/informes', icon: BarChart3, label: 'Informes' }] : [])
      ]
    }
  ] : [
    {
      title: null,
      items: [
        { path: '/cliente', icon: UtensilsCrossed, label: 'Menú', end: true },
        { path: '/cliente/reservations/new', icon: Calendar, label: 'Reservar' },
        { path: '/cliente/reservations', icon: Calendar, label: 'Mis Reservas', end: true },
        { path: '/cliente/orders', icon: ShoppingCart, label: 'Mis Pedidos' }
      ]
    }
  ];

  return (
    <>
      <nav className="nb-top">
        <button type="button" className="nb-menu-btn" onClick={() => setSidebarOpen(true)} aria-label="Abrir menú">
          <Menu size={24} />
          <span>Menú</span>
        </button>

        <Link to={homePathFor(user.role)} className="nb-brand">
          <BrandMark size={32} />
          <span className="nb-wordmark">Bar<span className="nb-amp">&amp;</span>Go</span>
        </Link>

        <div className="nb-user">
          <span className="nb-user__name">{user.name}</span>
          <span className={`nb-role${backOffice ? '' : ' nb-role--customer'}`}>{USER_ROLE_LABELS[user.role] ?? user.role}</span>
        </div>
      </nav>

      {sidebarOpen && <div className="nb-scrim" onClick={closeSidebar} />}

      <aside className={`nb-sidebar${sidebarOpen ? ' nb-sidebar--open' : ''}`} aria-hidden={!sidebarOpen}>
        <div className="nb-sidebar__header">
          <div className="nb-brand">
            <BrandMark size={40} />
            <div>
              <div className="nb-wordmark">Bar<span className="nb-amp">&amp;</span>Go</div>
              <div className="nb-tagline">{backOffice ? 'Panel de gestión' : 'Portal cliente'}</div>
            </div>
          </div>
          <button type="button" className="nb-icon-btn" onClick={closeSidebar} aria-label="Cerrar menú">
            <X size={22} />
          </button>
        </div>

        <div className="nb-profile">
          <div className="nb-avatar">{user.name.charAt(0).toUpperCase()}</div>
          <div>
            <div className="nb-profile__name">{user.name}</div>
            <div className="nb-profile__email">{user.email}</div>
          </div>
        </div>

        <nav className="nb-links">
          {sections.map((section) => (
            <div key={section.title ?? 'main'} className="nb-section">
              {section.title && <div className="nb-section__title">{section.title}</div>}
              {section.items.map((item) => (
                <NavLink key={item.path} to={item.path} end={item.end} onClick={closeSidebar}
                         className={({ isActive }) => `nb-link${isActive ? ' nb-link--active' : ''}`}>
                  <item.icon size={20} aria-hidden="true" />
                  <span>{item.label}</span>
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="nb-footer">
          <button type="button" className="nb-logout" onClick={handleLogout}>
            <LogOut size={20} />
            Cerrar sesión
          </button>
        </div>
      </aside>
    </>
  );
};

export default Navbar;
