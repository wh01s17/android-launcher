# Entorno de desarrollo

Resumen práctico de [`plan.md` §10](../plan.md#10-entorno-de-desarrollo).
Probado en Arch Linux el 2026-11-02.

## Requisitos

| Componente | Versión | Notas |
| --- | --- | --- |
| Android Studio | 2026.2.2 | Paquete `android-studio` (AUR), en `/opt/android-studio` |
| JDK para Gradle | JBR 25.0.4 de Android Studio | `/opt/android-studio/jbr`. **No** usar el JDK 27 del sistema |
| Android SDK | API 37.0, build-tools 37.0.0, command-line tools 23.0.0 | `~/Android/Sdk`, instalado desde *SDK Manager* |
| Imagen de emulador | API 35 `google_apis` x86_64 | Misma API que el Redmi Note 13 Pro 5G |
| `adb` | `android-tools` del sistema | No mezclar con el `adb` del SDK |

El Gradle Wrapper (9.8.2) descarga Gradle solo; no hace falta instalarlo.

## Configuración local (no versionada)

```bash
# JDK con el que corre Gradle (una vez por sesión o en ~/.zshrc)
export JAVA_HOME=/opt/android-studio/jbr

# Ruta del SDK para Gradle
printf 'sdk.dir=%s/Android/Sdk\n' "$HOME" > local.properties
```

Si prefieres no exportar `JAVA_HOME`, añade
`org.gradle.java.home=/opt/android-studio/jbr` a `~/.gradle/gradle.properties`.

## Comandos

```bash
./gradlew check            # tests, Lint, ktlint, detekt, longitud de archivos
./gradlew assembleDebug    # APK en app/build/outputs/apk/debug/app-debug.apk
./gradlew ktlintFormat     # corrige el formato
```

## Emulador del Redmi

AVD `redmi-api35`: API 35, 1220×2712, 480 dpi, teclado del PC habilitado.

```bash
~/Android/Sdk/emulator/emulator -avd redmi-api35            # con ventana
~/Android/Sdk/emulator/emulator -avd redmi-api35 -no-window  # sin ventana
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell cmd role add-role-holder android.app.role.HOME com.wh01s17.launcher.debug
```

Para recrearlo:

```bash
JAVA_HOME=/opt/android-studio/jbr ~/Android/Sdk/cmdline-tools/latest/bin/avdmanager \
  create avd -n redmi-api35 -k "system-images;android-35;google_apis;x86_64" -d pixel_7
# Después, en ~/.android/avd/redmi-api35.avd/config.ini:
# hw.lcd.width=1220, hw.lcd.height=2712, hw.lcd.density=480, hw.keyboard=yes
```

## Teléfono Xiaomi (HyperOS)

En *Opciones de desarrollador* hay que activar, además de *Depuración USB*:

- **Instalar vía USB**, para `adb install`. Sin esta opción falla con
  `INSTALL_FAILED_USER_RESTRICTED`.
- **Depuración USB (ajustes de seguridad)**, para `pm grant` y
  `settings put`. Después de activarla hay que reconectar ADB.

Detalles y pruebas en [`xiaomi.md`](xiaomi.md).
