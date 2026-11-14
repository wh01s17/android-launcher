# Guía de uso

wh01s17 es un launcher que lleva al teléfono el uso de Hyprland: escritorios numerados, tiles en
mosaico, una barra arriba, el menú wh01s17 y atajos de teclado. Esta guía es el recorrido de uso;
cada sección enlaza a la guía detallada.

## 1. Instalar y elegirlo como launcher

1. Instala el APK de la [última versión](https://github.com/wh01s17/android-launcher/releases)
   con Obtainium o `adb install -r wh01s17-launcher-X.Y.Z.apk`. En Xiaomi, activa antes
   *Instalar vía USB* en Opciones de desarrollador.
2. Ajustes de Android → Aplicaciones → Aplicaciones predeterminadas → *Pantalla de inicio* →
   **wh01s17**.
3. Batería de la app en *Sin restricciones* (Ajustes → Compatibilidad lo abre directamente).

Para compilarlo tú mismo: [setup.md](setup.md). Para publicar una versión: [release.md](release.md).

## 2. Navegación en Xiaomi (HyperOS)

Con un launcher que no es el oficial, HyperOS apaga los gestos del sistema y deja los tres botones.
wh01s17 trae sus propios gestos de borde (deslizar desde un lado: atrás; desde abajo: inicio;
desde abajo y mantener: recientes):

1. Ajustes → Accesibilidad → *Gestos wh01s17* → activar.
2. (Opcional, para ocultar los botones) por ADB, con *Depuración USB (ajustes de seguridad)* activada:
   `adb shell pm grant com.wh01s17.launcher android.permission.WRITE_SECURE_SETTINGS`.
3. Mantén pulsado el escritorio → `ajustes` → Compatibilidad → activa el modo *gestos wh01s17*.

Si el servicio muere, los botones vuelven solos. Si algo sale mal y te quedas sin navegación:
`adb shell settings put global force_fsg_nav_bar 0`. Todo en [xiaomi.md](xiaomi.md).

## 3. Escritorios y tiles

- **Deslizar a los lados** cambia de escritorio (1–10); tocar `[n]` en la barra va directo.
- **Tocar un tile** abre su app. **Tocar un hueco** abre el menú.
- **Mantener pulsado** (un tile o un hueco) entra en **edición**: `+ app`, `+ widget`, arrastrar para
  colocar a un lado o agrupar (soltar en el centro), arrastrar a `[1]…[0]` para mover de escritorio,
  asas verdes para el tamaño, `flotar`, `completa`, `dwindle`/`master`, `quitar`, `deshacer`, `listo`.
- **Scratchpad:** dos dedos hacia abajo (o `ALT + S`): un escritorio que aparece encima y se oculta.
- **Widgets:** del sistema o propios (reloj, pomodoro, metrónomo, TIDAL, uso del sistema).

Detalle en [workspaces.md](workspaces.md).

## 4. Menú wh01s17

Deslizar hacia arriba, tocar la W de la barra o `ALT + SPACE`. Busca apps, comandos, ajustes y
atajos de apps en el mismo campo; lo que más usas sube. Submenús *Apps, Learn, Trigger, Style,
Setup, Install, Remove, Update, About, System*. Mantener pulsada una app: atajos, info y
desinstalar. Detalle en [menu.md](menu.md).

## 5. Barra

Arriba, por zonas (izquierda, centro, derecha), como `shell.json`: menú, escritorios, reloj (tocar:
calendario), avisos, media, pomodoro, metrónomo, red, Bluetooth, audio y batería. Deslizar la barra
hacia abajo abre la cortina de Android. Se configura en `launcher.json`: [bar.md](bar.md).

## 6. Teclado físico

Con un teclado Bluetooth o USB, `ALT` hace de `SUPER` (HyperOS no entrega la tecla Meta).
`ALT + K` muestra todos los atajos y permite cambiarlos tocando una fila. Los más usados:

| Atajo | Acción |
| --- | --- |
| `ALT + 1…0` | Ir al escritorio |
| `ALT + SHIFT + 1…0` | Llevar el tile a ese escritorio |
| `ALT + SPACE` | Menú |
| `ALT + RETURN` | Terminal (Termux) |
| `ALT + flechas` | Mover el foco entre tiles |
| `ALT + W` / `T` / `F` / `J` | Quitar / flotar / pantalla completa / girar el split |
| `CTRL + ALT + P` / `O` / `I` | Reproducir o pausar / siguiente / anterior |

Lista completa y formato en [bindings.md](bindings.md).

## 7. Notificaciones y media

Opcional: Menú → Sistema → Notificaciones explica el acceso y abre el ajuste de Android. Con él, los
tiles llevan el número de avisos de su app, la barra el total y hay un panel propio. Los textos no
se guardan ni se envían a ningún sitio (la app no tiene permiso de Internet).
[notifications.md](notifications.md).

## 8. Estilo, fondos y salvapantallas

Menú → Style: paleta (16, `phosphor` de fábrica), modo OLED, escala de letra, huecos y radio de
los tiles, iconos (monocromos, originales o pixelados), fondo propio (W y lunas, o una imagen tuya),
girar con el sensor y tiempo hasta el salvapantallas (la W girando). [style.md](style.md),
[screensaver.md](screensaver.md).

## 9. Copia de seguridad

Estilo → *Exportar* guarda `launcher.json` (barra, atajos, estilo) donde elijas; *Importar* lo
valida y, si tiene errores, dice dónde. La distribución de los escritorios no se exporta.

## 10. Accesibilidad

Con TalkBack, la barra y los tiles se leen (nombre de la app aunque no quepa, el tile con foco como
seleccionado) y todo lo táctil tiene al menos 48 dp. El texto cumple el contraste AA con cualquier
paleta. [accessibility.md](accessibility.md).
