package com.catchuppos.app.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages KDS (Kitchen Display System) server settings.
 * Settings are persisted in SharedPreferences and survive app restarts.
 */
class KdsSettingsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "kds_server_prefs"
        private const val KEY_ENABLED = "kds_server_enabled"
        private const val KEY_PORT = "kds_server_port"
        private const val DEFAULT_PORT = 8080
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Whether the KDS server is currently enabled (should be running)
     */
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /**
     * The port the KDS WebSocket server listens on
     */
    var port: Int
        get() = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        set(value) {
            // Validate port range (1024-65535 for non-root)
            val validatedPort = when {
                value < 1024 -> DEFAULT_PORT
                value > 65535 -> DEFAULT_PORT
                else -> value
            }
            prefs.edit().putInt(KEY_PORT, validatedPort).apply()
        }

    /**
     * Get the local device's IP address on the WiFi network.
     * Falls back gracefully if WiFi is unavailable or permission is missing.
     */
    fun getLocalIpAddress(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val wifiInfo = wifiManager?.connectionInfo
            val ipInt = wifiInfo?.ipAddress ?: 0
            String.format(
                "%d.%d.%d.%d",
                ipInt and 0xFF,
                (ipInt shr 8) and 0xFF,
                (ipInt shr 16) and 0xFF,
                (ipInt shr 24) and 0xFF
            )
        } catch (e: Exception) {
            android.util.Log.w("KdsSettings", "Failed to get local IP: ${e.message}")
            "127.0.0.1"
        }
    }

    /**
     * Reset settings to defaults
     */
    fun reset() {
        prefs.edit()
            .putBoolean(KEY_ENABLED, false)
            .putInt(KEY_PORT, DEFAULT_PORT)
            .apply()
    }
}
