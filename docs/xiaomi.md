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
7. (2026-11-03) Al volver a poner `com.miui.home` con `cmd role add-role-holder`, HyperOS
   **no** restaura los gestos: deja `force_fsg_nav_bar = 0` y `navigation_mode = 0` (3
   botones). Hay que escribir `force_fsg_nav_bar = 1` a mano; con el launcher oficial,
   `navigation_mode` pasa solo a `2` y los gestos funcionan.

8. (2026-11-03) Si muere el proceso de un servicio de accesibilidad, Android lo marca como
   *crashed* (`dumpsys accessibility` → `Crashed services`) y no lo vuelve a enlazar, y MIUI
   rechaza reiniciarlo sin *Inicio automático* (`AutoStartManagerService: Reject
   RestartService`). Quitarlo y volver a ponerlo en `enabled_accessibility_services` lo
   revive (lo hace `AccessibilityReviver`, con `WRITE_SECURE_SETTINGS`).
9. (2026-11-03) **«Forzar detención» quita el servicio de `enabled_accessibility_services`**
   y mata los dos procesos: con el modo activo y otra app delante, el teléfono queda sin
   botones ni gestos. Se recupera reiniciando (al arrancar, `MainActivity.onResume` devuelve
   los botones) o por ADB.
10. (2026-11-03) Mientras hay una conexión `UiAutomation` (pruebas instrumentadas), Android
    suspende los demás servicios de accesibilidad salvo con
    `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`.
11. (2026-11-03) `connectedDebugAndroidTest` desinstala la app al acabar (y con ella su rol
    HOME y el permiso concedido); la reinstalación siguiente pide confirmar *Instalar vía USB*
    en el teléfono. Para evitarlo:
    `-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`.

12. (2026-11-04) En la prueba ADB del menú se observó que `uiautomator dump`
    suspende momentáneamente el servicio y cambia los insets; tras terminar, la
    vigilancia vuelve a enlazar el servicio y oculta los botones. Las coordenadas
    del XML pueden no coincidir con la pantalla estable. Se ajustaron los toques
    mediante capturas; detalles y resultados en [`menu.md`](menu.md).

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
| 2026-11-03 | wh01s17 (Hito 2) | Modo *gestos wh01s17* (capas 2 + 3): botones ocultos | ✓ | ✓ (ambos lados) | ✓ (subir y mantener) | — |
| 2026-11-03 | `com.miui.home` | Capa 3 con botones visibles (`force_fsg_nav_bar = 0`) | ✓ | ✓ (ambos lados) | ✓ | — |
| 2026-11-03 | wh01s17 (Hito 4) | Modo *gestos wh01s17* tras `adb reboot` y desbloquear | ✓ | ✓ (izquierda) | — | ✓ (servicio enlazado y botones ocultos solos, `force_fsg_nav_bar = 1`) |

Gestos simulados con `adb shell input swipe` / `input motionevent`; las acciones salen del log
`WhNavigation: action: …`. Con el teclado abierto, la franja inferior no recibe toques y las
laterales terminan encima del teclado (y = 1589 en vez de 2664).

## Vigilancia (modo *gestos wh01s17*)

| Fecha | Caso | Resultado |
| --- | --- | --- |
| 2026-11-03 | Matar el proceso `:nav` (`run-as … kill -9`) | Botones de vuelta en 66 ms; servicio revivido y botones ocultos de nuevo en 760 ms |
| 2026-11-03 | Test instrumentado `NavigationWatchdogTest` | Pasa (< 1 s; ~5–70 ms tras el aviso de muerte) |
| 2026-11-03 | Desactivar el servicio en Ajustes | `onUnbind` devuelve los botones; no se revive |
| 2026-11-03 | HyperOS pone `force_fsg_nav_bar = 0` (simulado) | Se reescribe; al 6.º en un minuto se rinde (`GaveUp`) y quedan los botones |
| 2026-11-03 | «Forzar detención» con Ajustes delante | Sin botones ni gestos hasta volver al inicio o reiniciar (observación 9) |
| 2026-11-03 | `am force-stop` con el launcher delante | Android quita el servicio de `enabled_accessibility_services`; al reabrir, la vigilancia devuelve los botones (`navigation binding died`). El servicio no se revive: hay que activarlo otra vez en Accesibilidad |

## Recuperación

Si el teléfono queda sin botones ni gestos: **reinicia** (mantén el botón de encendido →
*Reiniciar*). Al arrancar, el launcher comprueba el servicio y devuelve los botones. Con un PC:

```bash
adb shell settings put global force_fsg_nav_bar 0
```

Con el launcher oficial de nuevo como predeterminado, para recuperar los gestos (no los
devuelve solo, observación 7):

```bash
adb shell cmd role add-role-holder android.app.role.HOME com.miui.home 0
adb shell settings put global force_fsg_nav_bar 1
```

O, sin PC: elegir de nuevo el launcher oficial en *Ajustes → Aplicaciones →
Aplicaciones predeterminadas → Pantalla de inicio* (si es accesible), lo que
devuelve los botones.

## Probar la navegación por ADB

```bash
SVC=com.wh01s17.launcher.debug/com.wh01s17.launcher.core.system.navigation.WhNavigationService
adb shell pm grant com.wh01s17.launcher.debug android.permission.WRITE_SECURE_SETTINGS
adb shell settings put secure enabled_accessibility_services $SVC   # ojo: sustituye la lista
adb shell settings put secure accessibility_enabled 1
adb shell dumpsys accessibility | grep -E "Bound|Crashed"
adb logcat -s WhNavigation WhNavigationWatchdog
./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
```

El test instrumentado pone el launcher como HOME, activa el servicio y el modo, mata `:nav` y
lo deja todo como estaba (rol, ajustes, servicios, archivo del modo).
