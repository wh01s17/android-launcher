package com.wh01s17.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wh01s17.launcher.core.config.EditResult
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.settings.R
import com.wh01s17.launcher.feature.settings.style.StyleActions
import com.wh01s17.launcher.feature.settings.style.StyleScreen
import com.wh01s17.launcher.feature.settings.style.StyleViewModel
import com.wh01s17.launcher.ui.HomeWallpaper
import com.wh01s17.launcher.ui.LauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException
import android.graphics.Color as AndroidColor

/** Style and the SAF launchers live here; feature/settings only renders and edits config. */
class StyleActivity : ComponentActivity() {
    private val container get() = (application as LauncherApplication).container
    private val model: StyleViewModel by viewModels {
        viewModelFactory { initializer { StyleViewModel(container.configRepository) } }
    }
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                perform {
                    container.wallpapers.importImage(uri)
                    val patch = JsonObject(mapOf("appearance" to JsonObject(mapOf("wallpaper" to JsonPrimitive("imported")))))
                    report(container.configRepository.editValues(patch))
                }
            }
        }
    private val importer =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                perform {
                    val bytes =
                        contentResolver.openInputStream(uri)?.use { readConfigBytes(it) }
                            ?: throw IOException("No se pudo abrir el archivo")
                    if (bytes.size > MAX_CONFIG_BYTES) throw IOException("El archivo supera 1 MiB")
                    report(container.configRepository.importDocument(bytes.toString(Charsets.UTF_8)))
                }
            }
        }
    private val exporter =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                perform {
                    val text = container.configRepository.exportDocument()
                    contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                        ?: throw IOException("No se pudo escribir el archivo")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(AndroidColor.TRANSPARENT), SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        setContent {
            LauncherTheme(container) {
                val state by model.config.collectAsStateWithLifecycle()
                val error by model.errors.collectAsStateWithLifecycle()
                var wallpapers by rememberSaveable { mutableStateOf(intent.getBooleanExtra(WALLPAPERS, false)) }
                BackHandler(wallpapers && !intent.getBooleanExtra(WALLPAPERS, false)) { wallpapers = false }
                val actions =
                    StyleActions(
                        wallpapers = { wallpapers = true },
                        importImage = { imagePicker.launch(arrayOf("image/*")) },
                        systemWallpaper = {
                            perform {
                                container.wallpapers.setSystem(
                                    model.config.value.config
                                        ?.appearance
                                        ?.wallpaper ?: "none",
                                )
                            }
                        },
                        importConfig = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        exportConfig = { exporter.launch("launcher.json") },
                        preview = { startActivity(Intent(this, ScreensaverActivity::class.java)) },
                        dreamSettings = { openDreamSettings() },
                    )
                state.config?.let {
                    StyleScreen(
                        it,
                        error,
                        wallpapers,
                        model::change,
                        actions,
                        Modifier.background(WhTheme.colors.background),
                        wallpaperPreview = {
                            if (it.appearance.wallpaper != "none") {
                                Box(Modifier.fillMaxWidth().height(WhTheme.spacing.menuWidth / 2)) {
                                    HomeWallpaper(it.appearance.wallpaper, container.wallpapers)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    private fun readConfigBytes(input: java.io.InputStream): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var count = input.read(buffer)
        while (count >= 0) {
            if (output.size() + count > MAX_CONFIG_BYTES) throw IOException("El archivo supera 1 MiB")
            output.write(buffer, 0, count)
            count = input.read(buffer)
        }
        return output.toByteArray()
    }

    private fun openDreamSettings() {
        try {
            startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
        } catch (
            _: android.content.ActivityNotFoundException,
        ) {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
    }

    private fun report(result: EditResult) {
        if (result is EditResult.Refused) throw IOException(result.errors.joinToString("\n"))
    }

    private fun perform(block: suspend () -> Unit) {
        lifecycleScope.launch {
            val error =
                withContext(Dispatchers.IO) {
                    try {
                        block()
                        null
                    } catch (error: IOException) {
                        error.message ?: error.javaClass.simpleName
                    } catch (error: SecurityException) {
                        error.message ?: error.javaClass.simpleName
                    }
                }
            val text = if (error == null) getString(R.string.style_saved) else getString(R.string.style_failure, error)
            Toast.makeText(this@StyleActivity, text, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val MAX_CONFIG_BYTES = 1_048_576
        private const val WALLPAPERS = "wallpapers"

        fun intent(
            context: Context,
            wallpapers: Boolean = false,
        ): Intent = Intent(context, StyleActivity::class.java).putExtra(WALLPAPERS, wallpapers)
    }
}
