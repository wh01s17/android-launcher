# Rendimiento

Objetivos del Hito 11 (plan.md §7): arranque en frío **< 400 ms** en el Redmi Note 13 Pro 5G y
**120 fps** sostenidos al deslizar escritorios y al abrir el menú (pantalla a 120 Hz: 8,3 ms por
fotograma).

## Compilaciones

| Tipo | R8 | Baseline Profile | Id | Uso |
| --- | --- | --- | --- | --- |
| `debug` | No | No | `.debug` | Desarrollo; LeakCanary. ART interpreta casi todo |
| `fast` | Sí | Sí | `.debug` | Uso diario en el teléfono (`./gradlew installFast`), encima de la debug |
| `release` | Sí | Sí | — | La que se publica ([release.md](release.md)) |
| `benchmarkRelease` | Sí | Sí | — | La que se mide; la crea el plugin de Baseline Profile |

R8 reduce el APK de 38 MB (debug) a 3,6 MB. `profileinstaller` instala el perfil al primer arranque
aunque la app llegue por ADB u Obtainium.

## Baseline Profile

El perfil está en `app/src/main/generated/baselineProfiles/` (`baseline-prof.txt` y
`startup-prof.txt`, que R8 usa para ordenar el dex). Es un archivo generado: queda fuera del control
de longitud (§1 R3). Lo genera `BaselineProfileGenerator` (`:benchmark`) recorriendo arranque,
escritorios, menú con búsqueda y menú sin ella. Se regenera a mano cuando cambia mucho el código del
arranque o de esos recorridos:

```bash
~/Android/Sdk/emulator/emulator -avd redmi-api35 -no-window &   # o cualquier API 33+
ANDROID_SERIAL=emulator-5554 ./gradlew :app:generateBaselineProfile
```

Hazlo en el emulador: en el teléfono funciona igual, pero suspende un rato los gestos wh01s17 (ver
abajo).

## Medir en el Redmi

HyperOS no entrega a la app medida los broadcasts de `profileinstaller` que usa Macrobenchmark
(instalar el perfil, vaciar la caché de shaders): con `connectedBenchmarkReleaseAndroidTest`, tres de
las cuatro pruebas fallan con «broadcast was not received». `tools/run-benchmarks.sh` compila la app
con `cmd package compile` (sin perfil: `verify`; con perfil: `speed-profile` después de un primer
arranque) y pasa `wh01s17.manualCompilation=true` para que Macrobenchmark no toque la compilación:

```bash
./gradlew :app:installBenchmarkRelease :benchmark:installBenchmarkRelease
ANDROID_SERIAL=498e4e7a tools/run-benchmarks.sh build/benchmarks
adb uninstall com.wh01s17.launcher.benchmark; adb uninstall com.wh01s17.launcher
```

- La app medida es `com.wh01s17.launcher` (otra app, con datos vacíos), no la del uso diario: matar
  su proceso no toca el launcher ni el servicio de gestos.
- La primera instalación de esa app la confirma HyperOS en pantalla (*Instalar vía USB*).
- **Mientras corre cada prueba, Android suspende los servicios de accesibilidad** (lo hace
  `UiAutomation`): los gestos wh01s17 se apagan, la vigilancia devuelve los botones y, al terminar, el
  servicio vuelve y los oculta solo. Es la vigilancia de §5.9 funcionando; no hay que hacer nada.
- Desbloqueado, con la pantalla encendida (`settings put global stay_on_while_plugged_in 7` y luego
  el valor anterior) y sin tocarlo.

## Resultados (2026-11-14, Redmi Note 13 Pro 5G, HyperOS 2.0.204, 120 Hz)

**Arranque en frío** (`timeToInitialDisplayMs`, 10 iteraciones, sin vaciar la caché de shaders):

| Compilación | Mínimo | Mediana | Máximo |
| --- | --- | --- | --- |
| Debug (Hito 0, `am start -W`) | — | 1750 | — |
| Release sin perfil (`verify`) | 444 | 461 | 667 |
| Release con Baseline Profile | 398 | 406 | 479 |
| Ídem con la caché de nombres de apps¹ | 362 | 403 | 524 |

¹ Medido junto con el aplazamiento que se descartó más abajo; la diferencia con la fila anterior
está dentro del ruido.

**Fotogramas** (`FrameTimingMetric`, 5 iteraciones, con perfil). `frameOverrunMs` negativo = llegó a
tiempo:

| Recorrido | CPU P50 | CPU P90 | CPU P99 | Overrun P90 | Overrun P99 |
| --- | --- | --- | --- | --- | --- |
| Deslizar escritorios (6 deslizamientos) | 3,5 | 6,8 | 9,5 | −2,6 | +0,3 |
| Abrir y cerrar el menú (3 veces) | 3,4 | 6,5 | 13,2 | +0,9 | +13,8 |

**Valoración:**

- **Escritorios: cumple.** Prácticamente todos los fotogramas a tiempo a 120 Hz (P99 a 0,3 ms del
  límite).
- **Arranque: en el límite, sin cumplirlo del todo.** Mediana de 403–406 ms frente a < 400 ms. R8 y
  el perfil lo bajaron de 461 ms y el mínimo ya baja de 400 ms. Lo que queda es el primer fotograma de
  Compose en el hilo principal.
- **Menú: no cumple del todo.** Alrededor de un 10–15 % de los fotogramas llega tarde por ~1 ms
  al abrirlo; el P99 coincide con la subida del teclado (el menú enfoca la búsqueda al abrirse, como
  rofi).

## Lo que se encontró con simpleperf

`simpleperf record --app com.wh01s17.launcher` (el del teléfono; la app de benchmark es
*profileable*) durante un arranque en frío: el **61 % de la CPU de los tres primeros segundos** era
`LauncherActivityInfo.getLabel`, es decir, Android abriendo los recursos de las 156 apps para leer sus
nombres, y los tiles esperaban vacíos. Desde el Hito 11 los nombres se guardan entre arranques
(`LabelCache`, en `no_backup/app-labels.tsv` de los datos de la app): la clave incluye la ruta del APK y el idioma, así
que una actualización o un cambio de idioma vuelven a leer sólo ese nombre. Apenas mueve el primer
fotograma (mediana de 406 a 403 ms), pero los tiles salen con nombre al momento.

Se probó aplazar la vigilancia y los temporizadores hasta que el hilo principal quedara libre
(`IdleHandler`): no se notó en la medida y, si una alarma del pomodoro despierta el proceso, el aviso
de fin de fase se perdería. Se descartó.

**Siguiente paso, si se quiere bajar de 400 ms:** leer una traza de Perfetto del arranque (las guarda
Macrobenchmark, una por iteración) para ver qué compone el primer fotograma, y aplazar lo que no se
ve (menú, panel de notificaciones, selector de widgets).

## Memoria

- LeakCanary en la debug. Recorrido en el emulador (escritorios, menú con búsqueda, Ajustes, Estilo
  y salvapantallas abiertos y cerrados dos veces): **10 actividades destruidas vigiladas y ningún
  objeto retenido**.
- La caché de iconos está acotada a 1/8 del heap (LRU por bytes, `IconCache`, con su prueba).
