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
- **Fila de menú y fila de popup.** `WhListRow` tiene dos tamaños: `Popup` (`popup-row-height`,
  texto `body`) y `Menu` (las medidas de `Menu.qml`). La fila elegida no lleva borde salvo que el
  tema fije `selected-border-width`, como en el shell.
- **Estados.** Prioridad *Pressed > Focus > Selected > Hover > Normal*. *Pressed* usa el color y el
  borde de hover con el relleno de `pressed-fill-alpha`, que es lo único que define el TOML.
- **`Color` guarda el alfa en 8 bits:** 0,70 se lee 0,702. Las pruebas comparan con 0,005.

## Barra

Las medidas de la barra (`barHeight`, `barItemPaddingX`, `barWorkspaceWidth`, subrayado y `gridCell`)
salen de `WhSpacing`. Una barra superior usa `size-horizontal` (36 px) y no `size-vertical`. El
relleno de los módulos (5 dp) es menor que el del shell (8,5 px): con él no cabe la barra a 407 dp.
Ver [`bar.md`](bar.md).

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

Hito 1: «la galería coincide con el shell». Se comparó el 2026-11-03 contra los TOML, el código del
shell (`~/.local/share/wh01s17/shell`: `plugins/menu/Menu.qml`,
`plugins/notifications/components/NotificationCard.qml`, `Commons/Geometry.qml`) y capturas con
`grim` del menú (`wh01s17 menu summon`) y de una notificación, puestas junto a la galería en el Redmi.

| Elemento | En el escritorio | En la galería | |
| --- | --- | --- | --- |
| Paleta | `colors.toml` | Los 19 swatches con su hex; una prueba compara los valores | ✓ |
| Tipografía | Departure Mono | Departure Mono, escala ×1,25 | ✓ |
| Iconos | Pixelarticons en code points Nerd Font | 70 glifos (la captura del catálogo tiene su propia ventana, más alta); una prueba comprueba que la fuente los trae | ✓ |
| Módulos entre corchetes | `[ texto ]`, corchetes en `muted` | `WhBracketLabel`: igual, colores de `classColors` | ✓ |
| Borde activo | Gradiente `#84c959ee → #45d9eaee`, extremos de `gradientEndpoints` | Misma fórmula (`gradientLine`): en el menú, alto, el verde queda arriba y el cian abajo; en la notificación, ancha, a izquierda y derecha | ✓ |
| Estados de control | `shell.controls.toml` | Cinco estados con sus alfas (prueba) | ✓ |
| Borde del menú | `max(1, space(2))`: 2 px, porque `shell.menu.toml` no fija ancho | `menuBorderSize` = 2 × escala (antes 1) | ✓ corregido |
| Fila seleccionada del menú | Fondo y texto verdes, **sin borde** (`selected-border-width` no existe → 0) | Igual (antes llevaba borde verde @ 0,55) | ✓ corregido |
| Medidas del menú | Ancho 300 px, filas de 50 px, texto `heading`, icono `icon-large`, `›` al 36 % | `WhListRowSize.Menu`, `menuWidth`, `menuRowHeight`, `chevron`; sección *menu* de la galería | ✓ corregido |
| Cabecera del menú | `Go…` en `heading` al 58 %, 34 px de alto | En la galería sólo como referencia; el panel real llega en el Hito 5 | Hito 5 |
| Notificaciones | Título y cuerpo en **Liberation Sans** (negrita el título), borde de 1 px, ancho 380 px | Departure Mono (§4.4); el panel llega en el Hito 8 | diferencia aceptada |

Diferencias que quedan, a propósito:

- **Fuente de las notificaciones.** El shell las escribe en Liberation Sans. El launcher usa sólo
  Departure Mono (plan §4.4 y §11.1): no se empaqueta una segunda familia sólo para eso.
- **Desenfoque.** El menú del shell es translúcido con desenfoque; en Android se aplica en el Hito 5
  con `RenderEffect` (API 31+, plan §4.6).

Medidas escritas en código en `Menu.qml` (no salen del TOML) y copiadas en `WhSpacing`: fila de
50 px, ancho de 300 px y borde de 2 px. Si cambian en el shell, hay que cambiarlas a mano.
