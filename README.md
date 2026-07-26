# PaceLab

PaceLab es una aplicación Android privada para analizar entrenamientos ya finalizados. Samsung Health sigue siendo la fuente de registro; PaceLab lee una copia mediante Health Connect y la normaliza localmente.

## Estado actual

Milestone 2 en curso: fundación e importador Health Connect.

- Proyecto Kotlin + Jetpack Compose + Material 3.
- Room con entidades para sesiones, series temporales, anotaciones y estado de sincronización.
- Health Connect aislado en `data/healthconnect` con lectura paginada de sesiones, FC, velocidad, distancia, calorías, VO₂max, elevación, cadencia y ruta disponible.
- Historial, resumen semanal, progreso, detalle y ajustes conectados a Room. Una instalación release limpia empieza vacía; los fixtures solo viven en tests.
- Motor Android-free para ritmo, velocidad, zonas de frecuencia cardiaca, parciales, agregación semanal y deduplicación SHA-256.
- Tests unitarios del motor de cálculo.

La importación conserva estados explícitos de proveedor, permisos, lecturas vacías y errores. La validación con entrenamientos Samsung reales sigue siendo obligatoria antes de afirmar compatibilidad definitiva; este entorno no tiene un teléfono conectado.

## Compilar

Requisitos: Android Studio reciente, JDK 17 y Android SDK 35.

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

El APK debug queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar por ADB

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

La variante debug usa `applicationIdSuffix = ".debug"` para no reemplazar una futura instalación release.

## Health Connect

1. Instala Health Connect y sincroniza Samsung Health.
2. Abre PaceLab → Ajustes → Abrir diagnósticos.
3. Concede únicamente los permisos de lectura solicitados.
4. Usa «Leer diagnóstico real» para inspeccionar sesiones y series disponibles.

PaceLab no escribe datos en Health Connect, no tiene backend, no incluye analítica remota y no solicita Internet.

## Backup y distribución

El formato `.pacelab`, restauración, CSV y las releases firmadas se incorporarán en los milestones de persistencia y release. La clave release debe permanecer fuera de GitHub.
