package com.wh01s17.launcher.core.apps.model

/**
 * Identidad estable de una app lanzable: su actividad (`paquete/clase`) y el perfil de usuario.
 *
 * [userSerial] es el número de serie de `UserManager.getSerialNumberForUser`, que, a diferencia
 * del `UserHandle`, se mantiene entre reinicios y por eso se puede guardar en `launcher.json`.
 */
data class AppKey(
    val packageName: String,
    val className: String,
    val userSerial: Long,
) {
    /**
     * Forma de la config (plan.md §6.1): `com.termux/.HomeActivity`, como
     * `ComponentName.flattenToShortString()`. El perfil personal no lleva sufijo; los demás,
     * `@<serial>`.
     */
    fun toConfigString(personalSerial: Long): String {
        val shortClass = if (className.startsWith("$packageName.")) className.removePrefix(packageName) else className
        val component = "$packageName/$shortClass"
        return if (userSerial == personalSerial) component else "$component@$userSerial"
    }

    companion object {
        /**
         * Lee el formato de [toConfigString]; `null` si no es válido. Sin `@<serial>`, la app es
         * del perfil [personalSerial].
         */
        fun parse(
            value: String,
            personalSerial: Long,
        ): AppKey? {
            val at = value.lastIndexOf('@')
            val component = if (at == -1) value else value.substring(0, at)
            val serial = if (at == -1) personalSerial else value.substring(at + 1).toLongOrNull()
            val slash = component.indexOf('/')
            if (serial == null || slash <= 0 || slash == component.lastIndex) return null
            val packageName = component.substring(0, slash)
            val rawClass = component.substring(slash + 1)
            val className = if (rawClass.startsWith('.')) packageName + rawClass else rawClass
            return AppKey(packageName, className, serial)
        }
    }
}
