# Plan: wh01s17 Launcher para Android

Launcher de Android que recrea el **uso** de Hyprland, el escritorio de
`~/dotfiles`: workspaces numerados, mosaico (tiling) con huecos y bordes
verde/cian, barra superior modular, menú wh01s17 invocable desde cualquier
lugar, scratchpad, atajos de teclado (Super + …) y su equivalente en gestos.
Visualmente aplica el tema `wh01s17`: superficies casi negras, Departure Mono,
iconos pixel y verde fósforo `#84c959`.

Este documento es la **fuente de verdad** del proyecto. Cualquier agente o
persona debe poder retomar el trabajo leyendo sólo este archivo y el código.

---

## 0. Cómo retomar el trabajo (protocolo para agentes)

1. Lee este archivo completo, en especial [§12 Estado actual](#12-estado-actual)
   y el [§13 Registro de actualizaciones](#13-registro-de-actualizaciones).
2. Toma la **primera tarea sin marcar** del hito en curso. No empieces un hito
   nuevo si el anterior no cumple su *Definición de terminado*.
3. Antes de escribir código, lee los archivos que vas a tocar y respeta las
   [reglas obligatorias](#1-reglas-obligatorias), sobre todo el límite de
   **1000 líneas por archivo**. Sólo se supera con una justificación
   registrada (R1a).
4. Al terminar una tarea:
   - márcala con `[x]` en su hito;
   - ejecuta `./gradlew check` (incluye el control de longitud de archivos);
   - añade una fila al registro de actualizaciones (fecha absoluta
     `AAAA-MM-DD`, hito, resumen, archivos clave);
   - actualiza §12 si cambia el hito en curso o aparece un bloqueo.
5. Si una decisión de este plan resulta inviable, **no la ignores en
   silencio**: documenta el motivo en §11 (Decisiones) y ajusta el plan.
6. Si no puedes continuar (falta SDK, dispositivo, permiso del usuario),
   anótalo como bloqueo en §12 y detente.

`AGENTS.md` y `CLAUDE.md` en la raíz deben existir (Hito 0) y sólo apuntar a
este archivo, para no duplicar información.

**Skills del proyecto** (`.claude/skills/`, instaladas desde un registro y
fijadas en `skills-lock.json`). Úsalas cuando la tarea coincida:

| Skill | Cuándo |
| --- | --- |
| `android-gradle-build-logic` | `build-logic/`, catálogo de versiones, toolchains |
| `android-compose-foundations` | UI en Compose (Hitos 1, 3, 4, 5, 7, 10) |
| `android-kotlin-core` | Kotlin idiomático en cualquier módulo |
| `android-coroutines-flow` | Repositorios y estado con `Flow` (Hitos 2, 3, 8) |
| `android-architecture-clean` | Límites entre módulos, ViewModels, UDF (§5.2–5.3) |
| `android-testing-unit` | Tests de tiling, fuzzy, config y ViewModels (§9) |
| `bash-defensive-patterns` | Scripts de `tools/` |
| `android-di-hilt` | **No aplica**: DI manual (§5.1, §11.1) |
| `android-networking-retrofit-okhttp` | **No aplica**: sin red (R9) |

**Modelo y esfuerzo por hito.** Recomendación del 2026-11-02. El riesgo de
cada hito decide el modelo: lo crítico (no dejar el teléfono sin navegación,
el motor de tiling) va con el modelo principal y esfuerzo alto. Lo bien
acotado va con el modelo equilibrado.

| Hito | Claude Code | Codex (GPT) | Motivo |
| --- | --- | --- | --- |
| 1 · Sistema de diseño | Sonnet 5.5 · `high` | `gpt-6.1-terra` · `high` | Bien especificado; sólo el generador de tokens es delicado |
| 2 · Apps + navegación por accesibilidad | **Opus 5.5 · `xhigh`** | **`gpt-6.1-sol` · `xhigh`** | La vigilancia de §5.9 evita dejar el Redmi sin navegación |
| 3 · Barra | Sonnet 5.5 · `high` | `gpt-6.1-terra` · `high` | Módulos repetitivos sobre un patrón definido |
| 4 · Motor de tiling y workspaces | **Opus 5.5 · `xhigh`** | **`gpt-6.1-sol` · `xhigh`** | Núcleo del proyecto: geometría, árbol dwindle, tests de propiedades |
| 5 · Menú y búsqueda difusa | Sonnet 5.5 · `high` | `gpt-6.1-terra` · `high` | Algoritmo conocido, UI acotada |
| 6 · Teclado y gestos | Opus 5.5 · `high` | `gpt-6.1-sol` · `high` | Conflictos con gestos del sistema y la tecla Super |
| 7 · Widgets | Sonnet 5.5 · `high` | `gpt-6.1-terra` · `high` | `AppWidgetHost` bien documentado |
| 8 · Scratchpad, notificaciones, media | Sonnet 5.5 · `high` | `gpt-6.1-terra` · `high` | APIs estándar |
| 9 · Freeform (experimental) | Opus 5.5 · `xhigh` | `gpt-6.1-sol` · `xhigh` | Hay que investigar la plataforma |
| 10 · Estilo y salvapantallas | Sonnet 5.5 · `medium` | `gpt-6.1-terra` · `medium` | Ajustes de UI y portar `w-spin.py` |
| 11 · Rendimiento y versión 1.0 | Opus 5.5 · `high` | `gpt-6.1-sol` · `high` | Medición de rendimiento, accesibilidad, firma |
| Tareas mecánicas (sincronizar tema, `ktlintFormat`, registro del plan, commits) | Haiku 5.5 · `low` | `gpt-6-luna` · `low` | Sin decisiones de diseño |
| Atasco en algo difícil (bug de geometría, comportamiento raro de HyperOS) | Fable 5.1 · `high` | `gpt-6-astra` · `high` | El más capaz y el más caro; sólo puntualmente |

- **Equivalencias entre proveedores:** son por posicionamiento según la
  descripción de cada fabricante, no por benchmarks propios. Modelos de Codex
  tomados de `~/.codex/models_cache.json` (Codex CLI 0.166.0).
- **Cómo fijarlo:** en Claude Code, `/model` y `/effort`. En Codex,
  `codex -m gpt-6.1-sol -c model_reasoning_effort="xhigh"`, o
  `model`/`model_reasoning_effort` en `~/.codex/config.toml`. En Codex,
  Sol, Astra y Terra arrancan en `low`: fija el esfuerzo siempre.
- **No usar** `max` ni el `ultra` de Codex salvo petición expresa. `ultra`
  coordina varios agentes en paralelo y multiplica el coste.
- **Al cerrar un hito hecho con el modelo equilibrado** (Sonnet o Terra),
  revisarlo con el principal (`/code-review` con Opus, o una revisión con
  Sol) antes de marcarlo como terminado.
- **Cada hito en una sesión nueva:** este plan basta para retomar sin el
  historial. Codex lee `AGENTS.md`, que apunta aquí.

---

## 1. Reglas obligatorias

| # | Regla | Cómo se verifica |
| --- | --- | --- |
| R1 | **Ningún archivo puede superar 1000 líneas sin una justificación registrada.** Aplica a todo archivo de texto versionado: Kotlin, XML, `*.gradle.kts`, TOML, JSON, scripts, Markdown. Se cuentan líneas físicas, incluidas vacías y comentarios. Cualquier archivo puede pasar el límite si está justificado según R1a. | `tools/check-file-length.sh` + tarea Gradle `checkFileLength`, enganchada a `check` y a CI. Un archivo de más de 1000 líneas sin justificación hace fallar el build. |
| R1a | **Cómo se justifica:** una línea en `tools/file-length.allow` con el formato `ruta \| justificación` (mínimo 20 caracteres) y una fila en §11.1 con la fecha. La justificación explica por qué dividir el archivo empeoraría el código o el documento: por ejemplo, una tabla de datos que debe leerse entera, una jerarquía `sealed` que el compilador exige en un solo archivo, o un documento que es la fuente única de verdad. **No valen** "no hubo tiempo" ni "se dividirá luego". | El script falla si una entrada no tiene justificación y avisa (`WARN`) si una entrada ya no hace falta (el archivo bajó de 1000 líneas o no existe). Se revisa en cada PR. |
| R2 | Umbral de aviso a **600 líneas** en archivos de código (`*.kt`, `*.kts`, `*.py`, `*.sh`): al cruzarlo, evalúa dividir el archivo en la misma tarea (por responsabilidad, no por tamaño arbitrario). La documentación sólo está sujeta a R1. | El mismo script emite `WARN`. |
| R3 | Fuera del control (sin justificación): archivos generados o binarios (`build/`, `*.png`, `*.otf`, `*.ttf`, `gradle/wrapper/*`, `*.lockfile`), listados en `tools/file-length.exclude`. Añadir un patrón ahí exige una fila en §11. | Revisión del exclude en cada PR. |

Contenido inicial de `tools/file-length.allow`:

```text
# ruta | justificación (R1a). Una entrada por archivo; registrar también en §11.1.
plan.md | Fuente única de verdad del proyecto para agentes; dividirlo obligaría a leer varios archivos para retomar el trabajo.
```
| R4 | Una función ≤ 80 líneas; un `@Composable` ≤ 120 líneas. | detekt (`LongMethod`). |
| R5 | El código y sus identificadores en inglés; documentación, plan y textos de UI por defecto en español (con `strings.xml` para traducir). Commits en inglés con Conventional Commits, como en `~/dotfiles`. | Revisión. |
| R6 | Los colores, tipografías y medidas sólo salen de los *tokens* del tema (§4). Prohibido escribir `Color(0xFF…)` o `dp` mágicos fuera de `:core:designsystem`. | detekt regla propia o `grep` en CI. |
| R7 | El motor de tiling (`:core:tiling`) es Kotlin puro (JVM), sin dependencias de Android, y con cobertura de tests ≥ 90 %. | Kover en CI. |
| R8 | Nada de permisos peligrosos sin un flujo de *opt-in* explicado al usuario. El launcher funciona sin ellos (degradación elegante). | Revisión + test manual de §9. |
| R9 | Sin red ni telemetría. La app no declara `INTERNET`. | `lint` + revisión del manifest. |

Contenido de `tools/check-file-length.sh` (referencia, crearlo en el Hito 0):

```bash
#!/usr/bin/env bash
# Fails when a tracked text file exceeds MAX lines without a justified entry
# in tools/file-length.allow (R1, R1a); warns above WARN for code files (R2).
set -euo pipefail
MAX=${MAX:-1000}; WARN=${WARN:-600}; status=0
cd "$(git rev-parse --show-toplevel)"
trim() { sed 's/^[[:space:]]*//;s/[[:space:]]*$//' <<<"$1"; }

declare -A allow=()
while IFS='|' read -r path reason; do
  path=$(trim "$path"); reason=$(trim "${reason:-}")
  [[ -z "$path" || "$path" == \#* ]] && continue
  if (( ${#reason} < 20 )); then
    echo "ERROR file-length.allow: $path needs a justification (R1a)"; status=1
  fi
  allow[$path]=1
done < tools/file-length.allow

mapfile -t files < <(git ls-files --cached --others --exclude-standard \
  | grep -vEf tools/file-length.exclude || true)
for f in "${files[@]}"; do
  [[ -f "$f" ]] && grep -Iq . "$f" 2>/dev/null || continue  # skip binaries
  n=$(wc -l < "$f")
  if (( n > MAX )); then
    if [[ -n "${allow[$f]:-}" ]]; then echo "INFO  $f: $n lines (justified)"
    else echo "ERROR $f: $n > $MAX lines; split it or justify it (R1a)"; status=1; fi
  elif (( n > WARN )) && [[ "$f" =~ \.(kt|kts|py|sh)$ ]]; then
    echo "WARN  $f: $n > $WARN lines"
  fi
done

for p in "${!allow[@]}"; do
  if [[ ! -f "$p" ]] || (( $(wc -l < "$p") <= MAX )); then
    echo "WARN  file-length.allow: $p no longer needs an exception"
  fi
done
exit $status
```

---

## 2. Visión y alcance

### 2.1 Qué es

Un launcher (`category.HOME`) que convierte la pantalla de inicio en un
"escritorio Hyprland de bolsillo":

- **Workspaces 1–10** que se recorren deslizando horizontalmente (animación
  `slide` con curva `easeOutQuint`, como `looknfeel.lua`).
- Cada workspace es un **lienzo en mosaico**: las apps y widgets son *ventanas*
  (tiles) que el motor de tiling coloca con el algoritmo *dwindle* (por
  defecto) o *master*, con `gaps_in = 2`, `gaps_out = 2`, `border_size = 1` y
  `rounding = 10`.
- El tile con foco lleva el borde activo degradado `#84c959 → #45d9ea` a 45°;
  los demás, el inactivo `#243038` al 67 %.
- **Barra superior** configurable por zonas `left / center / right`, con
  módulos equivalentes a los de `shell.json` (menú, workspaces, reloj,
  indicadores, batería, red, Bluetooth, audio, energía…).
- **Menú wh01s17** (Super + Space): paleta de comandos con búsqueda difusa
  de apps, acciones y submenús (*Apps, Style, Setup, System…*).
- **Scratchpad** (Super + S): workspace especial que aparece superpuesto.
- **Atajos de teclado físico** cuando hay teclado (Bluetooth/USB/DeX) y
  **gestos** equivalentes en pantalla táctil.
- **Salvapantallas** con la W girando (como `w-spin.py`), vía `DreamService`.

### 2.2 Qué no es (límites de Android)

Android no permite a un launcher gestionar las ventanas de otras apps como
lo hace un compositor. Por eso:

| Concepto Hyprland | Realidad en Android | Decisión |
| --- | --- | --- |
| Ventanas de apps en mosaico | Un launcher no puede redimensionar apps ajenas en pantalla completa. | Los tiles son *lanzadores vivos*: icono, nombre, estado (notificaciones, media) o un widget. Al tocarlo se abre la app. |
| Tiling real de apps | Posible sólo con ventanas *freeform* (modo escritorio de Android 15+/16, tablets, pantallas externas, o la opción de desarrollador "freeform windows"). | Hito 9 experimental: `ActivityOptions.setLaunchBounds()` con los rectángulos del motor de tiling cuando el dispositivo lo soporte. |
| Split en dos apps | `FLAG_ACTIVITY_LAUNCH_ADJACENT` sólo funciona si el sistema ya está en multiventana. | Acción "abrir al lado" disponible sólo cuando `isInMultiWindowMode`. |
| `Super + W` cerrar ventana | No se pueden cerrar apps ajenas. | En el launcher cierra/quita el tile; nunca mata procesos. |
| Lock screen propio | No reemplazable por un launcher. | Fuera de alcance. El salvapantallas sí. |
| Tecla Super | Android 14+ reserva algunos `Meta + …` para el sistema. | Los atajos se capturan cuando el launcher tiene foco; los que el sistema reserve se documentan y se ofrece una tecla modificadora alternativa configurable. |
| Gestos de navegación del sistema en Xiaomi | MIUI/HyperOS los desactiva con launchers no oficiales y fuerza 3 botones. | Compatibilidad en tres capas, ver [§5.9](#59-compatibilidad-con-xiaomi-miui--hyperos). |

### 2.3 Usuarios y dispositivos

- Usuario principal: el dueño de `~/dotfiles` (uso personal, *sideload*).
- **Dispositivo objetivo: Redmi Note 13 Pro 5G** (código `garnet`) con
  **HyperOS 2.0.204**, basado en Android 15 (API 35). **No recibirá más
  actualizaciones**, así que esta combinación es fija. Las decisiones se prueban
  primero ahí; ver §5.9. Datos relevantes: pantalla AMOLED de 6,67″ a
  1220×2712 y hasta 120 Hz, Snapdragon 7s Gen 2, navegación por gestos
  bloqueada con launchers ajenos. Confirmado por ADB (2026-11-02):
  `ro.mi.os.version.incremental = OS2.0.204.0.VNRMIXM` (ROM **global**,
  `MI` = región global, `V` = Android 15) y `ro.build.version.sdk = 35`.
- Teléfonos en vertical (prioridad), tablets y modo escritorio/pantalla
  externa (secundario, Hito 9).
- `minSdk 29` (Android 10), `targetSdk`/`compileSdk` = la última API estable
  disponible al iniciar el Hito 0 (≥ 36). Verificarlo y anotarlo en §11.
  Como el teléfono se queda en API 35, cualquier API más nueva va protegida
  con `Build.VERSION.SDK_INT` y tiene alternativa. El emulador de CI y de
  pruebas usa API 35 con resolución 1220×2712.

---

## 3. Mapeo Hyprland → launcher

Atajos tomados de `installer/KEYBINDINGS.md` y `hypr/bindings.lua`.

| Hyprland / shell | Teclado en Android | Gesto táctil |
| --- | --- | --- |
| `Super + 1…0` ir a workspace | `Super + 1…0` | Deslizar izq./der.; tocar `[n]` en la barra |
| `Super + Shift + 1…0` mover ventana a workspace | `Super + Shift + 1…0` | Mantener tile → arrastrar al número de la barra |
| `Super + TAB` siguiente workspace | `Super + TAB` | Deslizar con dos dedos |
| `Super + Space` menú wh01s17 | `Super + Space` | Deslizar hacia arriba / tocar módulo menú |
| `Super + Alt + Space` menú de apps | `Super + Alt + Space` | Deslizar hacia arriba desde el borde inferior |
| `Super + Escape` menú de sistema | `Super + Escape` | Mantener el módulo de energía |
| `Super + Return` terminal | `Super + Return` → Termux (configurable) | Doble toque en hueco vacío |
| `Super + W` cerrar ventana | `Super + W` (quita el tile) | Arrastrar tile fuera de la pantalla |
| `Super + F` pantalla completa | `Super + F` (tile ocupa el workspace) | Doble toque en el tile |
| `Super + T` flotante/tiling | `Super + T` | Menú contextual del tile |
| `Super + J` alternar split | `Super + J` | Pellizco rotacional sobre el tile |
| `Super + ←/→/↑/↓` mover foco | `Super + flechas` | Tocar tile (foco) |
| `Super + Ctrl + Shift + flechas` redimensionar | igual | Arrastrar el borde compartido entre tiles |
| `Super + S` scratchpad | `Super + S` | Deslizar hacia abajo con dos dedos |
| `Super + K` lista de atajos | `Super + K` | Ajustes → Atajos |
| `Super + Ctrl + Space` fondos | `Super + Ctrl + Space` | Menú → Style → Background |
| `Ctrl + Alt + O/P/I` media | igual | Módulo media de la barra |
| `Ctrl + Alt + A/Z` volumen | igual | Módulo audio |
| Deslizar barra hacia abajo | — | Abre la cortina de notificaciones del sistema (vía accesibilidad opcional) |

Los atajos se definen en configuración (§6.4), no en código, para que el
usuario pueda reasignarlos igual que en `bindings.lua`.

---

## 4. Sistema de diseño (tema `wh01s17`)

Fuente: `~/dotfiles/desktop/.config/wh01s17/themes/wh01s17/`.

### 4.1 Sincronización de tokens

- `tools/sync-theme.py` copia desde los dotfiles a `theme/wh01s17/`:
  `colors.toml`, `shell.*.toml`, `hyprland.lua` (sólo se extraen los colores
  de borde y el radio) y `brand/logo.svg`.
- Una tarea Gradle (`:core:designsystem:generateTokens`) lee esos TOML y
  genera `build/generated/.../WhTokens.kt`. El código **nunca** edita los
  tokens a mano; para cambiarlos se edita el dotfile y se vuelve a sincronizar.
- Así el launcher sigue al tema del escritorio (mismo flujo que
  `wh01s17 theme set wh01s17`). `theme/wh01s17/SHA256SUMS` guarda los hashes y
  `tools/sync-theme.py --check` (parte de `check`) falla si un archivo se editó a mano
  y avisa si diverge de `~/dotfiles`. Un test compara el hash de los TOML
  versionados con los de `~/dotfiles` cuando existen, y avisa si divergen.

### 4.2 Paleta (de `colors.toml`)

| Token | Valor | Uso |
| --- | --- | --- |
| `background` / void | `#0a0e0f` | Fondo base |
| `darkBackground` | `#070a0b` | Scrims |
| `darkerBackground` | `#040607` | Salvapantallas |
| `surface` | `#11161a` | Barra, menú, popups |
| `lighterBackground` / raised | `#171e23` | Tooltips, tiles |
| `border` | `#243038` | Borde inactivo, controles |
| `muted` | `#33454f` | Corchetes de módulos, separadores |
| `foreground` | `#cbd5ce` | Texto |
| `darkForeground` | `#748178` | Placeholder, estado inactivo |
| `brightForeground` | `#f3fbee` | Texto sobre selección |
| `accent` / green | `#84c959` | Señal, selección, foco táctil |
| `selectionBackground` | `#2b4221` | Fondo de selección |
| `cyan` | `#45d9ea` | Foco de teclado, segundo color del borde |
| `yellow` | `#e8b53f` | Aviso |
| `orange` | `#e88c3f` | Aviso secundario |
| `red` | `#ff6b66` | Error, workspace activo en la barra (`bar.active`) |
| `blue` | `#4aa8ff` | Enlaces |
| `magenta` | `#b98cff` | Traza / especial |
| Variantes `bright*` | ver TOML | Hover / pressed |

Borde activo: `linearGradient(#84c959ee → #45d9eaee, 45°)`; inactivo:
`#243038aa`.

### 4.3 Estados de controles (de `shell.controls.toml`)

| Estado | Color | Relleno α | Borde α |
| --- | --- | --- | --- |
| normal | `#cbd5ce` | 0.035 | `#243038` @ 0.85 |
| hover / cursor | `#84c959` | 0.08 | 0.55 |
| focus (teclado) | `#45d9ea` | 0.09 | 0.80 |
| selected | `#84c959` | 0.16 | 0.60 |
| pressed | — | 0.22 | — |

Todos con borde de 1 px. En táctil, *pressed* sustituye a *hover*.

### 4.4 Tipografía (de `shell.font.toml`)

- Familia: **Departure Mono** (OFL) en `core/designsystem/src/main/res/font/` (Compose carga
  las fuentes desde `res/font`); las licencias, en `src/main/assets/licenses/`.
- Iconos: **wh01s17 Pixel Icons** (`wh01s17-pixel-icons.ttf`, Pixelarticons MIT)
  usando los mismos code points Nerd Font del shell. Mapa en
  `:core:designsystem/icons/WhIcons.kt` (constantes `WhGlyph`, de tipo `String`: varios code
  points superan U+FFFF y no caben en un `Char`).
- Escala (sp, con factor 1.25 sobre los px del shell para legibilidad móvil;
  ajustable en ajustes): caption 10, bodySmall 11, body 12, subtitle 13,
  title 14, heading 16, display 24, displayLarge 28.
- Sin cursivas ni pesos falsos: Departure Mono sólo tiene Regular.

### 4.5 Medidas (de `shell.spacing.toml`, `shell.bar.toml`, `looknfeel.lua`)

`controlGap 8`, `controlPaddingX 11`, `controlPaddingY 6`, `rowHeight 30`,
`rowPaddingX 12`, `panelGap 14`, `panelPadding 18`, `popupPadding 16`,
`barHeight 36`, `gapsIn 2`, `gapsOut 2`, `borderSize 1`, `rounding 10`,
todas en `dp` y multiplicadas por `spacing.scale`.

### 4.6 Superficies

| Superficie | Fondo | α | Borde |
| --- | --- | --- | --- |
| Barra | `#11161a` | 0.96 (o transparente, `bar.transparent`) | activo @ 0.55 |
| Menú / launcher | `#11161a` | 0.97–0.98 | activo @ 0.90, 2 px (`max(1, space(2))` de `Menu.qml`); scrim `#0a0e0f` @ 0.65–0.70 |
| Seleccionado en menú | `#84c959` @ 0.10 | — | Ninguno (`selected-border-width` no existe → 0, como en el shell); texto verde |
| Popups / notificaciones | `#11161a` | 0.98 | activo 1 px @ 0.90 |
| Tooltip | `#171e23` | 0.97 | `#33454f` @ 0.90 |

Desenfoque (blur 3, 2 pasadas) sólo con `RenderEffect` en API 31+; en API
< 31, se aumenta la opacidad del scrim.

### 4.7 Reglas visuales (de `DESIGN.md`)

- Colores planos: sin degradados (excepto el borde activo), ruido, glow, CRT
  ni 3D.
- Estética de terminal precisa: módulos de la barra entre corchetes `[ ]` en
  `#33454f` (como `brackets: true` de `StatusModule.qml`).
- Iconos de apps: por defecto el icono **monocromo** adaptativo
  (`AdaptiveIconDrawable.getMonochrome()`, API 33+) teñido con `foreground`;
  si no existe, icono original con filtro de saturación reducida
  (configurable: original / monocromo / pixelado).
- Fondos: se generan variantes verticales (1220×2712 para el Redmi, más
  1080×2400 y 1440×3200 genéricas) de los
  wallpapers *pixel art* con un adaptador de `sources/pixel-art.py`
  (`tools/render-wallpapers.py`). **Sólo se versionan en el repo los que no
  contienen propiedad de terceros** (marca W, lunas). Gengar y Opeth quedan
  como importables por el usuario desde su propio almacenamiento.

---

## 5. Arquitectura

### 5.1 Stack

| Área | Elección | Motivo |
| --- | --- | --- |
| Lenguaje | Kotlin 2.x (K2) | Estándar Android |
| UI | Jetpack Compose (sin Material como estilo; sólo `foundation` y primitivas) | Diseño propio total |
| Arquitectura | UDF/MVI: `State` inmutable + `Intent` + `ViewModel` con `StateFlow` | Predecible y testeable |
| Concurrencia | Coroutines + Flow | — |
| DI | Manual (`AppContainer` en `Application`) | Pocas dependencias, arranque rápido |
| Persistencia | `config/launcher.json` (kotlinx.serialization) + DataStore para estado efímero (último workspace, conteo de uso) | Config legible y exportable como `shell.json` |
| Build | Gradle Kotlin DSL, *version catalog* `gradle/libs.versions.toml`, *convention plugins* en `build-logic/` | Multi-módulo ordenado |
| JDK | Gradle corre con el JBR 25 de Android Studio; *bytecode* Java 17. No se usa el JDK 27 del sistema | Ver §10 |
| Calidad | ktlint, detekt (2.0 alpha, §11.1), Android Lint, Kover; líneas de máx. 140 columnas (`.editorconfig` y `detekt.yml`) | — |
| Tests | JUnit5 en módulos JVM; JUnit4 en módulos Android (lo exigen Robolectric y Roborazzi); Compose UI test; Roborazzi (capturas) | — |

Fijar versiones estables verificadas en el Hito 0 y anotarlas en §11; no usar
`+` ni rangos.

### 5.2 Módulos

```
android-launcher/
├── app/                      # Application, MainActivity (HOME), navegación, AppContainer
├── build-logic/              # convention plugins (android-lib, compose, jvm-lib, quality)
├── core/
│   ├── designsystem/         # tokens generados, WhTheme, fuentes, iconos, componentes base
│   ├── tiling/               # JVM puro: árbol dwindle, master, foco, resize, float
│   ├── config/               # esquema launcher.json, parser, migraciones, bindings
│   ├── apps/                 # índice de apps (LauncherApps), iconos, perfiles de trabajo
│   ├── system/               # batería, red, BT, audio, hora, media, notificaciones, oem/xiaomi
│   ├── input/                # teclado físico, gestos, resolución de atajos
│   └── fuzzy/                # JVM puro: búsqueda difusa y ranking por uso
├── feature/
│   ├── workspaces/           # pager de workspaces, lienzo de tiles, modo edición
│   ├── bar/                  # barra y registro de módulos
│   ├── menu/                 # menú wh01s17 / paleta de comandos / drawer de apps
│   ├── scratchpad/           # workspace especial superpuesto
│   ├── widgets/              # AppWidgetHost + widgets nativos (reloj, pomodoro, metrónomo)
│   ├── settings/             # ajustes, atajos (Super+K), importar/exportar config
│   └── screensaver/          # DreamService con la W girando
├── theme/wh01s17/            # TOML sincronizados desde ~/dotfiles
├── tools/                    # check-file-length.sh, sync-theme.py, render-wallpapers.py
├── docs/                     # documentación por área (límite R1)
├── AGENTS.md, CLAUDE.md      # punteros a plan.md
└── plan.md
```

Dependencias permitidas (flecha = "depende de"):

```
app → feature/* → core/designsystem, core/config, core/apps, core/system, core/input
feature/workspaces → core/tiling
feature/menu → core/fuzzy
core/config → core/tiling (tipos de layout)
core/tiling, core/fuzzy → (nada; JVM puro)
```

Ningún `feature` depende de otro `feature`; se comunican por `app` (navegación)
o por un `LauncherCommandBus` definido en `core/input`.

### 5.3 Flujo de datos

```
 LauncherApps / BroadcastReceivers / NotificationListener (opt-in)
                      │  Flow
                      ▼
 core/apps, core/system (repositorios) ──► core/config (launcher.json)
                      │                         │
                      ▼                         ▼
             feature ViewModels  ◄──── LauncherCommandBus ◄── core/input
             (StateFlow<UiState>)             ▲               (teclas, gestos)
                      │                       │
                      ▼                       │
             Composables (WhTheme) ───────────┘  intents del usuario
```

- **`LauncherCommand`**: `sealed interface` con todas las acciones
  (`FocusWorkspace(n)`, `MoveTileToWorkspace(id, n)`, `OpenMenu`,
  `ToggleScratchpad`, `ToggleFloating`, `ToggleSplit`, `Resize(dx, dy)`,
  `Launch(app)`, `CloseTile`, `MediaPlayPause`…). Teclado, gestos, barra y
  menú emiten comandos; nadie llama a ViewModels ajenos directamente.
- **`WorkspaceState`**: `id`, `layout` (`Dwindle | Master`), árbol de tiles,
  `focusedTileId`, `floating: List<FloatingTile>`, `fullscreenTileId?`.
- **`Tile`**: `id`, `content` (`App(componentKey) | Widget(appWidgetId) |
  Native(kind) | Group(tiles)`), `splitRatio`.

### 5.4 Motor de tiling (`core/tiling`)

- Entrada: rectángulo del área útil (pantalla − barra − insets), lista/árbol de
  tiles, parámetros (`gapsIn`, `gapsOut`, `border`), layout.
- Salida: `Map<TileId, Rect>` en píxeles enteros, determinista.
- **Dwindle** (por defecto, como Hyprland): árbol binario; cada nuevo tile
  divide el nodo con foco por su lado largo; `splitRatio` 0.1–0.9;
  `toggleSplit` invierte la orientación del nodo padre.
- **Master**: un master (ancho `mfact` 0.55) y pila; en vertical, el master va
  arriba.
- Operaciones puras: `insert`, `remove`, `swap`, `moveFocus(direction)`
  (vecino geométrico más cercano), `resize(edge, delta)`, `toggleFloating`,
  `toggleFullscreen`, `serialize/deserialize`.
- Límite práctico en teléfono: 8 tiles por workspace (configurable); por
  encima, el nuevo tile se agrupa (`Group`, como los grupos de Hyprland, con
  pestañas).
- Tests: propiedades (sin solapes, cubre el área exacta menos los huecos,
  `remove(insert(x)) == x`), y casos dorados de layouts.

### 5.5 Barra (`feature/bar`)

- Configuración igual a `shell.json`: `position` (`top`), `transparent`,
  `outline`, `layout.left/center/right` con `{ "id": "...", ...opciones }`.
- Interfaz `BarModule { id; @Composable Content(settings, onCommand) }`
  registrada en un `BarModuleRegistry`. Un archivo por módulo.
- Módulos del MVP: `core.menu`, `core.workspaces` (`[1] [2] …`, activo en
  `#ff6b66`, ocupados en `foreground`, vacíos en `darkForeground`),
  `wh01s17.clock` (`ddd d MMM HH:mm:ss`, alterno `d MMMM 'W'ww yyyy`, semana
  inicia lunes), `wh01s17.indicators` (notificaciones, no molestar),
  `wh01s17.power` (batería), `wh01s17.network`, `wh01s17.bluetooth`,
  `wh01s17.audio`.
- Módulos posteriores: `pomodoro`, `wh01s17.metronome`, `media`,
  `system-usage` (RAM/almacenamiento), `git-branch` (texto fijo configurable).
- Soporte de `classColors`, `dimClasses` y `brackets` como `StatusModule.qml`.
- Respeta los *insets* de la barra de estado del sistema: la barra wh01s17 se
  dibuja **debajo** del status bar (modo por defecto) o lo oculta en modo
  inmersivo (opción).

### 5.6 Menú wh01s17 (`feature/menu`)

- Panel centrado (o anclado abajo en teléfono) con superficie de §4.6.
- Raíz: `Apps`, `Learn`, `Trigger`, `Style`, `Setup`, `Install`, `Remove`,
  `Update`, `About`, `System` (iconos del mapa de `pixel-icons.py`).
  En Android: `Install/Remove` → abrir Play Store / desinstalar;
  `Update` → info de versión; `Style` → tema, fondo, fuente, transparencia,
  huecos; `System` → bloquear (accesibilidad opcional), apagar (diálogo del
  sistema vía accesibilidad opcional), ajustes.
- Búsqueda difusa (`core/fuzzy`): coincidencias por subsecuencia con bonus por
  inicio de palabra y por frecuencia/recencia de uso. Objetivo: < 16 ms con
  500 apps.
- Navegación completa por teclado (flechas, Enter, Escape, Tab) con foco cian.

### 5.7 Entrada (`core/input`)

- `KeyBindingResolver`: convierte `KeyEvent` (modificadores + tecla) en
  `LauncherCommand` según `bindings` de la config.
- `GestureBindingResolver`: deslizamientos (1 y 2 dedos, direcciones,
  bordes), doble toque, mantener, pellizco.
- Hápticos sutiles en cambios de workspace y acciones de edición.
- Los gestos de borde respetan la navegación por gestos del sistema
  (`systemGestureExclusionRects` sólo en la barra propia).

### 5.8 Integraciones con el sistema

| Función | API | Permiso | Obligatorio |
| --- | --- | --- | --- |
| Ser launcher | `intent-filter` `MAIN` + `HOME` + `DEFAULT` | — | Sí |
| Listar/lanzar apps | `LauncherApps`, `UserManager` (perfil de trabajo) | `QUERY_ALL_PACKAGES` no; usar `<queries>` con `MAIN/LAUNCHER` | Sí |
| Atajos de apps | `LauncherApps.getShortcuts` (requiere ser HOME por defecto) | — | No |
| Widgets | `AppWidgetHost`, `bindAppWidgetIdIfAllowed` | `BIND_APPWIDGET` vía diálogo | No |
| Batería, red, BT | `BatteryManager`, `ConnectivityManager`, `BluetoothManager` | `BLUETOOTH_CONNECT` (API 31+) opt-in | No |
| Notificaciones y media | `NotificationListenerService`, `MediaSessionManager` | Acceso a notificaciones (ajuste del sistema) | No |
| Bloquear / cortina / apagado | `AccessibilityService.performGlobalAction` | Servicio de accesibilidad (opt-in, explicar uso) | No |
| Fondo de sistema | `WallpaperManager` | `SET_WALLPAPER` (normal) | No |
| Salvapantallas | `DreamService` | — | No |
| Freeform | `ActivityOptions.setLaunchBounds`, `ActivityOptions.makeBasic().setLaunchDisplayId` | — | No (Hito 9) |
| Forzar gestos en Xiaomi | `Settings.Global.putInt("force_fsg_nav_bar", 1)` | `WRITE_SECURE_SETTINGS`, declarado en el manifest y otorgado una vez por ADB (`pm grant`) | No (§5.9) |

### 5.9 Compatibilidad con Xiaomi (MIUI / HyperOS)

MIUI y HyperOS desactivan los **gestos de navegación del sistema** (inicio,
atrás, recientes) cuando el launcher por defecto no es el oficial y obligan a
usar los 3 botones. Esto **no** afecta a los gestos dentro del launcher
(§5.7: cambiar de workspace, menú, editar tiles), que recibe la propia app.
La solución tiene tres capas independientes; ninguna es obligatoria.

> **Resultado en el Redmi Note 13 Pro 5G (HyperOS 2.0.204, 2026-11-02):**
> forzar `force_fsg_nav_bar = 1` con un launcher ajeno **oculta los 3
> botones, pero ningún gesto funciona** (ni inicio, ni atrás, ni recientes).
> En HyperOS 2 los gestos los procesa el launcher oficial
> (`com.miui.home` 5.39.40). **Estrategia para este equipo:** la capa 3 es
> la navegación principal y la capa 2 sólo sirve para ocultar los botones
> mientras la capa 3 está activa (modo *gestos wh01s17*, abajo). Sin capa 3
> activa, nunca se deja `force_fsg_nav_bar = 1`, porque el teléfono quedaría
> sin navegación.

**Capa 1: no depender de los gestos del sistema.** Todo el launcher se puede
usar con navegación de 3 botones. Ninguna función exige gestos del sistema.

**Capa 2: `XiaomiGestureCompat`** (`core/system/oem/xiaomi/`), que fuerza y
mantiene el ajuste oculto `force_fsg_nav_bar`:

- Detecta Xiaomi/Redmi/POCO (`Build.MANUFACTURER`/`Build.BRAND`) y la versión
  de MIUI o HyperOS (`ro.miui.ui.version.name`, `ro.mi.os.version.name`,
  leídas con `getprop`; si faltan, se usa sólo el fabricante).
- Si la app tiene `WRITE_SECURE_SETTINGS` y el usuario activó la opción,
  escribe `force_fsg_nav_bar = 1`. El sistema debe estar en modo
  *Pantalla completa → Gestos*.
- Reaplica el valor (comprobado en el Redmi: HyperOS lo pone en `0` y cambia
  `navigation_mode` a `0` en cuanto un launcher ajeno pasa a ser el
  predeterminado):
  - justo después de que el usuario nos elige como launcher predeterminado
    (`RoleManager.isRoleHeld(ROLE_HOME)` cambia a `true`);
  - en `MainActivity.onResume`. Como launcher HOME, el sistema la abre en cada
    arranque, así que no hace falta *Inicio automático* ni `BOOT_COMPLETED`;
  - con un `ContentObserver` sobre `Settings.Global.getUriFor("force_fsg_nav_bar")`
    mientras el proceso vive. Si SystemUI lo reinicia a 0 (hay reportes de que
    ocurre a diario), se restaura enseguida.
- Protección contra peleas con el sistema: como máximo 5 reescrituras por
  minuto. Si se supera, deja de insistir, muestra un aviso en la barra (clase
  `warning`, `#e8b53f`) y sugiere la capa 3.
- Si al forzar el ajuste `navigation_mode` sigue en `0` y no vuelven los
  gestos, la capa 2 no basta en este equipo: se pasa a la capa 3 y se anota en
  `docs/xiaomi.md`.
- Opción de ocultar la barrita de gestos (`hide_gesture_line`, global,
  observado en HyperOS 2) con el mismo permiso.
- Al desactivar la opción escribe `0` y vuelven los botones.
- Ajustes → *Compatibilidad*: asistente que muestra el fabricante, la versión,
  si hay permiso y el valor actual. También los comandos ADB para copiar
  (ver §10) y cómo activar *Depuración USB (ajustes de seguridad)* en
  Opciones de desarrollador. Xiaomi lo exige para `pm grant` **y también para
  `adb shell settings put`**: sin él, el shell no tiene
  `WRITE_SECURE_SETTINGS` (comprobado en el Redmi con una
  `SecurityException`). Suele pedir cuenta Mi y SIM insertada. **Después de
  activarlo hay que reconectar ADB** (desconectar y volver a conectar el
  cable, o apagar y encender *Depuración USB*). La sesión abierta antes sigue
  sin poder escribir. Se mencionan LADB o Shizuku para hacerlo sin PC.
- El manifest declara `WRITE_SECURE_SETTINGS` con
  `tools:ignore="ProtectedPermissions"`; sin el `pm grant`, la capa 2 sólo
  muestra el asistente.
- Los recientes los sigue dibujando el sistema; no se intenta reemplazarlos.

**Modo *gestos wh01s17* (capa 2 + capa 3), el principal en el Redmi:**

- Se activa sólo si están el servicio de accesibilidad **y**
  `WRITE_SECURE_SETTINGS`. Primero arranca la capa 3; después se escribe
  `force_fsg_nav_bar = 1` para ocultar los botones.
- **Vigilancia para no dejar el teléfono sin navegación:**
  - `WhNavigationService.onUnbind/onDestroy` y la desactivación del servicio
    escriben `force_fsg_nav_bar = 0` antes de terminar;
  - `MainActivity.onResume` comprueba que el servicio esté vivo
    (`AccessibilityManager.getEnabledAccessibilityServiceList`); si no lo
    está, escribe `0` y avisa;
  - si la app pierde el rol HOME o se desinstala, la vigilancia no puede
    actuar. Por eso el asistente explica cómo recuperarlo:
    `adb shell settings put global force_fsg_nav_bar 0`, o bien elegir de
    nuevo el launcher oficial, que vuelve a los botones.
- Test instrumentado obligatorio: matar el servicio debe dejar
  `force_fsg_nav_bar = 0` en menos de 1 s.

**Capa 3: gestos de borde propios** (navegación principal en el Redmi, Hito 2;
respaldo en otros equipos). El mismo servicio de
accesibilidad de §5.8 (`WhNavigationService`) añade franjas finas en los
bordes con `TYPE_ACCESSIBILITY_OVERLAY`:

| Gesto | Acción (`performGlobalAction`) |
| --- | --- |
| Deslizar desde el borde izq. o der. | `GLOBAL_ACTION_BACK` |
| Deslizar desde abajo | `GLOBAL_ACTION_HOME` |
| Deslizar desde abajo y mantener | `GLOBAL_ACTION_RECENTS` |
| Deslizar desde arriba en el borde der. (opcional) | `GLOBAL_ACTION_NOTIFICATIONS` |

- Se activa por gesto: por ejemplo, si en HyperOS 2 sólo falla *atrás* con la
  capa 2, se habilita únicamente el *atrás* propio.
- Grosor de la franja, sensibilidad y hápticos configurables; las franjas
  no tapan el teclado en pantalla (se ocultan con el IME visible).
- MIUI suele matar servicios en segundo plano. El asistente pide poner la
  batería de la app en *Sin restricciones* y activar *Inicio automático*,
  abriendo directamente la pantalla de ajustes de la app.

**Matriz de pruebas:** `docs/xiaomi.md` registra modelo, versión de
HyperOS/MIUI y región de la ROM. Para cada capa anota el resultado de inicio,
atrás, recientes, la animación de vuelta al launcher y si sobrevive a un
reinicio. Se actualiza tras cada actualización del sistema del teléfono.

Opción descartada: root con módulos LSPosed. Desbloquear el bootloader en
HyperOS está muy restringido y saca al usuario de las actualizaciones OTA.

---

## 6. Configuración

### 6.1 Archivo

`files/config/launcher.json`, exportable/importable por *Storage Access
Framework*. Estructura inspirada en `shell.json`:

```json
{
  "version": 1,
  "theme": "wh01s17",
  "general": { "gapsIn": 2, "gapsOut": 2, "borderSize": 1, "rounding": 10,
               "layout": "dwindle", "maxTilesPerWorkspace": 8 },
  "animations": { "workspaces": { "style": "slide", "bezier": "easeOutQuint", "speed": 2 } },
  "idle": { "screensaver": 150 },
  "bar": {
    "position": "top", "transparent": true, "outline": false,
    "layout": {
      "left":   [{ "id": "core.menu" }, { "id": "core.workspaces" }],
      "center": [{ "id": "wh01s17.clock", "format": "ddd d MMM HH:mm:ss" }],
      "right":  [{ "id": "wh01s17.network" }, { "id": "wh01s17.bluetooth" },
                 { "id": "wh01s17.audio" }, { "id": "wh01s17.power" }]
    }
  },
  "workspaces": [
    { "id": 1, "name": "term", "layout": "dwindle",
      "tiles": [{ "app": "com.termux/.HomeActivity" }] }
  ],
  "bindings": [
    { "keys": "SUPER + SPACE", "command": "menu.open" },
    { "keys": "SUPER + 1", "command": "workspace.focus", "args": { "id": 1 } },
    { "gesture": "swipe-up", "command": "menu.open" }
  ],
  "apps": { "terminal": "com.termux/.HomeActivity", "iconStyle": "monochrome" }
}
```

### 6.2 Esquema y migraciones

- `version` entero. Cada cambio incompatible sube la versión y añade
  `MigrationN_to_N+1.kt` en `core/config/migrations/` (un archivo por
  migración) con test de ida.
- Config inválida → se conserva la anterior, se muestra un aviso en la barra
  (clase `error`, `#ff6b66`) y se registra el error; nunca deja el launcher
  inutilizable.

### 6.3 Valores por defecto

`core/config/src/main/assets/default-launcher.json` reproduce el escritorio
del usuario: barra arriba transparente, reloj al centro, 5 workspaces
iniciales, dwindle, atajos de §3.

### 6.4 Atajos

Lista `bindings` (teclado o gesto → comando + args). Pantalla `Super + K`
genera la tabla a partir de esta lista (igual que `keybindings-md.sh`).

---

## 7. Hitos

Cada hito termina con: `./gradlew check` verde, APK instalable, entrada en
§13 y, si aplica, capturas en `docs/screenshots/`. Estimaciones en sesiones
de trabajo de agente (S ≈ 1, M ≈ 2–3, L ≈ 4–6).

### Hito 0 — Fundaciones (M)

- [x] Instalar el Android SDK (abrir Android Studio una vez, §10), configurar
      Gradle con el JBR 25 de Android Studio y documentarlo en `docs/setup.md`.
- [x] `git init`, `.gitignore` Android, licencia (MIT, como los dotfiles).
      Repositorio público: https://github.com/wh01s17/android-launcher
- [x] Proyecto Gradle multi-módulo con `build-logic/` y version catalog;
      Gradle wrapper fijado (9.8.2, SHA-256 del jar y de la distribución verificados).
- [x] Módulos vacíos de §5.2 con sus convention plugins
      (`wh01s17.android.application|library|compose`, `wh01s17.jvm.library`).
- [x] `MainActivity` con `intent-filter` HOME que dibuja fondo `#0a0e0f`;
      `android:launchMode="singleTask"`, `stateNotNeeded`, sin animación
      de entrada; iconos claros en las barras del sistema; *atrás* no la cierra.
      (Volver al workspace actual con Home llega en el Hito 4.)
- [x] `tools/check-file-length.sh`, `tools/file-length.exclude`,
      `tools/file-length.allow` (con la entrada de `plan.md`) y tarea
      `checkFileLength` dependiente de `check` (R1–R3).
- [x] ktlint, detekt (con `LongMethod` R4), Lint, Kover configurados.
      detekt es 2.0.0-alpha.6: la 1.23 no corre con JDK 25 (§11.1).
- [x] CI GitHub Actions: `./gradlew check assembleDebug` + artefacto APK
      (`.github/workflows/ci.yml`, primera ejecución en verde el 2026-11-02).
- [x] `AGENTS.md` y `CLAUDE.md` que remiten a `plan.md`.
- [x] Registrar versiones elegidas en §11.
- [x] Redmi Note 13 Pro 5G: región (global), API 35 y `garnet` confirmados
      por ADB; `force_fsg_nav_bar` probado con otro launcher ajeno
      (`docs/xiaomi.md`). APK instalado (con *Instalar vía USB*) y probado como
      HOME: Inicio vuelve al launcher y *atrás* no lo cierra. Se restauró
      `bitpit.launcher` como predeterminado.

**Terminado cuando:** el APK se instala, se elige como launcher y muestra
pantalla negra wh01s17; CI verde. Un archivo de prueba de 1001 líneas hace
fallar `check`, pasa al añadirlo a `file-length.allow` con justificación, y
vuelve a fallar si la justificación queda vacía.

### Hito 1 — Sistema de diseño (M)

- [x] `tools/sync-theme.py` + `theme/wh01s17/*.toml` sincronizados. Además `--check`,
      `SHA256SUMS` y pruebas en `tools/tests/`.
- [x] Generador `generateTokens` → `WhTokens.kt` y `wh_tokens.xml` (colores, alfas, medidas,
      tipografía). Plugin `wh01s17.design.tokens` en `build-logic`, con 17 pruebas que incluyen
      el tema real.
- [x] Fuentes Departure Mono y wh01s17 Pixel Icons en `res/font`, licencias en
      `assets/licenses`.
- [x] `WhTheme` (`CompositionLocal`s), `WhIcons` (56 glifos), `Modifier.whBorder(active)`
      con degradado 45°, `rememberWhControlState` y `Modifier.whControlSurface(...)`.
- [x] Componentes: `WhText`, `WhIcon`, `WhButton`, `WhPanel`, `WhScrim`,
      `WhBracketLabel`, `WhListRow`, `WhTextField`, `WhTooltip`, `WhToggle`.
- [x] Pantalla *gallery* (sólo debug, `GalleryActivity`) con todos los componentes y estados.
- [x] Capturas Roborazzi de la galería como referencia (9, en `src/test/screenshots/`).
- [x] `tools/check-no-literal-colors.sh` (R6) enganchado a `check`.
- [x] Contrastar a ojo el menú y los popups con el shell abierto al lado
      (`docs/design-system.md`). Hecho el 2026-11-03 con capturas de `grim` y el código del
      shell: se corrigieron el borde del menú (2 px), la fila seleccionada (sin borde) y las
      medidas de fila de `Menu.qml` (`WhListRowSize.Menu`).

**Terminado cuando:** la galería coincide con el shell del escritorio
(comparación visual manual documentada en `docs/design-system.md`) y no hay colores
literales fuera del módulo (R6, comprobado por `check`).

### Hito 2 — Índice de apps y lanzamiento (M)

- [x] `AppRepository` con `LauncherApps` + callback de cambios de paquetes,
      perfiles de usuario/trabajo. `LauncherAppRepository` relee sólo el paquete que cambia
      y todo con un cambio de perfil o de idioma; `LauncherAppsGateway` aísla Android para las
      pruebas; `AppKey` con el formato `paquete/.Clase[@serial]` de la config. Conectado en
      `AppContainer`. En el Redmi: 156 apps, primera carga ≈ 3,4 s en debug (los nombres).
- [x] Carga de iconos en segundo plano con caché LRU; estilos monocromo /
      original / pixelado. `LauncherIconLoader` + `IconRenderer` + `IconCache` (1/8 del heap,
      clave con `iconStamp`, que cambia al actualizar la app). Monocromo: capa `monochrome`
      teñida a 1,5× sin máscara; sin ella, original al 30 % de saturación. Pixelado: rejilla
      de 24, alfa 0/255. En el Redmi: 72 iconos en 377 ms. Los iconos antiguos (no
      adaptativos) se dibujan tal cual, sin redondear.
- [x] Lanzar app con animación de origen (`ActivityOptions.makeScaleUpAnimation`).
      `LauncherAppActions.launch` con `LaunchOrigin` (vista + icono); un perfil en pausa pide
      reanudarse (`requestQuietModeEnabled`) en vez de fallar.
- [x] Acciones: info de la app, desinstalar, atajos de app (si somos HOME).
      Diálogo del sistema para desinstalar (`REQUEST_DELETE_PACKAGES`, normal); atajos del
      manifiesto y dinámicos, máx. 4. Probado en el Redmi como HOME temporal.
- [x] `core/fuzzy` con tests y benchmark (500 apps < 16 ms). DP tipo fzf, sin tildes,
      varias palabras, palabras clave (paquete) sólo como subcadena, bonus de uso. Mediana
      0,19 ms en el PC; cobertura 99 %.
- [x] Drawer de apps provisional (lista + búsqueda) para poder usar el
      launcher a diario. `feature/menu/drawer`: deslizar hacia arriba o tocar el inicio;
      Enter abre el primero; mantener pulsado → atajos, info, desinstalar; marcas de
      trabajo, pausa y suspendida, que cambian en vivo.
- [ ] `WhNavigationService` (capa 3 de §5.9): gestos de borde para atrás,
      inicio y recientes con `performGlobalAction`. Es la navegación principal
      en el Redmi.
- [ ] `XiaomiGestureCompat` (capa 2 de §5.9) en modo *gestos wh01s17*:
      oculta los botones sólo con la capa 3 viva, con la vigilancia que
      restaura `0` y tests con un `SettingsGateway` falso.
- [ ] Ajustes → Compatibilidad: asistente con estado, comandos ADB y accesos
      a batería/inicio automático.

**Terminado cuando:** se puede usar como launcher diario mínimo en el
Redmi, con gestos wh01s17 (capas 2 y 3) o con 3 botones (capa 1), y matar el
servicio de accesibilidad devuelve los botones.

### Hito 3 — Barra (M)

- [ ] `BarModule`, `BarModuleRegistry`, parseo de `bar` desde la config.
- [ ] Módulos: menú, workspaces, reloj (+ popup calendario semanal lunes),
      batería, red, Bluetooth, audio, indicadores.
- [ ] `classColors`, `dimClasses`, `brackets`, `transparent`, `outline`.
- [ ] Repositorios de `core/system` como `Flow` con ciclo de vida.
- [ ] Gesto: deslizar la barra hacia abajo → cortina del sistema (si
      accesibilidad concedida; si no, no hace nada y lo indica una vez).

**Terminado cuando:** la barra refleja el estado real y cambia en vivo.

### Hito 4 — Motor de tiling y workspaces (L) — núcleo del proyecto

- [ ] `core/tiling`: dwindle, master, foco direccional, resize, float,
      fullscreen, grupos; serialización. Tests de propiedades (R7).
- [ ] `feature/workspaces`: `HorizontalPager` de workspaces con animación
      slide/easeOutQuint; persistencia del workspace actual.
- [ ] Render de tiles: app (icono grande + nombre + badge de notificaciones),
      bordes activo/inactivo, huecos, radio 10.
- [ ] Modo edición (mantener en hueco vacío): añadir tile, arrastrar para
      reordenar/swap, arrastrar a `[n]` de la barra para mover de workspace,
      redimensionar arrastrando bordes, alternar split/float/fullscreen.
- [ ] Comandos vía `LauncherCommandBus`.
- [ ] Workspaces vacíos se ocultan en la barra salvo el activo (como Hyprland
      con `persistent` configurable).

**Terminado cuando:** se pueden armar 5 workspaces con tiles, reordenarlos y
persisten tras reiniciar el teléfono.

### Hito 5 — Menú wh01s17 y paleta de comandos (M)

- [ ] Panel del menú con submenús de §5.6 y sus iconos pixel.
- [ ] Búsqueda unificada: apps + comandos + ajustes del sistema
      (`Settings.ACTION_*`) + atajos de app.
- [ ] Ranking por uso (DataStore) y recencia.
- [ ] Navegación por teclado completa.
- [ ] Sustituye al drawer provisional del Hito 2.

**Terminado cuando:** cualquier acción del launcher es alcanzable desde el
menú. **Fin del MVP (Hitos 0–5)** → etiquetar `v0.1.0`.

### Hito 6 — Teclado físico y gestos configurables (M)

- [ ] `KeyBindingResolver` y `GestureBindingResolver` leyendo `bindings`.
- [ ] Todos los atajos de §3; detección de los reservados por el sistema y
      modificador alternativo configurable.
- [ ] Pantalla `Super + K` con la tabla de atajos generada.
- [ ] Editor de atajos en ajustes (captura de combinación).
- [ ] `Super + Return` abre la terminal configurada (Termux por defecto).
- [ ] Refinar la capa 3 (§5.9): cada gesto activable por separado, grosor y
      sensibilidad configurables, ocultos con el teclado en pantalla,
      animación de vuelta al launcher.
- [ ] Completar la matriz de `docs/xiaomi.md` con las tres capas.

### Hito 7 — Widgets (L)

- [ ] `AppWidgetHost` y selector de widgets del sistema como tiles.
- [ ] Widgets nativos wh01s17: reloj/calendario, pomodoro (estados y colores
      de `docs/pomodoro.md`: trabajo `#ff6b66`, descanso corto `#84c959`,
      largo `#45d9ea`), metrónomo (bpm, compás, subdivisión), uso del sistema.
- [ ] Módulos de barra correspondientes (`pomodoro`, `wh01s17.metronome`).
- [ ] Servicio en primer plano sólo para pomodoro/metrónomo activos.

### Hito 8 — Scratchpad, notificaciones y media (M)

- [ ] Scratchpad: workspace especial superpuesto con scrim y animación desde
      arriba (`Super + S` / dos dedos abajo).
- [ ] `NotificationListenerService` opt-in: badges en tiles, contador en
      indicadores, panel de notificaciones propio con la superficie de
      `shell.notifications.toml` (barra de cuenta atrás `#84c959`).
- [ ] Controles de media (`MediaSessionManager`) en barra y atajos
      `Ctrl + Alt + O/P/I`.

### Hito 9 — Modo escritorio / freeform (L, experimental)

- [ ] Detectar soporte freeform y pantallas externas (`DisplayManager`).
- [ ] Al lanzar desde un tile en un dispositivo compatible, usar el `Rect`
      del motor de tiling con `setLaunchBounds` (tiling real de apps).
- [ ] Workspace por pantalla externa (`Ctrl + Alt + TAB` cambia de monitor).
- [ ] Documentar dispositivos probados y limitaciones en `docs/freeform.md`.
- [ ] Si un fabricante no lo soporta, la función se oculta sin errores.

### Hito 10 — Estilo, fondos, salvapantallas y config (M)

- [ ] Menú *Style*: tema, fondo, fuente (escala), transparencia, huecos,
      radio, estilo de iconos; cambios en vivo.
- [ ] `tools/render-wallpapers.py`: variantes verticales de los fondos
      (marca W y lunas en el repo; Gengar/Opeth importables por el usuario).
- [ ] Selector de fondos (`Super + Ctrl + Space`), dibujado propio y opción
      de fijarlo como fondo del sistema.
- [ ] `DreamService` con la W girando (portar la lógica de `w-spin.py`) sobre
      `#040607`; `idle.screensaver` desde config.
- [ ] Importar/exportar `launcher.json` por SAF; validación con errores
      legibles.

### Hito 11 — Endurecimiento y versión 1.0 (M)

- [ ] Rendimiento: arranque en frío < 400 ms en el Redmi Note 13 Pro 5G y
      120 fps sostenidos en el pager y la apertura del menú a 120 Hz
      (Macrobenchmark + Baseline Profile).
- [ ] Memoria: sin fugas (LeakCanary en debug), caché de iconos acotada.
- [ ] Accesibilidad: TalkBack en tiles y barra, contraste AA (verificado con
      los tokens), tamaños táctiles ≥ 48 dp aunque el visual sea compacto.
- [ ] Rotación, plegables, modo multiusuario, perfil de trabajo.
- [ ] Firma de release, `versionCode` automático, notas de versión.
- [ ] Documentación de usuario `docs/usage.md`.

---

## 8. Actualizaciones y versionado

### 8.1 Versionado de la app

- SemVer: `MAJOR.MINOR.PATCH`. MVP = `0.1.0`; cada hito posterior sube
  `MINOR`; `1.0.0` al cerrar el Hito 11.
- `versionCode = MAJOR*10000 + MINOR*100 + PATCH`, calculado en
  `build-logic` desde `version.properties` (única fuente).
- Tag Git `vX.Y.Z` por versión; `CHANGELOG.md` en formato *Keep a Changelog*
  generado a partir de los commits convencionales.

### 8.2 Distribución y actualización del dispositivo

- Sin Play Store. CI publica el APK firmado como *GitHub Release* al crear el
  tag. El usuario actualiza con Obtainium (o `adb install -r`).
- La clave de firma **nunca** se versiona: secretos de CI
  (`SIGNING_KEYSTORE_B64`, `SIGNING_PASSWORD`, …). Documentado en
  `docs/release.md`.
- La app no comprueba actualizaciones por sí misma (R9: sin red).

### 8.3 Compatibilidad de datos entre versiones

- `launcher.json` migra hacia delante (§6.2). Antes de migrar se guarda
  `launcher.v<N>.bak.json`.
- DataStore con versión propia; si falla, se reinicia sin afectar la config.
- Los IDs de tiles y de widgets (`appWidgetId`) se preservan entre versiones.

### 8.4 Actualizar el tema

Cuando cambie el tema en `~/dotfiles`: `python3 tools/sync-theme.py`,
`./gradlew :core:designsystem:generateTokens check`, revisar capturas
Roborazzi, commit `chore(theme): sync wh01s17 tokens`.

---

## 9. Estrategia de pruebas

| Nivel | Qué | Herramienta | Cuándo |
| --- | --- | --- | --- |
| Unitarias JVM | tiling, fuzzy, config/migraciones, resolución de atajos | JUnit5, pruebas de propiedades (Kotest property o jqwik) | Siempre en CI |
| Herramientas | `tools/*.py`, generador de tokens de `build-logic` | `unittest` (Python), JUnit5 en `build-logic` | Parte de `check` |
| Android unitarias | repositorios con fakes, ViewModels | Robolectric, Turbine | CI |
| UI | barra, menú, modo edición | Compose UI test | CI |
| Capturas | componentes y pantallas en tema wh01s17 (a 480 dpi y 407 dp, como el Redmi) | Roborazzi + Robolectric (API 35) sobre JDK 25 | `verifyRoborazziDebug` forma parte de `check`; regrabar con `recordRoborazziDebug` |
| Instrumentadas | lanzar apps, widgets, rol HOME | Emulador (Gradle Managed Devices) | Antes de cada release |
| Rendimiento | arranque, pager | Macrobenchmark | Hito 11 y releases |
| Manual | checklist `docs/manual-qa.md` (permisos denegados, rotación, teclado BT) | Dispositivo real | Antes de cada release |

---

## 10. Entorno de desarrollo

Estado revisado el 2026-11-02:

| Componente | Estado | Ruta / versión |
| --- | --- | --- |
| Android Studio | Instalado (paquete `android-studio`) y configurado | `/opt/android-studio`, 2026.2.2 (`AI-262.10968.63`) |
| JDK de Android Studio (JBR) | Instalado | `/opt/android-studio/jbr`, OpenJDK 25.0.4 |
| JDK del sistema | Instalado (`jdk-openjdk`, por defecto en `archlinux-java`) | `/usr/lib/jvm/java-27-openjdk`, OpenJDK 27.0.1 |
| `adb` | Instalado (`android-tools` 37.0.0). **Usar siempre este**; el del SDK (37.0.1) no se usa, para evitar conflictos de servidor ADB | `/usr/bin/adb` |
| Android SDK | Instalado por el asistente de Studio | `~/Android/Sdk` |
| Plataforma | Android 17 (API 37.0) → `compileSdk` | `platforms/android-37.0` |
| Build-tools | 37.0.0 (y 36.0.0) | `build-tools/` |
| Command-line tools | 23.0.0 (`sdkmanager`, `avdmanager`); necesitan `JAVA_HOME=/opt/android-studio/jbr` | `cmdline-tools/latest/bin` |
| Emulador / platform-tools | 37.2.13 / 37.0.1; KVM accesible | `emulator/`, `/dev/kvm` |
| Imágenes | API 35 `google_apis` x86_64 (rev. 9, permite `adb root`) y API 37 `google_apis_playstore` x86_64 | `system-images/` |
| AVD del Redmi | `redmi-api35`: API 35, 1220×2712, 480 dpi, teclado físico del PC habilitado (`hw.keyboard=yes`) para probar atajos. Hyprland puede capturar la tecla Super antes que el emulador | `~/.android/avd/redmi-api35.avd` |
| AVD genérico | `Medium_Phone_API_37.0` | `~/.android/avd/` |
| Gradle | No hace falta global (Gradle Wrapper) | — |

- **JDK para Gradle: el JBR 25 de Android Studio**, no el JDK 27 del sistema.
  Es el JDK con el que Google prueba el AGP que trae esa versión de Studio. El
  JDK 27 es más nuevo de lo que Gradle/AGP pueden soportar todavía; verificarlo
  en el Hito 0 y anotarlo en §11. Se fija en `gradle.properties` del usuario
  (`~/.gradle/gradle.properties`, no versionado):
  `org.gradle.java.home=/opt/android-studio/jbr`. El *bytecode* se compila
  para Java 17 (`jvmToolchain`/`compileOptions`), compatible con Android.
- **Instalar el SDK** (una de dos):
  1. Recomendado: abrir Android Studio una vez y completar el asistente
     inicial (*Standard*). Descarga el SDK en `~/Android/Sdk` con
     platform-tools, la última plataforma, build-tools y el emulador. Después,
     en *SDK Manager*, añadir `Android 15 (API 35)` y su imagen de sistema
     `x86_64` para el emulador del Redmi.
  2. Sin interfaz: `android-sdk-cmdline-tools-latest` (AUR) y
     `sdkmanager "platform-tools" "platforms;android-<API>" "platforms;android-35"
     "build-tools;<ver>" "emulator" "system-images;android-35;google_apis;x86_64"`.
- Definir `ANDROID_HOME=$HOME/Android/Sdk` en `terminal/.zshrc` de los
  dotfiles (o `sdk.dir` en `local.properties`, no versionado).
- El proyecto usa Gradle Wrapper; no hace falta Gradle global.
- Comandos habituales:

```bash
./gradlew check                 # tests, lint, detekt, ktlint, longitud de archivos
./gradlew assembleDebug         # APK en app/build/outputs/apk/debug/
./gradlew installDebug          # instalar en el dispositivo conectado
adb shell cmd role add-role-holder android.app.role.HOME com.wh01s17.launcher
# Xiaomi (§5.9): todo `pm grant` y `settings put` requiere activar antes
# "Depuración USB (ajustes de seguridad)"; si no, SecurityException
adb shell pm grant com.wh01s17.launcher android.permission.WRITE_SECURE_SETTINGS
adb shell settings get global force_fsg_nav_bar    # 1 = gestos forzados
adb shell settings put global force_fsg_nav_bar 0  # volver a 3 botones
tools/check-file-length.sh      # control R1 rápido
python3 tools/sync-theme.py     # traer el tema desde ~/dotfiles
```

- Identificador de aplicación: `com.wh01s17.launcher` (debug:
  `com.wh01s17.launcher.debug`, instalable junto a la release).

---

## 11. Decisiones y riesgos

### 11.1 Decisiones tomadas

| Fecha | Decisión | Motivo |
| --- | --- | --- |
| 2026-11-02 | Tiles como lanzadores vivos; tiling real de apps sólo en freeform (Hito 9) | Restricción de la plataforma (§2.2) |
| 2026-11-02 | Compose sin Material como estilo | El tema exige estética propia |
| 2026-11-02 | DI manual, sin Hilt | Arranque rápido, menos dependencias |
| 2026-11-02 | Config JSON compatible en espíritu con `shell.json` | Familiaridad y exportación |
| 2026-11-02 | Tokens generados desde los TOML de los dotfiles | Una sola fuente del tema |
| 2026-11-02 | Límite duro de 1000 líneas por archivo, aviso a 600 | Requisito del usuario; facilita trabajo por agentes |
| 2026-11-02 | Cualquier archivo puede superar 1000 líneas si está justificado y registrado en `tools/file-length.allow` (R1a) | Decisión del usuario: el límite orienta, no obliga a dividir mal un archivo |
| 2026-11-02 | `plan.md` excede el límite (primera entrada de `file-length.allow`) | Fuente única de verdad para agentes; dividirlo obligaría a leer varios archivos para retomar el trabajo |
| 2026-11-02 | Sin permiso `INTERNET` | Privacidad; un launcher no lo necesita |
| 2026-11-02 | Xiaomi como dispositivo objetivo; gestos del sistema en tres capas (3 botones, `force_fsg_nav_bar` reaplicado, gestos propios por accesibilidad); sin root | MIUI/HyperOS bloquea los gestos con launchers ajenos (§5.9) |
| 2026-11-02 | En el Redmi, la capa 3 es la navegación principal y pasa al Hito 2; la capa 2 sólo oculta los botones mientras la capa 3 está viva | Probado: con un launcher ajeno, `force_fsg_nav_bar = 1` oculta los botones pero los gestos no funcionan |
| 2026-11-02 | AGP 9.5.0 (el máximo que soporta Android Studio 2026.2.2), Gradle 9.8.2, Kotlin 2.4.21 (Kotlin integrado en AGP 9), Compose BOM 2026.09.00, `compileSdk`/`targetSdk` 37, `minSdk` 29, bytecode Java 17 | Últimas estables al 2026-11-02 (`gradle/libs.versions.toml`) |
| 2026-11-02 | Gradle corre con el JBR 25 de Android Studio; CI con Temurin 25 | El JDK 27 del sistema es más nuevo de lo que soporta la cadena de herramientas |
| 2026-11-02 | **Excepción a versiones estables:** detekt 2.0.0-alpha.6 | detekt 1.23.8 falla con JDK 25 (`25.0.4` no reconocido por su compilador de Kotlin 2.0). Volver a una estable en cuanto salga detekt 2.0 |
| 2026-11-02 | Roborazzi 1.77.0 + Robolectric 4.17 con `@Config(sdk = [35])` | Funcionan sobre JDK 25; los tests de módulos Android usan JUnit4 (lo exigen) |
| 2026-11-02 | `tomlj` 2.2.0 en `build-logic` para leer los TOML del tema | Parser estándar; evita uno propio |
| 2026-11-02 | Los tokens generados son `internal`; los demás módulos usan `WhTheme.*` | Un solo punto de acceso y sin literales (R6) |
| 2026-11-02 | Fuentes en `res/font`, no en `assets` | Compose las carga con `Font(R.font.…)` |
| 2026-11-02 | Dirección del borde activo `(cos θ, sin θ)` en pantalla: 45° = arriba-izq. → abajo-der. | Medido en una ventana real de Hyprland (`docs/design-system.md`) |
| 2026-11-02 | Máximo de 140 columnas por línea (ktlint y detekt) | Valor por defecto de `ktlint_official`; el modo `android` de ktlint imponía 100 |
| 2026-11-02 | Skills `android-di-hilt` y `android-networking-retrofit-okhttp` **no aplican** | Contradicen la DI manual (§5.1) y R9 (sin red) |
| 2026-11-03 | Las medidas que `Menu.qml` escribe en código (fila de 50 px, ancho de 300 px, borde de 2 px) se copian en `WhSpacing` | No están en ningún TOML; sin ellas el menú no coincide con el shell (`docs/design-system.md`) |
| 2026-11-03 | La fila seleccionada del menú no lleva borde aunque el TOML defina `selected-border` | El shell sólo lo dibuja si el tema fija `selected-border-width`, y el tema wh01s17 no lo hace |
| 2026-11-03 | Las notificaciones usan Departure Mono, no la Liberation Sans del shell | Una sola familia en el launcher (§4.4); revisable en el Hito 8 si se prefiere igualar el shell |

### 11.2 Riesgos

| Riesgo | Impacto | Mitigación |
| --- | --- | --- |
| Atajos `Meta + …` interceptados por el sistema | Algunos atajos no llegan | Modificador alternativo; documentar por versión de Android |
| Fabricantes que matan el proceso del launcher o limitan servicios | Estado perdido, widgets congelados | Estado persistido en cada cambio; sin trabajo en segundo plano innecesario |
| Freeform no disponible en la mayoría de teléfonos | Hito 9 de alcance limitado | Hito marcado experimental; no bloquea 1.0 |
| Accesibilidad usada para acciones globales puede ser rechazada por el usuario | Faltan bloquear/cortina/apagado | Funciones opcionales, explicadas, con fallback |
| Departure Mono pequeña en móvil | Legibilidad | Factor de escala de fuente configurable (§4.4) |
| Assets de terceros (Gengar, Opeth) | Licencias | No versionarlos; importación por el usuario |
| Archivos que crecen (ViewModels, pantallas) | Violan R1 | Aviso a 600 líneas y división por responsabilidad |
| HyperOS ignora o reinicia `force_fsg_nav_bar`, o deja *atrás* a medias | Sin gestos del sistema | Reaplicación con `ContentObserver` (capa 2) y gestos propios (capa 3) |
| Una actualización OTA de HyperOS cambia el comportamiento | Regresión inesperada | No aplica al Redmi actual (se queda en HyperOS 2.0.204). Para otros equipos o futuros teléfonos: repetir la matriz de `docs/xiaomi.md` tras cada OTA |
| El comportamiento de HyperOS 2 está fijo y puede ser hostil (gesto *atrás* a medias) | No hay arreglo de Xiaomi que esperar | Capa 3 como solución definitiva si la capa 2 no basta |
| MIUI mata el servicio de accesibilidad | Se pierden los gestos de la capa 3 | Batería *Sin restricciones* + *Inicio automático* desde el asistente |
| Quedar sin navegación: botones ocultos (`force_fsg_nav_bar = 1`) sin la capa 3 viva | Teléfono inutilizable sin ADB | Vigilancia del modo *gestos wh01s17* (§5.9) y test instrumentado; recuperación documentada |
| `pm grant` rechazado en Xiaomi | Capa 2 inutilizable | Activar *Depuración USB (ajustes de seguridad)*; si no, capa 3 |

---

## 12. Estado actual

- **Hito en curso:** Hito 2. **Hitos 0 y 1 terminados.**
- **Última tarea completada:** drawer provisional (`feature/menu/drawer`) con búsqueda
  difusa (`core/fuzzy`), lanzamiento desde el icono, info, desinstalar y atajos. Probado en
  el Redmi (como HOME temporal para los atajos).
- **Siguiente tarea:** `WhNavigationService` (capa 3 de §5.9). Es la parte delicada del
  hito: no dejar el teléfono sin navegación. Modelo recomendado en §0: Opus 5.5 · `xhigh`.
  Pendiente de medir: la primera carga del índice tarda ≈ 3,4 s en debug; si sigue lenta
  en release, guardar el último índice en disco (Hito 11).
- **Bloqueos:** ninguno.
- **Launcher predeterminado del Redmi:** `com.miui.home` con gestos
  (`force_fsg_nav_bar = 1`, `navigation_mode = 2`), comprobado el 2026-11-03, hasta que el
  nuestro sea usable a diario (fin del Hito 2). Ya no es `bitpit.launcher`.
- **Dispositivo objetivo:** Redmi Note 13 Pro 5G, HyperOS
  `OS2.0.204.0.VNRMIXM` (ROM global), Android 15 / API 35, sin más
  actualizaciones. ADB funciona desde el PC de desarrollo, con
  *Depuración USB (ajustes de seguridad)* activada.
- **Gestos en el Redmi:** probado el 2026-11-02 con un launcher ajeno.
  Forzar `force_fsg_nav_bar = 1` oculta los botones, pero ningún gesto
  funciona. Se restauró `0` (3 botones). La capa 3 pasa a ser la navegación
  principal (Hito 2). Observaciones, ajustes y matriz de pruebas en
  [`docs/xiaomi.md`](docs/xiaomi.md).
- **Versión publicada:** ninguna.

---

## 13. Registro de actualizaciones

Añadir una fila por cada sesión de trabajo, la más reciente arriba.

| Fecha | Hito | Resumen | Archivos clave |
| --- | --- | --- | --- |
| 2026-11-03 | 2 | Acciones de app (`LauncherAppActions`: lanzar desde el icono, info, desinstalar, atajos), `core/fuzzy` (`FuzzyIndex`, `FuzzyMatcher`, benchmark), drawer provisional (`AppDrawerViewModel` + Compose, 7 pruebas), `HomeScreen`; `WhListRow` con título enriquecido, `leading` y `onLongClick`. Probado en el Redmi: búsqueda, Enter, toque, menú, info, diálogo de desinstalar (cancelado), suspensión en vivo (`pm suspend`) y atajos como HOME temporal; se restauró `com.miui.home` y `force_fsg_nav_bar = 1` | `core/apps/actions/`, `core/fuzzy/`, `feature/menu/`, `app/` |
| 2026-11-03 | 2 | Iconos: `IconStyle` (`monochrome`/`original`/`pixelated`), `IconRenderer`, `IconCache` LRU en bytes, `LauncherIconLoader` con insignia de perfil; `AppEntry.iconStamp`. 9 pruebas con Robolectric (píxeles reales) | `core/apps/.../icons/` |
| 2026-11-03 | 2 | `AppRepository`: catálogo ordenado por idioma, perfiles (personal, trabajo, espacio privado), avisos de `LauncherApps` y broadcasts de perfil/idioma, `<queries>` MAIN/LAUNCHER; `AppContainer` y `LauncherApplication` (DI manual). 18 pruebas con un gateway falso | `core/apps/`, `app/.../di/AppContainer.kt` |
| 2026-11-03 | 1 | **Hito 1 terminado.** Comparación con el shell (capturas `grim` del menú y de una notificación, código de `Menu.qml`): borde del menú a 2 px, fila seleccionada sin borde, `WhListRowSize.Menu` con las medidas de `Menu.qml` y `›`, nueva sección y captura *menu*. Notificaciones: se mantiene Departure Mono (§11.1) | `core/designsystem/`, `docs/design-system.md` |
| 2026-11-02 | 0 | **Hito 0 terminado.** APK instalado en el Redmi y probado como HOME vía `cmd role` (Inicio vuelve, *atrás* no cierra); se restauró `bitpit.launcher`. Arranque en frío del build debug: 1,75 s (se mide en release en el Hito 11) | `plan.md`, `docs/xiaomi.md` |
| 2026-11-02 | — | Tabla de modelo y esfuerzo por hito en §0, con equivalentes de Codex (`gpt-6.1-sol`, `gpt-6.1-terra`, `gpt-6-luna`, `gpt-6-astra`) | `plan.md` |
| 2026-11-02 | 1 | Sistema de diseño: `sync-theme.py`, generador de tokens (`build-logic`), tema, iconos, 10 componentes, galería debug, capturas Roborazzi, R6 y `docs/design-system.md` | `core/designsystem/`, `build-logic/`, `tools/`, `theme/`, `docs/design-system.md` |
| 2026-11-02 | 0 | Historial reescrito en commits semánticos (force push a `main`); CI en verde en GitHub Actions | `.github/workflows/ci.yml` |
| 2026-11-02 | 0 | Proyecto Gradle (AGP 9.5.0, Kotlin 2.4.21, Gradle 9.8.2), 15 módulos con convention plugins, `MainActivity` HOME verificada en emulador, calidad (ktlint, detekt 2 alpha, Lint, Kover, longitud de archivos), CI, `docs/setup.md`, sección de skills | `build-logic/`, `app/`, `.github/workflows/ci.yml`, `docs/setup.md` |
| 2026-11-02 | 0 | Entorno completo: command-line tools 23.0.0, build-tools 37.0.0, imagen API 35 `google_apis`; AVD `redmi-api35` creado (1220×2712, 480 dpi, teclado) | `plan.md`, `~/.android/avd/redmi-api35.avd` |
| 2026-11-02 | — | SDK instalado vía Android Studio: API 37.0, build-tools 36.0.0, emulador, AVD API 37; faltan command-line tools e imagen API 35 | `plan.md` |
| 2026-11-02 | — | Entorno revisado: Android Studio 2026.2.2 instalado (sin abrir), JBR 25 para Gradle, JDK 27 del sistema no se usa; sólo falta el SDK | `plan.md` |
| 2026-11-02 | — | Límite de 1000 líneas flexible con justificación registrada (R1a, `tools/file-length.allow`, script actualizado); `plan.md` es la primera entrada | `plan.md` |
| 2026-11-02 | — | Observaciones del Redmi movidas a `docs/xiaomi.md` con matriz de gestos | `plan.md`, `docs/xiaomi.md` |
| 2026-11-02 | — | Con un launcher ajeno, los gestos no funcionan aunque `force_fsg_nav_bar = 1`; se restauró `0`. Nueva estrategia: capa 3 principal (Hito 2) + modo *gestos wh01s17* con vigilancia | `plan.md` |
| 2026-11-02 | — | Capa 2 probada en el Redmi con un launcher ajeno: `force_fsg_nav_bar = 1` se mantiene y `navigation_mode` pasa solo a `2`; hizo falta reconectar ADB tras activar el interruptor de seguridad | `plan.md` |
| 2026-11-02 | — | `adb shell settings put` falla con `SecurityException` (falta `WRITE_SECURE_SETTINGS`) sin *Depuración USB (ajustes de seguridad)* | `plan.md` |
| 2026-11-02 | — | HyperOS escribe `force_fsg_nav_bar = 0` y `navigation_mode = 0` al cambiar a un launcher ajeno; la capa 2 reaplica al obtener el rol HOME; nota sobre `hide_gesture_line` | `plan.md` |
| 2026-11-02 | — | Por ADB: `garnet`, launcher `com.miui.home`, navegación por gestos, `force_fsg_nav_bar = 1` estable más de 28 h | `plan.md` |
| 2026-11-02 | — | Confirmado por ADB: `OS2.0.204.0.VNRMIXM` (ROM global), API 35 | `plan.md` |
| 2026-11-02 | — | Dispositivo fijado: Redmi Note 13 Pro 5G, HyperOS 2.0.204 sin OTA; emulador API 35 a 1220×2712, fondos a esa resolución, objetivo de 120 fps, riesgos ajustados | `plan.md` |
| 2026-11-02 | — | Compatibilidad con Xiaomi: §5.9 (gestos en tres capas), tareas en Hitos 0, 2 y 6, comandos ADB, decisión y riesgos | `plan.md` |
| 2026-11-02 | — | Plan inicial: visión Hyprland en Android, tema wh01s17, arquitectura, hitos 0–11, reglas (máx. 1000 líneas/archivo) | `plan.md` |

---

## 14. Glosario

- **Tile:** "ventana" del launcher dentro de un workspace (app, widget,
  widget nativo o grupo).
- **Dwindle / Master:** algoritmos de tiling de Hyprland; dwindle divide
  recursivamente el tile con foco, master reserva un área principal.
- **Scratchpad:** workspace especial que se superpone y se oculta a demanda.
- **Token:** valor de diseño (color, alfa, medida, tipografía) generado desde
  el tema de los dotfiles.
- **Comando (`LauncherCommand`):** acción abstracta que pueden disparar
  teclado, gestos, barra o menú.
- **Freeform:** modo de ventanas libres de Android que permite fijar el tamaño
  y posición de apps ajenas.
