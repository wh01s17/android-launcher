# Publicar en tiendas

Qué haría falta para que el launcher llegue por F-Droid, IzzyOnDroid y Google Play, además del
canal actual (GitHub Releases + Obtainium, [release.md](release.md)). Es una guía, no una decisión:
[`plan.md` §8.2](../plan.md#82-distribución-y-actualización-del-dispositivo) sigue diciendo "sin
Play Store" hasta que se decida otra cosa.

Requisitos revisados el **2026-11-16**. Las tiendas cambian sus políticas a menudo: vuelve a
leer los enlaces de [Fuentes](#fuentes) antes de empezar.

## Resumen

| Canal | Viabilidad | Coste | Quién firma | Bloqueos principales |
| --- | --- | --- | --- | --- |
| GitHub + Obtainium (actual) | Ya funciona | 0 | Tu clave | Verificación de desarrolladores (2027) |
| F-Droid | Viable | 0 | F-Droid, o tu clave si la build es reproducible | Metadatos, `versionCode` legible, build reproducible, nombre TIDAL |
| IzzyOnDroid | **Muy improbable** | 0 | Tu clave (usan tu APK) | Rechaza apps escritas con IA generativa |
| Google Play | Difícil | 25 USD + 12 testers × 14 días | Google (Play App Signing) | Servicio de accesibilidad, `USE_EXACT_ALARM`, declaraciones |

Recomendación: preparar primero lo común (siguiente sección), que sirve para todas, y pedir la
inclusión en F-Droid. Play sólo si compensa mantener una variante recortada.

## Antes de cualquier tienda

Estos cambios son comunes. Ninguno está hecho todavía.

### 1. Metadatos Fastlane en el repo

F-Droid e IzzyOnDroid leen la ficha de la app desde el propio repositorio, con esta estructura:

```text
fastlane/metadata/android/
├── es-ES/
│   ├── title.txt                 # wh01s17 Launcher
│   ├── short_description.txt     # < 80 caracteres, sin punto final
│   ├── full_description.txt      # descripción larga (admite HTML básico)
│   ├── changelogs/
│   │   └── 10104.txt             # notas de la versión; < 500 caracteres; nombre = versionCode
│   └── images/
│       ├── icon.png              # 512 × 512
│       ├── featureGraphic.png    # 1024 × 500 (opcional en F-Droid, obligatorio en Play)
│       └── phoneScreenshots/
│           ├── 1.png
│           └── …
└── en-US/                        # recomendable: la mayoría de usuarios de F-Droid lee inglés
    └── …
```

- **Icono:** `rsvg-convert -w 512 -h 512 theme/wh01s17/brand/logo.svg -o …/images/icon.png`.
- **Capturas:** las de [`docs/screenshots/`](screenshots) sirven tal cual (1220 × 2712). Bastan
  4–6: escritorio, menú, edición, estilo, widgets y TIDAL.
- **Changelog:** un archivo por `versionCode`. Se puede generar desde `CHANGELOG.md` con
  `python3 tools/release-notes.py notes X.Y.Z`, recortado a 500 caracteres.
- **Descripción:** debe declarar los permisos delicados y por qué se usan (accesibilidad,
  notificaciones, micrófono para el visualizador, `WRITE_SECURE_SETTINGS` por ADB). Las tiendas
  rechazan las funciones con permisos delicados que no se explican.

### 2. `versionCode` legible sin ejecutar Gradle

F-Droid detecta versiones nuevas con expresiones regulares sobre los archivos del repo; **no
ejecuta Gradle**. Hoy `versionCode` se calcula en `build-logic` a partir de `version.properties`, así
que F-Droid no lo puede leer. Solución: escribirlo también como literal y que `build-logic`
compruebe que coincide con la fórmula (y falle si no):

```properties
version=1.1.4
versionCode=10104
```

### 3. Quitar el bloque de dependencias del APK

AGP mete en el bloque de firma del APK un bloque cifrado con la lista de dependencias que sólo
Google puede leer. F-Droid e IzzyOnDroid piden quitarlo porque no se puede auditar. En
`AndroidApplicationConventionPlugin`:

```kotlin
dependenciesInfo {
    includeInApk = false     // APK de GitHub, F-Droid e IzzyOnDroid
    includeInBundle = true   // el AAB de Play puede conservarlo
}
```

### 4. Build reproducible

Con una build reproducible, F-Droid compila desde el código, comprueba que sale el mismo APK que el
de GitHub y **publica tu APK con tu firma**. Ventajas:

- Los usuarios pueden pasar entre GitHub/Obtainium y F-Droid sin desinstalar (misma firma).
- Si algún día hay que registrar la clave para la verificación de Google, basta registrar una.

Cómo comprobarlo:

1. Compila la release dos veces en entornos limpios (por ejemplo, un contenedor Debian con el
   OpenJDK de Debian, no el JBR de Android Studio) desde el mismo tag.
2. Compara los APK con [`apksigcopier compare`](https://github.com/obfusk/apksigcopier) o
   `diffoscope`. Deben coincidir salvo la firma.
3. Causas típicas de diferencias: rutas absolutas embebidas, JDK distinto (usa una LTS),
   `baseline-prof.txt` regenerado entre builds, marcas de tiempo, compilar en Windows.

Si no sale reproducible, F-Droid firma con **su propia clave**: la versión de F-Droid y la de GitHub
serían apps incompatibles entre sí (hay que desinstalar para cambiar, y se pierden escritorios y
ajustes; ver [release.md](release.md#instalar-la-release-en-el-teléfono)).

### 5. Nombres de terceros

El widget se llama **TIDAL** y la documentación cita Hyprland y HyperOS. F-Droid exige permiso
para usar marcas registradas, y Play prohíbe usarlas en la ficha sin autorización. Antes de enviar:

- En la ficha de la tienda, describe el widget como "widget de música con visualizadores", sin
  logotipos ni nombres de terceros en el título o la descripción corta.
- Valora renombrar el widget en la UI (por ejemplo, "música") y dejar TIDAL sólo como una de las
  apps compatibles. Mencionar Hyprland como inspiración en la descripción larga suele ser
  aceptable.

### 6. Transparencia sobre el uso de IA

El historial y [`plan.md`](../plan.md) dejan claro que el código se escribió con agentes de IA
(Claude Code y Codex; 23 commits con `Co-Authored-By: Claude`). Hay que decirlo en la solicitud:
ocultarlo es motivo de rechazo o retirada en IzzyOnDroid, y F-Droid está discutiendo su política
(ver cada sección).

## F-Droid

**Requisitos que ya se cumplen:** código público con licencia libre (MIT), sin servicios de Google
ni Firebase, sin rastreadores ni anuncios, sin red (no declara `INTERNET`), y dependencias libres
desde Maven Central y Google Maven. Las fuentes llevan sus licencias (Departure Mono, OFL;
Pixelarticons, MIT). El paquete `com.wh01s17.launcher` corresponde a un dominio propio
(`wh01s17.com`), algo que F-Droid comprueba.

**Puntos a vigilar:**

- **Toolchain:** el servidor de F-Droid compila con OpenJDK de Debian y el SDK de Android de
  código abierto. Comprueba que `./gradlew assembleRelease` funciona con OpenJDK 21 o 25 de Debian
  (el plan usa el JBR 25; el bytecode es Java 17).
- **`gradle-wrapper.jar`:** el escáner de F-Droid lo detecta como binario. Su receta suele
  usar `gradle: [yes]` con su propio Gradle; puede hacer falta `scandelete` o un `prebuild` que lo
  borre.
- **Antifeatures:** probablemente ninguna. El widget de música lee la sesión multimedia de
  cualquier reproductor, así que no depende de una app privativa (`NonFreeDep`) mientras funcione
  con otros reproductores. Decláralo si los revisores lo piden; una antifeature no declarada es
  motivo de rechazo.
- **IA:** a fecha de este documento F-Droid **no prohíbe** el código generado con IA. Hay un
  borrador de política interina (adaptado de Debian) que ni lo respalda ni lo prohíbe, pero que
  considera incompatible la salida de IA sin revisar. Hay que mencionar el uso de IA y que el
  código se revisa y se prueba (`./gradlew check`, tests, QA manual en el Redmi).

### Pasos

1. Haz los cambios de [Antes de cualquier tienda](#antes-de-cualquier-tienda) y publica una
   versión nueva con tag `vX.Y.Z` (ya es el formato actual; F-Droid exige que el tag coincida con
   `versionName`).
2. Crea una cuenta en GitLab.com y haz *fork* de
   [`fdroid/fdroiddata`](https://gitlab.com/fdroid/fdroiddata). Crea una rama llamada
   `com.wh01s17.launcher`.
3. Crea `metadata/com.wh01s17.launcher.yml`. Borrador, a validar con `fdroid lint`:

   ```yaml
   Categories:
     - Theming
     - System
   License: MIT
   AuthorName: wh01s17
   AuthorWebSite: https://wh01s17.com
   WebSite: https://github.com/wh01s17/android-launcher
   SourceCode: https://github.com/wh01s17/android-launcher
   IssueTracker: https://github.com/wh01s17/android-launcher/issues
   Changelog: https://github.com/wh01s17/android-launcher/blob/HEAD/CHANGELOG.md

   AutoName: wh01s17 Launcher

   RepoType: git
   Repo: https://github.com/wh01s17/android-launcher.git
   Binaries: https://github.com/wh01s17/android-launcher/releases/download/v%v/wh01s17-launcher-%v.apk

   Builds:
     - versionName: 1.1.5
       versionCode: 10105
       commit: v1.1.5
       subdir: app
       gradle:
         - yes

   # Huella SHA-256 del certificado de firma (release.md), en minúsculas y sin ':'.
   AllowedAPKSigningKeys: 7e84373c66e8ea760f7261a9c3200a37332307c346108027559eca574c343ec1

   AutoUpdateMode: Version
   UpdateCheckMode: Tags ^v[0-9.]+$
   UpdateCheckData: version.properties|versionCode=(\d+)|.|version=([\d.]+)
   CurrentVersion: 1.1.5
   CurrentVersionCode: 10105
   ```

   - Las categorías válidas están en `config/categories.yml` de fdroiddata; cámbialas si no
     existen.
   - `Binaries` + `AllowedAPKSigningKeys` activan la verificación reproducible. Si la build no
     es reproducible, quita ambas líneas: F-Droid firmará con su clave.
   - `UpdateCheckData` depende del `versionCode` literal del paso 2 de la sección común.
4. Prueba la receta, en local con el contenedor de `fdroidserver` (unos 2 GB de descarga y 5 GB de
   disco) o dejando que lo haga la CI de tu fork:

   ```bash
   fdroid readmeta
   fdroid rewritemeta com.wh01s17.launcher
   fdroid checkupdates --allow-dirty com.wh01s17.launcher
   fdroid lint com.wh01s17.launcher
   fdroid build -v -l com.wh01s17.launcher
   ```

5. Commit `New App: com.wh01s17.launcher`, *push* a tu fork y abre un *merge request* contra
   `fdroiddata`. En la descripción explica:
   - qué hace la app y por qué pide cada permiso delicado;
   - que no tiene red;
   - que usa IA generativa en el desarrollo y cómo se revisa;
   - que la build es reproducible, si lo es.
6. Responde a los revisores. Tras el *merge*, la app aparece en 24–48 h. Las versiones siguientes
   llegan solas: el bot de F-Droid detecta el tag nuevo (`AutoUpdateMode`) y las compila. El ciclo
   suele tardar varios días.

## IzzyOnDroid

Repositorio alternativo que distribuye **tu APK de GitHub** (no compila) y verifica builds
reproducibles con builders propios. Es más rápido que F-Droid y conserva tu firma.

**Bloqueo:** su [política de inclusión](https://izzyondroid.org/docs/general/AppInclusionPolicy/)
dice que se opone con firmeza a las apps creadas total o parcialmente con IA generativa y que
rechaza las apps *vibe-coded*. Sólo admite texto generado en la documentación y el uso de LLM para
tareas de "sólo lectura" (investigar, depurar) cuya salida no acabe en el código. Ocultarlo puede
hacer que se rechace el proyecto. Con el historial actual, **lo esperable es un rechazo**, así que no
merece la pena enviarla salvo que cambie su política.

Si cambiara, los requisitos serían:

- licencia libre, código público y sin rastreadores;
- metadatos Fastlane en el repo;
- APK firmado con clave de release, publicado en un *GitHub Release* con tag (ya se cumple);
- APK de hasta unos 30 MB;
- solicitud por la vía que indica su página de nuevas inclusiones.

## Verificación de desarrolladores de Android

Afecta a **todos los canales fuera de Play**, incluido el actual.

- Desde el **30 de septiembre de 2026**, los dispositivos Android certificados (Android 7+)
  exigen que la app esté registrada por un desarrollador verificado en Brasil, Indonesia, Singapur
  y Tailandia. **En 2027 se amplía al resto del mundo**, Chile y el Redmi incluidos.
- Para apps distribuidas sólo fuera de Play se usa la **Android Developer Console**: verificar la
  identidad y registrar el paquete con su clave de firma.
- Existe una cuenta de **distribución limitada** para estudiantes y aficionados: hasta 20
  dispositivos, sin documento de identidad ni tasa. Encaja con una app de un solo usuario.
- Hay un "flujo avanzado" para que usuarios avanzados instalen apps sin verificar. Según
  información de terceros, implica avisos y esperas, y las instalaciones por ADB quedarían fuera.
  Confírmalo en la documentación de Google cuando llegue.
- F-Droid se opone al programa y pide a los desarrolladores que no se registren. Con una build
  reproducible, la app de F-Droid lleva tu firma, así que un solo registro cubriría todos los
  canales.

**Antes de 2027, como mínimo:** decide entre la cuenta de distribución limitada (uso propio) y el
registro completo (si se publica en tiendas). Comprueba también si `adb install` sigue exento para
el flujo diario con `installFast`.

## Google Play

Play es el canal más exigente para este launcher: la app usa varios permisos que Play restringe a
categorías concretas. Lo realista es una **variante `play`** (*product flavor*) sin las piezas que
Play no acepta.

### Cuenta y requisitos generales

1. **Cuenta de desarrollador** en Play Console: 25 USD (pago único) y verificación de identidad.
   - **Cuenta personal** creada después del 13-11-2023: antes de pedir acceso a producción hay que
     pasar una **prueba cerrada con al menos 12 testers inscritos de forma continua durante 14
     días**. Si Google rechaza la solicitud, la prueba vuelve a empezar.
   - **Cuenta de organización:** exenta, pero exige número D-U-N-S (puede tardar semanas).
2. **Formato:** las apps nuevas se suben como **AAB** (`./gradlew bundleRelease`), con **Play App
   Signing**. Google guarda la clave de la app y tú firmas con una clave de subida. Al darte de alta
   puedes subir la clave actual (huella en [release.md](release.md)) para que la versión de Play sea
   compatible con la de GitHub. Si dejas que Google genere una nueva, son apps incompatibles entre
   sí.
3. **`targetSdk`:** Play exige apuntar a una API reciente. El launcher usa 37, así que cumple.
4. **Política de privacidad:** URL pública obligatoria aunque la app no recoja datos (por ejemplo,
   una página en `wh01s17.com`). Debe explicar el acceso a notificaciones, a la sesión multimedia y
   al micrófono, y que nada sale del teléfono.
5. **Sección de seguridad de los datos:** "no se recogen ni se comparten datos". Es cierto: sin
   `INTERNET` no hay forma de enviar nada.
6. **Clasificación de contenido** (cuestionario IARC) y **público objetivo** (mayores de 13 años,
   por ejemplo).
7. **Ficha:** título (≤ 30 caracteres), descripción corta (≤ 80), descripción larga (≤ 4000),
   icono de 512 × 512, gráfico destacado de 1024 × 500 (puede salir del banner del README) y al
   menos 2 capturas. Sin marcas de terceros (ver [Nombres de terceros](#5-nombres-de-terceros)).

### Permisos que Play revisa

| Permiso / API | Uso en el launcher | Riesgo en Play | Qué hacer en la variante `play` |
| --- | --- | --- | --- |
| `AccessibilityService` (navegación) | Gestos propios en HyperOS, ventana activa, reactivarse con `WRITE_SECURE_SETTINGS` | **Alto.** Los launchers no cuentan como herramienta de accesibilidad. Fuera de esa categoría hacen falta un aviso destacado en la app, consentimiento, declaración en Play Console y vídeo. `canRetrieveWindowContent` y la reactivación automática (`AccessibilityReviver`) empeoran la revisión. | Quitar el servicio y la navegación propia (usar la del sistema), o mantenerlo con aviso, consentimiento y declaración y asumir el riesgo de rechazo |
| `USE_EXACT_ALARM` | Fin de fase del pomodoro | **Alto.** Reservado a apps cuya función principal es alarma o calendario | Usar sólo `SCHEDULE_EXACT_ALARM` (el usuario lo concede) o alarmas inexactas |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Pomodoro visible | Medio. Exige declaración de tipo de servicio y descripción o vídeo | Declararlo en Play Console; valorar otro tipo de servicio en primer plano si encaja |
| `RECORD_AUDIO` | Visualizador (`Visualizer`) | Medio. Requiere aviso destacado y justificarlo en seguridad de los datos | Pedirlo sólo al activar el visualizador, con explicación; sin él, visualizador desactivado |
| `NotificationListenerService` | Notificaciones y sesión multimedia | Medio. Requiere explicar el uso | Aviso antes de enviar al usuario a ajustes (ya hay *opt-in*, R8) |
| `WRITE_SECURE_SETTINGS` | Navegación HyperOS por ADB | Bajo/medio. El usuario no puede concederlo desde la app; los revisores no verán la función | Explicarlo en la ficha o quitarlo junto con la navegación |
| `REQUEST_DELETE_PACKAGES` | Desinstalar desde el menú | Bajo | Mantener |
| `<queries>` (sin `QUERY_ALL_PACKAGES`) | Lista de apps y widgets | Bajo. Ya se evita el permiso restringido (plan §5.8) | Mantener |

### Pasos

1. Crea el *product flavor* `play` en `build-logic` / `app`, con un manifiesto que haga
   `tools:node="remove"` de lo que Play no acepta (tabla anterior). Las funciones que dependen de
   ello deben degradarse con elegancia (R8).
2. Añade los avisos destacados en la app antes de pedir accesibilidad, micrófono y notificaciones.
3. Prepara la política de privacidad y la ficha (puedes reutilizar los metadatos Fastlane;
   `fastlane supply` sube la ficha a Play).
4. Crea la app en Play Console (`com.wh01s17.launcher`), activa Play App Signing y sube el AAB a
   **prueba interna** para comprobarlo.
5. Pasa a **prueba cerrada** con al menos 12 testers durante 14 días seguidos (cuenta personal).
6. Rellena las declaraciones: seguridad de los datos, accesibilidad (si se mantiene el servicio),
   servicio en primer plano, clasificación de contenido y público objetivo.
7. Solicita acceso a **producción**. Tras la aprobación, cada versión se publica desde CI con la
   API de Play Developer (por ejemplo, `fastlane supply` con una cuenta de servicio guardada como
   secreto del repositorio, igual que la clave de firma en [release.md](release.md)).

## Fuentes

Consultadas el 2026-11-16.

- [F-Droid: guía rápida para enviar una app](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
- [F-Droid: política de inclusión](https://f-droid.org/docs/Inclusion_Policy/)
- [F-Droid: borrador de política interina sobre IA generativa](https://gitlab.com/fdroid/admin/-/issues/699)
- [F-Droid: carta abierta contra la verificación de desarrolladores](https://f-droid.org/2026/02/24/open-letter-opposing-developer-verification.html)
- [IzzyOnDroid: política de inclusión](https://izzyondroid.org/docs/general/AppInclusionPolicy/)
- [IzzyOnDroid: consejos para builds reproducibles](https://izzyondroid.org/docs/reproducibleBuilds/RBDevHints/)
- [Android: verificación de desarrolladores](https://developer.android.com/developer-verification)
- [Play Console: requisito de pruebas para cuentas personales nuevas](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Play Console: uso de la API de accesibilidad](https://support.google.com/googleplay/android-developer/answer/10964491)
- [Android 14: alarmas exactas](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
