# Compatibilidad con Xiaomi (HyperOS)

Matriz de pruebas y observaciones de la estrategia de gestos descrita en
[`plan.md` §5.9](../plan.md#59-compatibilidad-con-xiaomi-miui--hyperos).
Se actualiza tras cada prueba en el dispositivo; lo más reciente, arriba en
cada tabla.

## Dispositivo

| Dato | Valor | Fuente |
| --- | --- | --- |
| Modelo | Redmi Note 13 Pro 5G (`2312DRA50G`) | `adb devices -l` |
| Código | `garnet` (producto `garnet_global`) | `ro.product.device` |
| HyperOS | `OS2.0.204.0.VNRMIXM` (ROM global; `V` = Android 15) | `ro.mi.os.version.incremental` |
| Android | API 35 | `ro.build.version.sdk` |
| Launcher oficial | `com.miui.home` 5.39.40 (`RELEASE-5.39.40.11719-11271650`) | `dumpsys package` |
| Actualizaciones | No recibirá más; combinación fija | Usuario |

## Ajustes relevantes

| Ajuste | Tabla | Significado observado |
| --- | --- | --- |
| `force_fsg_nav_bar` | global | `1` = gestos a pantalla completa forzados (oculta los botones) |
| `navigation_mode` | secure | `0` = 3 botones, `2` = gestos; HyperOS lo cambia solo según `force_fsg_nav_bar` |
| `hide_gesture_line` | global | `1` = barrita de gestos oculta |
| `persist.security.adbinput` | prop | `1` = *Depuración USB (ajustes de seguridad)* activada |
| `persist.security.adbinstall` | prop | Vacío en el Redmi = *Instalar vía USB* desactivado; `adb install` falla con `INSTALL_FAILED_USER_RESTRICTED` |

## Observaciones (2026-11-02)

1. Con `com.miui.home` como launcher: `force_fsg_nav_bar = 1`,
   `navigation_mode = 2`, estable más de 28 h sin reiniciar.
2. Al elegir un launcher ajeno (`bitpit.launcher`), HyperOS escribe al
   instante `force_fsg_nav_bar = 0` y `navigation_mode = 0` (3 botones), sin
   reiniciar.
3. `adb shell settings put global …` falla con `SecurityException` (falta
   `WRITE_SECURE_SETTINGS`), aunque `persist.security.adbinput = 1` y
   `com.android.shell` tiene `WRITE_SECURE_SETTINGS: granted=true`.
   **Funciona tras reconectar ADB** (sesión nueva) después de activar el
   interruptor de seguridad.
4. Con el launcher ajeno, al escribir `force_fsg_nav_bar = 1` HyperOS pasa
   solo `navigation_mode` a `2` y el valor se mantiene, **pero ningún gesto
   funciona** (inicio, atrás, recientes): los botones desaparecen y el
   teléfono queda sin navegación. En HyperOS 2 los gestos los procesa
   `com.miui.home`.
5. Al restaurar `force_fsg_nav_bar = 0`, `navigation_mode` vuelve solo a `0`
   en unos segundos.
6. Con *Instalar vía USB* activado (`persist.security.adbinstall = 1`),
   `adb install` funciona. `cmd role add-role-holder android.app.role.HOME
   <paquete>` cambia el launcher predeterminado sin diálogos, y sirve tanto
   para poner el nuestro como para restaurar `bitpit.launcher`.

**Conclusión:** la capa 2 sola no sirve en este equipo. La navegación con
gestos depende de la capa 3 (accesibilidad) y la capa 2 sólo se usa para
ocultar los botones mientras la capa 3 está viva (modo *gestos wh01s17*).

## Matriz de gestos

`✓` funciona, `✗` no funciona, `—` sin probar.

| Fecha | Launcher | Configuración | Inicio | Atrás | Recientes | Tras reinicio |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-11-02 | `bitpit.launcher` | Capa 2 sola (`force_fsg_nav_bar = 1`) | ✗ | ✗ | ✗ | — |
| 2026-11-02 | `bitpit.launcher` | Capa 1 (3 botones) | ✓ | ✓ | ✓ | — |
| 2026-11-02 | wh01s17 (Hito 0, pantalla negra) | Capa 1 (3 botones) | ✓ | ✓ (no cierra el launcher) | — | — |
| — | wh01s17 | Capa 3 con botones visibles | — | — | — | — |
| — | wh01s17 | Modo *gestos wh01s17* (capas 2 + 3) | — | — | — | — |

## Recuperación

Si el teléfono queda sin botones ni gestos:

```bash
adb shell settings put global force_fsg_nav_bar 0
```

O, sin PC: elegir de nuevo el launcher oficial en *Ajustes → Aplicaciones →
Aplicaciones predeterminadas → Pantalla de inicio* (si es accesible), lo que
devuelve los botones.
