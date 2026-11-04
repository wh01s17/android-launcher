# Menú wh01s17 y paleta de comandos

El menú se abre desde la barra, tocando un hueco del escritorio o deslizando
hacia arriba. La misma superficie contiene los submenús, la búsqueda unificada
y el selector de apps para añadir tiles. Ya no hay un drawer separado.

## Buscar y ejecutar

El campo busca por subsecuencia, sin distinguir tildes, entre:

- Apps instaladas, con su icono y marcas de perfil, pausa o suspensión.
- Acciones del launcher: editar, añadir, dwindle/master, float, fullscreen,
  split, foco, intercambio, tamaño, pestañas de grupo y escritorios 1–10.
- Accesos a ajustes de Android: Wi-Fi, Bluetooth, sonido, pantalla,
  accesibilidad, fondo y ajustes generales.
- Atajos habilitados de apps. Android sólo permite leerlos si wh01s17 es el HOME.

La consulta puede tener varias palabras; el paquete también sirve como palabra
clave. Los atajos muestran `App: acción` para distinguir su origen. Al cambiar el
catálogo o volver a abrir el menú se actualiza el índice, fuera del hilo de UI.

Con la consulta vacía se ve la raíz. Apps muestra inicialmente sólo apps; al
escribir, la búsqueda vuelve a ser global. En el selector de tiles siempre se
filtran sólo apps: seleccionar entrega la app al escritorio, sin abrirla ni
contar un uso.

Un fallo de lanzamiento conserva el menú y la consulta y muestra el motivo
(perfil pausado, petición de reanudación o acción no disponible). El menú
contextual de una app conserva sus atajos, info y desinstalación mediante el
diálogo de Android. Quitar un tile nunca desinstala una app.

## Teclado y tacto

| Entrada | Resultado |
| --- | --- |
| Escribir | Filtra apps, acciones, ajustes y atajos |
| Arriba / Abajo | Resultado o fila anterior/siguiente; recorrido circular |
| Tab / Shift+Tab | Siguiente/anterior sin perder la búsqueda |
| Enter o acción Ir del teclado en pantalla | Ejecuta lo seleccionado |
| Derecha, con búsqueda vacía | Entra en el submenú seleccionado |
| Izquierda, con búsqueda vacía | Vuelve desde un submenú |
| Escape o Atrás | Cierra el contextual; después borra la consulta, vuelve a la raíz o cierra |
| Shift+F10 o tecla Menú | Menú contextual de la app seleccionada |
| Tocar una fila | Ejecuta o navega |
| Mantener una app | Menú contextual |
| Tocar fuera | Cierra |
| Inicio | Vuelve al escritorio limpio |

El cursor de teclado tiene contorno cian. Las páginas y resultados se desplazan
para conservar la selección visible. El campo recibe el teclado físico al abrir,
y el teclado en pantalla aparece al tocarlo. El contextual es modal y recibe su
propio recorrido de teclado. La página abierta se conserva al girar la pantalla.

Escape se ejecuta al soltar la tecla, con respaldo en la actividad para el modo
táctil de HyperOS; no depende de recibir primero el evento de pulsación.

## Submenús

| Entrada | Función |
| --- | --- |
| Apps | Catálogo de apps con búsqueda y menú contextual |
| Aprender | Guía breve de uso |
| Acciones | Operaciones frecuentes de los escritorios; las demás se encuentran buscando |
| Estilo | Pantalla y fondo de Android y layout del escritorio; personalización propia en Hito 10 |
| Configurar | Compatibilidad, accesibilidad y ajustes de Android |
| Instalar | Abre Play Store si está instalada |
| Desinstalar | Acceso al catálogo y explicación de la desinstalación contextual |
| Actualizaciones | Versión instalada e información de distribución por APK |
| Acerca de | Versión, identidad y licencia |
| Sistema | Bloquear, diálogo de apagado, cortina y ajustes del sistema |

Bloquear, apagado y cortina usan el servicio opcional de accesibilidad. Sin él se
explica el requisito y se ofrece abrir Accesibilidad; no se concede ningún permiso
automáticamente. Apagado abre el diálogo del sistema: no apaga el teléfono por su
cuenta. Las difusiones al servicio no se exportan a otras apps.

## Ranking persistente

`DataStoreUsageRepository` guarda contador y última fecha de uso por identidad
estable (`app:componentKey`, `shortcut:componentKey:id`, `command:action`). Las apps
de perfiles diferentes no comparten contador. Sólo se cuentan lanzamientos y
comandos aceptados; fallos, navegación, info, desinstalación y selección de tiles
no suman.

El ranking usa el algoritmo de `core/fuzzy`: frecuencia logarítmica y recencia con
vida media de tres días. Con consulta vacía las apps más usadas aparecen primero;
con texto el uso desempata sin reemplazar la coincidencia.

Archivo: `files/menu-usage.preferences_pb`, esquema propio 1, independiente de
`launcher.json` y de los escritorios. DataStore serializa las escrituras en IO. Si
hay corrupción se reinicia sólo este historial. Un fallo de escritura no impide
abrir apps; la próxima lista de resultados muestra el aviso de historial. La siguiente
escritura correcta lo retira.

## Arquitectura y pruebas

- `MenuCatalog`: páginas, acciones y catálogo localizado para buscar comandos.
- `MenuViewModel`: página; `MenuNavigation`: recorrido por teclado y acciones UI.
- `MenuSearchViewModel`: índice unificado, selección, acciones de apps y ranking.
- `SearchResults` y `AppMenuOverlay`: resultados y contextual reutilizables.
- `app/MenuController` y `MenuActions`: conexión con Android y otros features.

Los features se comunican a través de `app` y de comandos; no dependen entre sí.
Los textos y medidas provienen de recursos y del sistema de diseño.

```bash
./gradlew :feature:menu:recordRoborazziDebug
./gradlew check assembleDebug
```

Se prueban búsqueda mixta, cambios del catálogo/rol, selector, fallos, ranking,
persistencia y corrupción, teclado y menú contextual. Las tres capturas de
referencia están en `feature/menu/src/test/screenshots/`; `check` las compara.

La edición del campo se mantiene inmediatamente en Compose y sólo se reinicia con
una señal explícita de abrir/cerrar/limpiar. El índice lleva la consulta y generación
que produjeron sus resultados. Enter pendiente sólo se ejecuta para esa generación;
escribir otra consulta lo cancela. Así la carga del índice no mueve el cursor ni
permite ejecutar una fila de una consulta anterior.

Las teclas de navegación se interceptan antes del teclado en pantalla, mediante la
[API de Compose para eventos previos al IME](https://developer.android.com/reference/kotlin/androidx/compose/ui/input/key/onPreInterceptKeyBeforeSoftKeyboard.modifier).
El contextual restaura el callback de su ventana al cerrarse.

## Validación del MVP en el Redmi — 2026-11-04

APK debug `0.1.0` instalado con ADB en Redmi Note 13 Pro 5G, HyperOS 2.0.204,
Android 15. wh01s17 ya era el HOME y se conservó.

| Caso | Resultado |
| --- | --- |
| Buscar Wi-Fi y ejecutar con Enter | Abre ajustes de Android sin cambiar la red |
| Buscar Calculadora y ejecutar | Abre la app y registra uso |
| Apps después de usar Calculadora | Calculadora aparece primero |
| Recrear sólo el proceso de UI | Ranking conservado e historial idéntico byte a byte |
| Buscar y abrir Calculadora: Científica | Abre el atajo real y registra su uso |
| Tecla Menú y Escape | Abre y cierra el contextual |
| Escape con consulta | Borra la consulta; otro Escape cierra |
| Búsqueda de atajos con `:` | Muestra seis atajos de apps visibles en el equipo de prueba |
| Añadir app | Selector dentro del mismo menú; cancelado sin añadir tiles |
| Ir al escritorio 2 y volver al 1 | Un Enter por comando, estado inicial y tiles conservados |
| Flechas abajo/derecha/izquierda | Abren Configurar y vuelven a la raíz |
| Buscar apagado | Abre el diálogo de HyperOS; cancelado sin apagar |

El diálogo de apagado se comprobó visualmente: UI Automator no devolvía su árbol y
podía dejar el XML anterior. Las pruebas no dependen de ese árbol para confirmar
ventanas del sistema. `uiautomator dump` también puede suspender momentáneamente
accesibilidad y cambiar los insets; se verificó la recuperación de los gestos.

Shift+Tab y Shift+F10 están cubiertos por Compose/Robolectric; la apertura contextual
por ADB se comprobó con la tecla Menú. El bloqueo real de pantalla no se accionó.
Las acciones que necesitan accesibilidad muestran su explicación si falta el servicio.

Se corrigieron durante la validación la edición bajo carga, los resultados cruzados,
la autocorrección de SwiftKey y el envío de teclas a la ventana del contextual.
La suite de menú suma 21 pruebas; `./gradlew check assembleDebug` pasa.

Capturas del dispositivo: [raíz](screenshots/menu-redmi.png),
[Sistema](screenshots/menu-system-redmi.png) y [búsqueda](screenshots/menu-search-redmi.png).
