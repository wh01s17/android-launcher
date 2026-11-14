# Accesibilidad

Lo que garantiza el launcher con TalkBack, contraste y áreas táctiles (plan.md §7 Hito 11).

## Contraste (WCAG AA)

Todo texto tiene al menos **4,5:1** con su fondo (AA para texto normal: Departure Mono a 10–16 sp
nunca cuenta como texto grande). Los tokens vienen de los dotfiles y no se editan a mano (§4.1), así
que el tema no cambia la paleta. `WhTheme` pasa los colores de texto por `WhColors.readable()`
(`core/designsystem/theme/WhContrast.kt`): si uno no llega al mínimo sobre alguna de las superficies
con texto, lo aclara (u oscurece, si el fondo es claro) en pasos de 1/64 hasta llegar.

Superficies comprobadas: el fondo (`background`), la barra y los paneles (`surface`), los tiles
(`lighterBackground`) y la fila seleccionada del menú (acento al 10 % sobre el panel).

Con `phosphor` sólo cambia `darkForeground`, el texto secundario: `#748178` (4,14:1 sobre los tiles
y 3,75:1 en la fila seleccionada) pasa a **`#838f87`** (5,0:1 y 4,54:1). El resto ya cumplía con
margen:

| Texto | Fondo | Barra/paneles | Tiles |
| --- | --- | --- | --- |
| `foreground` `#cbd5ce` | 12,9 | 12,1 | 11,2 |
| `accent` `#84c959` | 9,7 | 9,1 | 8,4 |
| `cyan` `#45d9ea` | 11,4 | 10,7 | 9,9 |
| `red` `#ff6b66` | 7,0 | 6,5 | 6,1 |
| `darkForeground` ajustado | 5,8 | 5,4 | 5,0 |

Con otras paletas de Estilo se aplica lo mismo, y también al texto de los widgets nova (TIDAL,
pomodoro…) sobre su fondo y su banda (`PlayerPalette.readable()`). `WhContrastTest` lo comprueba con
las 16 paletas, en normal y en modo OLED.

**Fuera de la regla, con motivo:** `muted` (`#33454f`, corchetes `[ ]` y separadores: decoración),
los escritorios vacíos de la barra al 35 % y las apps no disponibles al 45 % (estado inactivo, que
WCAG excluye), y el relleno de *pressed* (dura lo que dura el toque).

## TalkBack

Sin lector de pantalla, el inicio no publica su árbol de accesibilidad: el servicio de gestos de
la capa 3 siempre está activo y Compose recalcularía el árbol en cada fotograma
(`screenReaderSemantics`, §11.1). Al activar TalkBack vuelve en el acto.

| Elemento | Qué lee |
| --- | --- |
| Tile de app | El nombre de la app, también si no cabe en pantalla; «seleccionado» si tiene el foco. Acciones: abrir y, fuera de edición, editar el escritorio |
| Tile de widget | Lo que publique el widget (vista de Android) |
| Fondo del escritorio | «escritorio N» (o «scratchpad»); acciones: abrir el menú y editar |
| Módulos de la barra | Una descripción por módulo: menú, escritorios (pestañas, el actual seleccionado), reloj, batería, red, Bluetooth, audio, avisos, pomodoro, metrónomo y media |
| Insignia de notificaciones | El número de avisos de esa app |

`BarAccessibilityTest` y `WorkspacesScreenTest` comprueban que todo lo que se puede tocar tiene
algo que leer.

## Áreas táctiles

Todo lo táctil mide al menos **48 × 48 dp** de área, aunque se dibuje más pequeño (las celdas de la
barra miden 26 dp, o 20 dp en modo compacto). Compose amplía el área de toque de cualquier
`pointerInput` hasta `minimumTouchTargetSize` (48 dp) sin cambiar el dibujo; si dos se solapan, gana
el más cercano al dedo. Los componentes del sistema de diseño usan `Modifier.whTouchTarget()`.
Las mismas pruebas miden el área real (`touchBoundsInRoot`) de cada elemento.

## Prueba manual

Con TalkBack activado en el Redmi: recorrer la barra deslizando a la derecha, abrir una app desde un
tile pequeño, entrar en edición con la acción de TalkBack y abrir el menú. Anotar el resultado en
[manual-qa.md](manual-qa.md).
