package com.wh01s17.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wh01s17.launcher.core.config.BindingEdit
import com.wh01s17.launcher.core.config.EditResult
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.oem.xiaomi.SystemScreens
import com.wh01s17.launcher.feature.settings.R
import com.wh01s17.launcher.feature.settings.compat.CompatibilityActions
import com.wh01s17.launcher.feature.settings.compat.CompatibilityScreen
import com.wh01s17.launcher.feature.settings.compat.CompatibilityViewModel
import com.wh01s17.launcher.feature.settings.shortcuts.ShortcutEditor
import com.wh01s17.launcher.feature.settings.shortcuts.ShortcutEditorViewModel
import com.wh01s17.launcher.feature.settings.shortcuts.ShortcutTable
import com.wh01s17.launcher.feature.settings.shortcuts.ShortcutsScreen
import com.wh01s17.launcher.feature.settings.shortcuts.rowFor
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

/**
 * Ajustes del launcher: Compatibilidad (plan.md §7, Hito 2), que se abre desde la barra de edición
 * o el menú, y Atajos (`Super + K`, Hito 6), con [shortcuts].
 */
class SettingsActivity : ComponentActivity() {
    private val container get() = (application as LauncherApplication).container

    private val compatibility: CompatibilityViewModel by viewModels {
        viewModelFactory { initializer { CompatibilityViewModel(container.compatibilityGateway) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        val actions =
            CompatibilityActions(
                openAccessibility = { SystemScreens.accessibility(this) },
                openAppDetails = { SystemScreens.appDetails(this) },
                openAutostart = { SystemScreens.autostart(this) },
                openDeveloperOptions = { SystemScreens.developerOptions(this) },
            )
        if (intent.getStringExtra(EXTRA_PAGE) == PAGE_SHORTCUTS) {
            setContent { ShortcutsPage() }
            return
        }
        setContent {
            WhTheme {
                val state by compatibility.state.collectAsStateWithLifecycle()
                CompatibilityScreen(
                    state = state,
                    onModeChange = compatibility::setModeEnabled,
                    onEdgesChange = compatibility::setEdges,
                    actions = actions,
                    modifier = Modifier.fillMaxSize().background(WhTheme.colors.background),
                )
            }
        }
    }

    private val editor: ShortcutEditorViewModel by viewModels()

    private val shortcutTable: ShortcutTable?
        get() =
            container.configRepository.state.value.config
                ?.let { ShortcutTable.of(it, container.superKeyDelivered) }

    /**
     * Con el editor abierto, todas las teclas van a él (capturar, o Escape para cerrar). AndroidX core
     * marca su override como interno; este es el callback público de Activity (como en MainActivity).
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (editor.state.value.trigger == null) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN) editor.onKey(event.keyCode, event.metaState, shortcutTable)
        return true
    }

    /** `Super + K` y su editor: tocar una fila captura una combinación nueva (plan.md §7 Hito 6). */
    @Composable
    private fun ShortcutsPage() {
        WhTheme {
            val config by container.configRepository.state.collectAsStateWithLifecycle()
            val table = config.config?.let { ShortcutTable.of(it, container.superKeyDelivered) }
            Box(Modifier.fillMaxSize().background(WhTheme.colors.background)) {
                val editorState by editor.state.collectAsStateWithLifecycle()
                ShortcutsScreen(table = table, onRowClick = editor::open, header = { EditHeader() })
                val row = editorState.trigger?.let { table?.rowFor(it) }
                if (row != null && table != null) {
                    BackHandler(onBack = editor::close)
                    ShortcutEditor(row, table, editorState.captured, onEdit = { edit(it, editor::close) }, onDismiss = editor::close)
                }
            }
        }
    }

    @Composable
    private fun EditHeader() {
        Column(verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            WhText(stringResource(R.string.shortcuts_edit_hint), color = WhTheme.colors.darkForeground)
            WhButton(onClick = { edit(BindingEdit.ResetAll) {} }) { WhText(stringResource(R.string.shortcuts_reset)) }
        }
    }

    /** Guarda en `launcher.json`; si no se puede (archivo con errores), lo explica y no escribe nada. */
    private fun edit(
        change: BindingEdit,
        done: () -> Unit,
    ) {
        lifecycleScope.launch {
            when (val result = container.configRepository.editBindings(change)) {
                EditResult.Saved -> done()
                is EditResult.Refused -> {
                    val reason =
                        result.errors
                            .firstOrNull()
                            ?.toString()
                            .orEmpty()
                    Toast.makeText(this@SettingsActivity, getString(R.string.shortcuts_edit_failed, reason), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Al volver de Ajustes del sistema (accesibilidad, batería…) el estado puede haber cambiado. */
    override fun onResume() {
        super.onResume()
        compatibility.refresh()
    }

    companion object {
        private const val EXTRA_PAGE = "page"
        private const val PAGE_SHORTCUTS = "shortcuts"

        /** La tabla de atajos (`Super + K`, plan.md §6.4). */
        fun shortcuts(context: Context): Intent = Intent(context, SettingsActivity::class.java).putExtra(EXTRA_PAGE, PAGE_SHORTCUTS)
    }
}
