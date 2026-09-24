# Guía de arranque

Pasos para poner en marcha Bar&Go CRM en local, ejecutar los tests y hacer un despliegue de prueba. Para la descripción del proyecto y de la API, ver el [README](../README.md).

## 1. Requisitos

| Herramienta | Versión | Comprobación |
|---|---|---|
| JDK | 17 o superior | `java -version` |
| Node.js | 20.19+ o 22.12+ (requisito de Vite 7) | `node -v` |
| Docker | Docker Desktop o Docker Engine con Compose v2 | `docker compose version` |

Maven no hace falta instalarlo: el proyecto incluye el wrapper (`backend/mvnw`).

## 2. Variables de entorno

```bash
cp .env.example .env
```

Hay que editar `.env` y dar valor como mínimo a:

| Variable | Qué poner |
|---|---|
| `DB_PASSWORD` | Cualquier contraseña; la usa el contenedor de PostgreSQL al crearse |
| `JWT_SECRET` | Clave aleatoria en Base64 de al menos 32 bytes |
| `DB_PORT` | `5432`, o `5433` si ya hay un PostgreSQL instalado en local |

Para generar `JWT_SECRET`:

```bash
# Linux, macOS o Git Bash
openssl rand -base64 32
```

```powershell
# PowerShell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

`.env` está en `.gitignore` y no debe subirse. El backend lo lee directamente, tanto desde la raíz como desde `backend/`.

## 3. Base de datos

```bash
docker compose up -d --wait
```

Levanta PostgreSQL 17 con los datos de `.env` y espera a que el healthcheck responda. Los datos persisten en el volumen `postgres-data`. Para empezar desde cero:

```bash
docker compose down -v
```

## 4. Backend

```bash
cd backend
./mvnw spring-boot:run          # En Windows (cmd/PowerShell): mvnw.cmd spring-boot:run
```

Arranca en `http://localhost:8080` con el perfil `dev`, que:

- crea o actualiza el esquema automáticamente;
- si la base de datos está vacía, carga 4 usuarios y 33 productos de demo;
- registra en el log las sentencias SQL.

Comprobación rápida:

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bar.com","password":"admin123"}'
```

Debe devolver un JSON con `accessToken`.

## 5. Frontend

```bash
cd frontend
npm install
npm run dev
```

Abrir `http://localhost:5173`. En desarrollo, la pantalla de login muestra accesos rápidos con las cuentas de demo:

| Rol | Email | Contraseña |
|---|---|---|
| Admin | admin@bar.com | admin123 |
| Cliente | ripleynostromo@example.com | cliente123 |

Si la API no está en `http://localhost:8080/api`, crear `frontend/.env.local` con `VITE_API_BASE_URL` (ver `frontend/.env.example`).

## 6. Tests y calidad

```bash
cd backend && ./mvnw verify           # 44 tests: unitarios, seguridad e integración
cd frontend && npm run lint && npm run build
```

Los tests de integración usan Testcontainers y necesitan Docker en marcha. Sin Docker se omiten, y el resto de la suite se ejecuta igual.

## 7. Despliegue de prueba (perfil prod)

Reproduce en local un despliegue de producción: jar ejecutable, build estático del frontend y perfil `prod`.

```bash
# 1. Base de datos limpia
docker compose down -v && docker compose up -d --wait

# 2. Jar del backend
cd backend
./mvnw clean package

# 3. Crear el esquema (prod solo lo valida). Arrancar con dev, esperar a
#    "Datos de demo cargados" en el log y parar con Ctrl+C
SPRING_PROFILES_ACTIVE=dev java -jar target/crm-backend-0.0.1-SNAPSHOT.jar

# 4. Arrancar en prod, permitiendo el origen del preview de Vite
SPRING_PROFILES_ACTIVE=prod CORS_ALLOWED_ORIGINS=http://localhost:4173 \
  java -jar target/crm-backend-0.0.1-SNAPSHOT.jar

# 5. En otra terminal: build de producción del frontend y servidor estático
cd frontend
VITE_SHOW_DEMO_LOGIN=true npm run build
npm run preview                      # http://localhost:4173
```

En PowerShell, las variables se definen antes del comando: `$env:SPRING_PROFILES_ACTIVE="prod"; java -jar ...`.

En Git Bash (Windows), un valor que empieza por `/`, como `VITE_API_BASE_URL=/api`, se convierte en una ruta de Windows (`C:/Program Files/Git/api`) y el frontend deja de encontrar la API. Hay que anteponer `MSYS_NO_PATHCONV=1` o usar PowerShell.

### Recorrido de verificación manual

1. Abrir `/cliente/orders` sin sesión: redirige a `/login`.
2. Login con una contraseña incorrecta: aparece "Email o contraseña incorrectos".
3. Entrar como cliente, añadir dos productos, abrir el carrito y pulsar "Proceder al Pedido".
4. Elegir fecha, hora y método de pago y confirmar. El modal muestra el total calculado por el servidor y el método de pago.
5. "Mis Pedidos" muestra el pedido en estado Creado y el método de pago.
6. Menú > Reservar: nombre y teléfono aparecen precargados. Un martes se rechaza; otro día se registra como Pendiente.
7. Con el cliente, abrir `/admin`: redirige a `/cliente`.
8. Cerrar sesión, entrar como admin, avanzar el pedido a "En Preparación" y confirmar la reserva.
9. Volver como cliente: el pedido aparece "En Preparación" y la reserva "Confirmada". Cancelar la reserva desde "Mis Reservas".
10. Manipular el token en `localStorage` (clave `session`) y recargar: la API responde 401 y la aplicación vuelve al login.

## 8. Acceso desde otro equipo por internet

Para probar la aplicación desde un móvil u otro ordenador fuera de la red local, se publica el frontend con un túnel. El servidor de preview de Vite reenvía `/api` al backend, así que basta un solo túnel: el backend (puerto 8080) no se expone y no hace falta tocar CORS.

```
navegador remoto --HTTPS--> túnel --> vite preview :4173 --/api--> backend :8080 --> PostgreSQL
```

Se usa Cloudflare Quick Tunnel, que no requiere cuenta. Instalación en Windows:

```powershell
winget install --id Cloudflare.cloudflared
```

Pasos (PowerShell, desde la raíz del repositorio; backend y base de datos preparados como en el apartado 7):

```powershell
# Terminal 1: backend en prod (CORS no necesita cambios: el navegador solo habla con el preview)
cd backend
$env:SPRING_PROFILES_ACTIVE="prod"; java -jar target/crm-backend-0.0.1-SNAPSHOT.jar

# Terminal 2: frontend con la API en ruta relativa y servidor admitiendo el dominio del túnel
cd frontend
$env:VITE_API_BASE_URL="/api"; $env:VITE_SHOW_DEMO_LOGIN="true"; npm run build
$env:VITE_ALLOWED_HOSTS=".trycloudflare.com"; npm run preview

# Terminal 3: túnel
cloudflared tunnel --url http://localhost:4173
```

`cloudflared` muestra una URL del tipo `https://<nombre-aleatorio>.trycloudflare.com`. Esa URL se abre en el otro dispositivo, y el recorrido del apartado 7 sirve igual.

Consideraciones:

- La URL es pública mientras el túnel esté activo, y las credenciales de demo también lo son. Hay que cerrar el túnel (Ctrl+C) al terminar.
- Cada ejecución de `cloudflared` genera una URL nueva. No hace falta recompilar: `VITE_ALLOWED_HOSTS=.trycloudflare.com` admite cualquier subdominio.
- Con ngrok el procedimiento es el mismo: `ngrok http 4173` y `VITE_ALLOWED_HOSTS=.ngrok-free.app`.
- Si el backend no está en `localhost:8080`, se indica con `API_PROXY_TARGET` al arrancar el preview.

## 9. Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `docker compose up` falla con el puerto 5432 en uso | Hay un PostgreSQL instalado en local | `DB_PORT=5433` en `.env` |
| El backend no arranca: error de validación en `app.jwt` o "al menos 256 bits" | `JWT_SECRET` vacío o demasiado corto | Generar la clave como en el paso 2 |
| El backend no arranca: `Could not resolve placeholder 'DB_NAME'` | No encuentra `.env` | Ejecutarlo desde `backend/` o desde la raíz, o exportar las variables |
| Con `prod`: `Schema validation: missing table` | `prod` no crea el esquema | Arrancar una vez con `dev` (paso 7.3) |
| Error de enum al leer datos (`CLIENTE`, `CREADO`...) | Base de datos anterior a los enums en inglés | `docker compose down -v` |
| El navegador bloquea las peticiones por CORS | El origen del frontend no está permitido | Añadirlo a `CORS_ALLOWED_ORIGINS` (separado por comas) |
| `mvnw package` falla en Windows con "Unable to rename" | El jar lo tiene abierto un backend en ejecución | Parar el backend antes de empaquetar |
| Vite avisa de la versión de Node | Node por debajo de 20.19 / 22.12 | Actualizar Node |
| El login muestra "No se puede conectar con el servidor" con `VITE_API_BASE_URL=/api` | Git Bash convirtió `/api` en una ruta de Windows | Recompilar con `MSYS_NO_PATHCONV=1` o desde PowerShell |
| La URL del túnel responde "Blocked request. This host is not allowed" | El dominio del túnel no está en `VITE_ALLOWED_HOSTS` | Arrancar el preview con `VITE_ALLOWED_HOSTS=.trycloudflare.com` |
