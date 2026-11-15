package com.wh01s17.launcher.feature.widgets.native.hack

import androidx.compose.runtime.Composable
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/** Una frase de `fortune` y de dónde sale. */
internal class Fortune(
    val quote: String,
    val source: String,
)

/**
 * Frases de la cultura hacker en el cine y la terminal, para `fortune`. Son citas cortas y van con su
 * fuente; se dejan en el idioma original, como se recuerdan.
 */
internal val FORTUNES =
    listOf(
        Fortune("Hack the planet!", "Hackers (1995)"),
        Fortune("Wake up, Neo...", "The Matrix (1999)"),
        Fortune("Follow the white rabbit.", "The Matrix (1999)"),
        Fortune("There is no spoon.", "The Matrix (1999)"),
        Fortune("Hello, friend.", "Mr. Robot (2015)"),
        Fortune("Shall we play a game?", "WarGames (1983)"),
        Fortune("It's a UNIX system! I know this!", "Jurassic Park (1993)"),
        Fortune("Mess with the best, die like the rest.", "Hackers (1995)"),
        Fortune("The quieter you become, the more you are able to hear.", "Kali Linux"),
        Fortune("sudo make me a sandwich.", "xkcd 149"),
        Fortune("I'm in.", "every hacker movie"),
        Fortune("Talk is cheap. Show me the code.", "Linus Torvalds"),
    )

/** La frase de la hora: cambia cada hora y es la misma en todos los widgets que la muestren. */
internal fun fortuneOf(epochHour: Long): Fortune = FORTUNES[Math.floorMod(epochHour, FORTUNES.size.toLong()).toInt()]

/** `~$ fortune`: la frase en texto y su fuente apagada, como `fortune -s`. */
@Composable
internal fun HackFortune(fortune: Fortune) {
    HackPrompt("fortune")
    WhText(fortune.quote, style = WhTheme.typography.bodySmall, color = WhTheme.colors.foreground)
    WhText("    -- ${fortune.source}", style = WhTheme.typography.caption, color = WhTheme.colors.darkForeground, maxLines = 1)
}
