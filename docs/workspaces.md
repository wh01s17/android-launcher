# Escritorios y tiles

Los escritorios (`feature/workspaces`) son el inicio del launcher: diez workspaces que se recorren
deslizando, cada uno con sus apps en mosaico, colocadas por el motor de tiling (`core/tiling`).
Un tile es un *lanzador vivo* (plan.md §2.2): tocarlo abre la app, no la incrusta.

## Uso

| Gesto | Fuera del modo edición | En el modo edición |
| --- | --- | --- |
| Tocar un tile | Lo enfoca y abre la app (la ventana crece desde el tile) | Lo enfoca |
| Mantener pulsado un tile | Entra en edición con ese tile enfocado | — |
| Mantener pulsado un hueco | Entra en edición | Entra en edición |
| Tocar un hueco | Abre el menú wh01s17 | Sale de edición |
| Deslizar a los lados | Cambia de escritorio | Desactivado (para poder arrastrar) |
| Deslizar hacia arriba / abajo | Lo que digan `bindings` (de fábrica, arriba abre el menú; ver [bindings.md](bindings.md)) | — |
| Soltar un tile cerca del borde de otro | — | Lo deja a ese lado: el destino se queda donde estaba y comparte su hueco con él; el hueco que deja lo ocupa su vecino (como arrastrar una ventana en Hyprland, sin intercambiar) |
| Soltar un tile en el centro de otro (la mitad central) | — | Los agrupa: el arrastrado entra como pestaña visible |
| Arrastrar un tile a la tira `[1] … [0]` de arriba | — | Lo lleva a ese escritorio (sin seguirlo) |
| Arrastrar una asa verde de un borde | — | Cambia el tamaño de los dos lados |
| Arrastrar un flotante / su punto de la esquina | — | Lo mueve / le cambia el tamaño |
| *Atrás* o *Inicio* | — | Sale de edición |

Mientras se arrastra sobre otro tile, el escritorio se pinta como quedará al soltar (se ve si algún
tile se estira) y se resalta el hueco que ocupará, con «agrupar» si cae en el centro. Si no convence,
se suelta fuera o sobre el mismo tile y no cambia nada; y `deshacer` vuelve atrás lo que ya se soltó.
Para intercambiar dos tiles, `Super + Shift + flechas`.

En edición el escritorio se encoge entero para dejar sitio a la barra de herramientas, sin cambiar
la distribución. La barra de herramientas actúa sobre el tile con el foco:

| Botón | Equivale a | Qué hace |
| --- | --- | --- |
| `deshacer` | — | Vuelve atrás el último cambio de la distribución (mover, agrupar, tamaño, flotar, quitar, añadir…), hasta 50 pasos; un arrastre de borde cuenta como uno. Tocar o cambiar de escritorio no cuenta. Un widget del sistema quitado puede volver como «no disponible» si Android ya lo liberó |
| `+ app` | — | Abre el selector del menú para elegir una app; se añade al escritorio actual |
| `+ widget` | — | Abre el selector de widgets (también menú → Acciones → Añadir widget). Android pide permiso para enlazarlo la primera vez y algunos widgets se configuran antes de añadirse |
| `split` | `Super + J` | Invierte el split del tile (sólo en dwindle) |
| `flotar` | `Super + T` | Flotante ↔ mosaico |
| `completa` | `Super + F` | Pantalla completa (sólo se ve ese tile) |
| `dwindle` / `master` | `Super + L` | Cambia el layout del escritorio |
| `agrupar` | `Super + G` | Une el tile con el foco al último tile enfocado de otro hueco: toca uno, luego otro y `agrupar` |
| `desagrupar` | `Super + G` | Sólo en un grupo: cada pestaña vuelve a su propio hueco |
| `sacar` | `Super + Alt + G` | Sólo en un grupo: saca esa pestaña a un hueco propio, junto al grupo |
| `quitar` | `Super + W` | Quita el tile (la app no se cierra ni se desinstala) |
| `ajustes` | — | Ajustes de wh01s17 (Compatibilidad, etc.) |
| `listo` | — | Sale de edición |

Los atajos de teclado de la columna «Equivale a» llegan en el Hito 6; los comandos ya existen
(`WorkspaceCommand` en `core/input`) y los atiende `WorkspacesViewModel`.

## Comportamiento del mosaico

Como en Hyprland con la config de `~/dotfiles`:

- **Dwindle** (por defecto): cada tile nuevo parte el tile con el foco por su lado largo y queda a
  la derecha o abajo (`force_split = 2`). La orientación de cada split se recalcula con el tamaño
  (girar la pantalla reorganiza); `split` la fija.
- **Master**: el principal ocupa el 55 % del lado largo (arriba en vertical) y el resto se reparte
  en una pila. Un tile nuevo pasa a ser el principal (`new_status = "master"`).
- **Grupos**: como en Hyprland, sólo se crean a petición; un tile nuevo **nunca** se agrupa solo
  (siempre toma su hueco y los demás se achican). Se agrupa soltando un tile en el centro de otro,
  con `agrupar` o con los atajos de [bindings.md](bindings.md#comandos) (`group.toggle`,
  `group.move-into`, `group.move-out`). Las pestañas se ven arriba del grupo; tocar una la muestra.
- **Foco**: al quitar el tile con el foco, pasa al último que lo tuvo (`focus_on_close`).
- Huecos, borde y radio salen del tema (`gaps_in 2`, `gaps_out 2`, `border_size 1`,
  `rounding 10`) salvo que `general` los cambie. Entre dos tiles hay dos `gapsIn`, como en Hyprland.

## Configuración (`launcher.json`)

```json
{
  "version": 1,
  "general": { "layout": "dwindle", "maxTilesPerWorkspace": 8, "gapsIn": 2, "gapsOut": 2,
               "borderSize": 1, "rounding": 10 },
  "animations": { "workspaces": { "enabled": true, "style": "slide", "bezier": "easeOutQuint", "speed": 2 } },
  "bar": { "layout": { "left": [{ "id": "core.workspaces", "persistent": 5 }] } }
}
```

- `general.layout`: `dwindle` o `master`, para los escritorios vacíos (los que tienen tiles
  conservan el suyo).
- `general.maxTilesPerWorkspace`: 1–16, **obsoleto**. Antes, por encima de ese número de huecos un
  tile nuevo se agrupaba solo; ya no limita nada (los grupos son explícitos) y sólo se lee para no
  romper configs y `workspaces.json` guardados. Las medidas van en dp (`gapsIn`/`gapsOut` 0–32,
  `borderSize` 0–8, `rounding` 0–32); si faltan, las del tema.
- `animations.workspaces`: `speed` en décimas de segundo como en Hyprland (2 = 200 ms); `bezier`,
  una de las curvas de `looknfeel.lua` (`easeOutQuint`, `easeInOutCubic`, `linear`,
  `almostLinear`, `quick`). Sólo existe el estilo `slide`; `enabled: false` cambia sin animación.
- `core.workspaces` → `persistent`: cuántos escritorios se ven siempre en la barra (5, como el
  shell). Los demás sólo aparecen si tienen tiles o son el actual.

Igual que `bar`, lo que no se escribe toma el valor de fábrica, y una config con errores no se
aplica (se conserva la anterior y la barra lo avisa).

## Dónde se guarda

El estado (árboles, proporciones, foco, flotantes y qué app hay en cada tile) es de la app, no del
usuario: va en `files/state/workspaces.json` (`DeskCodec`), escrito con `AtomicFile` medio segundo
después del último cambio y al salir del inicio. Un archivo que no se entiende se aparta como
`workspaces.json.broken` y se empieza con los escritorios vacíos. Las apps se guardan por
`paquete/clase` y número de serie del perfil, que no cambian al reiniciar.

Una app desinstalada deja su tile como «no instalada» (atenuado) hasta que se quite: así una
actualización o un perfil de trabajo en pausa no borran la distribución.

## Pruebas

- `core/tiling`: propiedades y distribuciones de referencia (cobertura ≥ 90 % en `check`).
- `feature/workspaces`: modelo (`DeskTest`, `DeskCodecTest`), guardado (`DeskRepositoryTest`),
  ViewModel y la UI (`WorkspacesScreenTest`: capturas a 407 dp y gestos del modo edición).
  `./gradlew :feature:workspaces:recordRoborazziDebug` regraba las capturas.

## Scratchpad

El `special:scratchpad` de Hyprland: un escritorio aparte que baja desde arriba sobre el actual (que
se atenúa), con `Super + S` (`ALT + S` en el Redmi), deslizando dos dedos hacia abajo o tocando la
«S» de la barra. Dentro funciona como cualquier escritorio (abrir apps, modo edición, arrastrar,
deshacer) y, mientras se ve, lo que se añade o se quita va a él. Tocar fuera, *atrás* o cambiar de
escritorio lo oculta.

- **Llevar un tile:** arrástralo a la «S» magenta del final de la tira de arriba, o `Super + Alt + S`
  (con teclado físico con tecla Meta). **Sacarlo:** desde el scratchpad, arrástralo a un número.
- **En la barra:** si el scratchpad tiene algo o se ve, aparece su «S» en magenta tras los números,
  subrayada mientras se ve.
- Se guarda en `workspaces.json` (`"scratchpad"`); al volver al launcher empieza oculto.

## Widgets

Un tile puede ser un widget del sistema (Hito 7). Fuera del modo edición el widget recibe sus
toques (botones, listas) y mantenerlo pulsado entra en edición; en edición se arrastra como
cualquier tile. Quitar el tile libera el widget en Android. Si la app del widget se desinstala, el
tile se queda con «widget no disponible». En `files/state/workspaces.json` se guarda como
`{"id": 7, "widget": 35}` (el id del widget en el `AppWidgetHost` del launcher).

### Widgets propios (wh01s17)

El selector tiene un buscador como el del menú (sin tildes ni mayúsculas, por nombre del widget o
de su app); Enter añade el primero. La sección «wh01s17» añade widgets propios, guardados como
`{"native": "tidal"}`:

| Widget | Qué hace |
| --- | --- |
| TIDAL (tidalamp nova) | Carátula, tiempo, canción, avance (tocar salta) y ⏮ ⏯ ■ ⏭. Pide el acceso a notificaciones (sólo lee la sesión de música). Tocar «▍ tidalamp» cambia de paleta (las de tidalamp; *phosphor* de fábrica) |
| Pomodoro | Sistemas de `pomodoro.sh` (40/10, 25/5, 50/10, 90/20), iniciar/pausar, saltar, reiniciar, ±1 min; sigue contando con la app cerrada |
| Metrónomo | 40–240 BPM (tocar el número lo deja escribir con el teclado), tap tempo, compás y subdivisiones de `wh01s17.metronome`; acento en el primer tiempo |
| Reloj y calendario | Hora, fecha y la semana de lunes a domingo |
| Uso del sistema | RAM y almacenamiento, en amarillo por encima del 80 % |

