# Historial de cambios

El proyecto usa [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y
versionado semántico. El alcance de cada hito está en [plan.md](plan.md).

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
