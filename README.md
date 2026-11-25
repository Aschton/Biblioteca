# 1. Museo de los Museos

App móvil Android (**Kotlin + Jetpack Compose, MVVM**) para explorar museos virtuales de Iberoamérica en un mapa, revisar un catálogo de cursos y productos, y gestionar pedidos. Se conecta a **3 microservicios Spring Boot con MySQL** y consume la **API externa Open-Meteo** para mostrar el clima en la ciudad de cada museo.

> Proyecto académico de DSY1105 (Desarrollo de Aplicaciones Móviles), Evaluación Parcial 4, Duoc UC. Todos los datos, precios, cuentas y envíos son **ficticios**.

```
MuseoDeLosMuseos/
├── android-app/                     ← abrir con Android Studio
│   ├── app/src/main/java/cl/duoc/museo/
│   │   ├── data/
│   │   │   ├── local/               ← Room: entidades, DAOs, base de datos
│   │   │   ├── remote/              ← Retrofit: un archivo por API (MuseosApi, CatalogoApi,
│   │   │   │                          PedidosApi, WeatherApi), DTOs, mappers y RetrofitClient
│   │   │   └── repository/          ← repositorios (unen Room + microservicios + API externa)
│   │   ├── ui/
│   │   │   ├── screens/             ← pantallas Compose
│   │   │   ├── components/          ← componentes reutilizables (WeatherCard, etc.)
│   │   │   ├── viewmodel/           ← un archivo por ViewModel
│   │   │   ├── navigation/          ← rutas y NavHost
│   │   │   └── theme/               ← colores y tipografía
│   │   ├── notifications/           ← notificaciones locales
│   │   └── util/                    ← validaciones, formato, cálculo de envío, QR
│   ├── app/src/test/                ← pruebas unitarias (mismas carpetas que main)
│   ├── museo-release.jks            ← llave de firma (la creas tú, ver 5.4)
│   └── release/                     ← APK firmado
└── backend/
    ├── ms-museos/   (8081)
    ├── ms-catalogo/ (8082)
    ├── ms-pedidos/  (8083)
    └── levantar-microservicios.bat
```

Cada microservicio sigue la misma arquitectura en capas:

```
src/main/java/cl/duoc/museo/<servicio>/
├── controller/   ← endpoints REST (solo reciben y responden, sin lógica)
├── service/      ← lógica de negocio y validaciones
├── repository/   ← acceso a datos (Spring Data JPA)
├── model/        ← entidades JPA (tablas MySQL)
├── dto/          ← objetos de entrada/salida (un archivo por DTO)
├── exception/    ← GlobalExceptionHandler y formato de error JSON
├── config/       ← CORS y datos de demostración (DataLoader)
└── client/       ← solo ms-pedidos: cliente HTTP hacia ms-catalogo
```

## 2. Integrantes

| Integrante | GitHub | Responsabilidades principales |
|---|---|---|
| Taylor Medolphe | [@ink616](https://github.com/ink616) | _completar_ |
| _Nombre compañero/a_ | _@usuario_ | _completar_ |

Planificación en Trello: _pegar aquí el enlace al tablero_

## 3. Funcionalidades

**Roles:** Administrador, Investigador y Cliente, cada uno con su propia navegación y permisos.

- **Inicio de sesión y registro** con validación de formularios. La sesión se guarda en el teléfono y los datos de cada usuario se mantienen separados.
- **Mapa de museos** (OpenStreetMap) con filtros por país y proyecto, y botón "mi ubicación".
- **Ficha de museo** con visita virtual, favoritos, código QR y **clima actual + pronóstico de 3 días (Open-Meteo)**.
- **CRUD de fichas de museo:** el Investigador crea y corrige fichas; el Administrador las publica, las retira o las **elimina**.
- **Catálogo** de cursos, libros, materiales y artículos, con búsqueda, filtros y validación de stock contra el backend.
- **CRUD de productos (Administrador):** crear, modificar, desactivar y **eliminar**.
- **Carrito y pedidos** con cálculo de envío ficticio (nacional, Latinoamérica o resto del mundo; estándar, express o retiro en sede). El descuento de stock lo hace `ms-pedidos` llamando a `ms-catalogo`.
- **Gestión de solicitudes (Administrador):** cambiar el estado de un pedido (el cliente recibe una notificación) o **eliminarlo**.
- **Lector QR** con CameraX + ML Kit (también desde la galería) e historial de lecturas.
- **Notificaciones** locales y avisos publicados en `ms-museos`.
- **Cámara** para la foto de perfil y las fotos de fichas y productos.
- **Modo sin conexión:** caché en Room (SQLite). Lo que se crea sin internet se sincroniza después.

## 4. Endpoints utilizados

### 4.1 API externa: Open-Meteo

API pública y gratuita, no necesita API key. Se consume con Retrofit (`data/remote/WeatherApi.kt`) desde `WeatherRepository`, y el resultado se muestra en la tarjeta **"Clima ahora en …"** de la ficha de cada museo. Sus datos **no se guardan en Room ni se envían a los microservicios**.

| Método | URL |
|---|---|
| GET | `https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lng}&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=3` |

Ejemplo (Santiago): https://api.open-meteo.com/v1/forecast?latitude=-33.43&longitude=-70.64&current=temperature_2m,weather_code&timezone=auto

### 4.2 Microservicios propios (Spring Boot 3 + JPA + MySQL)

Cada microservicio tiene **su propia base de datos** (`museo_museos`, `museo_catalogo`, `museo_pedidos`) y se comunican por REST.

**ms-museos: `http://localhost:8081`**

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/museums?country=&project=` | Lista con filtros |
| GET | `/api/museums/{id}` | Detalle |
| POST | `/api/museums` | Crear ficha |
| PUT | `/api/museums/{id}` | Actualizar ficha |
| DELETE | `/api/museums/{id}` | Eliminar ficha |
| GET | `/api/qr/{code}` | Resolver QR `MUSEO:<id>` |
| GET / POST | `/api/notices` | Listar / publicar avisos |
| DELETE | `/api/notices/{id}` | Eliminar aviso |

**ms-catalogo: `http://localhost:8082`**

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/products?category=&q=` | Catálogo con filtros |
| GET | `/api/products/{id}` | Detalle |
| POST | `/api/products` | Crear |
| PUT | `/api/products/{id}` | Actualizar |
| PATCH | `/api/products/{id}/deactivate` | Desactivar |
| DELETE | `/api/products/{id}` | Eliminar |
| GET | `/api/products/{id}/availability?qty=` | Validar stock |
| POST | `/api/products/{id}/decrease-stock` | Descontar stock (lo llama ms-pedidos) |
| GET | `/api/qr/{code}` | Resolver QR `PROD:<id>` |

**ms-pedidos: `http://localhost:8083`**

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/orders` | Crear pedido o solicitud de información (consulta precios y stock en ms-catalogo) |
| GET | `/api/orders?userEmail=` | Historial del cliente o de todos (Administrador) |
| GET | `/api/orders/{id}` | Detalle |
| PATCH | `/api/orders/{id}/status` | Cambiar estado |
| DELETE | `/api/orders/{id}` | Eliminar |
| POST | `/api/shipping/quote` | Cotizar envío |

## 5. Pasos para ejecutar

### 5.1 Requisitos
- Android Studio (incluye JDK 17+).
- MySQL 8 (XAMPP, Laragon o MySQL Server). Por defecto se usa el usuario `root` **sin clave**, como en XAMPP.
- Maven, solo si vas a levantar los microservicios por cmd (IntelliJ ya lo trae).

### 5.2 Levantar los microservicios
1. Enciende MySQL (en XAMPP: **Start** en MySQL). Las bases de datos se crean solas.
2. Si tu MySQL tiene otro usuario o clave, configúralos en la misma ventana de cmd antes de levantar:
   ```cmd
   set DB_USER=root
   set DB_PASSWORD=tu_clave
   ```
3. Levanta los tres:
   - **Opción A (cmd):** doble clic en `backend\levantar-microservicios.bat`. Se abren 3 ventanas.
   - **Opción B (IntelliJ):** abre cada carpeta `ms-*` y ejecuta su clase `Ms...Application`.
   - **Opción C (cmd, uno por uno):**
     ```cmd
     cd backend\ms-museos
     mvn spring-boot:run
     ```
4. Comprueba en el navegador:
   - http://localhost:8081/api/museums
   - http://localhost:8082/api/products
   - http://localhost:8083/api/orders

   La primera vez se cargan datos de demostración (12 museos, 8 productos).

> Levanta **ms-catalogo antes de crear pedidos**: ms-pedidos lo necesita para validar precios y stock.

### 5.3 Ejecutar la app
1. En Android Studio: **File → Open → `android-app`** y espera el *Gradle Sync*.
2. Crea un emulador (Device Manager → Pixel, API 34 o 35) y presiona **Run ▶**.
3. El emulador llega a tu PC por `10.0.2.2`, que ya está configurado.

**Teléfono físico:** en `app/build.gradle.kts`, reemplaza `10.0.2.2` por la IP de tu PC (la ves en cmd con `ipconfig`) en `MUSEOS_URL`, `CATALOGO_URL` y `PEDIDOS_URL`. Agrega esa IP en `app/src/main/res/xml/network_security_config.xml` y permite los puertos 8081 a 8083 en el Firewall de Windows.

**Cuentas de prueba:**

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@museo.cl | Admin123 |
| Investigador | investigador@museo.cl | Investiga123 |
| Cliente | cliente@museo.cl | Cliente123 |

### 5.4 Generar el APK firmado
1. **Crear la llave** (solo una vez): doble clic en `android-app\crear-llave.bat`, o en cmd:
   ```cmd
   cd android-app
   keytool -genkeypair -v -keystore museo-release.jks -alias museo -keyalg RSA -keysize 2048 -validity 10000
   ```
   Si cmd no reconoce `keytool`, usa `"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"`.
2. Copia `keystore.properties.example` como `keystore.properties` y escribe las claves que elegiste.
3. Genera el APK con doble clic en `android-app\generar-apk.bat`, o en cmd:
   ```cmd
   cd android-app
   gradlew assembleRelease
   ```
   El APK queda en `app\build\outputs\apk\release\app-release.apk`. El `.bat` además lo copia a `release\MuseoDeLosMuseos-release.apk`.
4. Verifica la firma:
   ```cmd
   "%LOCALAPPDATA%\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --print-certs release\MuseoDeLosMuseos-release.apk
   ```

La configuración de firma está en `app/build.gradle.kts` (`signingConfigs.release`). Lee `keystore.properties` para no escribir las claves directamente en el código.

> ⚠️ En un proyecto real, el `.jks` y `keystore.properties` **nunca** se suben a un repositorio público. Aquí se incluyen porque la evaluación lo pide expresamente.

### 5.5 Pruebas unitarias y cobertura
**App (122 pruebas con JUnit 4 + MockK + kotlinx-coroutines-test + MockWebServer)** sobre ViewModels, repositorios, utilidades, mapeos de red y la API externa:
```cmd
cd android-app
gradlew testDebugUnitTest
gradlew koverHtmlReportDebug
gradlew koverVerifyDebug
```
- Reporte de cobertura: `app\build\reports\kover\htmlDebug\index.html`
- `koverVerifyDebug` falla si la cobertura de la lógica baja del **80%**.
- Se mide la lógica: ViewModels, repositorios, `util` y `data.remote`. Quedan fuera la interfaz Compose, las clases que genera Room y el código de arranque.

**Microservicios (JUnit 5 + Mockito + MockMvc, con H2 en memoria, así que no necesitan MySQL):**
```cmd
cd backend\ms-pedidos
mvn test
```

## 6. Captura del APK firmado y del .jks

_Agregar aquí las capturas:_
- `docs/apk-firmado.png`: salida de `apksigner verify --print-certs`, o el APK en la carpeta `release/`.
- `docs/jks.png`: el archivo `museo-release.jks` en el proyecto, o el asistente de Android Studio.

```markdown
![APK firmado](docs/apk-firmado.png)
![Llave .jks](docs/jks.png)
```

## Arquitectura

```
UI (Compose) → ViewModel (StateFlow) → Repository ─┬─ Room (caché local, SQLite)
                                                   ├─ Retrofit → ms-museos   :8081 → MySQL museo_museos
                                                   ├─ Retrofit → ms-catalogo :8082 → MySQL museo_catalogo
                                                   ├─ Retrofit → ms-pedidos  :8083 → MySQL museo_pedidos
                                                   │                  └── REST → ms-catalogo (precios y stock)
                                                   └─ Retrofit → Open-Meteo (API externa)
```
