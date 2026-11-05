# La W girando

Style → Estilo y configuración → Salvapantallas permite ver una vista previa, elegir los segundos
de inactividad y abrir los ajustes de salvapantallas de Android. La vista previa se cierra tocando
la pantalla o pulsando una tecla.

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
