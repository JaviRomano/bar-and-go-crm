# Cambios aplicados y motivos

Registro de la revisión previa a la publicación del repositorio: qué se cambió en backend, frontend y configuración, y por qué. Parte de una auditoría inicial (secretos, archivos versionables, código muerto, convenciones y documentación) y de las decisiones tomadas a partir de ella.

## Resumen

| Área | Antes | Después |
|---|---|---|
| Autenticación | Sin autenticación real: el frontend comparaba contraseñas hardcodeadas y generaba un token falso | JWT firmado (HS256) emitido por el backend y validado por Spring Security |
| Autorización | `permitAll()` en toda la API; cualquiera podía crear un ADMIN | Reglas por rol y comprobaciones de propiedad en pedidos y reservas |
| Pedidos | El cliente enviaba el precio y el total se calculaba con él | El cliente envía producto, cantidad y método de pago; el precio sale de la carta |
| Secretos | Contraseña de BD en `application.properties` | Variables de entorno (`.env`, no versionado) |
| Tests | 1 test que necesitaba una BD en marcha | 44 tests: unitarios, reglas de seguridad e integración con Testcontainers |
| Frontend | 20 errores de lint; el formulario de reservas fallaba al enviar | Lint limpio, contrato de API nuevo y un recorrido de 21 casos verificado en un despliegue |

## Contrato de la API: cambios incompatibles

| Antes | Ahora | Motivo |
|---|---|---|
| Sin login en la API | `POST /api/auth/login`, `/register` y `GET /api/auth/me` | Autenticación real |
| `POST /api/orders` con `userId`, `productName`, `unitPrice` | `{ timeTakeAway, paymentMethod, items: [{ productId, quantity }] }` | Impedir que el cliente fije precios o haga pedidos a nombre de otro |
| Enums en español (`CLIENTE`, `CREADO`, `PENDIENTE`...) | En inglés (`CUSTOMER`, `CREATED`, `PENDING`...) | Coherencia con un código escrito en inglés |
| `GET /api/orders/user/{id}` usado por el cliente | `GET /api/orders/me` | El usuario sale del token, no de un parámetro manipulable |
| `GET /api/reservations` usado por el cliente | `GET /api/reservations/me` | El cliente descargaba todas las reservas (fuga de datos) |
| Respuestas de pedido sin método de pago | Incluyen `paymentMethod` | El método de pago se guarda |

## Backend

### Secretos y configuración

| Cambio | Motivo |
|---|---|
| Credenciales de BD y clave JWT leídas de variables de entorno; `.env.example` como plantilla | La contraseña estaba en texto plano en un archivo versionado y habría quedado en el historial de git |
| `spring.config.import` de `.env` (raíz o `backend/`) | Arranque local sin exportar variables a mano; las del sistema siguen teniendo prioridad |
| Perfiles `dev`, `prod` y `test` | Antes había `spring.profiles.active=prod` sin archivo `prod`, logs de depuración y `ddl-auto=update` activos siempre |
| `prod`: `ddl-auto=validate` y sin detalles internos en los errores | En producción no se modifica el esquema ni se exponen mensajes internos |
| `DataInitializer` solo con el perfil `dev`; contraseñas de demo configurables | Se ejecutaba en cualquier entorno y creaba un ADMIN con contraseña conocida. Las credenciales de demo se mantienen porque son intencionadas y están documentadas |
| Eliminada la configuración de consola H2, el dialecto explícito y un nivel de log que apuntaba a un paquete inexistente | Configuración muerta o redundante |
| `spring.jpa.open-in-view=false` | Evita consultas perezosas fuera de la transacción; las conversiones a DTO ya se hacen en los servicios |

### Autenticación y autorización

| Cambio | Motivo |
|---|---|
| JWT con `spring-boot-starter-oauth2-resource-server` (Nimbus) en lugar de jjwt | Spring valida la firma, la caducidad y el emisor, sin un filtro propio que mantener. Se retira la dependencia jjwt, que estaba declarada pero no se usaba |
| HS256 con clave de al menos 256 bits; si es más corta, la aplicación no arranca | Una sola aplicación emite y valida tokens; una clave corta es inaceptable. Si hubiera más servicios, convendría RS256 |
| Claims `sub` (email), `uid` y `role`; el rol se traduce a `ROLE_*` | Permite reglas `hasRole` e identificar al usuario sin consultar la BD |
| El registro público siempre crea `CUSTOMER` | Antes cualquiera podía darse de alta como ADMIN enviando `role` |
| Mismo mensaje para email inexistente y contraseña errónea | No revelar qué cuentas existen |
| Reglas por ruta centralizadas en `SecurityConfig`, con `{id:\d+}` en las rutas con identificador | Que `/api/orders/upcoming` no encaje en la regla de `/api/orders/{id}` y quede abierto a clientes |
| Comprobación de propiedad en el servicio (pedido por id y cancelación de reserva) | La regla de ruta no sabe de quién es el recurso |
| CORS centralizado y configurable (`CORS_ALLOWED_ORIGINS`) | Había `@CrossOrigin("*")` repetido en cada controller |
| Sesión `STATELESS` y CSRF desactivado | Con un token Bearer en la cabecera y sin cookies de sesión, CSRF no aplica |

### Pedidos

| Cambio | Motivo |
|---|---|
| Nombre y precio de cada línea se copian de la carta al crear el pedido | Manipulación de precios: el total se calculaba con el precio que enviaba el cliente |
| La copia se guarda en la línea de pedido y no como referencia | Un cambio posterior de precio en la carta no debe alterar pedidos pasados |
| Se rechazan productos no disponibles | El cliente podía pedir productos retirados de la carta |
| Método de pago (`CARD`, `CASH`) obligatorio y persistido | La interfaz lo pedía, pero se descartaba en el servidor |
| Nuevos endpoints `GET /orders/me?status=`, `GET /orders?from=&to=`, `GET /orders/upcoming` | Exponen métodos de repositorio que existían sin uso y tienen caso de uso (historial propio, informes por fechas, vista de cocina) |
| `ItemOrder` renombrado a `OrderItem` (tabla `order_items`) | Nombre idiomático en inglés |

### Reservas

| Cambio | Motivo |
|---|---|
| El horario (12-16 h, 20-23 h, martes cerrado) está en `ReservationSchedule` y se aplica también al modificar | Con un PUT se podía mover una reserva a un martes o fuera de horario |
| `GET /reservations/me` (asociación por teléfono) | La pantalla del cliente necesitaba sus reservas sin descargar las de todos |
| Cancelación por el cliente (`PATCH /{id}/cancel`) solo si la reserva es suya | Caso de uso real; antes solo podía el admin |
| Nuevos `GET /reservations?from=&to=` y `/upcoming?status=` | Métodos de repositorio sin uso con caso de uso (vista de agenda) |
| Eliminado `findByCategory` de productos | No tenía caso de uso: la carta pública filtra disponibles y el admin usa el listado completo |

### Manejo de errores

| Cambio | Motivo |
|---|---|
| 401, 403 y 409 con el formato de error común | Las excepciones de seguridad y de integridad acababan en 500 |
| Las excepciones de Spring MVC conservan su estado (404, 405...) | Una ruta inexistente o un método no soportado devolvían 500 |
| JSON mal formado o enum inválido: 400 | Antes, 500 |
| Cast seguro a `FieldError` y control de `null` en el tipo requerido | Posibles `ClassCastException` y `NullPointerException` dentro del propio manejador |

### Convenciones y limpieza

| Cambio | Motivo |
|---|---|
| Paquete `com.barandgo.crm`, groupId `com.barandgo`, artifactId `crm-backend` | Nombres incoherentes entre sí y con el repositorio |
| Enums en inglés sin etiqueta; las etiquetas visibles están en el frontend | El constructor recibía una etiqueta y la descartaba |
| Eliminados los logs de cada petición en los controllers, los de lectura y los que incluían emails | Duplicaban el log de los servicios y registraban datos personales |
| Eliminados los `@Autowired` redundantes, los imports wildcard y los constructores con argumentos que no se usaban | Limpieza y coherencia |
| Se mantienen el constructor vacío, los getters/setters y `removeItem` | Compatibilidad con JPA y Jackson; `removeItem` se usará cuando el proyecto crezca hacia ERP/CRM |
| `@Valid` en el argumento de tipo (`List<@Valid ...>`) | La forma anterior está obsoleta en Hibernate Validator 9 y generaba un aviso |

### Versiones

| Cambio | Motivo |
|---|---|
| Spring Boot 3.2.0 a 4.1.1 | La línea 3.x ya no tiene soporte OSS |
| Starters de Boot 4 (`webmvc`, slices de test por módulo) y Testcontainers 2 | Requisitos de la nueva versión |

### Tests

| Suite | Cubre |
|---|---|
| `OrderServiceImplTest` | Precio tomado de la carta, producto no disponible, acceso a pedidos ajenos, rango de fechas invertido |
| `ReservationServiceImplTest` | Horario al modificar y cancelación propia y ajena |
| `AuthServiceImplTest` | Login correcto e incorrecto, mensaje uniforme, registro siempre como CUSTOMER |
| `TokenServiceTest` | Claims, firma con otra clave, otro emisor y clave corta |
| `ReservationScheduleTest` | Límites del horario y el martes |
| `SecurityRulesTest` | Rutas públicas y protegidas, 401 y 403 por rol, CORS |
| `BarAndGoCrmApplicationTests` | Flujo completo contra PostgreSQL con Testcontainers; se omite si no hay Docker |

El test original (`contextLoads`) necesitaba un PostgreSQL local, así que `mvnw verify` fallaba en cualquier máquina sin esa BD.

## Frontend

### Autenticación y sesión

| Cambio | Motivo |
|---|---|
| Login real contra `/api/auth/login`; token, caducidad y usuario en `session.js` | El login era simulado: descargaba todos los usuarios y comparaba contraseñas escritas en el código |
| Sesión caducada o 401: se cierra la sesión y se redirige al login sin avisos intermedios | Antes aparecía un `alert` de error justo antes de la redirección |
| Un 401 del propio login no redirige | Recargaba la página y se perdía el mensaje "Email o contraseña incorrectos" |
| El carrito se borra al cerrar sesión | Pasaba al siguiente usuario del mismo navegador |
| Accesos rápidos de demo solo en desarrollo o con `VITE_SHOW_DEMO_LOGIN=true` | Las cuentas de demo solo existen con el perfil `dev` del backend |
| `AuthContext` separado en provider, contexto y hook; estado inicial síncrono | Tres errores de lint (Fast Refresh, setState dentro de un efecto, try/catch inútil) |

### Adaptación al contrato y funcionalidad

| Cambio | Motivo |
|---|---|
| Checkout envía `productId`, `quantity` y `paymentMethod`; el modal muestra el total del servidor | Nuevo contrato de pedidos |
| "Mis Pedidos" y la lista de admin muestran el método de pago | El dato ya se guarda |
| "Mis Pedidos" y "Mis Reservas" usan `/me` | Nuevo contrato; "Mis Reservas" filtraba en el navegador todas las reservas de todos los clientes |
| Botón "Cancelar reserva" con confirmación en "Mis Reservas" | Nueva funcionalidad; solo aparece en reservas propias, futuras y no canceladas |
| El formulario de reserva precarga nombre y teléfono del usuario | Las reservas se asocian por teléfono; un teléfono distinto dejaría la reserva fuera de "Mis Reservas" |
| Valores de enums en inglés y etiquetas en español (incluido el rol de la navbar) | Nuevo contrato, sin cambiar lo que ve el usuario |

### Corrección de errores

| Error | Corrección |
|---|---|
| `ReservationForm` usaba variables sin definir: fallaba al crear una reserva | Estado, navegación e import del modal añadidos |
| Los errores de negocio del backend no se mostraban | El backend siempre envía `validationErrors`, aunque sea vacío; ahora se tratan como errores de campo solo si traen contenido |
| Fechas mínimas y "hoy" calculados en UTC | `utils/dates.js` trabaja en hora local; cerca de medianoche el día era incorrecto |
| Horas mostradas como `21:00:00` | Formato `HH:mm` |
| El enlace "Reservar" de la navbar abría "Mis Reservas" | Apunta a `/cliente/reservations/new` |

### Limpieza y configuración

| Cambio | Motivo |
|---|---|
| Eliminados `App.css`, `assets/react.svg`, `src/index.html` y `pages/ClientDashboard.jsx` | Sin uso. `src/index.html` era un duplicado que Vite no usa; sus metadatos útiles pasan al `index.html` real |
| `NAvbar.jsx` renombrado a `Navbar.jsx` | Errata en el nombre |
| `VITE_API_BASE_URL` y `frontend/.env.example` | La URL de la API estaba escrita en el código |
| `package.json`: nombre del proyecto y `engines` de Node | Nombre heredado de la plantilla y requisito de Vite 7 sin documentar |
| README propio del frontend | Era el de la plantilla de Vite |
| Eliminados los emojis de la interfaz; los que marcaban el estado de pedidos y reservas pasan a ser iconos de `lucide-react` | Presentación sobria y coherente con el resto de la interfaz, que ya usaba esa librería de iconos |
| Proxy de `/api` y hosts externos admitidos (`VITE_ALLOWED_HOSTS`) en el servidor de Vite; el proxy elimina la cabecera `Origin` | Permite publicar la aplicación con un único túnel (frontend y API en el mismo origen), sin exponer el backend ni abrir CORS. Sin quitar `Origin`, el backend rechazaría las peticiones reenviadas por CORS |
| Globales de Node para los archivos `*.config.js` en ESLint | `vite.config.js` se ejecuta en Node; con los globales de navegador, `process` daba error de lint |

## Repositorio y entorno

| Cambio | Motivo |
|---|---|
| `.gitignore` raíz con Node/Vite, `.env*` (excepto `.env.example`), IDE y Maven | Solo cubría Java; `node_modules`, `dist` y `.env` podían acabar versionados |
| `docker-compose.yml` con PostgreSQL 17, healthcheck y puerto configurable | No existía, aunque la configuración contaba con él |
| `backend/mvnw` con bit ejecutable en git | Clonado en Linux o en CI, `./mvnw` fallaba por permisos |
| Node.js 22.11 a 22.23.2 en el entorno local | Vite 7 requiere 22.12 o superior |
| README reescrito, guía de arranque y este documento | El README original tenía una línea |

## Verificación

- Backend: `./mvnw verify` con 44 tests y 0 fallos, incluida la integración contra PostgreSQL 17.
- Frontend: `npm run lint` sin errores ni avisos y `npm run build` correcto.
- Despliegue local de producción (jar con `prod`, build de Vite servido con `vite preview` y PostgreSQL en Docker): recorrido de 21 casos en Chrome, todos correctos, sin errores ni avisos en el log del backend. Se ejecutó dos veces: con la API en URL absoluta (CORS) y a través del proxy de `/api`, este último con el CORS del backend en su valor por defecto.
- Configuración para el túnel, comprobada con la cabecera `Host`: un dominio `*.trycloudflare.com` se admite (200) y uno desconocido se rechaza (403). Un login con el `Origin` del túnel funciona a través del proxy y se rechaza si va directo al backend.

## Pendiente

- **Migraciones versionadas (Flyway).** `prod` valida el esquema pero no lo crea.
- **Relación `Reservation` a `User`.** Hoy la asociación es por teléfono; si el formato difiere, la reserva no aparece en "Mis Reservas".
- **Token en `localStorage`.** Queda expuesto ante un XSS. La alternativa más robusta es una cookie `HttpOnly` emitida por el backend.
