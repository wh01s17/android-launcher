# wh01s17 Launcher

Launcher para Android que recrea el uso de Hyprland (workspaces, tiling,
barra modular, menú y atajos) con el tema `wh01s17` de
[dotfiles](https://github.com/wh01s17/dotfiles).

Versión **1.1.4**: los hitos 0–8, 10 y 11 están terminados (el 9, ventanas libres, se descartó).
Diez escritorios con tiles en mosaico (dwindle y master), edición, grupos y scratchpad; barra
modular; menú wh01s17 con búsqueda difusa de apps, comandos, ajustes y atajos; atajos de teclado y
gestos configurables como en `bindings.lua` (`ALT` hace de `SUPER`); widgets del sistema y propios
(TIDAL, pomodoro, metrónomo, reloj, uso del sistema); notificaciones y media; estilo con 16 paletas,
fondos y la W girando como salvapantallas; y navegación propia para HyperOS. Los tiles abren las
apps; no las incrustan en el escritorio. El avance está en [`plan.md` §12](plan.md#12-estado-actual).

El APK firmado se publica en [Releases](https://github.com/wh01s17/android-launcher/releases)
([cómo se publica](docs/release.md)); para el uso diario desde el código, `./gradlew installFast`.

Probado en Redmi Note 13 Pro 5G con HyperOS 2.0.204 (Android 15).

## Capturas del Redmi

Capturas reales de la app instalada. Toca una miniatura para verla completa.

| Escritorio | Menú | Edición |
| --- | --- | --- |
| <a href="docs/screenshots/home-redmi.png"><img src="docs/screenshots/home-redmi.png" width="220" alt="Escritorio principal con reloj nova y tiles"></a> | <a href="docs/screenshots/menu-current-redmi.png"><img src="docs/screenshots/menu-current-redmi.png" width="220" alt="Menú wh01s17"></a> | <a href="docs/screenshots/edit-redmi.png"><img src="docs/screenshots/edit-redmi.png" width="220" alt="Edición de tiles y escritorios"></a> |

| Opciones de Estilo | Paletas | Temáticas de widgets |
| --- | --- | --- |
| <a href="docs/screenshots/style-redmi.png"><img src="docs/screenshots/style-redmi.png" width="220" alt="Opciones de tema y escritorio"></a> | <a href="docs/screenshots/palettes-redmi.png"><img src="docs/screenshots/palettes-redmi.png" width="220" alt="Selector de paletas"></a> | <a href="docs/screenshots/widget-themes-redmi.png"><img src="docs/screenshots/widget-themes-redmi.png" width="220" alt="Selector de temáticas nova y hack"></a> |

[Más opciones de Estilo](docs/screenshots/style-widgets-redmi.png) · [Salvapantallas](docs/screenshots/style-screensaver-redmi.png) · [Configuración](docs/screenshots/style-config-redmi.png) · [Selector de widgets](docs/screenshots/widget-picker-redmi.png) · [Menú Estilo](docs/screenshots/style-menu-redmi.png)

### Widgets propios

| Widget | nova | hack |
| --- | --- | --- |
| Reloj | <a href="docs/screenshots/home-redmi.png"><img src="docs/screenshots/home-redmi.png" width="220" alt="Reloj y calendario nova"></a> | <a href="docs/screenshots/widgets-hack-clock-redmi.png"><img src="docs/screenshots/widgets-hack-clock-redmi.png" width="220" alt="Reloj tty-clock hack"></a> |
| Pomodoro y metrónomo | <a href="docs/screenshots/workspace-4-redmi.png"><img src="docs/screenshots/workspace-4-redmi.png" width="220" alt="Pomodoro y metrónomo nova"></a> | <a href="docs/screenshots/widgets-hack-timers-redmi.png"><img src="docs/screenshots/widgets-hack-timers-redmi.png" width="220" alt="Pomodoro y metrónomo hack"></a> |
| Sistema | <a href="docs/screenshots/workspace-5-redmi.png"><img src="docs/screenshots/workspace-5-redmi.png" width="220" alt="Monitor del sistema nova"></a> | <a href="docs/screenshots/widgets-hack-system-redmi.png"><img src="docs/screenshots/widgets-hack-system-redmi.png" width="220" alt="Monitor neofetch y sysmon hack"></a> |

### TIDAL: todos los visualizadores

Capturados mientras sonaba música en el Redmi. Cada visualizador se cambia tocando su gráfico.

| nova | hack |
| --- | --- |
| **fine**<br><a href="docs/screenshots/tidal-nova-fine-redmi.png"><img src="docs/screenshots/tidal-nova-fine-redmi.png" width="240" alt="TIDAL nova, visualizador fine"></a> | **blocks**<br><a href="docs/screenshots/tidal-hack-blocks-redmi.png"><img src="docs/screenshots/tidal-hack-blocks-redmi.png" width="240" alt="TIDAL hack, visualizador blocks"></a> |
| **bars**<br><a href="docs/screenshots/tidal-nova-bars-redmi.png"><img src="docs/screenshots/tidal-nova-bars-redmi.png" width="240" alt="TIDAL nova, visualizador bars"></a> | **cava**<br><a href="docs/screenshots/tidal-hack-cava-redmi.png"><img src="docs/screenshots/tidal-hack-cava-redmi.png" width="240" alt="TIDAL hack, visualizador cava"></a> |
| **mirror**<br><a href="docs/screenshots/tidal-nova-mirror-redmi.png"><img src="docs/screenshots/tidal-nova-mirror-redmi.png" width="240" alt="TIDAL nova, visualizador mirror"></a> | **scope**<br><a href="docs/screenshots/tidal-hack-scope-redmi.png"><img src="docs/screenshots/tidal-hack-scope-redmi.png" width="240" alt="TIDAL hack, visualizador scope"></a> |
| **curve**<br><a href="docs/screenshots/tidal-nova-curve-redmi.png"><img src="docs/screenshots/tidal-nova-curve-redmi.png" width="240" alt="TIDAL nova, visualizador curve"></a> | **hexdump**<br><a href="docs/screenshots/tidal-hack-hexdump-redmi.png"><img src="docs/screenshots/tidal-hack-hexdump-redmi.png" width="240" alt="TIDAL hack, visualizador hexdump"></a> |

[Historial de cambios](CHANGELOG.md).

Documentación:

- [Guía de uso](docs/usage.md).
- [Preparar el entorno y compilar](docs/setup.md).
- [Usar y editar los escritorios](docs/workspaces.md).
- [Configurar la barra](docs/bar.md).
- [Menú wh01s17](docs/menu.md).
- [Atajos de teclado y gestos](docs/bindings.md).
- [Sistema de diseño](docs/design-system.md).
- [Gestos en Xiaomi, pruebas y recuperación](docs/xiaomi.md).
- [Estilo y fondos](docs/style.md), [notificaciones y media](docs/notifications.md) y
  [salvapantallas](docs/screensaver.md).
- [Rendimiento](docs/performance.md), [accesibilidad](docs/accessibility.md),
  [publicar una versión](docs/release.md) y [pruebas manuales](docs/manual-qa.md).

Licencia: [MIT](LICENSE).
