package com.wh01s17.launcher.core.system.oem.xiaomi

/** Tabla de `Settings` donde vive un ajuste. */
enum class SettingsTable { Global, Secure }

/** Ajustes de HyperOS/MIUI que toca la capa 2 (plan.md §5.9, `docs/xiaomi.md`). */
enum class SystemSetting(
    val table: SettingsTable,
    val key: String,
) {
    /** `1` = gestos a pantalla completa forzados: oculta los 3 botones. */
    ForceFsgNavBar(SettingsTable.Global, "force_fsg_nav_bar"),

    /** `1` = barrita de gestos oculta. */
    HideGestureLine(SettingsTable.Global, "hide_gesture_line"),

    /** `0` = 3 botones, `2` = gestos. HyperOS lo cambia solo; sólo se lee. */
    NavigationMode(SettingsTable.Secure, "navigation_mode"),
}

/** Lectura y escritura de ajustes del sistema. Escribir exige `WRITE_SECURE_SETTINGS` (`pm grant`). */
interface SecureSettingsGateway {
    fun canWrite(): Boolean

    fun read(setting: SystemSetting): Int?

    /** `false` si no hay permiso o el sistema lo rechaza. */
    fun write(
        setting: SystemSetting,
        value: Int,
    ): Boolean
}

/** Preferencias del modo *gestos wh01s17*: las escribe sólo el proceso del launcher. */
data class GestureModePrefs(
    val enabled: Boolean = false,
    /** Ocultar también la barrita de gestos de HyperOS, que con un launcher ajeno no funciona. */
    val hideGestureLine: Boolean = true,
    /** Valor de `hide_gesture_line` antes de activar el modo, para devolverlo al desactivarlo. */
    val previousGestureLine: Int? = null,
)

/** Qué hizo (o por qué no) la capa 2 en su última decisión. Lo muestra el asistente. */
enum class GestureModeStatus {
    /** El modo está desactivado: no se toca nada. */
    Off,

    /** No somos el launcher predeterminado: con el oficial, sus gestos funcionan solos. */
    NotHome,

    /** Falta `WRITE_SECURE_SETTINGS` o el sistema rechazó la escritura. */
    MissingPermission,

    /** Botones ocultos con la capa 3 viva: el modo funciona. */
    ButtonsHidden,

    /** HyperOS deshizo el ajuste demasiadas veces: se deja de insistir (los botones vuelven). */
    GaveUp,

    /** La capa 3 se fue y se devolvieron los botones. */
    Restored,
}

/**
 * Decisiones de la capa 2 en modo *gestos wh01s17* (plan.md §5.9). La regla de oro: **los
 * botones sólo se ocultan mientras la capa 3 está viva**, y sólo si somos el launcher
 * predeterminado (con el oficial, `force_fsg_nav_bar = 1` es su estado normal y no hay que tocarlo).
 *
 * La usan los dos procesos: el de navegación (`:nav`) llama a [onNavigationReady],
 * [onForceFsgChanged] y [onNavigationStopping]; el del launcher, a [ensureSafe], [enable] y
 * [disable]. Es pura: el sistema llega por [settings] e [isDefaultHome], el tiempo por [clock].
 */
class GestureModeController(
    private val settings: SecureSettingsGateway,
    private val isDefaultHome: () -> Boolean,
    clock: () -> Long,
    private val maxRewritesPerMinute: Int = MAX_REWRITES_PER_MINUTE,
) {
    private val rewrites = RateLimiter(maxRewritesPerMinute, WINDOW_MILLIS, clock)
    private var gaveUp = false

    /** La capa 3 acaba de quedar lista (franjas puestas): ya se pueden ocultar los botones. */
    fun onNavigationReady(prefs: GestureModePrefs): GestureModeStatus {
        rewrites.reset()
        gaveUp = false
        return hideButtons(prefs)
    }

    /**
     * `force_fsg_nav_bar` cambió con la capa 3 viva. HyperOS lo pone a 0 al cambiar de launcher
     * (y hay reportes de que a diario): se vuelve a escribir, como mucho [maxRewritesPerMinute]
     * veces por minuto. Si se supera, se deja de pelear con el sistema.
     */
    fun onForceFsgChanged(prefs: GestureModePrefs): GestureModeStatus =
        when {
            !prefs.enabled -> GestureModeStatus.Off
            !isDefaultHome() -> GestureModeStatus.NotHome
            settings.read(SystemSetting.ForceFsgNavBar) == 1 -> GestureModeStatus.ButtonsHidden
            gaveUp || !rewrites.tryAcquire() -> GestureModeStatus.GaveUp.also { gaveUp = true }
            else -> hideButtons(prefs)
        }

    /** La capa 3 se va (servicio desactivado o destruido): los botones vuelven antes de terminar. */
    fun onNavigationStopping(prefs: GestureModePrefs): GestureModeStatus = restoreButtons(prefs) ?: GestureModeStatus.Off

    /**
     * Vigilancia desde el launcher: si la capa 3 no está viva y los botones están ocultos por el
     * modo, se devuelven. `true` si hubo que hacerlo (para avisar al usuario).
     */
    fun ensureSafe(
        prefs: GestureModePrefs,
        navigationAlive: Boolean,
    ): Boolean = !navigationAlive && restoreButtons(prefs) == GestureModeStatus.Restored

    /** El usuario activa el modo: se guarda cómo estaba la barrita para devolverla luego. */
    fun enable(prefs: GestureModePrefs): GestureModePrefs =
        prefs.copy(enabled = true, previousGestureLine = prefs.previousGestureLine ?: settings.read(SystemSetting.HideGestureLine))

    /**
     * El usuario desactiva el modo. Devuelve las preferencias a guardar **antes** de escribir en
     * el sistema, para que el proceso de navegación no vuelva a ocultar los botones al verlo.
     */
    fun disable(prefs: GestureModePrefs): GestureModePrefs = prefs.copy(enabled = false, previousGestureLine = null)

    /** Tras guardar [disable]: botones de vuelta y la barrita como estaba. */
    fun restoreAfterDisable(previous: GestureModePrefs) {
        // Con otro launcher por defecto, `force_fsg_nav_bar` es suyo: no se toca.
        if (isDefaultHome()) settings.write(SystemSetting.ForceFsgNavBar, 0)
        if (previous.hideGestureLine) previous.previousGestureLine?.let { settings.write(SystemSetting.HideGestureLine, it) }
    }

    private fun hideButtons(prefs: GestureModePrefs): GestureModeStatus =
        when {
            !prefs.enabled -> GestureModeStatus.Off
            !isDefaultHome() -> GestureModeStatus.NotHome
            !settings.canWrite() -> GestureModeStatus.MissingPermission
            else -> {
                if (prefs.hideGestureLine) settings.write(SystemSetting.HideGestureLine, 1)
                if (settings.write(
                        SystemSetting.ForceFsgNavBar,
                        1,
                    )
                ) {
                    GestureModeStatus.ButtonsHidden
                } else {
                    GestureModeStatus.MissingPermission
                }
            }
        }

    /** `Restored` si había botones ocultos y se devolvieron; `null` si no había nada que hacer. */
    private fun restoreButtons(prefs: GestureModePrefs): GestureModeStatus? {
        val hidden = prefs.enabled && isDefaultHome() && settings.read(SystemSetting.ForceFsgNavBar) == 1
        return if (hidden && settings.write(SystemSetting.ForceFsgNavBar, 0)) GestureModeStatus.Restored else null
    }

    companion object {
        /** plan.md §5.9: "como máximo 5 reescrituras por minuto". */
        const val MAX_REWRITES_PER_MINUTE = 5
        private const val WINDOW_MILLIS = 60_000L
    }
}
