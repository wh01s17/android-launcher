# Atajos de teclado y gestos

Los atajos viven en la lista `bindings` de `launcher.json` (plan.md §6.4), como `bindings.lua` en
Hyprland. `core/input/binding` define las combinaciones, los gestos, el catálogo de comandos y los
resolvedores (`KeyBindingResolver`, `GestureBindingResolver`); `core/config` lee la lista con errores
por ruta (`bindings[3].args.id`); `app/input/Shortcuts.kt` los aplica y envía el comando al bus.

## Formato

```json
"bindings": [
  { "keys": "SUPER + 1", "command": "workspace.focus", "args": { "id": 1 } },
  { "keys": "SUPER + W", "command": "none" },
  { "gesture": "swipe-down", "command": "notifications.open" }
]
```

- Cada entrada lleva `keys` **o** `gesture`, más `command` y, si el comando los usa, `args`.
- **Se suma a la de fábrica**: una entrada con la misma combinación o gesto la sustituye (en su
  sitio), `"command": "none"` la quita y las demás se añaden al final. No hace falta copiar la lista.
- Una combinación repetida dentro del mismo archivo es un error: no hay «la última gana».
- Con cualquier error se conserva la config anterior y la barra muestra el aviso (plan.md §6.2).

### Combinaciones

`MODIFICADOR + … + TECLA`, sin distinguir mayúsculas. Los modificadores tienen que coincidir
exactamente: `SUPER + 1` no salta con `SUPER + SHIFT + 1`.

| Modificador | También |
| --- | --- |
| `SUPER` | `MOD`, `META`, `WIN` |
| `CTRL` | `CONTROL` |
| `ALT` | — |
| `SHIFT` | — |

Teclas: `A`–`Z`, `0`–`9`, `F1`–`F12`, `SPACE`, `RETURN` (`ENTER`), `TAB`, `ESCAPE` (`ESC`),
`BACKSPACE`, `DELETE`, `INSERT`, `HOME`, `END`, `PAGEUP`, `PAGEDOWN`, `LEFT`, `RIGHT`, `UP`, `DOWN`,
`MINUS`, `EQUAL`, `COMMA`, `PERIOD`, `SLASH`, `BACKSLASH`, `SEMICOLON`, `APOSTROPHE`, `GRAVE`,
`BRACKETLEFT`, `BRACKETRIGHT`, `PRINT`, `MEDIAPLAYPAUSE`, `MEDIANEXT`, `MEDIAPREVIOUS`. Las del
teclado numérico cuentan como las normales.

- Con el menú abierto sólo valen las combinaciones con `SUPER`, `CTRL` o `ALT`: las demás son
  escribir en la búsqueda. Un atajo de escritorio con el menú abierto lo cierra primero.
- Mantener pulsado repite sólo `tile.resize` (como `binde`); el resto salta una vez.

### Gestos

| Gesto | Dónde | De fábrica |
| --- | --- | --- |
| `swipe-up` | Un dedo hacia arriba sobre el escritorio (fuera de edición) | `menu.open` |
| `swipe-down` | Un dedo hacia abajo sobre el escritorio | — |

Deslizar a los lados cambia siempre de escritorio y no se asigna. Los demás gestos de plan.md §3
(dos dedos, borde inferior, doble toque) llegan con la tarea «todos los atajos de §3» del Hito 6.

## Comandos

| Comando | Argumentos | Qué hace |
| --- | --- | --- |
| `menu.open` | — | Abre el menú wh01s17 |
| `notifications.open` | — | Cortina del sistema (necesita *Gestos wh01s17*) |
| `workspace.focus` | `id` 1–10 | Va al escritorio |
| `workspace.next` / `workspace.previous` | — | Siguiente/anterior con tiles |
| `workspace.layout` | — | dwindle ↔ master |
| `tile.move` | `id` 1–10, `follow` (true) | Lleva el tile a otro escritorio |
| `tile.focus` / `tile.swap` | `direction`: `left`, `right`, `up`, `down` | Mueve el foco / intercambia |
| `tile.resize` | `dx`, `dy` en dp (−1000…1000, 0 por defecto) | Como `resizeactive` |
| `tile.float` / `tile.split` / `tile.fullscreen` | — | Alterna flotante / split / pantalla completa |
| `tile.close` | — | Quita el tile (nunca cierra la app) |
| `group.next` / `group.previous` | — | Pestaña del grupo |
| `edit.toggle` | — | Entra o sale del modo edición |
| `none` | — | Quita un atajo de fábrica |

## De fábrica

Copiados de `bindings/tiling.lua` de los dotfiles, sólo con los comandos que ya existen
(`core/config/src/main/assets/default-launcher.json`): `SUPER + SPACE` menú; `SUPER + 1…0`
escritorio; `SUPER + SHIFT + 1…0` mover tile (y `+ ALT` sin seguirlo); `SUPER (+ SHIFT) + TAB`
siguiente/anterior; `SUPER + flechas` foco; `SUPER + SHIFT + flechas` intercambiar;
`SUPER (+ SHIFT) + MINUS/EQUAL` tamaño; `SUPER + W/T/J/F/L`; `SUPER + ALT (+ SHIFT) + TAB` y
`SUPER + CTRL + ←/→` grupo.

## Límite conocido: `SUPER` en Android 15

Probado el 2026-11-04 en el emulador `redmi-api35` con `adb shell input keycombination`: **ningún
evento con Meta llega a la actividad** (ni la tecla sola ni `META + 2`), y `SUPER + TAB` abre
Recientes. `CTRL + ALT + 3` y `F2` sí llegan y cambian de escritorio. Falta comprobarlo con un
teclado físico en el Redmi (estaba bloqueado durante la prueba). Lo resuelve la tarea siguiente
del Hito 6: detectar los atajos reservados y un modificador alternativo configurable.
