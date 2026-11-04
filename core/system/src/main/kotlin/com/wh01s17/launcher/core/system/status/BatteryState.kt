package com.wh01s17.launcher.core.system.status

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.flow.Flow

/** Batería y ahorro de energía (módulo `wh01s17.power`). */
data class BatteryState(
    /** 0–100; `null` si el dispositivo no tiene batería. */
    val percent: Int?,
    val charging: Boolean,
    val full: Boolean,
    val powerSave: Boolean,
) {
    private val level get() = percent ?: FULL_PERCENT

    /** 20 % o menos y sin cargador: avisa en amarillo (plan.md §5.5, `classColors`). */
    val low: Boolean get() = !charging && level <= LOW_PERCENT

    /** 10 % o menos y sin cargador: avisa en rojo. */
    val critical: Boolean get() = !charging && level <= CRITICAL_PERCENT

    companion object {
        val Unknown = BatteryState(percent = null, charging = false, full = false, powerSave = false)
        const val LOW_PERCENT = 20
        const val CRITICAL_PERCENT = 10
        private const val FULL_PERCENT = 100

        /** Pasa los extras de `ACTION_BATTERY_CHANGED` a un estado; [extras] `null` = sin batería. */
        fun fromExtras(
            level: Int,
            scale: Int,
            status: Int,
            plugged: Int,
            present: Boolean,
            powerSave: Boolean,
        ): BatteryState {
            val percent = if (present && level >= 0 && scale > 0) (level * FULL_PERCENT / scale).coerceIn(0, FULL_PERCENT) else null
            val full = status == BatteryManager.BATTERY_STATUS_FULL
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || (full && plugged != 0)
            return BatteryState(percent, charging, full, powerSave)
        }
    }
}

/** [BatteryState] con `ACTION_BATTERY_CHANGED` (sticky, sin permisos) y el modo de ahorro. */
class BatterySource(
    private val context: Context,
) : StatusSource<BatteryState> {
    private val powerManager = context.getSystemService(PowerManager::class.java)

    override fun observe(): Flow<BatteryState> =
        context.broadcastState(intentFilterOf(Intent.ACTION_BATTERY_CHANGED, PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)) { intent ->
            // `ACTION_POWER_SAVE_MODE_CHANGED` no trae los extras de batería: se piden al sticky.
            val battery = intent?.takeIf { it.hasExtra(BatteryManager.EXTRA_LEVEL) } ?: currentBatteryIntent()
            BatteryState.fromExtras(
                level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1,
                scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1,
                status =
                    battery?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                        ?: BatteryManager.BATTERY_STATUS_UNKNOWN,
                plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0,
                present = battery?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true) ?: false,
                powerSave = powerManager?.isPowerSaveMode == true,
            )
        }

    /** El `Intent` sticky actual sin registrar un receptor. */
    private fun currentBatteryIntent(): Intent? = context.registerReceiver(null, intentFilterOf(Intent.ACTION_BATTERY_CHANGED))
}
