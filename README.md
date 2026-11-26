# NutriExpert

App Android de terreno para el **cálculo nutricional de cultivos**: el agricultor o asesor registra un lote, calcula la demanda de nutrientes (N, P₂O₅, K₂O, CaO, MgO) y guarda el registro aunque no tenga señal. Cuando vuelve la conexión, los registros se sincronizan solos con los microservicios.

> Los valores de los cultivos son **de demostración**, no recomendaciones agronómicas. Todos los usuarios y claves de ejemplo son **ficticios**.

**Stack:** Kotlin · Jetpack Compose (Material 3) · MVVM con StateFlow · Room · Retrofit + Gson · MockK + Kover
**Backend:** [nutriexpert-backend](https://github.com/MagePower91/nutriexpert-backend) (Spring Boot 3, Java 17, H2)

---

## 1. Nombre del proyecto

**NutriExpert** — Desarrollo de Aplicaciones Móviles (DSY1105), Duoc UC.

## 2. Integrantes

| Integrante | Aportes principales |
|---|---|
| Ana Vidal Salva | App Android: pantallas y navegación, API externa del clima, edición de registros (app + `PUT` en ms-registros), pruebas unitarias |
| Dennis Fraser | Microservicios Spring Boot (ms-usuarios, ms-catalogo, ms-registros), integración de la app con los microservicios, scripts de despliegue en EC2 |

## 3. Funcionalidades

| Funcionalidad | Detalle |
|---|---|
| Registro e inicio de sesión | Formularios validados. Registro contra **ms-usuarios** (necesita señal); login con señal contra el servidor y sin señal contra la copia en Room. Clave guardada con hash (BCrypt en el servidor, PBKDF2 en el teléfono). |
| Home | Saludo, indicador **En línea / Sin conexión**, tarjeta del **clima del día**, últimos registros y botón **Sincronizar** cuando hay pendientes. |
| Clima en terreno (API externa) | Temperatura, humedad, lluvia y demanda de riego (ET₀) de **Open-Meteo**. Se guarda el clima del día: sin señal se muestran los datos guardados con su hora. |
| Nuevo registro de terreno | Cultivo, lote, meta de rendimiento y eficiencia de riego (validados), **foto foliar** con la cámara (opcional) y cálculo de nutrientes. Los cultivos vienen de **ms-catalogo** (o su copia local). |
| Historial | Lista de registros con estado **Pendiente / Sincronizado**. Permite **editar** (lote, meta, eficiencia; recalcula los nutrientes) y **borrar**. |
| Trabajo sin conexión | Todo se guarda primero en **Room**. La sincronización con **ms-registros** se dispara al abrir la app, al guardar, al editar, al volver la señal y con el botón Sincronizar. |
| CRUD con microservicios | Crear (`POST`), listar (`GET`), actualizar (`PUT`) y borrar (`DELETE`) registros en ms-registros. |

### Arquitectura (MVVM)

```
app/src/main/java/cl/duoc/nutriexpert/
├── ui/           Pantallas Compose (sin estado propio: reciben datos y avisan acciones)
├── viewmodels/   Estado de cada pantalla con StateFlow (Auth, Registros, CalculoNutricional, Clima, Main)
├── model/        UiState de cada pantalla
├── data/         Repositories (Auth, Catalogo, Sincronizacion, Clima), Room (entidades y DAOs), caché del clima
├── data/remote/  Retrofit: RetrofitInstance, ApiServices, DTOs y Conectividad
├── domain/       Lógica pura: cálculo nutricional, validación y recálculo al editar
└── navigation/   Rutas y eventos de navegación
```

## 4. Endpoints utilizados

### API externa: Open-Meteo (HTTPS, sin clave)

| Método | URL | Uso |
|---|---|---|
| GET | `https://api.open-meteo.com/v1/forecast?latitude=-32.88&longitude=-71.25&current=temperature_2m,relative_humidity_2m,precipitation&daily=et0_fao_evapotranspiration,precipitation_sum&timezone=auto&forecast_days=1` | Clima del día en Quillota (coordenadas de ejemplo) |

Se consume con Retrofit en `data/remote/ClimaApiService.kt`. No se mezcla con los datos propios: tiene su propio caché y no pasa por los microservicios.

### Microservicios propios (HTTP)

| Servicio | Puerto | Método | Ruta | Respuesta | Uso en la app |
|---|---|---|---|---|---|
| ms-usuarios | 8083 | POST | `/api/usuarios/registro` | 201 · 400 · 409 | Crear cuenta |
| ms-usuarios | 8083 | POST | `/api/usuarios/login` | 200 · 401 | Iniciar sesión |
| ms-catalogo | 8081 | GET | `/api/cultivos` | 200 | Lista de cultivos del formulario |
| ms-registros | 8082 | POST | `/api/registros` | 201 · 200 si ya existía · 400 | Enviar un registro nuevo |
| ms-registros | 8082 | PUT | `/api/registros/{id}` | 200 · 400 · 404 | Enviar un registro editado |
| ms-registros | 8082 | DELETE | `/api/registros/{id}` | 204 · 404 | Borrar un registro ya enviado |

Los errores responden `{ "error": "mensaje" }`. El detalle de cada microservicio (entidades, reglas, Postman) está en el README de [nutriexpert-backend](https://github.com/MagePower91/nutriexpert-backend).

## 5. Pasos para ejecutar

### Requisitos
- Android Studio (con un emulador, por ejemplo Pixel 8, o un teléfono con Android 8.0 o superior).
- JDK 17 o superior para los microservicios.

### 5.1 Levantar los microservicios
```bash
git clone https://github.com/MagePower91/nutriexpert-backend.git
cd nutriexpert-backend
bash levantar-local.sh
```
Comprobar: `curl http://localhost:8081/api/cultivos` devuelve los 3 cultivos. Para EC2, seguir la sección *Despliegue en AWS Academy* del README del backend.

### 5.2 Apuntar la app al servidor
1. `app/src/main/java/cl/duoc/nutriexpert/data/remote/RetrofitInstance.kt` → constante `HOST`:
   - `10.0.2.2` si la app corre en el emulador y los servicios en el mismo computador.
   - La **Elastic IP** de la instancia EC2 para la demo.
2. `app/src/main/res/xml/network_security_config.xml` → la misma IP. Los microservicios usan HTTP y Android solo lo permite hacia las direcciones listadas ahí.

### 5.3 Ejecutar la app
1. Abrir la carpeta del proyecto en Android Studio y esperar la sincronización de Gradle.
2. Elegir el emulador y presionar **Run ▶**.
3. Registrarse o iniciar sesión (ms-usuarios crea el usuario ficticio `demo@nutriexpert.cl`, ver README del backend).

### 5.4 Pruebas unitarias y cobertura
```bash
./gradlew testDebugUnitTest
./gradlew koverHtmlReportDebug
```
- Pruebas con **JUnit 4 + MockK + kotlinx-coroutines-test** sobre Repositories y ViewModels: se simulan Retrofit, Room y la red, así no necesitan internet ni emulador.
- Reporte de cobertura: `app/build/reports/kover/htmlDebug/index.html` (**más del 80 %** de la lógica). Se excluyen las pantallas Compose, Room, Retrofit y la Activity, que requieren un dispositivo.

## 6. APK firmado y llave (.jks)

<!-- Reemplazar por las capturas reales: docs/apk-firmado.png y docs/llave-jks.png -->
_Pendiente: agregar captura del APK firmado y de la llave `.jks`._

Pasos para generarlo en Android Studio:
1. **Build → Generate Signed App Bundle or APK… → APK**.
2. **Create new…** para crear la llave `.jks` (o elegir una existente) con su alias y contraseñas.
3. Variante **release** → **Create**. El APK queda en `app/release/app-release.apk`.

> La llave `.jks` y sus contraseñas **no se suben al repositorio** (están en `.gitignore`). Se entregan aparte según las instrucciones del docente.
