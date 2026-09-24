# Bar&Go CRM

Aplicación de gestión para un bar-restaurante: carta, pedidos para recoger, reservas de mesa y clientes. Monorepo con una API REST en Spring Boot y un cliente web en React.

| Capa | Tecnología |
|---|---|
| Backend | Java 17, Spring Boot 4.1, Spring Security (OAuth2 Resource Server, JWT HS256), Spring Data JPA |
| Base de datos | PostgreSQL 17 (docker-compose) |
| Frontend | React 19, Vite 7, React Router 7 |
| Tests | JUnit 5, Mockito, MockMvc, Testcontainers |

```
bar-and-go-crm/
├── backend/             API REST (Maven)
├── frontend/            Cliente web (Vite)
├── docker-compose.yml   PostgreSQL para desarrollo
└── .env.example         Variables de entorno
```

Documentación adicional: [guía de arranque](docs/GUIA-ARRANQUE.md) (paso a paso, despliegue de prueba y problemas frecuentes) y [registro de cambios y motivos](docs/CAMBIOS.md).

## Puesta en marcha

Requisitos: JDK 17+, Node 20.19+ o 22.12+ (requisito de Vite 7), Docker.

```bash
cp .env.example .env
# Rellenar DB_PASSWORD y JWT_SECRET (openssl rand -base64 32)
# Si ya hay un PostgreSQL local en 5432, poner DB_PORT=5433

docker compose up -d

cd backend
./mvnw spring-boot:run        # http://localhost:8080

cd ../frontend
npm install
npm run dev                   # http://localhost:5173
```

El backend lee el `.env` de la raíz (o de `backend/`). Las variables del sistema tienen prioridad, que es lo que se usa en despliegue.

### Perfiles

| Perfil | Uso | Esquema | Datos de demo |
|---|---|---|---|
| `dev` (por defecto) | Desarrollo local | `ddl-auto=update` | Sí, si la BD está vacía |
| `prod` | Despliegue (`SPRING_PROFILES_ACTIVE=prod`) | `ddl-auto=validate` | No |
| `test` | Suite de tests | `create` sobre contenedor efímero | No |

### Despliegue

```bash
# Backend: jar ejecutable
cd backend
./mvnw clean package
SPRING_PROFILES_ACTIVE=prod \
CORS_ALLOWED_ORIGINS=https://tu-dominio \
java -jar target/crm-backend-0.0.1-SNAPSHOT.jar

# Frontend: estáticos en dist/, servibles por cualquier servidor web con fallback a index.html
cd frontend
VITE_API_BASE_URL=https://tu-dominio/api npm run build
```

Con `prod` el esquema tiene que existir de antemano: si la base de datos está vacía, el arranque falla en la validación de Hibernate. Mientras no haya migraciones versionadas, basta con arrancar una vez con `dev` para crear el esquema (y cargar los datos de demo si se quieren).

### Credenciales de demo

Solo existen con el perfil `dev`. Son públicas a propósito para poder probar la aplicación:

| Rol | Email | Contraseña |
|---|---|---|
| ADMIN | admin@bar.com | admin123 |
| CUSTOMER | ripleynostromo@example.com | cliente123 |

Se pueden cambiar con `DEMO_ADMIN_PASSWORD` y `DEMO_CUSTOMER_PASSWORD`.

## Autenticación

`POST /api/auth/login` devuelve un token de acceso:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "user": { "id": 1, "name": "Admin Demo", "email": "admin@bar.com", "role": "ADMIN" }
}
```

El resto de peticiones lo envían en la cabecera `Authorization: Bearer <token>`. El token lleva el email (`sub`), el id (`uid`) y el rol (`role`). Está firmado con HS256 y caduca según `JWT_EXPIRATION` (2 horas por defecto). No hay refresh token: al caducar, la API responde 401 y hay que volver a iniciar sesión.

El registro público (`POST /api/auth/register`) siempre crea usuarios `CUSTOMER`; si el cuerpo incluye un rol, se ignora. Los administradores se crean desde `/api/users`, que solo es accesible para ADMIN.

## API

Leyenda de acceso: **público** (sin token), **auth** (cualquier usuario autenticado), **ADMIN**.

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | público | Inicio de sesión |
| POST | `/api/auth/register` | público | Alta de cliente |
| GET | `/api/auth/me` | auth | Usuario autenticado |
| GET | `/api/products`, `/available`, `/{id}`, `/category/{c}` | público | Carta |
| POST, PUT, PATCH, DELETE | `/api/products/**` | ADMIN | Gestión de la carta |
| POST | `/api/orders` | auth | Crear pedido para el usuario del token |
| GET | `/api/orders/me?status=` | auth | Pedidos propios, con filtro opcional por estado |
| GET | `/api/orders/{id}` | auth | Pedido propio (ADMIN: cualquiera) |
| GET | `/api/orders?from=&to=` | ADMIN | Todos, u opcionalmente por intervalo (ISO-8601) |
| GET | `/api/orders/upcoming` | ADMIN | Pedidos con recogida futura, por hora |
| GET | `/api/orders/status/{s}`, `/user/{id}` | ADMIN | Filtros |
| PATCH | `/api/orders/{id}/status` | ADMIN | Cambiar estado |
| DELETE | `/api/orders/{id}` | ADMIN | Eliminar |
| POST | `/api/reservations` | auth | Crear reserva |
| GET | `/api/reservations/me` | auth | Reservas propias (asociadas por teléfono) |
| PATCH | `/api/reservations/{id}/cancel` | auth | Cancelar reserva propia (ADMIN: cualquiera) |
| GET | `/api/reservations?from=&to=` | ADMIN | Todas, u opcionalmente por intervalo de fechas |
| GET | `/api/reservations/upcoming?status=` | ADMIN | Desde hoy, por estado (por defecto `CONFIRMED`) |
| GET | `/api/reservations/{id}`, `/date/{d}`, `/status/{s}` | ADMIN | Consultas |
| PUT, DELETE | `/api/reservations/{id}` | ADMIN | Modificar o eliminar |
| * | `/api/users/**` | ADMIN | Gestión de usuarios |

Los errores tienen siempre la misma forma:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/orders", "validationErrors": {} }
```

### Pedidos

El cliente envía producto, cantidad y método de pago (`CARD` o `CASH`, se paga en la recogida):

```json
{ "timeTakeAway": "2026-10-01T14:30:00", "paymentMethod": "CARD", "items": [ { "productId": 1, "quantity": 2 } ] }
```

Nombre y precio se copian de la carta en el momento del pedido. Así el cliente no puede fijar el precio, y los cambios posteriores de la carta no alteran los pedidos pasados. No se aceptan productos marcados como no disponibles.

Estados: `CREATED` → `IN_PREPARATION` → `READY` → `DELIVERED`, o `CANCELLED`.

### Reservas

- Horario: de 12:00 a 16:00 y de 20:00 a 23:00. Los martes el local está cerrado.
- Entre 1 y 50 personas, siempre con fecha futura.
- Estas reglas se aplican tanto al crear como al modificar una reserva.
- Estados: `PENDING`, `CONFIRMED`, `CANCELLED`.

## Tests

```bash
cd backend
./mvnw verify
```

| Tipo | Qué cubre |
|---|---|
| Unitarios (Mockito) | Servicios de pedidos, reservas y autenticación; horario de reservas; emisión y validación de tokens |
| Slice web (`@WebMvcTest`) | Reglas de autorización por rol y ruta; CORS |
| Integración (Testcontainers) | Registro, login, pedido y control de acceso contra PostgreSQL real |

Los tests de integración necesitan Docker; si no está disponible, se omiten y el resto de la suite se ejecuta igual.

## Decisiones técnicas

- **JWT con el resource server de Spring Security** en lugar de un filtro propio con jjwt. La validación de firma, caducidad y emisor la hace el framework (Nimbus), así que el código propio se limita a emitir el token y a traducir el claim `role` a autoridades.
- **HS256 con clave simétrica**: es suficiente mientras una sola aplicación emite y valida los tokens. Si hubiera más servicios, convendría pasar a RS256 con clave pública.
- **Spring Boot 4.1**: la línea 3.x ya no tiene soporte OSS.
- **DTOs como clases con constructor vacío, getters y setters**, por compatibilidad directa con Jackson y Bean Validation, y por coherencia con las entidades JPA, que necesitan esa forma. La alternativa sería usar `record` en los DTOs de respuesta: son inmutables y reducen código, pero conviven mal con la conversión incremental que hacen ahora los servicios (`dto.setX(...)`). Queda como mejora opcional.
- **Enums en inglés**, con las etiquetas visibles resueltas en el frontend.

## Limitaciones conocidas y trabajo pendiente

- **Migraciones de esquema**: en `dev` el esquema lo genera Hibernate. El perfil `prod` valida pero no crea el esquema; falta introducir migraciones versionadas (Flyway).
- **Reservas asociadas por teléfono**: una reserva no tiene relación directa con `User`. `/api/reservations/me` compara con el teléfono del usuario, de modo que un teléfono escrito con otro formato no se asocia. Si el proyecto crece hacia ERP/CRM, lo lógico es añadir la relación `Reservation → User`.
- **BD local anterior a los cambios de esquema** (enums en inglés, `order_items`, `payment_method`): hay que recrearla (`docker compose down -v`) o migrar los datos.
- **Token en `localStorage`**: queda expuesto ante un XSS. La alternativa más robusta es una cookie `HttpOnly` emitida por el backend.

## Licencia

MIT. Ver [LICENSE](LICENSE).
