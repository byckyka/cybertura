# Capítulo: Diseño de Ingeniería y Arquitectura

## 1. Introducción al Diseño del Sistema

Este capítulo detalla la arquitectura de software y las decisiones de ingeniería tomadas para el desarrollo del prototipo móvil educativo "Cybertura". 

A lo largo de la fase de desarrollo, la arquitectura del proyecto evolucionó respecto a la propuesta inicial del anteproyecto. Originalmente, se contemplaba el uso del motor gráfico **Unity** en conjunto con **Kotlin**. Sin embargo, durante el diseño detallado, se tomó la decisión estratégica de migrar hacia un desarrollo **100% Nativo en Android utilizando Kotlin y Jetpack Compose**. 

### 1.1 Justificación de la Evolución Arquitectónica (Kotlin Nativo vs Unity)
*   **Rendimiento y Accesibilidad:** Para el público objetivo (niños de 11 a 12 años en Bucaramanga), es probable que el acceso a dispositivos móviles abarque equipos de gama media y baja. Unity añade un *overhead* significativo (motor en segundo plano, librerías C++, mayor consumo de RAM y batería). El desarrollo nativo con Compose garantiza una ejecución fluida y un consumo mínimo de recursos.
*   **Tamaño de la Aplicación (APK):** Los proyectos de Unity tienen un peso base elevado (frecuentemente superior a 30-40 MB vacíos). Un desarrollo puramente nativo reduce drásticamente el tamaño de descarga, facilitando su instalación en dispositivos con poco almacenamiento.
*   **Ecosistema y Mantenibilidad:** Utilizar Jetpack Compose permite aplicar los paradigmas más modernos de desarrollo Android, facilitando el mantenimiento futuro y la escalabilidad del proyecto, prescindiendo de la complejidad de integrar código nativo con scripts de C# en Unity.

---

## 2. Arquitectura de Software

En lugar del patrón MVC (Modelo-Vista-Controlador) tradicionalmente usado en sistemas legados, el prototipo adopta el patrón **MVVM (Model-View-ViewModel)** en conjunto con **UDF (Unidirectional Data Flow)**. Este es el estándar arquitectónico recomendado por Google para aplicaciones desarrolladas con Jetpack Compose.

### 2.1 Patrón MVVM y Flujo de Datos Unidireccional (UDF)
El sistema se divide en responsabilidades claras, evitando el acoplamiento:
1.  **Capa de Interfaz de Usuario (View - Compose):** Pantallas declarativas (`Screens`) que *observan* el estado del ViewModel y reaccionan automáticamente a los cambios. Las vistas emiten *Eventos* hacia el ViewModel (ej. "Mover personaje hacia la derecha").
2.  **Capa de Lógica de Presentación (ViewModel):** Gestiona el estado de la UI y la lógica de negocio temporal. Procesa los eventos de las vistas, ejecuta reglas de negocio (ej. físicas, colisiones) y actualiza un objeto de estado inmutable (`StateFlow`).
3.  **Capa de Datos (Model/Repository):** Encargada de leer y escribir información persistente (progreso en DataStore, lectura del JSON de contenidos).

**Flujo UDF en Cybertura:**
`Vista (Emite Evento) -> ViewModel (Procesa y Actualiza Estado) -> Vista (Se Recompone con el Nuevo Estado)`

---

## 3. Capa de Presentación (Interfaz Gráfica)

La interfaz gráfica abandona el uso de XML tradicional en favor de **Jetpack Compose**, un toolkit UI declarativo.

### 3.1 Estructura de Pantallas
El flujo de navegación del usuario está orquestado y cuenta con las siguientes pantallas principales:
*   **`SplashScreen`:** Punto de entrada. Realiza una carga asíncrona de recursos antes de permitir el acceso al juego.
*   **`MainMenuScreen`:** Interfaz principal de selección. Permite iniciar/continuar la partida, acceder a la configuración o salir.
*   **`GameWorldScreen`:** La pantalla principal interactiva del juego. En lugar de vistas estándar, utiliza el componente de dibujo a bajo nivel `Canvas` para renderizar en tiempo real el mapa, el jugador, los obstáculos y los NPCs.
*   **`TheoryScreen` y pantallas de diálogo:** Componentes modulares superpuestos (overlays) que presentan el contenido educativo (textos, conceptos de ciberseguridad) sin interrumpir el contexto del juego.
*   **`SettingsScreen`:** Gestión de preferencias de audio y configuración del aplicativo.

---

## 4. Motor de Juego Personalizado (Game Engine Core)

Al prescindir de Unity, se diseñó e implementó un motor de juego 2D a medida directamente sobre Android nativo. Toda la lógica del motor reside en la clase `GameWorldViewModel`.

### 4.1 Ciclo de Juego (Game Loop)
El "Game Loop" es el corazón interactivo de *Cybertura*. Se implementó de manera asíncrona utilizando **Corrutinas de Kotlin** (`viewModelScope`).
*   Un bucle infinito (`while (true)`) se ejecuta en un hilo de fondo (Dispatcher optimizado).
*   Se utiliza un `delay` constante (aprox. 16ms) para simular un *tick rate* o tasa de refresco cercana a los 60 *Frames Per Second* (FPS).
*   En cada "tick", se evalúan las intenciones de movimiento, se calculan colisiones y se emite un nuevo estado del jugador.

### 4.2 Sistema de Físicas y Colisiones (Grid-Based)
El mundo del juego opera sobre un sistema de cuadrícula (Tilemap matricial). 
*   **Bounding Boxes:** Las colisiones se calculan utilizando cajas delimitadoras rectangulares (`Rect`).
*   **Detección:** Antes de confirmar el movimiento del personaje, el motor proyecta un "fantasma" en la posición destino. Si este fantasma intersecta con el `Rect` de una celda marcada como "Obstáculo" o "Límites del Mapa", el movimiento es denegado.
*   **Interpolación:** Para evitar saltos bruscos entre celdas (movimiento teletransportado), se implementó una interpolación lineal de coordenadas, logrando un desplazamiento suave (pixel-perfect) sobre la cuadrícula.

---

## 5. Gestión de Datos y Persistencia

Para alinearse con el alcance del prototipo y optimizar el rendimiento, se tomaron decisiones específicas sobre el manejo de la información.

### 5.1 Persistencia de Estado: Preferences DataStore
Se descartó el uso de bases de datos relacionales pesadas como SQLite/Room, ya que el modelo de datos del jugador en esta etapa no requiere relaciones complejas. En su lugar, se utilizó **Android Preferences DataStore**.
*   **Almacenamiento asíncrono y seguro:** Guarda datos primitivos (Módulo actual, Nivel completado, Estado del sonido) de manera reactiva (Flows).
*   **Ventaja:** Permite al niño cerrar la aplicación y retomar su aventura de ciberseguridad exactamente en el punto donde la dejó, garantizando una experiencia continua.

### 5.2 Desacoplamiento del Contenido Educativo (JSON)
Todo el material didáctico, los textos de los NPCs, las preguntas y las lecciones de ciberseguridad **no están quemados (hardcoded) en el código fuente**.
*   Se estructuró una base de conocimientos en un archivo estático `cyber_content.json` ubicado en el directorio de `assets`.
*   Un componente parser (usando la librería `Gson`) deserializa este archivo y lo inyecta en el estado de la aplicación.
*   **Justificación:** Esto permite que psicólogos, educadores o expertos en ciberseguridad modifiquen los textos, preguntas y lecciones del juego directamente en el archivo de texto, sin necesidad de saber programar ni de recompilar toda la aplicación.

---

## 6. Seguridad y Validaciones
Como producto dirigido a menores de edad, el diseño arquitectónico contempla:
*   **Entorno Cerrado (Sandbox):** Todo el progreso y ejecución ocurren localmente en el dispositivo. No se exponen datos personales a servidores externos ni se requieren inicios de sesión con datos sensibles, protegiendo la identidad del menor (cumplimiento ético del anteproyecto).
*   **Tolerancia a fallos:** El cargador de recursos json y el flujo de la UI están envueltos en bloques de seguridad (`try-catch` y manejo de nulos), asegurando que si falta un recurso gráfico o de texto, el aplicativo no se cierre inesperadamente (crash), sino que provea un valor o comportamiento por defecto, protegiendo la experiencia del usuario final.
