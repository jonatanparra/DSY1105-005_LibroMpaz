# Libro MPAZ — Evaluación Parcial 2 (DSY1105, sección 005, Grupo 8)

App Android del caso de Vinculación con el Medio **"Libro Digital de Clases Interactivo 'Libro MPAZ'"** (Escuela Escritora Marcela Paz). Hecha con Kotlin, Jetpack Compose, Material 3, MVVM y Room.

> Prototipo académico de uso interno. Sus datos no tienen validez oficial ante el MINEDUC ni el sostenedor (RNF-11).

## Integrantes

- Williams Rivera García
- Jonatan Parra
- Vicente Azocar

Repositorio GitHub: https://github.com/jonatanparra/DSY1105-005_LibroMpaz
Tablero Trello: https://trello.com/invite/b/6ad7bc4dacc0bff4c215c54b/ATTI07e4760a1e5650536d22f7976bb6ffceE3294BD8/libro-mpaz

## Alcance de esta entrega

Según lo indicado por el profesor, este avance implementa solo el requerimiento asignado, **RF-07**:

> UTP debe poder acceder a vistas analíticas consolidadas de cobertura curricular (OA y contenidos trabajados) por curso.

Lo que el RF-07 necesita de otros requerimientos (login por perfil, sesiones de clase con sus OA que registraría el docente en el RF-06) está **simulado con datos ficticios** que se cargan en Room la primera vez que se abre la app. Los demás módulos (asistencia, calificaciones, hoja de vida, Inspectoría y Dirección) quedan fuera de este avance y siguen con datos de ejemplo.

## Cómo ejecutar

1. Descomprimir el ZIP en una carpeta limpia.
2. Android Studio → **File > Open…** y elegir la carpeta que contiene `settings.gradle.kts`.
3. Esperar el Gradle Sync (la primera vez necesita internet). No actualizar Gradle ni el Android Gradle Plugin.
4. Ejecutar ▶ **Run 'app'** en un emulador o dispositivo (minSdk 24).
5. Ingresar como UTP con el usuario de prueba y abrir **Cobertura curricular por curso**.

## Usuarios de prueba (ficticios)

| Perfil | Correo | Clave |
|---|---|---|
| **UTP (el que se usa en el RF-07)** | utp@libromp.cl | utp123 |
| Docente (Matemática 3°A y 4°A) | docente.mat@libromp.cl | docente123 |
| Docente (Lenguaje 4°A) | docente.len@libromp.cl | docente123 |
| Inspectoría | inspectoria@libromp.cl | inspectoria123 |
| Dirección | direccion@libromp.cl | direccion123 |

## Qué hace el RF-07 en la app

- Login validado con Room, que lleva a cada perfil a su panel.
- En **Cobertura curricular**, UTP elige curso y asignatura y ve:
  - cuántos OA se han trabajado (por ejemplo "1 de 3") con una barra de avance animada,
  - el estado de cada OA (Trabajado / Pendiente),
  - los contenidos vistos en cada clase registrada.
- Puede **escuchar la cobertura en voz alta** con el botón "Escuchar cobertura".

## Requisitos de la rúbrica

| Requisito | Dónde está |
|---|---|
| Diseño Material 3 y navegación | `ui/`, `navigation/AppNavigation.kt` (botón atrás y cierre de sesión que limpia la pila) |
| Formularios validados con íconos y mensajes | `ui/screen/LoginScreen.kt`: ícono por campo, error junto al campo con ícono |
| Validaciones desde la lógica | `LibroMpazViewModel.validarUsuario()`, `validarClave()` y `login()` |
| Gestión de estado | `StateFlow` en el ViewModel y `collectAsState()` en las pantallas |
| Persistencia local (Room) | `data/local/` (`Entidades.kt`, `CoberturaDao.kt`, `AppDatabase.kt`) y `data/LibroMpazRepository.kt` |
| Animaciones | Barra de avance y porcentaje animados, color del estado de cada OA, mensaje de error del login |
| Recursos nativos (2) | Ver abajo |
| Pruebas | `CoberturaCurricularTest` (cálculo de cobertura del RF-07) |

### Arquitectura

```
Pantalla (Compose) → LibroMpazViewModel → LibroMpazRepository → CoberturaDao → AppDatabase (Room/SQLite)
```

- **Entity**: `UsuarioEntity`, `CursoEntity`, `AsignaturaEntity`, `ObjetivoEntity`, `SesionEntity` y `SesionOaEntity` (tabla intermedia sesión–OA).
- **DAO**: consultas con `Flow` para cursos, asignaturas, OA y sesiones, y `suspend` para buscar el usuario y cargar los datos iniciales.
- **Repository**: convierte las entidades a los modelos de la app y sabe autenticar y sembrar los datos.
- **ViewModel**: convierte los `Flow` en `StateFlow` con `stateIn` y contiene las validaciones.

### Recursos nativos y justificación

1. **Texto a voz (`TextToSpeech`).** En Cobertura curricular, el botón "Escuchar cobertura" lee en voz alta el avance del curso y la asignatura (por ejemplo "3° Básico A, Matemática: 1 de 3 OA trabajados"). Ayuda a UTP a revisar la cobertura sin leer la pantalla y mejora la accesibilidad. Usa el parlante, no el micrófono, y no pide permisos; en el manifest solo se declara la consulta al motor de voz (`<queries>`, obligatorio desde Android 11). Código: `util/Dispositivo.kt` (`LectorDeVoz`).
2. **Vibración.** Da respuesta táctil cuando el login falla y al pedir la lectura de la cobertura, sin depender de mirar la pantalla. Usa el permiso `VIBRATE`, que es un permiso normal declarado en el manifest. Código: `util/Dispositivo.kt` (`vibrar()`).

Ambos respetan las restricciones del caso: no se usa GPS, cámara, grabadora de voz ni biometría, y ningún dato sale del teléfono (sin exportar, compartir, notificaciones, correos, SMS ni WhatsApp; RN-01 y RNF-07).

## Datos

Todos los datos son ficticios y sintéticos: sin nombres, RUT ni antecedentes reales (Ley 19.628 y Ley 21.719).
