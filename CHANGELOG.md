# Historial de cambios

El proyecto usa [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y
versionado semántico. El alcance de cada hito está en [plan.md](plan.md).

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
