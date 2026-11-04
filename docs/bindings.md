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
  escribir en la búsqueda. El menú no usa Escape, Tab, flechas ni Enter si llevan uno de esos
  modificadores, y sólo actúa al soltar una tecla cuya pulsación vio. Un atajo de escritorio con el
  menú abierto lo cierra primero.
- Mantener pulsado repite sólo `tile.resize` y `audio.volume` (como `binde`); el resto salta una vez.

### `SUPER` y el modificador alternativo

Android 15 no entrega la tecla Meta a las apps (ver «Reservadas» abajo). Por eso
`keyboard.superAlternative` (de fábrica `ALT`) hace de `SUPER`: `ALT + 1` dispara `SUPER + 1`.

```json
"keyboard": { "superAlternative": "ALT" }
```

- Opciones: `ALT`, `CTRL` o `none` (sólo `SUPER` de verdad). `SHIFT` no: es escribir mayúsculas.
- Primero se busca la combinación tal cual: `CTRL + ALT + A` (volumen) gana a `SUPER + CTRL + A`.
- No se pueden pulsar con el alternativo los atajos que ya llevan ese modificador
  (`SUPER + ALT + SPACE` sería `ALT + ALT + SPACE`) ni los que caen en una reservada
  (`SUPER + TAB` → `ALT + TAB`, que abre Recientes). `KeyBindingResolver.availability` lo calcula y la
  tabla de `Super + K` lo muestra con el motivo. Asígnalos a otra combinación si los necesitas.
- Si algún día llega un evento con Meta (otro sistema o teclado), `SUPER` funciona tal cual.

### Gestos

| Gesto | Dónde | De fábrica |
| --- | --- | --- |
| `swipe-up` | Un dedo hacia arriba sobre el escritorio (fuera de edición) | `menu.open` |
| `swipe-down` | Un dedo hacia abajo sobre el escritorio | — |
| `two-finger-swipe-left` / `-right` | Dos dedos; el pager no se mueve | `workspace.next` / `workspace.previous` |
| `two-finger-swipe-up` / `-down` | Dos dedos | — (abajo: scratchpad, Hito 8) |
| `double-tap` | Doble toque en un hueco vacío | — (terminal, tarea `Super + Return`) |
| `double-tap-tile` | Doble toque en un tile (lo enfoca primero) | — |

Deslizar a los lados con un dedo cambia siempre de escritorio y no se asigna. Los dobles toques
sólo se esperan si tienen atajo: si no, un toque abriría la app o el menú con ~300 ms de retraso.
Mantener pulsado el módulo de energía de la barra abre el menú de sistema (como `Super + Escape`).

Quedan fuera, con su motivo (plan.md §3):

| Gesto de §3 | Motivo |
| --- | --- |
| Deslizar desde el borde inferior → apps | Con *Gestos wh01s17* esa franja es Inicio (capa 3, §5.9) |
| Arrastrar un tile fuera → quitar | El modo edición ya tiene `quitar` |
| Pellizco rotacional → split | El modo edición ya tiene `split`; poco fiable en un tile pequeño |

## Comandos

| Comando | Argumentos | Qué hace |
| --- | --- | --- |
| `menu.open` | — | Abre el menú wh01s17; si ya está en la raíz, lo cierra |
| `menu.apps` / `menu.system` | — | El menú en Apps / Sistema; si ya está ahí, lo cierra |
| `notifications.open` | — | Cortina del sistema (necesita *Gestos wh01s17*) |
| `shortcuts.show` | — | Tabla de atajos (`Super + K`; también menú → Aprender → Atajos) |
| `audio.volume` | `step` −10…10 | Volumen multimedia, con el panel del sistema |
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

## Editor (Ajustes → Atajos)

En la tabla de `Super + K`, tocar un atajo abre el editor: pulsa la combinación nueva (se guarda tal
cual se pulsa: `ALT + J`, no `SUPER + J`), o `quitar`. Escape sin modificadores cancela. Si la
combinación ya era de otro atajo, se avisa y, al guardar, el otro la pierde. Los gestos no se
capturan: sólo se pueden quitar. `restablecer los de fábrica` borra `bindings` del archivo.

El editor escribe en `files/config/launcher.json` sólo la diferencia con la fábrica (`"none"` para
los que se quitan) y conserva el resto del archivo, reescrito con sangría. Si el archivo tiene
errores, no escribe nada y lo dice: corrígelo antes a mano.

## De fábrica

Copiados de `bindings/tiling.lua` de los dotfiles, sólo con los comandos que ya existen
(`core/config/src/main/assets/default-launcher.json`): `SUPER + SPACE` menú, `SUPER + ALT + SPACE`
apps y `SUPER + ESCAPE` sistema (alternan, como `wh01s17-menu toggle`); `CTRL + ALT + A/Z` volumen
(plan.md §3); `SUPER + 1…0`
escritorio; `SUPER + SHIFT + 1…0` mover tile (y `+ ALT` sin seguirlo); `SUPER (+ SHIFT) + TAB`
siguiente/anterior; `SUPER + flechas` foco; `SUPER + SHIFT + flechas` intercambiar;
`SUPER (+ SHIFT) + MINUS/EQUAL` tamaño; `SUPER + W/T/J/F/L`; `SUPER + ALT (+ SHIFT) + TAB` y
`SUPER + CTRL + ←/→` grupo; dos dedos a los lados, escritorio siguiente/anterior.

Fuera de esta tarea: `Super + Return` (terminal) y `Super + K` (tabla) tienen tarea propia en el
Hito 6; `Super + S` y los atajos de media, Hito 8; `Super + Ctrl + Space` (fondos), Hito 10.

## Reservadas por el sistema

Probado el 2026-11-04 con `adb shell input keycombination` en el Redmi (HyperOS 2.0.204, Android 15)
y en el emulador `redmi-api35`, registrando lo que llega a `MainActivity.dispatchKeyEvent`:

| Combinación | Resultado |
| --- | --- |
| `SUPER + …` (y Meta sola) | No llega ningún evento a la app |
| `ALT + TAB` | No llega; abre Recientes |
| `ALT + 1…0`, `ALT + SPACE`, `ALT + ESCAPE`, `ALT + SHIFT + TAB`, `ALT + F4` | Llegan |
| `CTRL + SPACE`, `CTRL + ALT + A`, `CTRL + ALT + ←`, `CTRL + ALT + DEL` | Llegan |

`SystemReservedChords` guarda sólo lo comprobado. Sin consumir, `ALT + SPACE` genera además
`SEARCH` y `CTRL + ALT + DEL` genera `BACK` (teclas de respaldo de Android); al ser atajos se
consumen y no llegan. Queda por repetir con un teclado físico Bluetooth.
