# NutriExpert (app Android)

App de terreno para el cálculo nutricional de cultivos. Kotlin, Jetpack Compose, MVVM con StateFlow, Room y Retrofit.

## Conexión con los microservicios

La app consume tres microservicios Spring Boot del repositorio `nutriexpert-backend`:

| Servicio | Puerto | Uso en la app |
|---|---|---|
| ms-usuarios | 8083 | Registro (necesita señal) y login. Sin señal, el login se valida contra Room. |
| ms-catalogo | 8081 | Lista de cultivos. Se guarda una copia en Room para usarla sin señal. |
| ms-registros | 8082 | Los registros se guardan primero en Room como "Pendiente" y se envían solos cuando hay señal; al confirmarse pasan a "Sincronizado". |

### Configurar la dirección del servidor

1. `app/src/main/java/cl/duoc/nutriexpert/data/remote/RetrofitInstance.kt` → constante `HOST`:
   - `10.0.2.2` para el emulador con los servicios corriendo en tu computador.
   - La Elastic IP de la instancia EC2 para la demo.
2. `app/src/main/res/xml/network_security_config.xml` → la misma IP. Los servicios usan HTTP y Android solo lo permite hacia las direcciones listadas ahí.

### Código nuevo

```
data/remote/   RetrofitInstance, UsuariosApi, CatalogoApi, RegistrosApi, ApiModels (DTOs), Conectividad
data/          AuthRepository (servidor + respaldo Room), CatalogoRepository, SincronizacionRepository, CultivoLocal
```

- Los usuarios quedan en Room con el **id del servidor**: ese es el `usuarioId` que se envía a ms-registros.
- La sincronización se dispara al abrir la app, al guardar un registro, al volver la señal y con el botón "Sincronizar" del Home. Reintentar es seguro: el servidor no duplica un registro con el mismo `usuarioId + idLocal`.
- La foto no se envía al servidor: queda solo en el teléfono.
- La base de Room subió a la versión 3 y se recrea al actualizar (datos ficticios). Hay que volver a iniciar sesión con señal.
