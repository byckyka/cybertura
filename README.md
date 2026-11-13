# CyberTura — Una aventura de ciberseguridad

Juego móvil educativo para Android que enseña conceptos básicos de **ciudadanía y seguridad digital** a niños de **11 a 12 años**. El jugador recorre una ciudad 2D en vista cenital, habla con ocho maestros y supera un minijuego por cada tema: contraseñas, phishing, privacidad, ciberacoso, redes sociales, dispositivos del hogar, malware y juegos en línea.

> Prototipo desarrollado como Proyecto de Grado de Ingeniería de Sistemas. Nivel de madurez objetivo: **TRL 4**, un prototipo funcional validado en un entorno controlado.

---

## Contenido

- [Características](#características)
- [Módulos educativos](#módulos-educativos)
- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Requisitos](#requisitos)
- [Compilar y ejecutar](#compilar-y-ejecutar)
- [Pruebas](#pruebas)
- [Editar el contenido educativo](#editar-el-contenido-educativo)
- [Modo investigador](#modo-investigador)
- [Privacidad](#privacidad)
- [Documentación adicional](#documentación-adicional)
- [Autoría y créditos](#autoría-y-créditos)

---

## Características

- **Mundo 2D explorable**: mapa exterior de 50×50 tiles con interiores de casas y portales, renderizado con un motor propio sobre `Canvas` de Jetpack Compose.
- **Ocho maestros (NPC)** que entregan las misiones en un orden fijo. Una guía de objetivo (banner, brújula y flecha en la puerta) indica a dónde ir.
- **Un minijuego por módulo**: forja de contraseñas, clasificación con deslizamiento, historias de chat, búsqueda de pistas y defensa arcade.
- **Gamificación**: XP, niveles con rangos, medallas por módulo y una pantalla final al completar la aventura.
- **Retroalimentación inmediata** que no castiga el error: el niño puede reintentar sin perder progreso.
- **Audio**: música ambiente en bucle y efectos de pasos, entrada a casas, chat, acierto, error y módulo completado.
- **Tutorial** en la primera ejecución.
- **Modo investigador** oculto para la prueba piloto: registra cada intento de minijuego y lo exporta en un CSV.
- **Funciona 100 % sin conexión**. No tiene cuentas, no usa red y no recoge datos personales.

## Módulos educativos

El contenido vive en [`app/src/main/assets/cyber_content.json`](app/src/main/assets/cyber_content.json).

Los módulos se juegan en el orden de misiones definido en `NpcRegistry` (`game/engine/NpcData.kt`):

| # | Módulo | Tema | Minijuego |
|---|---|---|---|
| 1 | Comunidad y Respeto Digital | Ciberacoso | `CHAT_STORY`: el chat del grupo |
| 2 | Escudo de Privacidad | Datos personales | `SWIPE_SORT`: ¿lo comparto o lo guardo? |
| 3 | Guardianes de la Contraseña | Contraseñas seguras | `FORGE_KEY`: forjar una llave fuerte |
| 4 | Jugadores Inteligentes | Juegos en línea | `SPOT_THE_CLUES`: detective del chat del juego |
| 5 | Hogar Conectado y Seguro | Wi-Fi y dispositivos | `CHAT_STORY`: el chat de la familia |
| 6 | Bosque Libre de Virus | Malware | `TAP_DEFENSE`: defiende el bosque |
| 7 | Cazadores de Trampas | Phishing | `SWIPE_SORT`: ¿seguro o trampa? |
| 8 | Huella Digital Responsable | Redes sociales | `SPOT_THE_CLUES`: detective de publicaciones |

## Stack tecnológico

| Tecnología | Versión | Uso |
|---|---|---|
| Kotlin | 2.2.10 | Único lenguaje del proyecto |
| Android Gradle Plugin | 9.4.1 | Compilación |
| Jetpack Compose (BOM) | 2026.02.01 | Interfaz declarativa y `Canvas` del juego |
| Material 3 | por BOM | Tema y componentes |
| Navigation Compose | 2.8.8 | Navegación entre pantallas |
| Lifecycle ViewModel Compose | 2.8.7 | ViewModels con `StateFlow` |
| DataStore Preferences | 1.1.2 | Guardado del progreso |
| Gson | 2.11.0 | Lectura del JSON de contenido |
| Kotlin Coroutines / Flow | — | Bucle de juego y estado reactivo |
| JUnit 4 | 4.13.2 | Pruebas unitarias |

`minSdk 24` (Android 7.0) · `targetSdk 37` · `compileSdk 37` · Java 11

## Arquitectura

La app es un solo módulo (`:app`) con **MVVM** y **flujo de datos unidireccional (UDF)**: cada pantalla observa un `StateFlow` de su ViewModel y le envía eventos mediante lambdas. Las dependencias se inyectan a mano desde `CyberturaApplication`.

```mermaid
flowchart TB
    subgraph UI["UI (Jetpack Compose)"]
        SCR["ui.screens"]
        CMP["ui.components"]
        NAV["ui.navigation"]
        REN["game.renderer (Canvas)"]
    end
    subgraph VM["Presentación"]
        GWVM["GameWorldViewModel<br/>(bucle de juego)"]
        GPVM["GameplayViewModel"]
        BVM["BadgesViewModel"]
    end
    subgraph ENG["Motor de juego"]
        GW["GameWorld<br/>(mapas, portales)"]
        NPC["NpcRegistry"]
    end
    subgraph DATA["Datos"]
        REPO["CyberRepository"]
        DS["DataStoreManager"]
        JL["JsonDataLoader"]
    end
    AUD["audio<br/>(SoundManager, AmbientLoopPlayer)"]

    NAV --> SCR
    SCR --> CMP & REN
    SCR --> GWVM & GPVM & BVM
    GWVM --> GW & NPC
    GWVM & GPVM & BVM --> REPO
    SCR --> AUD
    REPO --> DS --> STORE[("DataStore<br/>cybertura_progress")]
    REPO --> JL --> JSON[("assets/<br/>cyber_content.json")]
```

### Navegación

```mermaid
flowchart LR
    START((Inicio)) -->|primera vez| TUT[Tutorial]
    START -->|ya visto| HOME[Mundo]
    TUT --> HOME
    HOME -->|hablar con maestro| MD[Detalle de módulo]
    MD --> GP[Minijuego]
    GP --> MR[Resultado del módulo]
    MR --> HOME
    MR -->|8 maestros completados| GC[Juego completado]
    HOME & MD & MR --> BD[Medallas]
```

## Estructura del proyecto

```text
cybertura/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml           # catálogo de versiones
└── app/
    ├── build.gradle.kts
    ├── docs/                           # documentación técnica y de diseño
    └── src/
        ├── main/
        │   ├── assets/cyber_content.json
        │   ├── java/com/example/cybertura/
        │   │   ├── CyberturaApplication.kt   # inyección manual de dependencias
        │   │   ├── MainActivity.kt
        │   │   ├── audio/        SoundManager, AmbientLoopPlayer
        │   │   ├── data/
        │   │   │   ├── local/       DataStoreManager, JsonDataLoader
        │   │   │   ├── model/       ContentModels, UserProgress
        │   │   │   └── repository/  CyberRepository
        │   │   ├── game/
        │   │   │   ├── engine/      GameWorld, GameState, NpcData, KeyStrength
        │   │   │   └── renderer/    WorldRenderer, SpriteRenderer, GameSprites
        │   │   ├── research/     ResearchLogger (modo investigador)
        │   │   └── ui/
        │   │       ├── components/  CyberComponents
        │   │       ├── navigation/  Screen, CyberturaNavHost
        │   │       ├── screens/     mundo, tutorial, minijuegos, resultados, medallas
        │   │       ├── theme/       Color, Type, Theme
        │   │       └── viewmodel/   GameWorld, Gameplay, Badges
        │   └── res/
        │       ├── drawable/     sprites de personajes y tiles (PNG)
        │       └── raw/          música y efectos de sonido (MP3)
        ├── test/                 pruebas unitarias
        └── androidTest/          pruebas instrumentadas
```

## Requisitos

- **Android Studio** compatible con AGP 9.4.1 y Kotlin 2.2.10 (se recomienda la versión estable más reciente).
- **Android SDK 37**. Android Studio ofrece instalarlo al sincronizar si falta.
- **Java**: Gradle se ejecuta con **JDK 25** (`gradle/gradle-daemon-jvm.properties`) y lo descarga solo la primera vez. El código se compila con nivel Java 11. Desde Android Studio no hay que instalar nada aparte.
- **Conexión a internet en la primera compilación**, para descargar Gradle 9.6, el JDK y las librerías. Después compila sin conexión.
- Un dispositivo o emulador con **Android 7.0 (API 24)** o superior.

## Compilar y ejecutar

```bash
git clone https://github.com/byckyka/cybertura.git
cd cybertura
```

**Desde Android Studio:** *File → Open*, selecciona la carpeta `cybertura`, espera la sincronización de Gradle y ejecuta la configuración `app`. Android Studio crea `local.properties` con la ruta del SDK.

**Desde la terminal:**

```powershell
# Windows
.\gradlew.bat assembleDebug     # genera app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat installDebug      # instala en el dispositivo o emulador conectado
```

```bash
# macOS / Linux
./gradlew assembleDebug
./gradlew installDebug
```

### Versión release (optimizada)

```powershell
.\gradlew.bat assembleRelease   # genera app/build/outputs/apk/release/app-release.apk (~5,5 MB)
.\gradlew.bat installRelease
```

La versión release se reduce y optimiza con R8 (`proguard-rules.pro` conserva las clases de `data.model` que lee Gson). Como la app no se publica en tiendas, se firma con la **clave debug** que Android Studio genera en cada PC; por eso compila en cualquier equipo sin configurar nada. Si se instala una versión firmada en otro PC sobre una ya instalada, Android pide desinstalar primero, y eso borra el progreso y los datos del piloto: **exporta el CSV antes**.

En Android Studio se cambia entre debug y release en *Build → Select Build Variant*.

No hace falta configurar variables de entorno, claves ni servicios externos.

## Pruebas

```powershell
.\gradlew.bat test                   # pruebas unitarias (JVM)
.\gradlew.bat connectedAndroidTest   # pruebas instrumentadas (requiere dispositivo)
```

Hay 16 pruebas unitarias:

- `ContentAndProgressUnitTest` (4): cálculo de progreso, nivel, XP y rango en `UserProgress`.
- `ContentValidationTest` (12): validan `cyber_content.json` y el mapa antes de compilar. Revisan que haya 8 módulos y 2500 XP en total, que los id no se repitan, que cada minijuego tenga sus datos completos, que la forja de llaves se pueda ganar, que los portales vayan y vuelvan a casillas transitables y que todos los maestros sean alcanzables desde el punto de inicio.

Si alguien edita el JSON y comete un error, `test` falla y dice qué módulo y qué dato revisar.

## Editar el contenido educativo

Los textos, preguntas y minijuegos no están escritos en el código: se leen de `app/src/main/assets/cyber_content.json`. Así, docentes y expertos pueden ajustar el contenido sin tocar Kotlin. Cada módulo tiene:

- `id`, `title` y metadatos del módulo
- `challenges`: lista de retos, cada uno con un `type` (`FORGE_KEY`, `SWIPE_SORT`, `CHAT_STORY`, `SPOT_THE_CLUES` o `TAP_DEFENSE`) y los datos de ese minijuego

Después de editar el JSON, ejecuta `.\gradlew.bat test` para validarlo y vuelve a compilar la app para ver los cambios.

> Los nombres de los recursos en `res/drawable` y `res/raw` solo pueden tener minúsculas, dígitos y `_`. Cualquier otro carácter rompe la compilación.

## Modo investigador

Herramienta oculta para la prueba piloto; los niños no la ven.

1. En la pantalla de medallas, toca **5 veces seguidas** el título *Sala de trofeos*.
2. Escribe el PIN **2026**.
3. En *Nueva sesión*, escribe un **código de participante** (por ejemplo `P01`), nunca el nombre del niño, y pulsa *Iniciar sesión*. La aventura se reinicia desde el tutorial para que cada participante empiece de cero.
4. Mientras la sesión está activa se registra cada intento de minijuego. Al terminar, pulsa *Terminar sesión*.
5. En *Exportar resultados*, elige **Guardar en carpeta** o **Compartir** (Drive, correo…). Se genera un solo archivo, `cybertura_resultados_<fecha>.csv`, que abre directo en Excel.

El CSV tiene una fila por participante y módulo, con estas columnas: Participante, Fecha, Módulo, Jefe, Tema, Minijuego, Resultado, Intentos, Tiempo, Tiempo (segundos), Aciertos, Errores, Ayudas, Estrellas y XP ganada. Al final incluye una guía que explica cada columna. *Ayudas* solo aplica al minijuego de detective (`SPOT_THE_CLUES`), y cuenta las veces que el niño tocó el botón 💡 Ayuda.

*Borrar todos los datos del piloto* elimina las sesiones del teléfono, así que conviene exportarlas antes.

## Privacidad

CyberTura está pensada para menores de edad:

- No pide el permiso `INTERNET` ni ningún otro.
- No tiene cuentas, analítica ni SDK de terceros.
- Guarda el progreso del juego en el propio dispositivo, con DataStore.
- El modo investigador guarda los intentos **solo en el teléfono**, identificados con un código (`P01`), sin nombres, ubicación ni identificadores del dispositivo. Nada sale del teléfono salvo que el investigador exporte el CSV.
- Los CSV del piloto y los consentimientos firmados **no deben subirse a este repositorio**, porque es público.

## Documentación adicional

- [`app/docs/DOCUMENTACION_TECNICA.md`](app/docs/DOCUMENTACION_TECNICA.md): auditoría técnica completa (arquitectura, persistencia, seguridad, pruebas y trabajo futuro). Es del 6 de octubre de 2026, así que no incluye el octavo módulo, los minijuegos ni el audio que se agregaron después.
- [`app/docs/CAPITULO_DISENO_ARQUITECTURA.md`](app/docs/CAPITULO_DISENO_ARQUITECTURA.md): capítulo de diseño de ingeniería, con la justificación de pasar de Unity a Kotlin nativo.
- El documento final del proyecto de grado se entrega por separado a la universidad y no está en este repositorio.

## Autoría y créditos

Desarrollado por **Santiago Matajira Rueda** ([@byckyka](https://github.com/byckyka)) como Trabajo de Grado para optar al título de Ingeniero de Sistemas.

| Rol | Nombre |
|---|---|
| Director | Javier Pinzón |
| Asesores | Yamid Gamba, Daniel Cabrera, Leonardo Talero, Leonardo Pardo |
| Institución | Universidad Autónoma de Bucaramanga, Facultad de Ingeniería, Ingeniería de Sistemas |

### Recursos de terceros

Los sprites, los tiles del mapa, la música y los efectos de sonido (`res/drawable/*.png` y `res/raw/*.mp3`) provienen de [Pixabay](https://pixabay.com) y se usan bajo la [Licencia de contenido de Pixabay](https://pixabay.com/service/license-summary/). Esa licencia permite el uso gratuito sin atribución obligatoria; aun así se reconoce aquí y en la pantalla de créditos del juego. El tileset del agua es de [Pipoya](https://pipoya.net). Los derechos de estos recursos pertenecen a sus autores.

El ícono de la app (`res/mipmap-*/ic_launcher*.png`) se generó con inteligencia artificial (ChatGPT, de OpenAI) y se adaptó como ícono adaptativo de Android.

© 2026 Santiago Matajira Rueda. Todos los derechos reservados sobre el código y el contenido educativo; no incluye los recursos de terceros listados arriba. Este repositorio es público para consulta académica. Para usar, copiar o redistribuir el código o el contenido hace falta autorización del autor.
