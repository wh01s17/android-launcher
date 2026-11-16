# Salvapantallas

Estilo → Estilo y configuración → Salvapantallas permite elegir qué se muestra y cómo, los segundos de
inactividad, ver una vista previa y abrir los ajustes de salvapantallas de Android. La vista previa
se cierra tocando la pantalla o pulsando una tecla.

## Qué se muestra (`screensaver` en `launcher.json`)

| Clave | Valores | Qué hace |
| --- | --- | --- |
| `style` | `w` (de fábrica), `clock`, `text`, `minimal` | Lo principal: la W girando, la hora en grande con los dos puntos parpadeando, tu texto en grande con un cursor de terminal, o nada grande |
| `showTime`, `showDate`, `showBattery`, `showText` | `true`/`false` | Líneas debajo: hora, fecha (`sáb 14-11-2026`), batería (`bat 82% +` cargando) y tu texto |
| `text` | hasta 24 caracteres (`wh01s17`) | El texto del estilo `text` y de la línea de texto |
| `color` | `accent`, `cyan`, `foreground`, `red`, `yellow`, `blue`, `magenta` | Papel de la paleta elegida en Estilo |
| `brightness` | 10–100 (%) | Atenúa el color: menos brillo, menos consumo |
| `spinText` | `true`/`false` | El texto del estilo `text` gira en 3D sobre su eje vertical, con el mismo ritmo que la W |
| `speed` | 25–200 (%) | Velocidad del giro de la W y del texto que gira |
| `size` | 40–100 (%) | Tamaño de lo principal (y de las líneas en `minimal`) |

En Estilo, el estilo se elige en un panel con la descripción de cada uno; el texto se guarda al
terminar de escribir. Todo se aplica también al salvapantallas de Android.

**OLED:** el fondo es siempre negro puro (`#000000`): esos píxeles están apagados. Todo deriva
despacio para no marcar la pantalla, y sólo la W se anima a 30 fps; los demás estilos se repintan dos
veces por segundo (lo justo para el parpadeo). Estas tres cosas no se configuran.

## La W

La W conserva las cinco cajas extruidas, la perspectiva, inclinación e iluminación de
`desktop/.config/fastfetch/w-spin.py`, representadas con su rampa ASCII. Da una vuelta cada ocho
segundos sobre negro puro OLED (`#000000`) y se desplaza suavemente para no fijar siempre los
mismos píxeles. Los fotogramas se calculan fuera del hilo de interfaz a unos 15 fps; se cancelan
al ocultarse o desmontarse la vista. El render adaptado usa una muestra por celda, sin el brillo
especular del terminal, para limitar el coste en el teléfono.

Hay dos formas de usarlo:

- **En el inicio:** `idle.screensaver` fija la inactividad en segundos (150 de fábrica, 0 lo
  desactiva, máximo 3600). Sólo cuenta mientras el launcher está en primer plano. Tocar o pulsar
  una tecla lo cierra y consume ese gesto para no abrir un tile accidentalmente. Salir del inicio
  cancela el salvapantallas. No modifica el tiempo de apagado ni mantiene la pantalla despierta:
  si Android la apaga antes, prevalece su configuración.
- **Al cargar o en una base:** el servicio `WhDreamService` aparece como «wh01s17 · W girando» en
  los ajustes de salvapantallas del sistema. Android decide cuándo arranca, según la configuración
  que el usuario elija allí; `idle.screensaver` sólo controla el inicio del launcher. El servicio
  está protegido con `BIND_DREAM_SERVICE` y una pulsación o un toque lo termina.

No sustituye el bloqueo de Android ni cambia la navegación por gestos.

Las zonas negras apagan los píxeles OLED. El texto verde se mueve lentamente; el render no usa
resplandor ni una capa gris que ilumine todo el panel.
