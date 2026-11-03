package com.wh01s17.launcher.core.designsystem.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhColors
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

private val Swatch = 44.dp
private val IconCell = 92.dp

private fun Color.hex(): String = "#%06X".format(toArgb() and 0xFFFFFF)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GalleryColors() {
    val c = WhTheme.colors
    val entries: List<Pair<String, Color>> = swatches(c)
    GallerySection("colors") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            entries.forEach { (name, color) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(Swatch).background(color, WhTheme.borders.shape).border(1.dp, c.border, WhTheme.borders.shape))
                    WhText(name, style = WhTheme.typography.caption, color = c.darkForeground)
                    WhText(color.hex(), style = WhTheme.typography.caption, color = c.darkForeground)
                }
            }
        }
    }
}

private fun swatches(c: WhColors): List<Pair<String, Color>> =
    listOf(
        "background" to c.background,
        "darkBg" to c.darkBackground,
        "darkerBg" to c.darkerBackground,
        "lighterBg" to c.lighterBackground,
        "border" to c.border,
        "muted" to c.muted,
        "foreground" to c.foreground,
        "darkFg" to c.darkForeground,
        "lightFg" to c.lightForeground,
        "brightFg" to c.brightForeground,
        "accent" to c.accent,
        "selection" to c.selectionBackground,
        "cyan" to c.cyan,
        "yellow" to c.yellow,
        "orange" to c.orange,
        "red" to c.red,
        "blue" to c.blue,
        "magenta" to c.magenta,
        "brown" to c.brown,
    )

@Composable
internal fun GalleryTypography() {
    val t = WhTheme.typography
    val styles: List<Pair<String, TextStyle>> =
        listOf(
            "caption" to t.caption,
            "bodySmall" to t.bodySmall,
            "body" to t.body,
            "subtitle" to t.subtitle,
            "title" to t.title,
            "heading" to t.heading,
            "display" to t.display,
            "displayLarge" to t.displayLarge,
        )
    GallerySection("typography - Departure Mono") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            styles.forEach { (name, style) ->
                WhText("$name ${style.fontSize.value}sp  wh01s17 0123", style = style)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GalleryIcons() {
    GallerySection("icons - wh01s17 Pixel Icons") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WhIcons.catalog.forEach { (name, glyph) ->
                Column(Modifier.size(IconCell, 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    WhIcon(glyph, size = WhTheme.spacing.iconSizeLarge, contentDescription = name)
                    WhText(name, style = WhTheme.typography.caption, color = WhTheme.colors.darkForeground, maxLines = 1)
                }
            }
        }
    }
}
