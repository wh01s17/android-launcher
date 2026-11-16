# Estilo y fondos

Menú → Estilo → Estilo y configuración abre los ajustes propios. Configurar ofrece la misma entrada.
Los cambios se guardan en `files/config/launcher.json` y se aplican sin reiniciar. La pantalla de
ajustes usa la escala elegida; los escritorios la reciben al volver al inicio.

La pantalla va en cinco secciones, de lo general a lo concreto: **Tema** (tema, paleta, modo OLED y
escala de letra), **Escritorio** (fondo, iconos, huecos y radio de los tiles, barra transparente y
rotación), **Widgets** (temática), **Salvapantallas** (inactividad, estilo, miniatura en vivo, qué
mostrar y su aspecto, vista previa) y **Configuración** (importar y exportar). Cada elección de una
lista es una fila «Nombre: actual ›» que abre un panel con todas las opciones (con su descripción o sus
muestras de color); elegir aplica y cierra, atrás o tocar fuera cierran sin cambiar.

- **Tema:** `wh01s17` es el único instalado. Sus colores, fuentes y medidas salen de los TOML
  sincronizados; no se ofrecen temas que la aplicación no contiene.
- **Fuente:** escala del 75 al 200 %, con Departure Mono; también adapta el espaciado al texto.
- **Paleta:** una de las 16 paletas de tidalamp (`phosphor` de fábrica, la de wh01s17). Tiñe todo el
  launcher: barra, menú, tiles, bordes, controles, widgets y la W del salvapantallas. Cada color del
  tema toma el papel equivalente de la paleta (`PaletteRemap`); los avisos (rojo, amarillo, naranja…)
  y el negro OLED no cambian. El widget de TIDAL ya no tiene paleta propia.
- **Widgets:** temática de los widgets propios (reloj, pomodoro, metrónomo, TIDAL y sistema),
  `appearance.widgetTheme`. `nova` (de fábrica) es el tema de tidalamp; `hack` los pinta como la
  salida de una terminal: prompt `~$ orden`, líneas `> clave valor`, barras ASCII `[####....]`,
  cursor que parpadea y teclas `[run]` que se encienden en vídeo inverso. Referencias: el reloj es
  `tty-clock` (cifras de bloques; en grande, avance del minuto, día y año; en completo, `cal` y una
  frase de `fortune` que cambia cada hora), el monitor a pantalla completa abre con `neofetch` (la W,
  el equipo y los cuadros de colores), el pomodoro grande muestra el ciclo `F b F b F b F B` y el
  metrónomo un péndulo `swing [|.....]`. TIDAL lleva la carátula a la izquierda de pista, artista y
  álbum (tocarla abre TIDAL; sin carátula, un hueco `[♪]` que también la abre); lo que no cabe se
  desliza ida y vuelta, todo a la vez, como el marquee de tidalamp; la barra de tiempo se toca o se
  arrastra para moverse por la canción (también en `nova`). Tocar el gráfico cambia el visualizador y
  la elección se recuerda. `nova` ofrece `fine`, `bars`, `mirror` y `curve`; `hack` ofrece `blocks`
  (ecualizador con picos), `cava` (columnas de `#`), `scope` (osciloscopio) y `hexdump` (filas tipo
  `xxd`). Las ocho variantes están en el [README](../README.md#tidal-todos-los-visualizadores).
  Mismos datos y botones;
  respeta las reglas del tema (colores planos, sin brillo ni CRT). Los widgets de otras apps no cambian.
  Para añadir una temática: una entrada en `WidgetTheme` (`core/designsystem/widgets`), su piel
  (`WidgetSkin` en `feature/widgets/native`) y su nombre en Estilo; si falta alguna de las dos
  últimas, no compila. Capturas de referencia en `feature/widgets/src/test/screenshots/`.
- **Barra:** transparencia activable.
- **Tiles:** huecos interior/exterior y radio de 0 a 32, sin cambiar la distribución guardada.
- **Iconos:** monocromos, originales o pixelados, tanto en tiles como en el menú.

## Fondos

Estilo → Escritorio → Fondo, o `SUPER + CTRL + SPACE` (`ALT + CTRL + SPACE` en el Redmi), abre el selector propio.
Incluye sin fondo, marca W, luna llena, luna con la marca escrita e imagen importada.
Los fondos incluidos y el fondo base del escritorio usan negro puro OLED (`#000000`).
Las imágenes importadas conservan sus colores originales. El fondo se
dibuja detrás de los tiles y no altera su contenido. Para verlo completo, usa un escritorio vacío.

Importar imagen abre el selector de documentos de Android. La imagen se decodifica y se copia a
almacenamiento privado, reducida a un máximo de 2048 píxeles de lado. No hacen falta permisos de
almacenamiento ni conservar acceso al documento original. Una imagen inválida conserva la anterior.
Puedes importar Gengar u Opeth desde tus propios archivos; no se distribuyen con la app.

**Usar también como fondo de Android** es una acción separada y explícita. Requiere
seleccionar antes una imagen disponible; no cambia el fondo de bloqueo. Elegir un fondo dentro del
launcher sólo cambia el launcher.

`python3 tools/render-wallpapers.py` genera las tres ilustraciones originales a 1220×2712,
1080×2400 y 1440×3200 en `core/designsystem/src/main/assets/wallpapers/`. Usa sólo Python estándar,
la paleta de `theme/wh01s17/colors.toml` y la geometría lunar de `pixel-art.py` de los dotfiles.
El comando admite `--output RUTA`; funciona sin tener los dotfiles instalados.

## Configuración

Ejemplo de las nuevas claves (las demás siguen tomando los valores de fábrica):

```json
{
  "version": 1,
  "theme": "wh01s17",
  "appearance": { "fontScale": 1.25, "wallpaper": "none", "palette": "phosphor" },
  "apps": { "iconStyle": "monochrome" },
  "general": { "gapsIn": 2, "gapsOut": 2, "rounding": 10 },
  "idle": { "screensaver": 150 }
}
```

Los editores conservan las claves desconocidas y los atajos del usuario; se niegan a escribir
sobre un documento inválido. No se migran ni se reescriben los escritorios al editar el estilo.

## Importar y exportar

En Estilo → Configuración, **Exportar launcher.json** abre el diálogo de creación de documento de
Android y copia el archivo completo, incluidas claves desconocidas. Si no existe archivo del
usuario, exporta la configuración de fábrica. **Importar launcher.json** abre un documento JSON
(máximo 1 MiB); valida tipos, rangos, comandos y versión antes de reemplazarlo de forma atómica.
Los errores muestran su ruta. Una importación fallida conserva el archivo y la última config buena.

La importación sustituye los ajustes y atajos, no los tiles ni los ids de widgets. La imagen
importada es un archivo independiente: no va dentro del JSON; en otro teléfono hay que importarla
también. Una configuración puede indicar `imported` sin tener imagen local: se usa el fondo base.

El salvapantallas se explica en [screensaver.md](screensaver.md).
