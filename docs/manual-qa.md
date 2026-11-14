# Pruebas manuales antes de una versión

Lo que no cubren las pruebas automáticas (plan.md §9). Se hace en el Redmi con la build que se va a
publicar (o `fast`, que es la misma con el id de debug). Copia la tabla al final con la fecha, la
versión y el resultado de cada fila.

## Navegación (nunca dejar el teléfono sin ella)

- [ ] Como HOME, con *gestos wh01s17*: atrás (izquierda y derecha), inicio y recientes; los botones
      de Android ocultos.
- [ ] Desactivar el servicio *Gestos wh01s17* en Accesibilidad: los botones vuelven en menos de 1 s.
- [ ] Reactivarlo: los botones se ocultan solos.
- [ ] `adb reboot`: tras arrancar, gestos y botones ocultos sin tocar nada.
- [ ] Teclado en pantalla (SwiftKey) abierto: *inicio* desde la franja fina inferior; las laterales
      no tapan el teclado.
- [ ] Recientes → X desde el inicio: vuelve al inicio, el launcher no se cierra.

## Permisos denegados (R8: todo funciona sin ellos)

- [ ] Sin acceso a notificaciones: el panel lo explica y ofrece los ajustes; sin insignias ni contador.
- [ ] Sin permiso de notificaciones: pomodoro y metrónomo funcionan, sin avisos.
- [ ] Sin `WRITE_SECURE_SETTINGS`: Compatibilidad muestra los comandos ADB; la capa 3 funciona sola.
- [ ] Sin micrófono: el visualizador de TIDAL lo explica.
- [ ] Sin servicio de accesibilidad: bloquear, cortina y apagado explican qué falta.

## Uso diario

- [ ] Arranque en frío tras `am force-stop`: el escritorio aparece sin pantalla negra larga.
- [ ] Deslizar los diez escritorios y volver; tocar `[n]` en la barra.
- [ ] Menú: abrir deslizando y tocando la W, buscar una app, abrirla, Escape/atrás.
- [ ] Modo edición: añadir app y widget, mover a otro escritorio, agrupar, `deshacer`, `listo`.
- [ ] Scratchpad: dos dedos abajo / `ALT + S`.
- [ ] Media en la barra con TIDAL sonando; `CTRL + ALT + P`.
- [ ] Pomodoro desde la barra: notificación con cuenta atrás; aviso al terminar la fase.
- [ ] Estilo: cambiar paleta y escala y volver; exportar la config (cancelar sin escribir vale).
- [ ] Salvapantallas: vista previa desde Estilo; tocar sale.

## Teclado físico Bluetooth

- [ ] `ALT + 1…5`, `ALT + SPACE`, `ALT + RETURN` (Termux), `ALT + K` (tabla), flechas en el menú.
- [ ] Editor de atajos: capturar una combinación y restablecer.

## Gestos multitáctiles (ADB no los simula)

- [ ] Dos dedos a los lados cambia de escritorio; dos dedos abajo, el scratchpad.

## Rotación

- [ ] De fábrica en vertical aunque el sistema gire.
- [ ] Estilo → *Girar con el sensor*: el escritorio gira y vuelve; desactivarlo.

## TalkBack ([accessibility.md](accessibility.md))

- [ ] Recorrer la barra: cada módulo dice qué es.
- [ ] Un tile pequeño dice el nombre de la app; el del foco, «seleccionado».
- [ ] Abrir el menú y una app con doble toque; entrar en edición con la acción de TalkBack.
- [ ] Al apagar TalkBack, deslizar escritorios va igual de fluido que antes.

## Registro

| Fecha | Versión | Resultado | Notas |
| --- | --- | --- | --- |
| 2026-11-14 | 1.0.0 (`fast`) | Parcial | Automático: arranque, fotogramas, contraste, etiquetas y áreas táctiles. Pendiente a mano: teclado Bluetooth, dos dedos y TalkBack |
