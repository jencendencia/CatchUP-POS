package com.catchuppos.app.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages ZeroTier configuration settings.
 * Settings are persisted in SharedPreferences and survive app restarts.
 */
class ZeroTierSettingsManager(context: Context) {

    companion object {
        private const val TAG = "ZeroTierSettings"
        private const val PREFS_NAME = "zerotier_prefs"
        private const val KEY_NETWORK_ID = "zt_network_id"
        private const val KEY_API_KEY = "zt_api_key"
        private const val KEY_ENABLED = "zt_enabled"
        private const val KEY_NODE_ID = "zt_node_id"
        private const val KEY_ASSIGNED_IP = "zt_assigned_ip"
        private const val KEY_AUTO_CONNECT = "zt_auto_connect"
        private const val KEY_NODE_SECRET = "zt_node_secret"
        private const val KEY_STORAGE_PATH = "zt_storage_path"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * ZeroTier network ID (16 hex characters, e.g. "1234567890abcdef")
     */
    var networkId: String
        get() = prefs.getString(KEY_NETWORK_ID, "") ?: ""
        set(value) {
            // Validate: must be 16 hex chars or empty
            val cleaned = value.trim().lowercase()
            if (cleaned.isEmpty() || cleaned.matches(Regex("^[0-9a-f]{16}$"))) {
                prefs.edit().putString(KEY_NETWORK_ID, cleaned).apply()
            }
        }

    /**
     * ZeroTier Central API key for auto-authorization
     */
    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    /**
     * Whether ZeroTier should auto-connect on app startup
     */
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /**
     * Auto-connect on app startup
     */
    var autoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()

    /**
     * The device's ZeroTier node ID (assigned by ZeroTier on first start)
     * This is the 10-hex-digit public identity of this node
     */
    var nodeId: String
        get() = prefs.getString(KEY_NODE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NODE_ID, value).apply()

    /**
     * The virtual IP address assigned to this device on the ZeroTier network
     */
    var assignedIp: String
        get() = prefs.getString(KEY_ASSIGNED_IP, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ASSIGNED_IP, value).apply()

    /**
     * Base64-encoded node secret (private key) for identity persistence
     */
    var nodeSecret: String
        get() = prefs.getString(KEY_NODE_SECRET, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NODE_SECRET, value).apply()

    /**
     * Path to ZeroTier storage directory on the filesystem
     */
    var storagePath: String
        get() = prefs.getString(KEY_STORAGE_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_STORAGE_PATH, value).apply()

    /**
     * Check if network ID is configured and valid
     */
    fun isNetworkConfigured(): Boolean {
        return networkId.matches(Regex("^[0-9a-f]{16}$"))
    }

    /**
     * Check if this device has a valid node identity
     */
    fun hasNodeIdentity(): Boolean {
        return nodeId.isNotEmpty() && nodeSecret.isNotEmpty()
    }

    /**
     * Get the network ID as a Long (for libzt API which takes hex as Long)
     */
    fun getNetworkIdAsLong(): Long {
        return try {
            networkId.toLong(16)
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Reset all settings to defaults
     */
    fun reset() {
        prefs.edit().clear().apply()
    }

    /**
     * Clear connection-specific data but keep user settings
     */
    fun clearConnectionState() {
        prefs.edit()
            .remove(KEY_ASSIGNED_IP)
            .apply()
    }
}
