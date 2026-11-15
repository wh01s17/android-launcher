# Historial de cambios

El proyecto usa [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y
versionado semántico. El alcance de cada hito está en [plan.md](plan.md).

## [Sin publicar]

## [1.1.3] — 2026-11-14

### Corregido

- El icono de Google Drive (y el de cualquier app cuya capa monocroma trae márgenes fijos en dp)
  salía como un cuadrado vacío en los tiles pequeños: ahora los iconos adaptativos se dibujan a su
  tamaño nativo (72 dp) y se reducen después.

## [1.1.2] — 2026-11-14

### Cambiado

- La barra de tiempo de TIDAL permite moverse por la canción en todas las temáticas: tocar salta a ese
  punto y arrastrar mueve la posición, que se aplica al soltar. En `hack`, la barra `[####....]` sube
  a medio objetivo táctil y sólo cuentan los `#…`.
- Reloj `hack`: sin el cursor junto a la fecha y con las cifras un 20 % más pequeñas, centradas en el
  mismo hueco para que el resto del widget no se mueva.

## [1.1.1] — 2026-11-14

### Corregido

- TIDAL con la temática `hack` vuelve a mostrar la carátula, a la izquierda de pista, artista y álbum,
  y tocarla abre TIDAL. Si la pista, el artista o el álbum no caben, se deslizan ida y vuelta, todos a
  la vez, como el marquee de tidalamp.

## [1.1.0] — 2026-11-14

Temáticas de widgets, salvapantallas configurable y Estilo reordenado.

### Añadido

- Salvapantallas configurable (Estilo → Salvapantallas, `screensaver` en la config): estilo `w` (la W
  girando), `clock` (la hora en grande), `text` (tu texto con cursor de terminal) o `minimal`; líneas
  de hora, fecha, batería y texto; color de la paleta, brillo, velocidad y tamaño. Siempre sobre negro
  puro OLED, con deriva anti-quemado y pocos fotogramas.
- Salvapantallas: interruptor «Girar el texto» (`screensaver.spinText`) que hace girar en 3D el texto
  del estilo `text`, con el ritmo y la velocidad de la W, y una miniatura en Estilo que cambia en vivo
  con cada ajuste (el botón «Vista previa» lo abre a pantalla completa).
- Estilo → *Widgets*: temática de los widgets propios (`appearance.widgetTheme`). Además de `nova`
  (la de siempre), `hack`: reloj con `date`, epoch en hex y `cal`; pomodoro como `focus` con barra
  ASCII; metrónomo como `tick` con el compás `[1][2][3][4]`; monitor `sysmon` con un núcleo por línea;
  TIDAL como `sniff` con cuatro visualizadores (`blocks`, `cava`, `scope`, `hexdump`; tocar cambia).
  En grande y a pantalla completa, el reloj es `tty-clock` con avance del día y del año, `cal` y
  `fortune`; el monitor abre con `neofetch`; el pomodoro muestra el ciclo y la cuenta atrás en bloques;
  el metrónomo, un péndulo. Preparado para añadir más temáticas.
- Cambiar de pestaña en un grupo (deslizando o tocando la pestaña) desliza el contenido: la nueva
  entra por el lado al que se va y la anterior sale por el otro, con la curva de los escritorios.

### Cambiado

- Estilo reordenado en cinco secciones (Tema, Escritorio, Widgets, Salvapantallas, Configuración);
  paleta, iconos, temática de widgets y estilo del salvapantallas se eligen en un panel con la
  descripción o las muestras de cada opción.
- Reloj de widgets: avance del día y del año en vez del segundero (éste sólo a pantalla completa),
  fecha `dd-mm-yyyy` en `hack`, contenido repartido a lo alto en mediano y mes que llena en grande.
  En `hack` las cifras van centradas y el cursor se enciende y apaga con cada segundo. Pomodoro nova
  grande con el ciclo de sesiones.

### Corregido

- La barra ya no se pisa cuando va muy llena (ocho escritorios, pomodoro, avisos, media, fecha…):
  tras compactarse y ocultar lo que está en reposo, acorta los textos (la fecha sin el día de la
  semana, el pomodoro en minutos) y, como último recurso, oculta módulos del grupo derecho.
- Un toque corto o un deslizamiento que empezaba en una zona sin botones de un widget del sistema
  (por ejemplo, el margen del de TIDAL) metía el escritorio en modo edición con vibración a los
  600 ms: la vista del widget no veía levantarse el dedo y su pulsación larga saltaba igual.

## [1.0.0] — 2026-11-14

Hito 11: rendimiento, memoria, accesibilidad y la primera versión firmada. El Hito 9 (freeform) se
descartó.

### Añadido

- Versión firmada: `version.properties` es la única fuente de la versión (`versionCode` automático),
  la release se firma con la clave de `keystore.properties` o de los secretos de CI, y al subir un tag
  `vX.Y.Z` el workflow `release.yml` publica el APK y su `mapping.txt` como GitHub Release con estas
  notas ([docs/release.md](docs/release.md)). La CI de cada push deja además el APK release como
  artefacto (firmado si están los secretos).
- R8 (código y recursos) y Baseline Profile en la release y en la `fast` del uso diario; el APK baja
  de 38 MB (debug) a 3,6 MB. Módulo `:benchmark` con el generador del perfil y Macrobenchmark de
  arranque en frío, escritorios y menú; `tools/run-benchmarks.sh` para medir en HyperOS
  ([docs/performance.md](docs/performance.md)).
- LeakCanary en la build debug.
- Los nombres de las apps se guardan entre arranques: un arranque en frío ya no abre los recursos de
  todas las apps (era el 61 % de la CPU de los tres primeros segundos) y los tiles salen con nombre al
  momento. Una actualización o un cambio de idioma vuelven a leer sólo ese nombre.
- TalkBack: un tile dice el nombre de su app aunque no quepa y el del foco se anuncia como
  seleccionado; el fondo dice en qué escritorio se está. Pruebas de etiquetas y de áreas táctiles de
  48 dp en la barra y los escritorios ([docs/accessibility.md](docs/accessibility.md)).
- Medido en el Redmi: arranque en frío de 461 ms (sin perfil) a 403–406 ms de mediana; deslizar
  escritorios a 120 Hz sin fotogramas tarde (P99 +0,3 ms); abrir el menú, P90 +0,9 ms.
- Guía de uso ([docs/usage.md](docs/usage.md)) y lista de pruebas manuales antes de publicar
  ([docs/manual-qa.md](docs/manual-qa.md)).
- Icono de la app: la «W» del botón del menú de la barra (trazo en el acento sobre el fondo del tema;
  adaptativo y monocromo).
- Estilo → *Modo OLED* (`appearance.oled`): fondos y paneles en negro puro con cualquier paleta.
- Vibración corta al abrir el menú tocando el escritorio o la «W» de la barra, y al entrar en edición
  manteniendo pulsado el escritorio.
- Deslizar en horizontal sobre un tile agrupado cambia de pestaña; en la primera o la última, el
  gesto sigue cambiando de escritorio.
- Estilo → *Girar con el sensor* (`appearance.autoRotate`); de fábrica el launcher va en vertical.

### Cambiado

- El texto cumple el contraste AA (4,5:1) con cualquier paleta: con `phosphor`, el texto secundario
  pasa de `#748178` a `#838f87` (antes 4,1:1 sobre los tiles).
- Salvapantallas al estilo de fastfetch: la W de puntos sombreada gira dos vueltas con pausas.
- Mantener pulsado el escritorio pide al menos 600 ms antes de entrar en edición.
- Con el teclado abierto, el gesto de *inicio* desde abajo sigue funcionando (franja más fina).
- La paleta de tidalamp pasa del widget de TIDAL a Estilo (`appearance.palette`) y tiñe todo el
  launcher: barra, menú, tiles, bordes, controles, widgets y salvapantallas. Los avisos conservan su color.

### Quitado

- El módulo vacío `feature/scratchpad` (el scratchpad vive en los escritorios desde el Hito 8).

### Corregido

- Limpiar los recientes (la X) desde el inicio vuelve al inicio en vez de dejar abierta la última app
  (esa app sigue abierta en recientes); con
  el launcher en `locked_apps` (paso por ADB, `docs/xiaomi.md`) ya no se cierra.
- La notificación de música (TIDAL u otra app) aparece en el panel y en el contador mientras suena
  y se oculta en pausa; antes era al revés.
- El visualizador de TIDAL ya no dibuja un tramo plano a la izquierda: los graves no repiten el
  mismo bin ni se recortan arriba (bordes fraccionarios desde el bin 2, compensación de 3 dB por
  octava y margen sobre 0 dB).
- Estilo: el botón «+» de la escala de fuente ya no se atasca en 104 % (redondeo en vez de truncado).
- La rueda y el puntero del ratón cuentan como actividad para el salvapantallas de inicio.
- El salvapantallas de Android usa la paleta elegida aunque arranque con el proceso dormido.
- Los fondos incluidos se cargan en el tamaño más cercano a la pantalla (1080, 1220 o 1440 px).
- Mensajes de error de fondos e importación en `strings.xml`.

## [0.5.0] — 2026-11-05

Hito 10: estilo, fondos, salvapantallas y configuración. El Hito 9 experimental queda para después de la 1.0.

### Añadido

- Editor de estilo con cambios inmediatos: escala de Departure Mono, transparencia de barra,
  huecos y radio de tiles, iconos monocromos/originales/pixelados. Tema instalado: wh01s17.
- Fondos W y lunas en tres resoluciones, selector propio (`SUPER + CTRL + SPACE`), vista previa,
  importación de imágenes y opción explícita de fijar el fondo de inicio de Android.
- W ASCII animada desde `w-spin.py`, vista previa, inactividad de HOME configurable y servicio
  de salvapantallas de Android al cargar. Tocar para salir consume el gesto.
- Importación/exportación de `launcher.json` mediante SAF, con validación antes de escribir y
  conservación de atajos y claves desconocidas al editar. Los escritorios y widgets se conservan.
- Negro puro OLED en los fondos incluidos, base del escritorio y salvapantallas; el render se
  desplaza suavemente y el servicio no fuerza brillo alto.

### Corregido

- La barra ya no superpone módulos cuando no caben (p. ej. escritorio 6 con el pomodoro en marcha):
  primero se compacta y, si hace falta, oculta el metrónomo parado y la media en pausa.
- El módulo `media` muestra el icono del botón: `⏸` mientras suena y `▶` en pausa.

## [0.4.0] — 2026-11-05

Hito 8: scratchpad, notificaciones y media.

### Añadido

- Scratchpad superpuesto con edición y guardado; `SUPER + S`, dos dedos abajo y «S» en la barra.
- Acceso opcional a notificaciones: insignias por app y perfil, contador en la barra y panel propio
  para abrir y descartar avisos. Aviso emergente configurable con barra de cuenta atrás verde.
- Módulo `media`: sesión de cualquier app, reproducir/pausar al tocar y panel de transporte al
  mantener; atajos `CTRL + ALT + O/P/I` y acciones en Sistema, incluso sin acceso a notificaciones.

### Corregido

- Media sigue a la sesión que empieza a reproducir aunque otra estuviese seleccionada.

## [0.3.0] — 2026-11-05

Hito 7: widgets, y el pulido y el rendimiento pedidos durante el hito.

### Añadido

- Widgets del sistema como tiles, con un selector propio que tiene buscador.
- Widgets propios: TIDAL (tema *nova* de tidalamp, paletas y visualizador elegibles), pomodoro,
  metrónomo (el tempo se escribe tocando el número), reloj con calendario y uso del sistema
  (estilo btop a pantalla completa). Se adaptan al tamaño del tile.
- Módulos de barra `pomodoro` y `wh01s17.metronome`, con su panel al mantener pulsado.
- Servicio en primer plano mientras el pomodoro o el metrónomo corren, con notificación (cuenta
  atrás, pausar, saltar, parar) y aviso al terminar cada fase; permiso de notificaciones opt-in.
- Grupos como en Hyprland, sólo a petición: soltar un tile en el centro de otro, botones del modo
  edición y `SUPER + G` / `SUPER + ALT + G` / `SUPER + ALT + flechas`.
- La «W» del escritorio en el botón del menú.
- Compilación `fast` para el uso diario: no depurable, mismos datos que la debug.

### Cambiado

- La barra de fábrica no repite la de Android (hora, red, bluetooth, batería): fecha a la derecha.
- El menú usa un solo tamaño de letra.

### Corregido

- Las apps nuevas ya no se agrupan solas al pasar de 8 tiles.
- El selector de widgets se cierra con *atrás* y Escape.
- El teclado aparecía «a cámara lenta»: los escritorios ya no se recalculan en cada fotograma de su
  animación. Deslizar escritorios y abrir el menú son mucho más fluidos.

## [0.2.0] — 2026-11-04

Hito 6: teclado físico y gestos configurables.

### Añadido

- Atajos de teclado y gestos en `bindings` de `launcher.json`, sumados a los de fábrica
  (copiados de `bindings/tiling.lua`); `"none"` quita uno.
- `ALT` hace de `SUPER` (`keyboard.superAlternative`), porque Android 15 no entrega la tecla Meta.
- `SUPER + SPACE` / `ALT + SPACE` / `ESCAPE` abren y cierran el menú, el de apps y el de sistema;
  `CTRL + ALT + A/Z` cambian el volumen; `SUPER + RETURN` abre la terminal (`apps.terminal`).
- Tabla de atajos (`SUPER + K`) con lo que hay que pulsar en el teléfono y los que no se pueden,
  y editor que captura una combinación nueva y la guarda en `launcher.json`.
- Gestos: deslizar arriba/abajo, dos dedos, doble toque (si están asignados) y mantener la batería.
- Gestos de borde configurables: cada gesto por separado, grosor y sensibilidad, en vivo.
- Animación propia de vuelta al launcher.

### Corregido

- El menú ya no usa Escape, Tab, flechas ni Enter cuando llevan `ALT`/`CTRL`: `ALT + ESCAPE`
  volvía atrás en lugar de cerrar.

## [0.1.0] — 2026-11-04

Primera versión del MVP (Hitos 0–5).

### Añadido

- Diez escritorios persistentes con tiling dwindle/master, grupos, flotantes,
  pantalla completa y modo edición.
- Catálogo de apps y perfiles, iconos, lanzamiento, info, desinstalación
  confirmada por Android y atajos de apps.
- Barra modular con reloj/calendario, batería, red, Bluetooth, sonido y estado.
- Menú wh01s17 con submenús y búsqueda unificada de apps, acciones, ajustes y atajos.
- Ranking por frecuencia y recencia en DataStore; selector de tiles integrado.
- Navegación de menú por teclado, menú contextual y foco cian.
- Gestos de navegación opcionales por accesibilidad y compatibilidad con HyperOS,
  con vigilancia para recuperar los botones si muere el servicio.
- Sistema de diseño sincronizado con los dotfiles wh01s17 y capturas de referencia.

### Corregido

- La escritura rápida conserva todas las letras y el cursor aunque la búsqueda vaya retrasada;
  Enter espera los resultados correctos y se cancela al cambiar de consulta.
- El contextual recibe teclas también sin foco Compose en HyperOS y restaura su ventana al cerrar.

- Escape se procesa al soltar para funcionar con los eventos observados en el Redmi.
- El menú conserva consulta y estado cuando un lanzamiento falla; los fallos y la
  selección de tiles no incrementan el ranking.

### Alcance

- Los tiles son accesos a apps; no incrustan ventanas de otras apps.
- Widgets, scratchpad, media, atajos globales configurables, fondos propios y
  salvapantallas quedan para los hitos siguientes.
- Esta versión se compila como APK debug. Firma y distribución de release se
  completan en el Hito 11; todavía no hay un APK release firmado publicado.
