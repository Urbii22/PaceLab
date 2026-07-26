# PaceLab — Plan completo de implementación del MVP

## 1. Propósito del documento

Este documento define el alcance, la arquitectura, las fases de implementación, los criterios de aceptación y la estrategia de validación de la primera versión funcional de **PaceLab**.

Está escrito como documento de handoff: una persona, agente o modelo debe poder comenzar la implementación sin necesitar el historial de conversación que dio origen al proyecto.

## 2. Visión del producto

PaceLab será una aplicación Android privada y de uso personal destinada a analizar entrenamientos después de haberlos realizado.

Samsung Health continuará siendo responsable de:

- Registrar el entrenamiento desde el Galaxy Watch.
- Recoger GPS, frecuencia cardiaca y métricas del reloj.
- Sincronizar el reloj con el teléfono.
- Mantener la fuente principal de datos de salud.

PaceLab será responsable de:

- Importar los entrenamientos ya finalizados.
- Mantener una copia local normalizada.
- Organizar el historial completo.
- Mostrar métricas y gráficas más claras que Samsung Health.
- Analizar progreso, volumen, frecuencia cardiaca, ritmo, velocidad y VO₂max.
- Permitir anotaciones personales.
- Exportar y restaurar la información local.

Flujo principal:

```text
Galaxy Watch
    ↓
Samsung Health en el teléfono
    ↓
Health Connect
    ↓
PaceLab
    ↓
Base de datos y analítica local
```

## 3. Restricciones y decisiones confirmadas

- Solo Android móvil.
- No habrá aplicación para el reloj.
- No se registrarán entrenamientos en directo.
- La aplicación es exclusivamente postentreno.
- El foco principal es running.
- También deben aparecer caminatas, senderismo, ciclismo y otros entrenamientos.
- La primera fuente de datos será Health Connect.
- Strava no forma parte del primer MVP.
- Samsung Health Data SDK directo será únicamente una alternativa si Health Connect no entrega datos imprescindibles.
- La aplicación es para una sola persona.
- No se publicará en Google Play ni Galaxy Store.
- La instalación se realizará mediante Android Studio, ADB o APK firmado.
- Las versiones estables podrán guardarse en Releases de un repositorio privado de GitHub.
- No habrá backend, cuenta de usuario ni sincronización cloud en el MVP.
- Los datos permanecerán en el teléfono salvo exportación explícita.
- No se escribirán ni modificarán datos de Samsung Health.
- El mapa no bloqueará la entrega del MVP si Samsung no comparte la ruta mediante Health Connect.

## 4. Objetivos del MVP

El MVP se considerará completo cuando permita:

1. Conectar con Health Connect.
2. Obtener permisos de lectura.
3. Importar el historial disponible de entrenamientos.
4. Sincronizar actividades nuevas, modificadas y eliminadas.
5. Evitar duplicados.
6. Mostrar un resumen semanal y mensual.
7. Navegar por el historial.
8. Filtrar por tipo de entrenamiento y periodo.
9. Abrir el detalle de una actividad.
10. Mostrar gráficas de frecuencia cardiaca y ritmo o velocidad.
11. Mostrar VO₂max y su evolución histórica cuando esté disponible.
12. Mostrar zonas de frecuencia cardiaca.
13. Calcular parciales cuando los datos tengan suficiente resolución.
14. Añadir notas y esfuerzo percibido.
15. Exportar y restaurar una copia local.
16. Instalar actualizaciones del APK sin perder los datos.

## 5. Funcionalidad fuera del MVP

No implementar inicialmente:

- Registro GPS propio.
- Acceso directo a los sensores del teléfono o reloj.
- Aplicación Wear OS.
- Entrenamientos en directo.
- Planificador de entrenamientos.
- Entrenador virtual.
- Recomendaciones médicas.
- Predicciones de lesiones.
- Funciones sociales.
- Seguidores, comentarios, kudos o clasificaciones.
- Segmentos al estilo Strava.
- Integración con Strava.
- Integración con Garmin u otros proveedores.
- Autenticación.
- Backend.
- Sincronización entre dispositivos.
- Actualizador automático.
- Publicación en tiendas.
- Suscripciones o pagos.

## 6. Riesgo técnico principal: calidad de los datos

Antes de construir la interfaz definitiva debe realizarse una prueba técnica con entrenamientos reales.

Health Connect puede exponer:

- Sesiones de ejercicio.
- Distancia.
- Velocidad.
- Frecuencia cardiaca.
- Calorías.
- VO₂max.
- Elevación.
- Cadencia.
- Rutas.

Sin embargo, la disponibilidad y resolución reales dependen de cómo Samsung Health sincronice cada dato.

Se debe comprobar:

- Si la frecuencia cardiaca llega como serie temporal o solo como resumen.
- La frecuencia de las muestras de FC.
- Si la velocidad llega como serie temporal.
- Si existe distancia acumulada suficientemente detallada.
- Si VO₂max puede asociarse inequívocamente con una carrera.
- Si Samsung exporta la ruta GPS.
- Si se incluyen elevación y cadencia.
- Si se incluyen laps o parciales originales.
- Cómo se representan las pausas automáticas.
- Qué diferencia existe entre tiempo activo y tiempo transcurrido.
- Qué aplicación y dispositivo aparecen como origen.

No se debe implementar el motor definitivo de gráficas suponiendo datos que todavía no se hayan observado.

## 7. Fase 0 — Prueba técnica de Health Connect

### 7.1 Objetivo

Demostrar que el teléfono puede leer datos reales suficientes para el producto.

### 7.2 Aplicación de diagnóstico

Crear una aplicación Android mínima con:

- Kotlin.
- Jetpack Compose.
- Java 17.
- `minSdk 29`.
- Health Connect estable.
- Una única pantalla de permisos.
- Una pantalla de resultados sin diseño definitivo.

### 7.3 Permisos que deben evaluarse

Solicitar lectura de:

- Sesiones de ejercicio.
- Frecuencia cardiaca.
- Distancia.
- Velocidad.
- Calorías totales.
- VO₂max.
- Elevación.
- Cadencia.
- Rutas de ejercicio.
- Datos anteriores a 30 días.

No solicitar todavía:

- Sueño.
- Peso.
- Nutrición.
- Pasos diarios.
- Oxígeno.
- Datos médicos.
- Permisos de escritura.

### 7.4 Datos de prueba mínimos

Probar con:

- Una carrera exterior con GPS.
- Una carrera con VO₂max.
- Una caminata.
- Un entrenamiento sin GPS, si existe.
- Una actividad con pausas automáticas.
- Una actividad suficientemente larga para generar miles de muestras.

### 7.5 Salida del diagnóstico

Por cada actividad mostrar y permitir exportar:

- ID del registro.
- Tipo de ejercicio.
- Aplicación de origen.
- Dispositivo de origen.
- Inicio y final.
- Zona horaria.
- Duración.
- Distancia.
- Calorías.
- Cantidad de registros de FC.
- Cantidad de registros de velocidad.
- Cantidad de registros de distancia.
- Cantidad de puntos de ruta.
- Cantidad de registros de VO₂max.
- Cantidad de registros de elevación.
- Cantidad de registros de cadencia.
- Primer y último valor de cada serie.
- Intervalo medio entre muestras.
- Campos ausentes.

### 7.6 Decisión de salida

Continuar solo con Health Connect si entrega:

- Sesiones.
- Distancia.
- Una serie utilizable de FC.
- Una serie utilizable de velocidad o información suficiente para calcular ritmo.
- VO₂max por entrenamiento o por fecha.

Si falta únicamente la ruta:

- Continuar con el MVP sin mapa.
- Mantener la ruta como mejora posterior.

Si faltan FC, velocidad o VO₂max:

- Investigar Samsung Health Data SDK.
- Activar su modo desarrollador solo en el dispositivo personal.
- Comparar los resultados antes de elegir la fuente definitiva.

### 7.7 Criterio de aceptación

La prueba termina cuando puede seleccionarse un entrenamiento real y visualizar todos los registros disponibles con su origen, timestamp y valor.

## 8. Arquitectura técnica

### 8.1 Stack

- Kotlin.
- Jetpack Compose.
- Material 3.
- Navigation Compose.
- Coroutines.
- StateFlow.
- Room.
- WorkManager solo si posteriormente se justifica sincronización en segundo plano.
- Hilt o una solución de inyección equivalente.
- Health Connect.
- JUnit.
- Room Test.
- Compose UI Test.
- Detekt.
- Formateador Kotlin.

### 8.2 Estructura inicial

Mantener un único módulo `app` durante el MVP:

```text
app/src/main/java/<package>/
  core/
    common/
    healthconnect/
    time/
    units/
  data/
    local/
      dao/
      entity/
      migration/
    repository/
    sync/
  domain/
    model/
    usecase/
  feature/
    onboarding/
    dashboard/
    history/
    activitydetail/
    progress/
    settings/
    diagnostics/
  ui/
    charts/
    components/
    theme/
```

No crear múltiples módulos Gradle salvo que el proyecto crezca lo suficiente para justificarlo.

### 8.3 Capas

#### Fuente externa

Responsable de leer Health Connect.

```text
HealthDataSource
  ├── HealthConnectDataSource
  └── FakeHealthDataSource
```

Reservar interfaces futuras:

```text
SamsungHealthDataSource
StravaDataSource
```

No implementarlas en el MVP.

#### Datos

Responsable de:

- DAOs.
- Entidades Room.
- Sincronización.
- Mapeo de Health Connect al modelo local.
- Repositorios.

#### Dominio

Responsable de:

- Modelos independientes de Android.
- Cálculos deportivos.
- Agregaciones.
- Casos de uso.

#### Presentación

Responsable de:

- ViewModels.
- Estado UI.
- Compose.
- Navegación.
- Gráficas.

### 8.4 Principios

- Ninguna pantalla debe consultar Health Connect directamente.
- La interfaz debe leer desde Room.
- La sincronización debe actualizar Room.
- Las clases de Health Connect no deben salir de la capa de datos.
- Los cálculos deben poder probarse sin Android.
- Los datos simulados deben poder alimentar todas las pantallas.
- Una métrica ausente no debe provocar errores ni pantallas vacías.

## 9. Modelo de datos local

### 9.1 WorkoutEntity

Campos recomendados:

```text
id
sourceProvider
sourceRecordId
sourceDataOrigin
sourceDeviceId
sourceDeviceName
sourceExerciseType
normalizedExerciseType
sourceTitle
startTimeUtc
endTimeUtc
zoneOffsetStart
zoneOffsetEnd
elapsedDurationSeconds
activeDurationSeconds
distanceMeters
totalCaloriesKcal
averageHeartRateBpm
maximumHeartRateBpm
averageSpeedMetersPerSecond
maximumSpeedMetersPerSecond
averagePaceSecondsPerKm
vo2MaxMlKgMin
elevationGainMeters
averageCadenceStepsPerMinute
hasRoute
fingerprint
sourceCreatedAt
sourceUpdatedAt
importedAt
lastSyncedAt
isDeleted
```

### 9.2 Series temporales

Crear tablas separadas:

- `HeartRateSampleEntity`.
- `SpeedSampleEntity`.
- `CadenceSampleEntity`.
- `ElevationSampleEntity`.
- `DistanceSegmentEntity`.
- `Vo2MaxSampleEntity`.
- `RoutePointEntity`, opcional.

Campos comunes:

```text
id
workoutId
timestampUtc
elapsedSeconds
distanceMetersFromStart
value
sampleIndex
```

`RoutePointEntity` puede incluir:

```text
latitude
longitude
altitudeMeters
horizontalAccuracyMeters
bearingDegrees
```

### 9.3 Anotaciones personales

`WorkoutAnnotationEntity`:

```text
workoutId
notes
perceivedEffort
feeling
isFavorite
updatedAt
```

Las anotaciones no deben sobrescribirse durante una resincronización.

### 9.4 Estado de sincronización

`SyncStateEntity`:

```text
recordType
changeToken
lastSuccessfulSyncAt
lastAttemptAt
lastErrorCode
lastErrorMessage
initialImportCompleted
```

### 9.5 Índices

Crear índices para:

- `sourceRecordId`.
- `startTimeUtc`.
- `normalizedExerciseType`.
- `sourceDataOrigin`.
- `workoutId + timestampUtc`.
- `isDeleted`.

### 9.6 Migraciones

- Crear migraciones Room explícitas desde la primera release.
- Probar cada migración.
- No utilizar `fallbackToDestructiveMigration` en release.
- Nunca obligar a desinstalar para actualizar el esquema.

## 10. Sincronización

### 10.1 Activadores

Sincronizar:

- Durante el onboarding.
- Al abrir la aplicación.
- Al volver al primer plano.
- Al hacer pull-to-refresh.
- Al pulsar “Sincronizar ahora”.

No implementar polling continuo.

### 10.2 Importación inicial

1. Comprobar disponibilidad.
2. Comprobar permisos.
3. Consultar sesiones de ejercicio por páginas.
4. Procesar intervalos de fechas.
5. Consultar métricas asociadas a cada sesión.
6. Normalizar.
7. Guardar cada actividad transaccionalmente.
8. Mostrar progreso.
9. Permitir reanudar una importación interrumpida.
10. Marcar la importación como completada.

### 10.3 Sincronización incremental

- Usar tokens de cambios separados por tipo.
- Leer inserciones, modificaciones y eliminaciones.
- Actualizar solo actividades afectadas.
- Confirmar el nuevo token únicamente después de guardar correctamente.
- Si una operación falla, conservar el token anterior.

### 10.4 Asociación de datos

Usar la sesión de ejercicio como registro principal.

Asociar las series por:

1. Intervalo temporal.
2. Origen de datos.
3. Dispositivo.
4. Tipo de actividad, cuando aplique.

Evitar:

- Asignar una muestra a dos sesiones.
- Mezclar frecuencia cardiaca general con frecuencia cardiaca del entrenamiento.
- Asociar un VO₂max a la carrera equivocada.

Registrar en diagnósticos cualquier asociación ambigua.

### 10.5 Deduplicación

Prioridad:

1. ID original de Health Connect.
2. Huella estable.

Huella de respaldo:

```text
sourceProvider
+ sourceDataOrigin
+ exerciseType
+ startTime
+ endTime
+ roundedDistance
```

La sincronización repetida debe ser idempotente.

### 10.6 Eliminaciones

- Marcar actividades eliminadas durante la transacción.
- Ocultarlas de las pantallas normales.
- Conservar temporalmente anotaciones desacopladas.
- Añadir limpieza definitiva desde ajustes.

### 10.7 Estados de error

Contemplar:

- Health Connect no disponible.
- Samsung Health no conectado a Health Connect.
- Permisos rechazados.
- Permisos revocados.
- Permiso histórico no concedido.
- Sin datos.
- Importación parcial.
- Error de paginación.
- Token inválido.
- Registro malformado.
- Sesiones solapadas.
- Samsung todavía no ha sincronizado el reloj.

## 11. Motor de cálculo

### 11.1 Unidades internas

Usar:

- Distancia: metros.
- Tiempo: segundos.
- Velocidad: metros por segundo.
- Elevación: metros.
- FC: latidos por minuto.
- VO₂max: ml/kg/min.
- Timestamp: UTC.

Convertir únicamente en la capa de presentación.

### 11.2 Ritmo

```text
paceSecondsPerKm = activeDurationSeconds / distanceKm
```

Separar:

- Ritmo activo.
- Ritmo total.
- Velocidad media.

No calcular ritmo si:

- La distancia es cero.
- Falta duración válida.
- La actividad no tiene una métrica aplicable.

### 11.3 Prioridad de valores

```text
Valor agregado fiable de Health Connect
→ cálculo desde muestras
→ valor no disponible
```

No sustituir silenciosamente un valor oficial por uno aproximado.

### 11.4 Frecuencia cardiaca

Calcular:

- FC media ponderada por tiempo.
- FC máxima.
- Tiempo por zona.
- Porcentaje por zona.

No interpolar huecos largos.

### 11.5 Zonas

Para el MVP:

- FC máxima introducida manualmente.
- Cinco zonas editables.
- Plantilla porcentual inicial.
- Etiqueta clara de que son métricas deportivas orientativas.

### 11.6 Parciales

Calcular parciales por kilómetro solo si existe distancia temporal suficientemente detallada.

Por parcial:

- Número.
- Tiempo.
- Ritmo.
- FC media.
- FC máxima.
- Desnivel, si existe.

Si la fuente no permite parciales fiables, ocultar la sección.

### 11.7 Picos y datos anómalos

- Conservar datos originales.
- Aplicar filtros únicamente a cálculos derivados.
- No eliminar picos sin dejar clara la regla.
- Evitar máximos de velocidad imposibles generados por errores GPS.

### 11.8 Estadísticas históricas

Calcular:

- Kilómetros diarios, semanales, mensuales y anuales.
- Tiempo acumulado.
- Número de actividades.
- Ritmo medio ponderado por distancia.
- FC media ponderada.
- Distancia más larga.
- Mayor volumen semanal.
- Evolución de VO₂max.
- Tiempo por zonas.
- Comparación con el periodo anterior.
- Ritmo frente a FC.
- Consistencia semanal.

No implementar aún puntuaciones de carga propietarias ni recomendaciones de entrenamiento.

## 12. Pantallas

### 12.1 Onboarding

Contenido:

1. Explicación del funcionamiento.
2. Estado de Health Connect.
3. Guía para conectar Samsung Health.
4. Solicitud de permisos.
5. Importación inicial.
6. Resultado:
   - actividades importadas;
   - periodo cubierto;
   - métricas disponibles;
   - posibles limitaciones.

### 12.2 Resumen

Mostrar:

- Kilómetros de la semana.
- Tiempo semanal.
- Número de carreras.
- Ritmo medio semanal.
- Último VO₂max.
- Comparación con la semana anterior.
- Última actividad.
- Volumen de las últimas semanas.

Running debe tener prioridad visual.

### 12.3 Actividades

- Lista cronológica.
- Agrupación por mes.
- Filtro por deporte.
- Filtro por periodo.
- Orden ascendente o descendente.
- Búsqueda por título o nota.

Cada fila:

- Tipo.
- Fecha.
- Distancia.
- Duración.
- Ritmo o velocidad.
- FC media.
- VO₂max, si existe.

### 12.4 Detalle de actividad

Cabecera:

- Tipo.
- Fecha.
- Duración.
- Distancia.
- Ritmo.
- FC media y máxima.
- Calorías.
- VO₂max.

Contenido:

- Gráfica temporal.
- Zonas de FC.
- Parciales.
- Elevación.
- Cadencia.
- Notas.
- Esfuerzo percibido.
- Mapa opcional.

### 12.5 Progreso

Periodos:

- 4 semanas.
- 8 semanas.
- 12 semanas.
- Año.
- Todo.

Gráficas:

- Kilómetros semanales.
- Tiempo semanal.
- Ritmo medio.
- FC media.
- VO₂max.
- Distancia acumulada.
- Zonas.
- Ritmo frente a FC.

### 12.6 Ajustes

- Estado de Health Connect.
- Gestión de permisos.
- Última sincronización.
- Sincronizar ahora.
- FC máxima.
- Configuración de zonas.
- Preferencia ritmo/velocidad.
- Exportar.
- Importar.
- Limpiar datos eliminados.
- Diagnósticos.
- Versión.

## 13. Sistema de gráficas

### 13.1 Requisitos

La librería elegida debe soportar:

- Jetpack Compose.
- Varias series.
- Zoom.
- Desplazamiento.
- Marcador interactivo.
- Ejes personalizados.
- Valores ausentes.
- Buen rendimiento.

Encapsularla en componentes propios.

### 13.2 Componentes

- `WorkoutTimelineChart`.
- `WeeklyVolumeChart`.
- `Vo2MaxTrendChart`.
- `HeartRateZoneChart`.
- `PaceHeartRateScatterChart`.
- `SplitChart`.

### 13.3 Gráfica principal

Series seleccionables:

- FC.
- Ritmo.
- Velocidad.
- Elevación.
- Cadencia.

Eje X:

- Tiempo.
- Distancia.

Marcador:

```text
Km 4,25
00:24:39
5:48 min/km
157 ppm
82 m
168 spm
```

Mostrar únicamente valores existentes.

### 13.4 Datos de diferente frecuencia

- Usar un eje común.
- No inventar precisión.
- No interpolar huecos largos.
- Marcar discontinuidades.
- Aplicar una ventana limitada para encontrar la muestra válida más cercana.

### 13.5 Rendimiento

Una actividad puede tener decenas de miles de muestras.

Implementar reducción visual:

- Aproximadamente 800–1.500 puntos visibles.
- Preservar mínimos y máximos.
- Recalcular según zoom.
- No perder picos relevantes de FC.

### 13.6 Accesibilidad

- No depender solo de colores.
- Añadir leyenda.
- Añadir descripción.
- Añadir tabla textual alternativa.
- Respetar tamaño de fuente.

## 14. Diseño de estados

Cada pantalla debe contemplar:

- Cargando.
- Datos correctos.
- Datos parciales.
- Sin datos.
- Sin permiso.
- Permiso revocado.
- Error recuperable.
- Error no recuperable.
- Sincronización en curso.
- Samsung Health todavía no sincronizado.

No mostrar cero cuando el valor realmente sea desconocido.

Usar “No disponible” o esconder la métrica según el contexto.

## 15. Privacidad y seguridad

- No incluir publicidad.
- No incluir analítica.
- No incluir crash reporting remoto.
- No incluir cuentas.
- No enviar datos a servidores.
- No registrar datos de salud en release.
- No registrar rutas GPS.
- No incluir exports reales en Git.
- No incluir bases de datos reales en Git.
- No incluir claves de firma en Git.
- No pedir permisos no utilizados.
- Mantener Room en el almacenamiento privado de Android.

Evitar el permiso de internet si no se incorpora un mapa con tiles remotos.

## 16. Backup y exportación

### 16.1 Backup completo

Crear formato versionado `.pacelab`.

Contenido:

- Versión de esquema.
- Fecha.
- Actividades.
- Series.
- Anotaciones.
- Configuración.
- Checksums.

Preferencia:

- Backup cifrado con contraseña.
- Salt aleatorio.
- Cifrado autenticado.
- Aviso de que la contraseña no es recuperable.

### 16.2 CSV

Permitir exportar:

- Resumen de actividades.
- VO₂max.
- Parciales.
- Series seleccionadas.

No exportar coordenadas GPS por defecto.

### 16.3 Restauración

Antes de importar:

- Validar integridad.
- Validar versión.
- Mostrar resumen.
- Detectar duplicados.
- Ejecutar transaccionalmente.
- No sobrescribir notas más recientes sin aviso.

## 17. Pruebas

### 17.1 Unitarias

Probar:

- Conversiones.
- Ritmo.
- Velocidad.
- Ponderaciones.
- Zonas.
- Parciales.
- Pausas.
- Agregaciones.
- Semanas ISO.
- Zona horaria.
- Cambio horario.
- Datos ausentes.
- Picos anómalos.
- Asociación temporal.
- Deduplicación.
- Tokens de sincronización.

### 17.2 Base de datos

Probar:

- DAOs.
- Índices.
- Transacciones.
- Migraciones.
- Eliminaciones.
- Restauración.
- Miles de muestras.

### 17.3 ViewModels

Probar:

- Carga.
- Refresco.
- Filtros.
- Permisos.
- Error.
- Periodos.
- Datos parciales.

### 17.4 Compose

Probar:

- Onboarding.
- Resumen.
- Historial.
- Detalle.
- Progreso.
- Ajustes.

### 17.5 Datasets sintéticos

Crear:

- Carrera completa.
- Carrera sin FC.
- Carrera sin VO₂max.
- Entrenamiento indoor.
- Caminata.
- Actividad con pausas.
- Actividad cruzando medianoche.
- Actividad en cambio horario.
- Actividad duplicada.
- Sesiones solapadas.
- Actividad eliminada.
- Actividad con 20.000 muestras.

### 17.6 Comandos

En cada cambio relevante:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Antes de una release:

```powershell
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat assembleRelease
```

## 18. Validación con el móvil real

Probar:

- Primera instalación.
- Onboarding.
- Concesión parcial de permisos.
- Concesión completa.
- Importación histórica.
- Sincronización tras una carrera.
- Sincronización repetida.
- Modificación de actividad.
- Eliminación de actividad.
- Aplicación sin internet.
- Aplicación después de reiniciar el teléfono.
- Permisos revocados.
- APK actualizado con `adb install -r`.
- Restauración de backup.

Comparar con Samsung Health:

- Distancia.
- Duración.
- Tiempo activo.
- Ritmo.
- FC media.
- FC máxima.
- Calorías.
- VO₂max.
- Elevación.
- Cadencia.

Documentar cualquier diferencia conocida.

## 19. Rendimiento

Objetivos iniciales:

- Historial fluido con 1.000 actividades.
- Detalle fluido con 20.000 muestras.
- Sincronización incremental sin bloquear UI.
- Procesamiento en dispatcher de IO o Default según corresponda.
- Consultas Room paginadas.
- Listas con LazyColumn.
- Gráficas reducidas para visualización.
- No cargar todas las series del historial en la pantalla Resumen.
- No cargar muestras de una actividad hasta abrir su detalle.

## 20. Accesibilidad

- Contraste suficiente.
- Áreas táctiles mínimas.
- Compatibilidad con fuente grande.
- Descripciones para iconos.
- Alternativas textuales a gráficas.
- No comunicar estados únicamente por color.
- Formatos numéricos y fechas según locale.

## 21. Firma y distribución privada

### 21.1 Variantes

- Debug: instalación de desarrollo.
- Release: uso diario.

Usar `applicationIdSuffix = ".debug"` para evitar que debug reemplace release.

### 21.2 Clave

- Crear una clave release estable.
- Guardarla fuera del repositorio.
- Crear dos copias seguras.
- No guardar contraseña en Git.
- No regenerar la clave entre versiones.

Perder la clave impediría actualizar la aplicación instalada.

### 21.3 Instalación

Desarrollo:

```powershell
adb install -r app-debug.apk
```

Release:

```powershell
adb install -r app-release.apk
```

### 21.4 GitHub privado

El repositorio debe contener:

- Código.
- README.
- Instrucciones de compilación.
- Instrucciones ADB.
- Changelog.
- Documentación de backup.

GitHub Releases puede contener:

- APK firmado.
- Changelog.
- Checksum SHA-256.

La clave release nunca debe subirse.

## 22. CI

Configurar GitHub Actions para:

- Compilar debug.
- Ejecutar pruebas.
- Ejecutar lint.
- Ejecutar análisis estático.

No firmar releases en CI durante el MVP.

Generar releases firmadas localmente para reducir el riesgo de exposición de la clave.

## 23. Fases de entrega

### Milestone 0 — Viabilidad

- Prueba Health Connect.
- Datos reales inspeccionados.
- Decisión sobre rutas.
- Decisión definitiva de fuente.

### Milestone 1 — Fundación

- Proyecto definitivo.
- Arquitectura.
- Room.
- Datos falsos.
- CI.

### Milestone 2 — Importador

- Onboarding.
- Permisos.
- Importación inicial.
- Sincronización incremental.
- Deduplicación.
- Diagnósticos.

### Milestone 3 — Historial

- Resumen.
- Lista.
- Filtros.
- Detalle básico.

### Milestone 4 — Analítica

- Gráfica principal.
- Zonas.
- Parciales.
- VO₂max.
- Progreso.

### Milestone 5 — Persistencia personal

- Notas.
- Esfuerzo percibido.
- Backup.
- Restauración.
- CSV.

### Milestone 6 — Release

- Pruebas reales.
- Rendimiento.
- Accesibilidad.
- APK firmado.
- Actualización conservando datos.
- Release privada.

## 24. Definition of Done

El MVP está terminado únicamente si:

- Health Connect funciona en el móvil real.
- El historial se importa.
- La sincronización es idempotente.
- No aparecen duplicados.
- Las modificaciones se actualizan.
- Las eliminaciones se reflejan.
- Running tiene prioridad visual.
- Otros entrenamientos siguen siendo visibles.
- FC puede representarse gráficamente.
- Ritmo o velocidad pueden representarse gráficamente.
- VO₂max aparece cuando existe.
- Las métricas ausentes se manejan correctamente.
- Las zonas funcionan con configuración personal.
- Los parciales solo aparecen si son fiables.
- Las anotaciones sobreviven a la sincronización.
- El backup se puede restaurar.
- Los tests pasan.
- El APK release está firmado.
- Una actualización conserva la base de datos.
- No existen claves ni datos personales en Git.
- El resultado ha sido utilizado con entrenamientos reales.

## 25. Orden obligatorio para el implementador

1. No diseñar todas las pantallas antes de completar la prueba de datos.
2. No introducir Samsung Health Data SDK sin demostrar una carencia de Health Connect.
3. No añadir Strava al MVP.
4. No añadir backend.
5. No añadir permisos de salud no utilizados.
6. Implementar importación y deduplicación antes de las gráficas definitivas.
7. Añadir tests del motor de cálculo antes de mostrar métricas derivadas.
8. Probar migraciones antes de instalar una release diaria.
9. Probar backup antes de depender de la base local.
10. Revisar diff, tests y contenido del APK antes de publicar una release privada.

## 26. Preguntas pendientes no bloqueantes

El implementador puede usar estos valores provisionales hasta recibir otra decisión:

- Nombre: `PaceLab`.
- Package: `com.urbii.pacelab`.
- Unidades: métricas.
- Métrica principal para running: ritmo en min/km.
- Zonas: cinco zonas basadas en FC máxima manual.
- Sincronización: al abrir y manual.
- Mapa: opcional según disponibilidad.
- Tema: seguir el tema del sistema.

## 27. Referencias oficiales

- Samsung Health mediante Health Connect:  
  https://developer.samsung.com/health/blog/en/accessing-samsung-health-data-through-health-connect
- Health Connect:  
  https://developer.android.com/health-and-fitness/health-connect
- Experiencias de entrenamiento:  
  https://developer.android.com/health-and-fitness/health-connect/experiences/workouts
- Lectura de datos:  
  https://developer.android.com/health-and-fitness/health-connect/read-data
- Sincronización:  
  https://developer.android.com/health-and-fitness/health-connect/sync-data
- Rutas de ejercicio:  
  https://developer.android.com/health-and-fitness/health-connect/features/exercise-routes
- Samsung Health Data SDK, alternativa:  
  https://developer.samsung.com/health/data
- Modo desarrollador de Samsung Health Data SDK:  
  https://developer.samsung.com/health/data/guide/developer-mode.html
