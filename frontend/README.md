# Bar&Go CRM - Frontend

Cliente web en React 19 + Vite 7. La documentación general del proyecto, la API y la puesta en marcha completa están en el [README de la raíz](../README.md).

## Comandos

```bash
npm install
npm run dev       # http://localhost:5173
npm run lint
npm run build     # genera dist/
npm run preview   # sirve dist/ en http://localhost:4173
```

Requiere Node `^20.19.0` o `>=22.12.0` (requisito de Vite 7).

## Configuración

Variables en `.env.local` (ver `.env.example`):

| Variable | Por defecto | Uso |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | URL base de la API |
| `VITE_SHOW_DEMO_LOGIN` | `false` | Muestra los accesos con cuentas de demo en un build de producción (en `npm run dev` se muestran siempre) |

Variables del servidor de Vite (`npm run dev` y `npm run preview`), leídas al arrancar:

| Variable | Por defecto | Uso |
|---|---|---|
| `API_PROXY_TARGET` | `http://localhost:8080` | Destino del proxy de `/api` |
| `VITE_ALLOWED_HOSTS` | (vacío) | Hosts externos admitidos, separados por comas (p. ej. `.trycloudflare.com` para un túnel) |

Hay dos formas de conectar con la API:

- **Directa** (`VITE_API_BASE_URL` con URL absoluta): el origen del frontend tiene que estar en `CORS_ALLOWED_ORIGINS` del backend.
- **A través del proxy** (`VITE_API_BASE_URL=/api`): Vite reenvía `/api` al backend y el navegador solo habla con un origen, así que no interviene CORS. Es la opción para publicar la aplicación con un túnel (ver la [guía de arranque](../docs/GUIA-ARRANQUE.md)).

## Estructura

```
src/
├── components/
│   ├── admin/      Gestión de carta, pedidos y reservas
│   ├── auth/       Login y rutas protegidas por rol
│   ├── client/     Carta, carrito, pedido y reservas del cliente
│   └── common/     Navbar, pie y modal de confirmación
├── context/        Sesión (AuthProvider y hook useAuth)
├── pages/          Paneles y páginas de ruta
├── services/       Cliente HTTP (axios) y sesión
└── utils/          Constantes y utilidades de fecha
```

## Sesión

El login llama a `POST /api/auth/login` y guarda en `localStorage` el token, su caducidad y los datos del usuario. Cada petición envía `Authorization: Bearer <token>`. Si el token ha caducado o la API responde 401, se cierra la sesión y se redirige a `/login`. El carrito también se borra al cerrar sesión.

El token en `localStorage` queda expuesto a un posible XSS. Es un compromiso habitual en una SPA sin backend-for-frontend; la alternativa más robusta es una cookie `HttpOnly` emitida por el backend.
