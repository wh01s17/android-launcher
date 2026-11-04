# La barra

Guía de `:feature:bar` y de la sección `bar` de `launcher.json`. La especificación está en
[`plan.md` §5.5](../plan.md#55-barra-featurebar); aquí, cómo se configura y cómo funciona.

La barra sigue a la del escritorio (`shell.json`): mismos nombres de opciones (`classColors`,
`dimClasses`, `brackets`, `format`, `formatAlt`, `weekStartDay`), para copiar una entrada de uno al otro.

## Configuración

`files/config/launcher.json` (en el teléfono, con la app debug:
`run-as com.wh01s17.launcher.debug`). Sin ese archivo se usa `default-launcher.json`
(`core/config/src/main/assets/`). Se vuelve a leer **al volver al inicio**: edita, sal y entra.

```json
{
  "version": 1,
  "bar": {
    "transparent": true,
    "outline": false,
    "centerAnchor": "wh01s17.clock",
    "layout": {
      "left":   [{ "id": "core.menu" }, { "id": "core.workspaces" }],
      "center": [{ "id": "wh01s17.clock", "format": "HH:mm", "formatAlt": "ddd d MMM" }],
      "right":  [{ "id": "wh01s17.power", "classColors": { "low": "#e8b53f" } }]
    }
  }
}
```

- `transparent` (por defecto `true`): sin fondo propio. `outline`: contorno con el borde activo.
- `centerAnchor`: el módulo que se mantiene centrado en la pantalla (el reloj, normalmente).
- **Sólo cambia lo que escribes:** lo que falta sale de la config de fábrica, clave a clave.
  `{"version": 1, "bar": {"outline": true}}` sólo enciende el contorno; un grupo de `layout` que no
  aparece se conserva, y `"right": []` lo vacía.
- Las claves que no se conocen se ignoran. Una config con errores **no se aplica**: se conserva la
  anterior, la barra muestra un icono rojo ⓘ y los errores salen con su ruta
  (`bar.layout.left[2].id: falta`). Si el archivo se arregla, el aviso desaparece.
- Un `id` que no existe se pinta como `[ ?id ]` en amarillo, no desaparece sin avisar.

## Opciones comunes a todos los módulos

| Opción | Efecto |
| --- | --- |
| `classColors` | `{ "clase": "#color" }`. Gana la primera clase del módulo con color; si ninguna, `default`; si tampoco, el color del módulo. Sustituye sólo las claves que define: los colores por defecto del módulo se heredan |
| `dimClasses` | Lista de clases que atenúan el módulo al 45 %. Sustituye la lista por defecto del módulo (`[]` la vacía) |
| `brackets` | `true` enmarca el módulo en `[ ]` |
| `bracketColor` | Color de los corchetes (`muted` por defecto) |

Colores: `#RGB`, `#RRGGBB` o `#RRGGBBAA`.

## Módulos

| `id` | Qué muestra | Clases | Por defecto |
| --- | --- | --- | --- |
| `core.menu` | Botón del menú; abre el drawer de apps (el menú wh01s17 llega en el Hito 5) | — | verde |
| `core.workspaces` | Números `1`–`persistent` (por defecto `5`, como el shell) más los que tienen tiles y el activo; los demás vacíos no se muestran. El 10 se llama `0`. Activo en verde con subrayado, ocupados en el color del texto, vacíos al 35 %. Tocar cambia de escritorio | — | — |
| `wh01s17.clock` | Hora y fecha. Tocar: calendario. Mantener: alterna `format` y `formatAlt` | — | `format` `HH:mm` |
| `wh01s17.indicators` | No molestar (luna). Sólo ocupa sitio si hay algo activo | `dnd` | — |
| `wh01s17.power` | Batería (icono + %). Sin batería, nada | `charging`, `full`, `low` (≤ 20 %), `critical` (≤ 10 %), `power-save` | cargando verde, `low` amarillo, `critical` rojo |
| `wh01s17.network` | Red por defecto: Wi-Fi, datos móviles, cable o sin conexión | `wifi`, `cellular`, `ethernet`, `other`, `none`, `no-internet`, `vpn` | `none` atenuado, `no-internet` amarillo |
| `wh01s17.bluetooth` | Apagado, encendido o conectado | `off`, `on`, `connected` | `off` atenuado, `connected` verde |
| `wh01s17.audio` | Volumen de medios | `muted`, `low` (< 50 %), `high` | `muted` atenuado |

El orden de las clases importa para el color: una batería al 8 % sin cargar tiene `critical` y
`low`, y gana `critical`.

## Formato de la hora

Los de `Qt.formatDateTime`, como el reloj del escritorio: `d dd ddd dddd`, `M MM MMM MMMM`,
`yy yyyy`, `h hh H HH`, `m mm`, `s ss`, `zzz`, `AP`/`ap` (12 horas) y `ww` (semana ISO). El texto
entre comillas simples es literal y `''` es una comilla. El reloj sólo usa un temporizador de
segundos si el formato muestra segundos; si no, uno de minutos. El idioma es el del sistema.

`format` y `formatAlt` alternan mientras el launcher vive; **no se guardan** (vuelve a `format`).

## Calendario

Un mes con la semana ISO, seis filas fijas, hoy marcado y los meses vecinos atenuados. La semana
empieza en `weekStartDay` (`monday` por defecto). Flechas: mes anterior y siguiente; el título
vuelve a hoy; tocar fuera o *atrás* cierra.

## Gesto de la barra

Deslizar la barra **hacia abajo** abre la cortina de notificaciones del sistema. Sólo puede hacerlo
el servicio de accesibilidad (*Gestos wh01s17*): sin él no pasa nada y se avisa **una sola vez**.

## Ancho en el Redmi (407 dp)

La config de fábrica cabe a 407 dp: menú, cinco escritorios, hora, cuatro indicadores y la batería.
Con más módulos, el centro se aparta hacia el espacio libre; si no hay, queda pegado a la izquierda
y puede tapar a la derecha. Con un escritorio ocupado fuera de 1–5, o un aviso, el reloj se desplaza.
`bar-states.png` (`feature/bar/src/test/screenshots/`) muestra los casos.

## Estado y datos

Cada indicador es un `Flow` frío de `:core:system` (`status/`): sólo escucha al sistema mientras la
barra se ve (con 5 s de margen para girar la pantalla). Sin permisos peligrosos (plan.md R8):

- **Bluetooth conectado** exige `BLUETOOTH_CONNECT`, que no se pide aún: hoy sólo se distingue
  encendido de apagado. El opt-in llegará con los ajustes.
- **Notificaciones** (contador) llegan con el acceso a notificaciones del Hito 8.
- **Escritorios**: los reales del Hito 4 (`docs/workspaces.md`): el actual y los que tienen tiles.

## Pruebas

`./gradlew :feature:bar:verifyRoborazziDebug` compara tres capturas a 407 dp (barra de fábrica,
todos los estados, calendario); `recordRoborazziDebug` las regraba.
