# Notificaciones y media

El acceso a notificaciones es opcional. Menú → Sistema → Notificaciones abre el panel propio;
si falta acceso, explica qué lee y ofrece abrir los ajustes de Android. Activa wh01s17 allí.
Sin acceso siguen funcionando los escritorios y los atajos multimedia. No se guardan ni envían
los textos de las notificaciones.

Con acceso, los tiles muestran el número de avisos de su app y perfil (hasta `99+`), y el módulo
`wh01s17.indicators` muestra el total. Tocarlo abre el panel. Cada fila muestra app, antigüedad,
título y texto; tocarla ejecuta su acción, la cruz la descarta y `limpiar` descarta las borrables.
Escape, Atrás o tocar fuera cierran el panel. Las notificaciones propias, resúmenes de grupo,
fijas y vacías se excluyen; el total cuenta notificaciones, no mensajes dentro de cada una.

El panel usa la superficie de `shell.notifications.toml` y Departure Mono. El aviso emergente
está apagado de fábrica para no duplicar el de Android. Se activa en `files/config/launcher.json`:

```json
{"version": 1, "notifications": {"toast": true, "toastSeconds": 5}}
```

`toastSeconds` admite 1–30 segundos. Con el launcher visible, una notificación nueva muestra
el aviso con barra verde de cuenta atrás; tocarlo la abre. La primera lectura no anuncia avisos
antiguos. Con el panel abierto no aparecen avisos emergentes.

El módulo `media` de la barra sólo aparece con acceso y una sesión multimedia activa, de cualquier
app. Prefiere la que está reproduciendo; si todas están en pausa, la prioritaria de Android.
Tocar reproduce o pausa; mantener abre título, artista, álbum y anterior/reproducir/siguiente.
Los atajos `CTRL + ALT + O/P/I` hacen reproducir o pausar/siguiente/anterior y funcionan también
sin acceso: envían las teclas multimedia a Android. Están además en Menú → Sistema.

El scratchpad se describe en [workspaces.md](workspaces.md). Las pruebas y resultados del cierre
del hito se registran en [plan.md](../plan.md).
