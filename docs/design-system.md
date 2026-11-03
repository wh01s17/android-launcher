# Sistema de diseño

Guía práctica de `:core:designsystem`. La especificación (paleta, medidas, reglas) está en
[`plan.md` §4](../plan.md#4-sistema-de-diseño-tema-wh01s17); aquí, cómo funciona y cómo tocarlo.

## De dónde sale el tema

```
~/dotfiles (tema wh01s17)                          este repositorio
 themes/wh01s17/*.toml, hyprland.lua, logo.svg ──► theme/wh01s17/            (tools/sync-theme.py)
 apps/.local/share/fonts/wh01s17/*              ──► core/designsystem/src/main/res/font/
                                                    core/designsystem/src/main/assets/licenses/
 theme/wh01s17/*.toml ──► :core:designsystem:generateTokens ──► WhTokens.kt + wh_tokens.xml
```

- **Nada de eso se edita a mano.** Para cambiar un color: edítalo en los dotfiles y ejecuta
  `python3 tools/sync-theme.py`. `theme/wh01s17/SHA256SUMS` registra los hashes: `./gradlew check`
  falla si un archivo sincronizado se editó a mano y avisa si diverge de `~/dotfiles`.
- `generateTokens` genera `WhTokens.kt` (Kotlin, interno al módulo) y `wh_tokens.xml` (colores de
  Android para el tema de ventana). Las reglas de tipos están en `TokenGenerator.kt`
  (`build-logic`): `#RRGGBB` → `Color`; clave `*alpha` o `scale` → `Float`; sección `font` → `sp`;
  el resto de números → `dp`; `hyprland.active-border*` → fuente de borde tipada. Un valor que no
  encaja hace **fallar** el build, no se adivina.
- Qué sobra o falta: `shell.lock.toml` y `shell.polkit.toml` se generan pero ningún componente los
  usa (un launcher no reemplaza la pantalla de bloqueo). `shell.image-picker.toml` llega en el Hito 10.

## Capas

| Capa | Archivos (`core/designsystem/src/main/kotlin/.../designsystem/`) | Qué es |
| --- | --- | --- |
| Tokens | `tokens/WhTokens.kt` (generado) | Valores crudos del TOML. **Internos**: los demás módulos no los ven |
| Tema | `theme/` | `WhColors`, `WhTypography`, `WhSpacing`, `WhControls`, `WhSurfaces`, `WhBorders`, `WhFonts`, `WhTheme` |
| Iconos | `icons/` | `WhGlyph`, `WhIcons` (catálogo curado) |
| Modificadores | `modifier/` | `whBorder`, `whControlSurface`, `rememberWhControlState`, `whTouchTarget` |
| Componentes | `components/` | `WhText`, `WhIcon`, `WhButton`, `WhPanel`, `WhScrim`, `WhBracketLabel`, `WhListRow`, `WhTextField`, `WhTooltip`, `WhToggle` |

Uso: todo va dentro de `WhTheme { }`; sin él, los componentes fallan con un mensaje claro.
Los valores se leen con `WhTheme.colors`, `WhTheme.spacing`, etc. (R6: nunca literales fuera del módulo).

## Decisiones que conviene saber

- **Sin Material.** Sólo `foundation` y `ui`; el estilo es propio.
- **Fuente ×1,25.** Los tamaños del shell son píxeles de escritorio; en el teléfono se multiplican
  por `WhDefaults.FONT_SCALE`. Como `scale-with-font = true`, las medidas de `shell.spacing.toml`
  también se multiplican: el diseño crece junto al texto.
- **Dibujo compacto, toque de 48 dp.** Botones, filas y toggles dibujan a la medida del shell y
  reciben el toque en una caja de 48 dp (`whTouchTarget`). Los campos de texto miden 48 dp.
- **Iconos como fuente.** `WhIcon` dibuja el glifo con `wh01s17 Pixel Icons`; Android no tiene la
  sustitución de glifos de fontconfig, así que un icono nunca va dentro de un `WhText`. 16 dp
  (48 px a 480 dpi) da 2 px por píxel del icono: nítido. Sin `contentDescription` el icono se
  oculta a TalkBack.
- **Borde activo.** Gradiente verde→cian con dirección `(cos θ, sin θ)` en pantalla (45°: arriba a
  la izquierda → abajo a la derecha). Se midió en una ventana real de Hyprland: el verde está arriba
  a la izquierda y el cian abajo a la derecha. Hyprland no escala igual en ventanas no cuadradas,
  así que en tiles muy anchos la transición difiere algo; con 1 dp de grosor apenas se nota.
- **Estados.** Prioridad *Pressed > Focus > Selected > Hover > Normal*. *Pressed* usa el color y el
  borde de hover con el relleno de `pressed-fill-alpha`, que es lo único que define el TOML.
- **`Color` guarda el alfa en 8 bits:** 0,70 se lee 0,702. Las pruebas comparan con 0,005.

## Galería y capturas

La galería (`WhGallery`) vive en `src/debug` del módulo: sólo existe en builds debug.

```bash
adb shell am start -n com.wh01s17.launcher.debug/com.wh01s17.launcher.GalleryActivity
./gradlew :core:designsystem:recordRoborazziDebug   # regrabar las capturas de referencia
./gradlew :core:designsystem:verifyRoborazziDebug   # compararlas (forma parte de ./gradlew check)
```

Las capturas están en `core/designsystem/src/test/screenshots/`, a 480 dpi y 407 dp de ancho (la
pantalla del Redmi). Se regrabán a propósito cuando cambia el diseño; un cambio inesperado en la
comparación es un fallo. Cada estado de control se muestra pasando por la misma ruta que un toque
real (`forcedSource` emite la interacción a cada colector), no se dibuja un estado falso.

## Comparación con el shell del escritorio

Hito 1: «la galería coincide con el shell». Se compara contra los TOML, la captura de la barra de
Hyprland de la sesión del 2026-11-02 y una medición del borde activo.

| Elemento | En el escritorio | En la galería | |
| --- | --- | --- | --- |
| Paleta | `colors.toml` | Los 19 swatches con su hex; una prueba compara los valores | ✓ |
| Tipografía | Departure Mono | Departure Mono, escala ×1,25 | ✓ |
| Iconos | Pixelarticons en code points Nerd Font | 56 glifos; una prueba comprueba que la fuente los trae | ✓ |
| Módulos entre corchetes | `[ texto ]`, corchetes en `muted` | `WhBracketLabel`: igual, colores de `classColors` | ✓ |
| Borde activo | Gradiente `#84c959ee → #45d9eaee` | Verde arriba-izq. → cian abajo-der. | ✓ |
| Estados de control | `shell.controls.toml` | Cinco estados con sus alfas (prueba) | ✓ |
| Menú, fila seleccionada | `shell.menu.toml` | Fondo, borde y texto verdes | ✓ |
| Proporciones exactas del shell | — | No comparadas píxel a píxel con una captura del menú | pendiente |

Pendiente: contrastar a ojo, con el shell abierto al lado, el menú (`Super+Space`) y los popups.
