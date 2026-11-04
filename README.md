# wh01s17 Launcher

Launcher para Android que recrea el uso de Hyprland (workspaces, tiling,
barra modular, menú y atajos) con el tema `wh01s17` de
[dotfiles](https://github.com/wh01s17/dotfiles).

Estado: en desarrollo, con los **hitos 0–6 terminados (`0.2.0`)**. Ya incluye catálogo
de apps con búsqueda difusa, barra modular, navegación propia para HyperOS
y diez escritorios con tiles, edición y persistencia. Los tiles abren las
apps; no las incrustan en el escritorio.

El menú wh01s17 reúne apps, comandos, ajustes y atajos, con ranking persistente
y navegación por teclado. Los atajos de teclado y los gestos se configuran como en
`bindings.lua` (`ALT` hace de `SUPER`; `ALT + K` muestra la tabla). El próximo paso es el
**Hito 7: widgets**. El avance se mantiene en [`plan.md` §12](plan.md#12-estado-actual).
Esta versión se instala como APK debug; la distribución firmada llega en el Hito 11.

Probado en Redmi Note 13 Pro 5G con HyperOS 2.0.204 (Android 15).

[Historial de cambios](CHANGELOG.md).

Documentación:

- [Preparar el entorno y compilar](docs/setup.md).
- [Usar y editar los escritorios](docs/workspaces.md).
- [Configurar la barra](docs/bar.md).
- [Menú wh01s17](docs/menu.md).
- [Atajos de teclado y gestos](docs/bindings.md).
- [Sistema de diseño](docs/design-system.md).
- [Gestos en Xiaomi, pruebas y recuperación](docs/xiaomi.md).

Licencia: [MIT](LICENSE).
