# Museo de los Museos — MVP Android

App móvil (Kotlin + Jetpack Compose, MVVM) con backend Spring Boot. Todos los datos, precios, cuentas y envíos son **ficticios**.

```
MuseoDeLosMuseos/
├── android-app/   ← abrir este con Android Studio
└── backend/       ← API REST Spring Boot (H2 en memoria)
```

## 1. Levantar el backend (opcional pero recomendado)

La app funciona **sin backend** (usa la caché Room/SQLite con datos de demostración), pero para mostrar la integración REST levántalo primero.

**Opción A – IntelliJ:** abre la carpeta `backend`, espera que Maven descargue dependencias y ejecuta `MuseoApiApplication`.

**Opción B – cmd** (requiere Maven y JDK 17+):

```cmd
cd backend
mvn spring-boot:run
```

Verifica en el navegador: http://localhost:8080/api/museums
Consola H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:museodb`, usuario `sa`, sin clave).

## 2. Abrir la app en Android Studio

1. **File → Open** → selecciona la carpeta `android-app`.
2. Espera el *Gradle Sync* (la primera vez descarga dependencias, tarda unos minutos). Si Android Studio ofrece actualizar AGP/Kotlin, puedes aceptar.
3. Crea un emulador (Device Manager → Pixel con API 34 o 35, con Google Play o Google APIs).
4. Presiona **Run ▶**.

El emulador llega a tu PC por `http://10.0.2.2:8080/` (ya configurado).

### Teléfono físico
1. Averigua la IP de tu PC en cmd: `ipconfig` (ej. `192.168.1.50`).
2. En `app/build.gradle.kts` cambia `API_BASE_URL` por `http://192.168.1.50:8080/`.
3. En `app/src/main/res/xml/network_security_config.xml` agrega `<domain>192.168.1.50</domain>`.
4. Permite el puerto 8080 en el Firewall de Windows.

## 3. Credenciales de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@museo.cl | Admin123 |
| Investigador | investigador@museo.cl | Investiga123 |
| Cliente | cliente@museo.cl | Cliente123 |

En la pantalla de inicio de sesión está el botón **"Usar credenciales de prueba"**. También puedes registrar nuevos clientes.

## 4. Cómo probar cada requisito

| Requisito | Dónde verlo |
|---|---|
| Navegación con barras | Barra inferior Inicio/Mapa/Catálogo/Avisos/Perfil (`ui/navigation/AppNavHost.kt`) |
| Formularios con validación | Registro, login, ficha de museo, producto, perfil (`ui/viewmodel/*`) |
| Cámara | Foto de avatar (registro/perfil), foto de ficha de museo y de producto (`rememberCameraCapture`) |
| Lectura QR | Escáner con CameraX + ML Kit, linterna, "Desde galería" (`ui/screens/QrScreens.kt`) |
| Mapas | Mapa OpenStreetMap con marcadores, filtros por país/proyecto y "mi ubicación" (`MapScreens.kt`) |
| Animaciones | Bienvenida, tarjetas del dashboard, check de QR, carrito, cambios de estado |
| SQLite / Room | `data/local/*`: usuarios, favoritos, carrito, historial QR, notificaciones, pedidos, caché |
| Datos separados por usuario | Favoritos, carrito, historial y notificaciones leídas se filtran por usuario activo (`SessionManager`) |
| Notificaciones push | **Pendiente (en desarrollo).** La pestaña Avisos y las opciones de Perfil están maquetadas; la lógica se implementará en la próxima entrega (ver comentarios `TODO (pendiente)`) |
| MVVM | `ui/viewmodel` + `data/repository` + `AppContainer` |
| REST con Retrofit | `data/remote/Api.kt` ↔ `backend/.../controller` |
| Cálculo de envío ficticio | `util/Utils.kt → ShippingCalculator` y `backend/.../ShippingService` |

### Probar el QR
- Entra como **Investigador** o **Administrador** y abre un museo o producto: abajo aparece su **código QR**.
- Escanéalo con otro teléfono que tenga la app, o toma una captura y usa **"Desde galería"** en el escáner.
- En el emulador, usa el ícono de **teclado** del escáner y escribe `MUSEO:1` o `PROD:2`.

## 5. Endpoints del backend

| Método | Ruta |
|---|---|
| GET | `/api/museums?country=&project=` · `/api/museums/{id}` |
| POST / PUT | `/api/museums` · `/api/museums/{id}` |
| GET | `/api/products?category=&q=` · `/api/products/{id}` · `/api/products/{id}/availability?qty=` |
| POST / PUT / PATCH | `/api/products` · `/api/products/{id}` · `/api/products/{id}/deactivate` |
| POST / GET | `/api/orders` · `/api/orders?userEmail=` |
| PATCH | `/api/orders/{id}/status` |
| POST | `/api/shipping/quote` |
| GET | `/api/qr/{code}` |
| GET / POST | `/api/notices` |
