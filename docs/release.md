# Publicar una versión

Cómo sale una versión firmada del launcher (plan.md §8). Sin Play Store: el APK se publica como
*GitHub Release* y se instala con [Obtainium](https://github.com/ImranR98/Obtainium) o
`adb install -r`. La app no busca actualizaciones por su cuenta (R9: sin red).

## Versión

La versión sólo se escribe en [`version.properties`](../version.properties):

```properties
version=1.0.0
```

`build-logic` calcula `versionName` y `versionCode = MAJOR × 10000 + MINOR × 100 + PATCH`
(`1.0.0` → `10000`, `0.5.0` → `500`). `MINOR` y `PATCH` van de 0 a 99; otro valor rompe la
compilación. Cada hito sube `MINOR`; un arreglo, `PATCH`.

## Clave de firma

La clave **nunca** se versiona (`*.jks`, `*.keystore` y `keystore.properties` están en
`.gitignore`). Sin clave, `assembleRelease` genera `app-release-unsigned.apk`; con datos a medias,
la compilación falla para no publicar algo sin firmar creyendo que lo está.

**Local:** `keystore.properties` en la raíz del repositorio. `storeFile` es relativo a ese archivo
o absoluto:

```properties
storeFile=/home/<usuario>/.android/wh01s17-release.jks
storePassword=…
keyAlias=wh01s17
keyPassword=…
```

La clave actual se creó el 2026-11-14 en `~/.android/wh01s17-release.jks` (PKCS12, RSA 4096,
10 000 días). Huella SHA-256 del certificado:
`7E:84:37:3C:66:E8:EA:76:0F:72:61:A9:C3:20:0A:37:33:23:07:C3:46:10:80:27:55:9E:CA:57:4C:34:3E:C1`.
**Respáldala con su contraseña**: sin ella, una versión nueva no se puede instalar encima de la
anterior y habría que desinstalar, perdiendo escritorios y ajustes.

**CI:** las mismas cuatro piezas como secretos del repositorio (Settings → Secrets → Actions):

| Secreto | Valor |
| --- | --- |
| `SIGNING_KEYSTORE_B64` | `base64 -w0 ~/.android/wh01s17-release.jks` |
| `SIGNING_STORE_PASSWORD` | `storePassword` |
| `SIGNING_KEY_ALIAS` | `keyAlias` |
| `SIGNING_KEY_PASSWORD` | `keyPassword` |

Con `gh`:

```bash
base64 -w0 ~/.android/wh01s17-release.jks | gh secret set SIGNING_KEYSTORE_B64
gh secret set SIGNING_STORE_PASSWORD   # pega el valor cuando lo pida
gh secret set SIGNING_KEY_ALIAS --body wh01s17
gh secret set SIGNING_KEY_PASSWORD
```

En CI la clave llega como variables de entorno (`SIGNING_KEYSTORE_FILE`, `SIGNING_STORE_PASSWORD`,
`SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`), que ganan a `keystore.properties`.

## Pasos

1. Sube `version.properties` y pasa la sección `[Sin publicar]` de `CHANGELOG.md` a
   `## [X.Y.Z] — AAAA-MM-DD` (formato *Keep a Changelog*). Esa sección son las notas de la versión:
   `python3 tools/release-notes.py notes X.Y.Z` las muestra.
2. `./gradlew check assembleRelease` en local y, si cambió algo de rendimiento, regenera el perfil y
   mide ([performance.md](performance.md)).
3. Commit, `git tag -a vX.Y.Z -m "wh01s17 Launcher X.Y.Z"` y
   `git push --follow-tags origin main`. `--follow-tags` sube las etiquetas anotadas.
4. [`.github/workflows/release.yml`](../.github/workflows/release.yml) comprueba que el tag coincide
   con `version.properties` y que el CHANGELOG tiene la sección (`tools/release-notes.py check`),
   pasa `check`, firma `assembleRelease` y publica `wh01s17-launcher-X.Y.Z.apk` y
   `mapping-X.Y.Z.txt` (para descifrar trazas de R8) con esas notas.

## APK release de cada push

Además, [`ci.yml`](../.github/workflows/ci.yml) compila la release en cada push a `main` y en cada PR, y
la deja como artefacto `wh01s17-launcher-release` (APK y `mapping.txt`) en la pestaña *Actions* de esa
ejecución. Con los secretos de firma sale firmada con la misma clave que las versiones publicadas, así
que se instala encima; sin ellos (un fork, un PR de fuera) sale `app-release-unsigned.apk`, que sólo
sirve para comprobar R8. Los artefactos caducan a los 90 días; lo que se instala a diario son las
versiones publicadas.

## Instalar la release en el teléfono

La release es `com.wh01s17.launcher`; la del uso diario (`fast`/`debug`) es
`com.wh01s17.launcher.debug`. Son apps distintas, con datos distintos: pasar a la release implica
volver a elegirla como launcher, conceder `WRITE_SECURE_SETTINGS` a su paquete
(`adb shell pm grant com.wh01s17.launcher android.permission.WRITE_SECURE_SETTINGS`), activar su
servicio de gestos y su acceso a notificaciones, y llevarse la config con *Exportar* /
*Importar* de Estilo ([style.md](style.md)). Los escritorios (`files/state/workspaces.json`) no se
exportan. Antes de cambiar de launcher con el modo *gestos wh01s17* activo, desactívalo en la app
que se deja ([xiaomi.md](xiaomi.md)).
