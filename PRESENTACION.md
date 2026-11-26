# NutriExpert · Guía para preparar la presentación

Documento de estudio: resumen del proyecto, cómo cumple el caso y los criterios del profesor, y explicación de la materia de DSY1105 con ejemplos tomados del propio código.

---

## Índice

**Parte A · El proyecto**
0. [Aviso: Nutri-Expert vs agronutri-android](#0-aviso-nutri-expert-vs-agronutri-android)
1. [El caso](#1-el-caso)
2. [Qué hace la app (guion de la demo)](#2-qué-hace-la-app-guion-de-la-demo)
3. [Criterios del profesor y dónde están en el código](#3-criterios-del-profesor-y-dónde-están-en-el-código)
4. [Arquitectura](#4-arquitectura)
5. [Las guías del profesor y cómo se adaptaron](#5-las-guías-del-profesor-y-cómo-se-adaptaron)
6. [Qué queda fuera del MVP](#6-qué-queda-fuera-del-mvp)
7. [Preguntas probables](#7-preguntas-probables)
8. [Presentación de 20 minutos y checklist](#8-presentación-de-20-minutos-y-checklist)

**Parte B · Explicación de la materia**
9. [Kotlin: lo básico que se usa en el proyecto](#9-kotlin-lo-básico-que-se-usa-en-el-proyecto)
10. [Jetpack Compose](#10-jetpack-compose)
11. [Material 3](#11-material-3)
12. [MVVM](#12-mvvm)
13. [StateFlow y el estado de la pantalla](#13-stateflow-y-el-estado-de-la-pantalla)
14. [Corrutinas](#14-corrutinas)
15. [Formularios y validaciones](#15-formularios-y-validaciones)
16. [Navegación](#16-navegación)
17. [Room / SQLite](#17-room--sqlite)
18. [Seguridad de contraseñas](#18-seguridad-de-contraseñas)
19. [Animaciones](#19-animaciones)
20. [Recursos nativos: cámara y permisos](#20-recursos-nativos-cámara-y-permisos)
21. [Conectividad y modo offline](#21-conectividad-y-modo-offline)
22. [DataStore](#22-datastore)
23. [Retrofit y Spring Boot](#23-retrofit-y-spring-boot)
24. [Pruebas unitarias](#24-pruebas-unitarias)
25. [Gradle, versiones y APK firmado](#25-gradle-versiones-y-apk-firmado)
26. [Git y GitHub](#26-git-y-github)
27. [Privacidad: Ley 19.628 y Ley 21.719](#27-privacidad-ley-19628-y-ley-21719)
28. [Glosario rápido](#28-glosario-rápido)

---

# PARTE A · EL PROYECTO

## 0. Aviso: Nutri-Expert vs agronutri-android

La carpeta **Nutri-Expert** es una copia de **agronutri-android** tomada en el **PR #8** (27/10, 22:42). Le falta lo que se unió después:

| PR | Qué falta en Nutri-Expert |
|---|---|
| #9 | Comentarios en domain, modelos y navegación |
| #10 | README completo |
| #11 | Textos con mayúsculas y puntos, botón "Volver" en negrita (el ícono ya se trajo) |
| #12 | **Indicador de conexión real** (`ui/Conexion.kt` + permiso `ACCESS_NETWORK_STATE`). En Nutri-Expert el chip "Sin conexión" es texto fijo. |
| — | `.gitignore` que excluye `*.jks`, `*.keystore`, `keystore.properties` y `/app/release/` |

En Nutri-Expert ya se hizo: ícono NutriExpert, `app_name` = NutriExpert, paquete `cl.duoc.nutriexpert`, tema `NutriExpertTheme` / `Theme.NutriExpert`, base `nutriexpert.db`.

**Decide antes de presentar:** usar agronutri-android, o traer esos cambios a Nutri-Expert.

---

## 1. El caso

| | |
|---|---|
| **Título** | Registro y Estimación Nutricional Agrícola en Terreno |
| **Cliente** | Consultora Gestión y Proyectos Ltda. (Pyme, 2009, Región de Valparaíso; fruticultura y horticultura intensiva) |
| **Asignatura** | DSY1105 · Desarrollo de Aplicaciones Móviles · CITT Viña del Mar |
| **Equipo** | Carolina Flores Flores · Dennis Alan Fraser Milling · Ana María Vidal Salva |

**Situación actual:** los asesores y agricultores anotan en libretas de papel el estado fenológico, las deficiencias de las hojas y las mediciones de suelo y agua. El cálculo de fertilización se hace después, en la oficina, con planillas.

**Problema:** demora entre la toma de datos y el programa nutricional, notas perdidas, errores al digitar e imposibilidad de ajustar el fertirriego en el momento.

**Mejora esperada:** prototipo móvil que digitalice la recolección en terreno, estime el programa de fertirriego y garantice la trazabilidad por predio **sin conexión**.

**Perfiles:**
- **Agricultor** (principal): ingresa datos del predio, consulta recomendaciones y toma fotos foliares.
- **Asesor agrónomo** (secundario): verifica los parámetros, valida las dosis y emite recomendaciones.

**Condiciones del caso:** Android, **offline obligatorio**, cámara nativa y sincronización automática al detectar red.

**Restricciones técnicas:** Kotlin, Android Studio, Jetpack Compose, Material 3, MVVM, Room/SQLite, Retrofit + Spring Boot, Git/GitHub, Trello, APK firmado en release, Ley 19.628 y Ley 21.719 con datos ficticios.

**Señales de éxito:** cero papel · menos tiempo para emitir el programa nutricional · funcionar sin cobertura.

---

## 2. Qué hace la app (guion de la demo)

Hacer toda la demo **en modo avión**.

1. **Splash**: logo NutriExpert por 1,7 s y pasa sola a la Bienvenida.
2. **Bienvenida**: texto animado y hoja que se balancea. Botones "Iniciar sesión" y "Registrarme".
3. **Registro**: nombre, correo, perfil (agricultor o asesor), contraseña, repetir contraseña y consentimiento. Mostrar un error a propósito (por ejemplo, contraseñas que no coinciden).
4. **Home**: "Hola, {nombre}", el perfil, el chip "Sin conexión · N pendientes" y los últimos 3 registros.
5. **Nuevo registro de terreno**: elegir cultivo (Palto, Cítricos o Tomate), lote, meta de rendimiento (t/há) y eficiencia de riego (%). Mostrar que acepta "6,5".
6. **Foto foliar**: pide el permiso de cámara la primera vez, abre la cámara del teléfono y muestra la foto en el formulario.
7. **Calcular**: el botón cambia de color, aparece un loader y luego la tarjeta con N, P₂O₅, K₂O, CaO y MgO (kg/há). La pantalla baja sola hasta el resultado. Se muestra la etiqueta *"Estimación referencial, datos de ejemplo"*.
8. **Guardar**: vuelve al Home, donde el registro aparece como pendiente.
9. **Historial**: lista de los registros de ese usuario, con opción de borrar.
10. **Cerrar sesión**: vuelve a la Bienvenida. Entrar con otro usuario para mostrar que no ve los registros del primero.

---

## 3. Criterios del profesor y dónde están en el código

Rutas relativas a `app/src/main/java/cl/duoc/nutriexpert/`.

| # | Criterio | Cómo se cumple | Archivos |
|---|---|---|---|
| 1 | **Autenticación** (SQLite + estados) | Tabla `usuarios` en Room con el correo único. Contraseña con PBKDF2-SHA256 + sal. Estados `cargando` y `errorGeneral`; la sesión queda en un `StateFlow`. | `data/Usuario.kt`, `data/UsuarioDao.kt`, `data/Contrasenas.kt`, `data/AuthRepository.kt`, `viewmodels/AuthViewModel.kt`, `ui/LoginScreen.kt`, `ui/RegistroScreen.kt` |
| 2 | **Navegación** | `NavHost` con 7 rutas. Los eventos se emiten desde `MainViewModel`. `popUpTo` evita volver al splash o al login con "atrás". | `MainActivity.kt`, `navigation/Screen.kt`, `navigation/NavigationEvent.kt`, `viewmodels/MainViewModel.kt` |
| 3 | **Home** | Nombre, rol, chip de conexión con pendientes y últimos 3 registros. | `ui/HomeScreen.kt`, `model/HomeUiState.kt` |
| 4 | **Formularios con validaciones** | Login, registro de usuario y registro de terreno. Errores `String?` y `validar(): Boolean`. | `viewmodels/AuthViewModel.kt`, `viewmodels/CalculoNutricionalViewModel.kt`, `model/*UiState.kt` |
| 5 | **Animaciones** | `AnimatedVisibility`, `animateColorAsState`, `Crossfade`, `rememberInfiniteTransition`, `animateScrollTo`. | `ui/CalculoNutricionalScreen.kt`, `ui/AuthComponentes.kt`, `ui/BienvenidaScreen.kt`, `ui/RegistroScreen.kt` |
| 6 | **Recurso nativo** | Cámara con `TakePicture` + `FileProvider` y permiso `CAMERA` pedido en tiempo de ejecución. | `ui/CalculoNutricionalScreen.kt`, `viewmodels/CalculoNutricionalViewModel.kt` (`crearFoto`), `AndroidManifest.xml`, `res/xml/file_paths.xml` |
| 7 | **Video demo** | Máximo 1,5 min, en modo avión, datos ficticios; se envía al cliente. | — |

---

## 4. Arquitectura

```
┌──────────────┐  observa StateFlow  ┌──────────────┐     ┌────────────┐     ┌────────┐
│ UI (Compose) │ ◀────────────────── │  ViewModel   │ ──▶ │ Repo / DAO │ ──▶ │ Room   │ ──▶ SQLite
│  pantallas   │ ──────────────────▶ │ estado+lógica│     └────────────┘     └────────┘
└──────────────┘   eventos (clics)   └──────────────┘
                                            │
                                            ▼
                                     domain/ (Kotlin puro: cálculo)
```

**Estructura de carpetas**

```
cl/duoc/nutriexpert/
├── MainActivity.kt     NavHost: une pantallas, ViewModels y navegación
├── data/               Room: entidades, DAOs, AppDatabase, AuthRepository, Contrasenas
├── domain/             Cálculo nutricional + datos de ejemplo (ficticios)
├── model/              UiState de cada pantalla
├── navigation/         Rutas (Screen) y eventos (NavigationEvent)
├── ui/                 Pantallas Compose y componentes de marca
│   └── theme/          Colores, tipografía, NutriExpertTheme
└── viewmodels/         MainViewModel, AuthViewModel, CalculoNutricionalViewModel, RegistrosViewModel
```

**Base de datos (Room, versión 2)**

| Tabla | Campos |
|---|---|
| `usuarios` | id, nombre, correo (único), rol, hashClave, sal, consentimientoEn |
| `registros_terreno` | id, usuarioId, cultivo, lote, metaRendimiento, eficienciaRiego, n, p2o5, k2o, cao, mgo, fotoUri, creadoEn, sincronizado |

**Fórmula del cálculo** (`domain/CalculoNutricional.kt`)

```
factor  = (meta / rendimientoBase) / (eficiencia / 100)
demanda = requerimientoBase × factor      (para N, P₂O₅, K₂O, CaO y MgO)
```

Es una regla de tres según el rendimiento, corregida por la eficiencia de riego. Si la eficiencia es menor, hay que aplicar más. **Está por confirmar con el cliente**: el prototipo web NutriExpert no muestra su fórmula. Los cultivos y valores de `DatosEjemplo.kt` son **ficticios**.

> El cálculo tiene un `delay(800)` en `CalculoNutricionalViewModel.calcular()`. Es solo para que se vea el loader; el cálculo en sí es instantáneo.

---

## 5. Las guías del profesor y cómo se adaptaron

Guías en `Downloads/Contexto/`: pantalla de inicio, navegación, formulario de registro, Room, animaciones, cámara, recursos nativos y DataStore. Están hechas sobre otro proyecto (`cl.duoc.myapplication`).

| Guía | Qué se tomó | Qué se adaptó |
|---|---|---|
| **Pantalla de inicio** | Splash + pantalla de bienvenida | Logo y colores de NutriExpert; el splash se borra del historial. |
| **Navegación** | `Screen` sealed class, `NavigationEvent`, `MainViewModel` con `SharedFlow`, `NavHost` | Se agregó `NavigateAndClear` (`popUpTo` + `inclusive`) para login, registro y cierre de sesión. |
| **Formulario** | UiState data class, errores `String?`, `onXChange` con `update { copy }`, `validar()`, Snackbar | Aplicado a 3 formularios. Coma decimal con `replace(',', '.')`. |
| **Room** | `@Entity`, `@Dao`, `@Database`, singleton con `synchronized` | La guía guarda "registros" genéricos; aquí hay 2 tablas: `usuarios` (con login) y `registros_terreno`. |
| **KSP** | Procesador de anotaciones de Room | La guía usa KSP 2.3.6 (AGP 9). Aquí se usa **2.2.10-2.0.2**, porque Android Studio solo abre AGP hasta 8.12. |
| **Animaciones** | `AnimatedVisibility`, `animateColorAsState` | Se sumó `Crossfade` en el botón y `rememberInfiniteTransition` en la bienvenida. |
| **Cámara / recursos nativos** | `TakePicture`, `FileProvider`, permiso en tiempo de ejecución | Se agregaron solo el permiso, el `uses-feature` y el `provider` al manifest, con la autoridad `cl.duoc.nutriexpert.fileprovider`. |
| **DataStore** | — | **No se usó** (no es criterio). Queda como próximo paso para recordar la sesión. |

---

## 6. Qué queda fuera del MVP

Dilo tú antes de que te lo pregunten: queda como planificación y no como olvido.

- **Sincronización (Retrofit + Spring Boot):** los registros quedan con `sincronizado = false`, pero no se envían a ningún servidor.
- **Validación del asesor:** el rol existe, pero el asesor todavía no tiene pantallas propias.
- **Datos reales:** los cultivos y requerimientos son ficticios; el cliente debe entregar las tablas.
- **Parámetros fisicoquímicos** (pH, CE, análisis de suelo, agua y hoja): no están en el formulario.
- **Módulo 2 de NutriExpert web:** dosis, calculadora de fertilizantes, etapas fenológicas y fertirriego con estanques.
- **Sesión recordada (DataStore):** al cerrar la app hay que iniciar sesión de nuevo.
- **Migración destructiva:** al cambiar la versión de la base, se borran los datos. Sirve con datos ficticios, pero no para producción.

---

## 7. Preguntas probables

**¿Por qué no se guarda la contraseña directamente?**
Se guarda un hash PBKDF2-SHA256 con una sal aleatoria por usuario y 100.000 iteraciones. Aunque alguien copie la base de datos, no puede leer las contraseñas, y dos usuarios con la misma contraseña tienen hashes distintos.

**¿Por qué el login dice "Correo o contraseña incorrectos" y no "el correo no existe"?**
Para no revelar qué correos tienen cuenta (enumeración de usuarios).

**¿Cómo funciona sin internet?**
Todo vive en Room/SQLite, dentro del teléfono. La app no hace ninguna llamada a la red.

**¿Qué es un StateFlow y por qué lo usan?**
Es un contenedor observable con el estado actual. Compose lo lee con `collectAsState()` y se redibuja solo cuando cambia. El `MutableStateFlow` es privado para que únicamente el ViewModel pueda modificarlo.

**¿Por qué MVVM?**
Separa la pantalla de la lógica. El ViewModel sobrevive a la rotación, la UI queda simple y la lógica se puede probar.

**¿Por qué el cálculo está en `domain/` y no en el ViewModel?**
Porque es Kotlin puro, sin Android, y así se prueba con JUnit sin emulador. Cuando el cliente entregue la fórmula real, solo cambia ese archivo.

**¿Cómo se evita que un usuario vea registros de otro?**
Cada registro guarda `usuarioId`. `MainActivity` filtra la lista por el id de la sesión.

**¿Qué pasa si se gira el teléfono mientras se escribe?**
El estado vive en el ViewModel, que sobrevive a la rotación, así que no se pierde lo escrito.

**¿Por qué `FileProvider` para la cámara?**
Desde Android 7 no se puede compartir una ruta `file://` con otra app. `FileProvider` entrega una dirección `content://` con permiso temporal para que la cámara escriba la foto.

**¿Cómo cumplen la ley de datos personales?**
Datos mínimos, consentimiento explícito con fecha guardada, datos solo en el teléfono, contraseñas con hash y datos de prueba ficticios.

**¿De dónde salen los valores de nutrientes?**
Son ficticios y la app lo advierte en pantalla. Las tablas reales las entrega el cliente.

---

## 8. Presentación de 20 minutos y checklist

| Bloque | Tiempo | Contenido |
|---|---|---|
| Contexto | 3 min | Cliente, problema del papel, perfiles, offline obligatorio |
| Demo en vivo | 8 min | Guion de la sección 2, en modo avión |
| Arquitectura | 3 min | MVVM, StateFlow, Room (2 tablas), domain con pruebas |
| Próximos pasos | 2 min | Sección 6 |
| Preguntas | 4 min | Sección 7 |

**Checklist**

- [ ] Decidir qué proyecto se presenta (sección 0)
- [ ] Usuarios ficticios creados (un agricultor y un asesor) y algunos registros ya cargados
- [ ] Modo avión probado de principio a fin
- [ ] Teléfono cargado, cable a mano, brillo alto
- [ ] Plan B: video grabado + APK ya instalado
- [ ] APK release firmado, con las claves **fuera** de Git
- [ ] Trello actualizado
- [ ] Repositorio en GitHub con README y ramas ordenadas (`main`, `develop`, ramas por tarea)

---

# PARTE B · EXPLICACIÓN DE LA MATERIA

## 9. Kotlin: lo básico que se usa en el proyecto

**`val` y `var`**
- `val` es inmutable (no se puede reasignar). `var` sí se puede reasignar.
- En Kotlin se prefiere `val` siempre que se pueda.

**Null safety (seguridad ante nulos)**
- `String` nunca es nulo; `String?` puede ser nulo.
- `?.` llama solo si no es nulo: `sesion?.nombre`.
- `?:` (Elvis) da un valor por defecto: `sesion?.id ?: 0`.
- `!!` fuerza "no es nulo" y se cae si lo es. Evítalo, salvo cuando ya validaste el valor (como `formulario.rol!!` después de `validar()`).

**`data class`**
Clase pensada para guardar datos. Kotlin genera solo `equals`, `toString` y `copy`.

```kotlin
data class Nutrientes(val n: Double, val p2o5: Double, val k2o: Double, val cao: Double, val mgo: Double)
```

`copy` crea una copia cambiando solo algunos campos. Es la base de cómo se actualiza el estado:

```kotlin
estado.copy(cargando = true)
```

**`sealed class`**
Una clase con un conjunto **cerrado** de subclases. Sirve para que `when` revise todos los casos:

```kotlin
sealed class Screen(val route: String) {
    data object Login : Screen("login_page")
    data object Home  : Screen("home_page")
}
```

**`object`**
Una única instancia (singleton). Por ejemplo, `CalculoNutricional`, `DatosEjemplo` y `Contrasenas`.

**`companion object`**
Lo "estático" de una clase. `AppDatabase.obtener(context)` vive ahí.

**`when`**
Un `switch` mejorado que devuelve un valor:

```kotlin
metaRendimiento = when {
    texto.isBlank() -> "Campo obligatorio"
    meta == null    -> "Ingresa un número válido"
    meta <= 0.0     -> "Debe ser mayor a 0"
    else            -> null
}
```

**Lambdas**
Funciones que se pasan como valor: `onClick = { viewModel.navigateTo(Screen.Login) }`. El tipo `() -> Unit` significa "una función sin parámetros que no devuelve nada".

**Funciones de colecciones**
`filter`, `map`, `count`, `take`, `find`, `none`:

```kotlin
registros.count { !it.sincronizado }  // cuántos pendientes
registros.take(3)                     // los 3 primeros
```

`it` es el nombre automático del parámetro cuando la lambda tiene uno solo.

**Convenciones de nombres**
`PascalCase` para clases y `camelCase` para funciones y variables. El guion bajo inicial (`_estado`) marca la versión privada y mutable de una propiedad.

---

## 10. Jetpack Compose

Compose es la forma moderna de hacer interfaces en Android: **la UI se describe con funciones de Kotlin** en vez de archivos XML.

**`@Composable`**
Marca una función que dibuja UI. Puede llamar a otras funciones `@Composable`.

**UI declarativa**
No se dice "cambia este texto". Se dice "con este estado, la pantalla se ve así". Cuando el estado cambia, Compose vuelve a ejecutar la función (**recomposición**) y actualiza solo lo que cambió.

**Layouts básicos**
- `Column`: elementos uno debajo del otro.
- `Row`: elementos uno al lado del otro.
- `Box`: elementos encima de otros.
- `Spacer`: espacio vacío.
- `Scaffold`: estructura de pantalla con barra superior, contenido y Snackbar.

**`Modifier`**
Cambia tamaño, márgenes, fondo, clics, etc. Se encadena y **el orden importa**:

```kotlin
Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 24.dp)
```

**`remember` y `mutableStateOf`**
Guardan un valor entre recomposiciones. `rememberSaveable` además sobrevive a la rotación. Se usan para estado **solo visual** (por ejemplo, mostrar u ocultar la contraseña). El estado del negocio va en el ViewModel.

**`LaunchedEffect`**
Ejecuta código suspendido cuando la pantalla aparece o cuando cambia una clave. Por ejemplo, el splash espera 1,7 s y navega, y la pantalla de cálculo baja sola cuando aparece el resultado.

**`@Preview`**
Permite ver la pantalla en Android Studio sin correr la app.

**State hoisting (elevar el estado)**
Los componentes reciben el valor y una función para avisar cambios, en vez de guardar su propio estado. Así son reutilizables, como `BotonPrincipal(texto, onClick, cargando)`.

---

## 11. Material 3

Es el sistema de diseño de Google. Da componentes listos (`Button`, `OutlinedButton`, `TextButton`, `OutlinedTextField`, `Card`, `Snackbar`, `FilterChip`) y un **tema** con colores, tipografía y formas.

- `NutriExpertTheme` (en `ui/theme/Theme.kt`) envuelve toda la app y define los colores.
- `dynamicColor = false`: no se usan los colores del fondo de pantalla del teléfono, para que la marca siempre se vea igual.
- `ColoresMarca` reúne los verdes del logo para reutilizarlos.
- Se crearon componentes propios (`BotonPrincipal`, `BotonSecundario`, `CampoTexto`) para que toda la app se vea consistente.

---

## 12. MVVM

**Model · View · ViewModel**: un patrón para separar responsabilidades.

| Capa | Responsabilidad | En el proyecto |
|---|---|---|
| **Model** | Datos y reglas de negocio | `data/` (Room), `domain/` (cálculo), `model/` (UiState) |
| **View** | Dibujar el estado y avisar las acciones del usuario | `ui/*Screen.kt` |
| **ViewModel** | Guardar el estado, validar, llamar a los datos | `viewmodels/` |

**Flujo de datos unidireccional**
1. El usuario escribe o toca algo → la View llama al ViewModel (`onCorreoChange`, `iniciarSesion`).
2. El ViewModel actualiza su estado.
3. La View observa el estado y se redibuja.

El estado **baja** y los eventos **suben**.

**¿Por qué el ViewModel sobrevive a la rotación?**
Android destruye y recrea la Activity al girar el teléfono, pero el ViewModel se conserva. Por eso no se pierde lo que se estaba escribiendo.

**`ViewModel` vs `AndroidViewModel`**
`AndroidViewModel` recibe la `Application`, que se necesita para abrir la base de datos o crear archivos (`CalculoNutricionalViewModel`, `RegistrosViewModel`, `AuthViewModel`).

**Repository**
`AuthRepository` es una capa intermedia entre el ViewModel y el DAO. Agrupa la lógica de datos (normalizar el correo, generar el hash, verificar la contraseña) para que el ViewModel no sepa cómo se guarda.

---

## 13. StateFlow y el estado de la pantalla

**Patrón usado en todos los ViewModels:**

```kotlin
private val _estado = MutableStateFlow(CalculoNutricionalUiState())        // solo el ViewModel lo cambia
val estado: StateFlow<CalculoNutricionalUiState> = _estado.asStateFlow()  // la pantalla solo lo lee
```

- **`StateFlow`** siempre tiene un valor actual (`.value`) y avisa a quien lo observe cuando cambia.
- **`update { it.copy(...) }`** reemplaza el estado por una copia con cambios. Es seguro aunque haya varias corrutinas a la vez.
- En la pantalla: `val estado by viewModel.estado.collectAsState()`.

**UiState**
Una `data class` con **todo** lo que la pantalla necesita mostrar: los textos de los campos, `cargando`, `resultado` y los errores. Si está en el UiState, se ve en pantalla; si no, no.

**`StateFlow` vs `SharedFlow`**
- `StateFlow` sirve para **estado** (siempre tiene valor y el último se repite a quien llega).
- `SharedFlow` sirve para **eventos de una sola vez**, como "navega a Home". `MainViewModel` usa `MutableSharedFlow<NavigationEvent>` para que la navegación no se repita al rotar.

**`Flow` desde Room**
`RegistroTerrenoDao.observar()` devuelve `Flow<List<RegistroTerreno>>`. Cada vez que se inserta o borra un registro, Room emite la lista nueva y el Home y el Historial se actualizan solos.

---

## 14. Corrutinas

Las operaciones lentas, como consultar la base de datos o calcular un hash, **no pueden hacerse en el hilo principal**, porque la pantalla se congelaría. Las corrutinas permiten escribir código asíncrono como si fuera secuencial.

- **`suspend fun`**: una función que puede "pausarse" sin bloquear el hilo. Los DAO de Room son `suspend`.
- **`viewModelScope.launch { ... }`**: inicia una corrutina ligada al ViewModel. Si el ViewModel se destruye, se cancela sola.
- **`withContext(Dispatchers.Default)`**: cambia a un hilo de fondo. `AuthRepository.login` lo usa porque el hash PBKDF2 es costoso.
- **`delay(ms)`**: espera sin bloquear (en el splash y en el loader del cálculo).
- **`try / catch`** dentro de `launch`: captura errores de la base de datos y muestra un mensaje en vez de cerrar la app.

**Dispatchers**
- `Main`: la UI.
- `IO`: disco y red.
- `Default`: cálculos pesados.

---

## 15. Formularios y validaciones

**Patrón**
1. Cada campo es un `String` en el UiState, y cada error es un `String?` (`null` = sin error).
2. `onXChange(valor)` actualiza el campo y **limpia su error**, para que el mensaje desaparezca al corregir.
3. `validar(): Boolean` revisa todo, guarda los errores en el estado y devuelve si está todo bien.
4. La acción (`iniciarSesion`, `calcular`, `registrar`) primero llama a `validar()`. Si devuelve `false`, no sigue.
5. `OutlinedTextField(isError = error != null, supportingText = { Text(error) })` muestra el error bajo el campo.

**Validaciones del proyecto**

| Campo | Regla |
|---|---|
| Nombre | Mínimo 2 letras |
| Correo | No vacío y con formato válido (`Patterns.EMAIL_ADDRESS`) |
| Contraseña (registro) | Mínimo 8 caracteres, con letras y números |
| Repetir contraseña | Igual a la contraseña |
| Perfil | Debe elegir agricultor o asesor |
| Consentimiento | Obligatorio |
| Cultivo / lote | No vacíos |
| Meta de rendimiento | Número mayor a 0, acepta coma |
| Eficiencia de riego | Número entre 1 y 100, acepta coma |

**Coma decimal**
En Chile se escribe "6,5". `toDouble()` solo entiende el punto, así que antes se hace `replace(',', '.')`. Se usa `toDoubleOrNull()`, que devuelve `null` si el texto no es un número, en vez de cerrar la app.

**Evitar el doble envío**
`if (estado.cargando) return` al inicio, y el botón se desactiva mientras carga.

**Snackbar**
Un mensaje temporal abajo de la pantalla para errores generales, como "Errores detectados. Revisa los campos".

---

## 16. Navegación

**Navigation Compose** cambia de pantalla dentro de una sola Activity.

- **`NavController`**: controla el historial de pantallas (back stack).
- **`NavHost`**: define qué pantalla corresponde a cada ruta, y cuál es la inicial (`startDestination = Screen.Splash.route`).
- **Rutas como `sealed class Screen`**: evita escribir los strings a mano y equivocarse.
- **Eventos desde el ViewModel**: las pantallas no conocen el `NavController`. Llaman a `viewModel.navigateTo(...)`, `MainViewModel` emite un `NavigationEvent` y `MainActivity` lo recibe en un `LaunchedEffect` para ejecutar la navegación.

**Back stack y `popUpTo`**

```kotlin
navController.navigate(ruta) {
    popUpTo(navController.graph.id) { inclusive = true }  // borra todo el historial
    launchSingleTop = true                                // no duplica la pantalla si ya está arriba
}
```

Se usa después del login, del registro y al cerrar sesión, para que "atrás" no vuelva a un formulario ya usado. `popBackStack()` vuelve a la pantalla anterior.

**Mapa de pantallas**

```
Splash ─▶ Bienvenida ─┬─▶ Login ────┬─▶ Home ─┬─▶ Cálculo nutricional
                      └─▶ Registro ─┘         └─▶ Historial
Home ──(cerrar sesión)──▶ Bienvenida
```

---

## 17. Room / SQLite

**SQLite** es una base de datos que vive en un archivo dentro del teléfono. **Room** es la capa de Android que facilita su uso desde Kotlin.

**Las 3 piezas**

1. **`@Entity`**: una clase que es una tabla. Cada propiedad es una columna.
   ```kotlin
   @Entity(tableName = "usuarios", indices = [Index(value = ["correo"], unique = true)])
   data class Usuario(@PrimaryKey(autoGenerate = true) val id: Long = 0, ...)
   ```
   `unique = true` hace que la base rechace dos usuarios con el mismo correo.

2. **`@Dao`**: una interfaz con las consultas. Room escribe el código por ti.
   ```kotlin
   @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
   suspend fun buscarPorCorreo(correo: String): Usuario?
   ```
   - `@Insert`, `@Query`, `@Delete`.
   - `suspend` para operaciones de una vez y `Flow` para observar cambios.
   - `OnConflictStrategy.ABORT` lanza un error si el correo ya existe.

3. **`@Database`**: une las entidades y los DAOs. Tiene un **número de versión**.

**Singleton**
`AppDatabase.obtener(context)` usa `@Volatile` + `synchronized` para que exista **una sola** instancia de la base en toda la app. Abrirla es costoso y tener dos instancias puede causar errores.

**KSP**
Room genera código a partir de las anotaciones en tiempo de compilación. KSP (Kotlin Symbol Processing) es la herramienta que lo hace, por eso en Gradle aparece `ksp(libs.androidx.room.compiler)`.

**Migraciones**
Al cambiar las tablas hay que subir la versión. Con `fallbackToDestructiveMigration(true)` Room borra la base y la crea de nuevo. Es aceptable con datos ficticios; en producción se escriben migraciones que conservan los datos.

---

## 18. Seguridad de contraseñas

**Nunca se guarda la contraseña.** Se guarda un **hash**: el resultado de una función matemática de un solo sentido.

**Registro**
1. Se genera una **sal**: 16 bytes aleatorios con `SecureRandom`, distinta para cada usuario.
2. Se calcula `hash = PBKDF2-SHA256(contraseña, sal, 100.000 iteraciones, 256 bits)`.
3. Se guardan la sal y el hash en Base64.

**Login**
1. Se busca el usuario por correo.
2. Se calcula el hash de la contraseña escrita, con **la misma sal** guardada.
3. Se compara con `MessageDigest.isEqual`, que tarda lo mismo aunque los valores difieran al principio. Así no se filtra información por el tiempo de respuesta.

**Por qué cada parte**
- **Sal:** dos personas con la misma contraseña tienen hashes distintos, y se inutilizan las tablas de hashes precalculados.
- **100.000 iteraciones:** hace lento probar millones de contraseñas por fuerza bruta.
- **`spec.clearPassword()`:** borra la contraseña de la memoria apenas se usa.
- **Mensaje genérico en el login:** no revela si el correo existe.

---

## 19. Animaciones

| API | Qué hace | Dónde |
|---|---|---|
| `AnimatedVisibility` | Aparece y desaparece un elemento con animación | Tarjeta de resultado, errores del registro, textos de bienvenida |
| `fadeIn` / `slideInVertically` | Tipo de entrada (aparece de a poco o se desliza) | Bienvenida |
| `animateColorAsState` | Cambia un color de forma suave | Botón calcular: verde → gris (calculando) → verde oscuro (listo) |
| `Crossfade` | Funde un contenido con otro | `BotonPrincipal`: el texto pasa a loader |
| `rememberInfiniteTransition` + `animateFloat` | Animación que se repite sin parar | La hoja que se balancea en la bienvenida |
| `animateScrollTo` | Scroll animado | Baja hasta el resultado del cálculo |
| `CircularProgressIndicator` | Loader circular | Botones mientras cargan |

**Idea clave**
En Compose las animaciones se **disparan por cambios de estado**. No se ordena "anima ahora"; se cambia el estado (`resultado != null`) y la animación ocurre sola. `tween(ms)` define la duración y `RepeatMode.Reverse` hace que vaya y vuelva.

---

## 20. Recursos nativos: cámara y permisos

**Permisos**
1. Se declaran en `AndroidManifest.xml`: `<uses-permission android:name="android.permission.CAMERA" />`.
2. Los permisos "peligrosos", como la cámara, **además se piden en tiempo de ejecución**: el usuario ve un diálogo y puede aceptar o rechazar.
3. `<uses-feature android:name="android.hardware.camera" android:required="false" />` permite instalar la app en aparatos sin cámara.

**Flujo en el proyecto**

```
Tocar "Tomar foto" ─▶ ¿tiene cámara? ─no─▶ Snackbar "Este aparato no tiene cámara"
                          │ sí
                          ▼
                   ¿permiso concedido? ─no─▶ pedir permiso ─rechazado─▶ Snackbar
                          │ sí                    │ aceptado
                          ▼                       ▼
            crearFoto() → content:// ─▶ abrir cámara (TakePicture)
                          │
                          ▼
            ¿se guardó la foto? ─sí─▶ onFotoTomada(uri) → se muestra y se guarda con el registro
```

**Activity Result API**
`rememberLauncherForActivityResult(contrato) { resultado -> ... }` es la forma moderna de abrir otra app o pedir permisos y recibir la respuesta.
- `ActivityResultContracts.RequestPermission()` pide un permiso y devuelve `true` o `false`.
- `ActivityResultContracts.TakePicture()` abre la cámara del sistema, que guarda la foto en la URI entregada y devuelve si se tomó.

**FileProvider**
La app de cámara es **otra app**, así que necesita permiso para escribir el archivo. Desde Android 7 no se pueden compartir rutas `file://`. `FileProvider` crea una dirección `content://` segura y temporal.
- Se declara en el manifest con la autoridad `${applicationId}.fileprovider`.
- `res/xml/file_paths.xml` dice qué carpetas puede compartir.
- Las fotos quedan en `filesDir/fotos/`, que es privado de la app.

---

## 21. Conectividad y modo offline

**Offline primero (offline-first)**
La app guarda todo en el teléfono y funciona igual con o sin red. La conexión solo sería necesaria para sincronizar en el futuro.

**Detectar la red** (`ui/Conexion.kt` en agronutri-android, PR #12)

```kotlin
val conectividad = contexto.getSystemService(ConnectivityManager::class.java)
val red = conectividad.activeNetwork ?: return false
val capacidades = conectividad.getNetworkCapabilities(red) ?: return false
return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
```

- Requiere el permiso `ACCESS_NETWORK_STATE`, que es normal: no se pide al usuario.
- Se revisa cada 2 segundos con un `LaunchedEffect` y `delay`, así el chip cambia al activar o quitar el modo avión.
- Una alternativa más eficiente es `registerNetworkCallback`, que avisa al instante sin consultar cada 2 segundos.

**Pendientes**
Cada registro nace con `sincronizado = false`. El Home cuenta los pendientes. Cuando exista el backend, al detectar red se enviarán y se marcarán como `true`.

---

## 22. DataStore

Guarda **datos pequeños de clave-valor** (preferencias), como "tema oscuro" o "id del usuario conectado". Reemplaza a `SharedPreferences`.

- **Preferences DataStore:** clave-valor simple.
- Es asíncrono (usa corrutinas y `Flow`), así que no bloquea la pantalla.
- **DataStore vs Room:** DataStore sirve para configuraciones sueltas; Room para datos con estructura y relaciones (usuarios, registros).
- **En el proyecto:** no se usó. Serviría para **recordar la sesión** al cerrar la app.

---

## 23. Retrofit y Spring Boot

Es parte del caso, pero **queda para la próxima etapa**.

- **API REST:** un servidor que expone direcciones (endpoints) para leer y escribir datos con HTTP: `GET` (leer), `POST` (crear), `PUT` (actualizar), `DELETE` (borrar). Los datos viajan en **JSON**.
- **Spring Boot:** framework de Java/Kotlin para construir ese servidor y conectarlo a una base de datos central.
- **Retrofit:** librería de Android que convierte esos endpoints en funciones Kotlin:
  ```kotlin
  interface RegistrosApi {
      @POST("registros")
      suspend fun enviar(@Body registro: RegistroDto): Response<Unit>
  }
  ```
- **Plan de sincronización:** al detectar red, se toman los registros con `sincronizado = false`, se envían con Retrofit y, si el servidor responde bien, se marcan como `true` en Room.

---

## 24. Pruebas unitarias

- **Prueba unitaria:** verifica una pieza pequeña de lógica de forma aislada, con JUnit, sin teléfono ni emulador. Están en `app/src/test/`.
- **Prueba instrumentada:** corre en un teléfono o emulador. Están en `app/src/androidTest/`.
- `CalculoNutricionalTest.kt` tiene 4 pruebas del cálculo. Se puede probar porque `domain/` es Kotlin puro.
- Estructura típica: **preparar** (datos), **ejecutar** (llamar la función) y **verificar** (`assertEquals`).
- Comando: `./gradlew :app:testDebugUnitTest`.

---

## 25. Gradle, versiones y APK firmado

**Gradle** compila la app y descarga las librerías.

| Archivo | Para qué |
|---|---|
| `settings.gradle.kts` | Nombre del proyecto y módulos (`:app`) |
| `build.gradle.kts` (raíz) | Plugins comunes |
| `app/build.gradle.kts` | Configuración de la app: `namespace`, `applicationId`, SDKs y dependencias |
| `gradle/libs.versions.toml` | **Catálogo de versiones**: todas las versiones en un solo lugar |

**Conceptos**
- **`applicationId`**: identificador único de la app en el teléfono y en Play Store (`cl.duoc.nutriexpert`). Si cambia, Android la trata como otra app.
- **`namespace`**: paquete del código y de la clase `R`.
- **`minSdk 26`**: Android 8.0 como mínimo (`java.util.Base64` lo requiere). **`targetSdk 36`**: la versión para la que está probada. **`compileSdk 36`**: con qué versión se compila.
- **Versiones del proyecto:** AGP 8.12.0 · Gradle 8.13 · Kotlin 2.2.10 · Compose BOM 2026.02.01 · Room 2.8.5 · KSP 2.2.10-2.0.2.
- **BOM de Compose:** fija versiones compatibles entre todas las librerías de Compose, así no hay que poner la versión en cada una.

**Debug vs Release**
- **Debug:** para desarrollar. Se firma con una clave de prueba automática.
- **Release:** para entregar. Se firma con **tu propia clave** (`.jks`). Se genera en Android Studio con *Build → Generate Signed App Bundle / APK*.
- **La clave `.jks` y sus contraseñas nunca van a Git.** Si se pierden, no se puede actualizar la app publicada.

---

## 26. Git y GitHub

- **Repositorio:** la carpeta del proyecto con su historial de cambios.
- **Commit:** una "foto" guardada de los cambios, con un mensaje. Se usan prefijos: `feat:` (funcionalidad), `fix:` (error), `chore:` (mantenimiento), `docs:` (documentación), `style:` (formato).
- **Rama (branch):** una línea de trabajo paralela.
  - `main`: versión estable.
  - `develop`: integración.
  - `feature/...` o `chore/...`: una tarea cada una, creada desde `develop`.
- **Push:** sube los commits a GitHub. **Pull:** baja los cambios.
- **Pull Request (PR):** pedir que una rama se una a otra, con revisión previa.
- **`.gitignore`:** archivos que Git no debe subir (`build/`, `local.properties`, `.idea/`, claves de firma).
- **`remote origin`:** la dirección del repositorio en GitHub (`git remote -v` para verla).

---

## 27. Privacidad: Ley 19.628 y Ley 21.719

- **Ley 19.628:** protección de la vida privada y los datos personales en Chile.
- **Ley 21.719:** la nueva ley de protección de datos personales, que moderniza la anterior. Exige consentimiento, minimización y seguridad, y crea una Agencia de Protección de Datos.

**Cómo se aplica en la app**

| Principio | En NutriExpert |
|---|---|
| **Minimización** | Solo se piden nombre, correo y perfil |
| **Consentimiento** | Casilla obligatoria; se guarda la fecha (`consentimientoEn`) |
| **Seguridad** | Contraseñas con hash + sal; datos en el almacenamiento privado de la app |
| **Finalidad** | Los datos se usan solo para el registro en terreno; no se envían a terceros |
| **Datos de prueba** | Solo usuarios, predios y valores ficticios |

---

## 28. Glosario rápido

| Término | Significado |
|---|---|
| **Activity** | Pantalla contenedora de Android. Aquí hay una sola: `MainActivity`. |
| **Composable** | Función que dibuja UI en Compose. |
| **Recomposición** | Compose vuelve a ejecutar una función porque cambió su estado. |
| **ViewModel** | Clase que guarda el estado de una pantalla y sobrevive a la rotación. |
| **StateFlow** | Contenedor observable de estado con un valor actual. |
| **SharedFlow** | Flujo para eventos de una sola vez (navegación). |
| **Corrutina** | Tarea asíncrona ligera que no bloquea la pantalla. |
| **suspend** | Función que puede pausarse sin bloquear el hilo. |
| **Entity / DAO** | Tabla / consultas en Room. |
| **KSP** | Generador de código para anotaciones (Room). |
| **Back stack** | Historial de pantallas para el botón "atrás". |
| **FileProvider** | Comparte archivos de forma segura con otras apps (`content://`). |
| **Hash / sal** | Huella irreversible de la contraseña / valor aleatorio que la hace única. |
| **Offline-first** | Funciona primero sin red y sincroniza después. |
| **MVP** | Producto mínimo viable: lo justo para validar la idea. |
| **Fenología** | Etapa de desarrollo del cultivo (floración, engorde, maduración). |
| **Fertirriego** | Aplicar fertilizantes disueltos en el agua de riego. |
| **N, P₂O₅, K₂O, CaO, MgO** | Nitrógeno, fósforo, potasio, calcio y magnesio, expresados como se venden en los fertilizantes. |
| **t/há · kg/há** | Toneladas por hectárea (rendimiento) · kilos por hectárea (dosis). |
