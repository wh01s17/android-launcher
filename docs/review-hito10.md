# Revisión de cierre del Hito 10

Revisión local de código y pruebas del 2026-11-05, sobre el alcance completo del hito y R1–R9.

Se revisaron los límites entre módulos, la edición del JSON, conservación de claves desconocidas,
SAF, decodificación y sustitución de imágenes, actualización de fuente/iconos, comandos y menú,
ciclo de vida del render y salida del salvapantallas. El tema instalado es sólo `wh01s17`;
esta limitación y la separación entre inactividad de HOME y Dream de Android constan en §11.1.

Correcciones durante la revisión:

- La lectura del JSON importado queda acotada a 1 MiB y usa APIs compatibles con Android 10.
- La imagen se valida y reduce antes de reemplazar la anterior. El bitmap se libera también si
  falla abrir la escritura atómica; los reemplazos se serializan. El cálculo de tamaño usa `Long`.
- HOME consume todo el gesto de despertar, cuenta sólo mientras está reanudado y evita activarse
  sobre ventanas que tengan el foco. Los fotogramas de la W se cancelan al ocultarse o desmontarse.
- Se declaró explícitamente la dependencia de fuentes usada por la variante release.
- Se conservó la acción Pantalla de Android en el catálogo, además del nuevo editor de estilo.
- Los campos numéricos quedan alineados con sus botones.
- A petición del usuario, salvapantallas, fondos incluidos y base de HOME usan negro puro OLED.
  La excepción acotada al token de hardware está documentada en §11.1; el resto del tema se conserva.

Pruebas: parser y edición/importación/exportación (incluidos errores y fallo de disco), imágenes
inválidas y grandes con decodificación nativa, geometría de la W, negro OLED y PNG, UI/capturas de
estilo y fondos, catálogo y encoding de comandos. La prueba instrumentada del Redmi confirma
activación por inactividad, salida sin abrir un tile, vista previa y escritorio idéntico byte a byte;
restaura su configuración original al terminar.

En el emulador API 35 se comprobaron los selectores SAF de imagen y JSON, exportación real a
Downloads, reimportación y rechazo de un `appearance.fontScale` inválido conservando el archivo
idéntico. Dream Manager enlazó `WhDreamService`, produjo fotogramas distintos y lo cerró al tocar.
No se cambió el fondo Android del usuario; esa acción explícita se mantiene separada del selector.
Los ajustes que el usuario eligió en el Redmi durante las pruebas se conservan.

La verificación final y la versión quedan registradas en `plan.md` y `CHANGELOG.md`.
