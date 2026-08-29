package com.catchuppos.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Detects the ZeroTier virtual network interface and provides the assigned IP.
 *
 * Strategy:
 * - The official ZeroTier Android app (com.zerotier.one) or any ZeroTier client creates
 *   a VPN/TUN interface (e.g., "tun0") with a 10.x.x.x address on the ZeroTier network.
 * - This manager scans for such interfaces and monitors connectivity changes.
 * - It does NOT run its own ZeroTier node — that's the official app's job.
 *
 * Why not embedded libzt:
 * - Android SELinux blocks /proc/net reads for untrusted apps
 * - The native node's Binder refresh (every 30s) closes/reopens sockets, each triggering
 *   SELinux denials, causing ZeroTier roots to consider the path dead
 * - This results in perpetual offline/reconnect cycles
 *
 * Usage:
 * ```kotlin
 * val manager = ZeroTierManager(context)
 * manager.startMonitoring()
 * // onIpAssigned callback fires when a ZeroTier interface is detected
 * ```
 */
class ZeroTierManager(private val context: Context) {

    companion object {
        private const val TAG = "ZeroTierManager"
        private const val STORAGE_DIR = "zerotier"

        // Known ZeroTier interface name prefixes and address ranges
        private val ZT_IFACE_PREFIXES = listOf("tun", "zt", "ztc", "utun", "zerotier")
        private val ZT_ADDR_PREFIXES = listOf("10.", "172.16.", "172.17.", "172.18.",
            "172.19.", "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
            "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31.")

        private const val SCAN_INTERVAL_MS = 5000L
    }

    private val settingsManager = ZeroTierSettingsManager(context)
    private val isRunning = AtomicBoolean(false)
    private var scanHandler: Handler? = null
    private var scanRunnable: Runnable? = null
    private var lastDetectedIp: String? = null

    // ── Status Callbacks ──────────────────────────────────────────

    /** Called when a ZeroTier interface is detected with an IP */
    var onIpAssigned: ((ip: String) -> Unit)? = null

    /** Called when the ZeroTier interface goes away */
    var onOnlineChanged: ((isOnline: Boolean) -> Unit)? = null

    /** Called when the node state changes */
    var onStateChanged: ((state: ZeroTierState) -> Unit)? = null

    /** Called when an error occurs */
    var onError: ((message: String) -> Unit)? = null

    // ── Public API ────────────────────────────────────────────────

    /**
     * Start monitoring for ZeroTier virtual interfaces.
     * Does NOT start any ZeroTier node — relies on the official ZeroTier app.
     */
    fun start(networkId: String = "", storagePath: String? = null) {
        if (isRunning.get()) {
            Log.w(TAG, "Already monitoring, ignoring start()")
            return
        }

        isRunning.set(true)
        onStateChanged?.invoke(ZeroTierState.STARTING)

        if (networkId.isNotEmpty()) {
            settingsManager.networkId = networkId
        }

        Log.i(TAG, "Starting ZeroTier interface monitor...")
        Log.i(TAG, "Network ID: ${settingsManager.networkId.ifEmpty { "(will auto-detect)" }}")

        // Start periodic scanning for ZeroTier interfaces
        scanHandler = Handler(Looper.getMainLooper())
        scanRunnable = object : Runnable {
            override fun run() {
                if (!isRunning.get()) return
                scanForZeroTierInterface()
                scanHandler?.postDelayed(this, SCAN_INTERVAL_MS)
            }
        }
        scanHandler?.post(scanRunnable!!)

        // Also register for network callbacks for instant detection
        registerNetworkCallback()
    }

    /**
     * Stop monitoring.
     */
    fun stop() {
        if (!isRunning.compareAndSet(true, false)) return

        Log.i(TAG, "Stopping ZeroTier interface monitor...")
        scanRunnable?.let { scanHandler?.removeCallbacks(it) }
        scanHandler = null
        scanRunnable = null
        unregisterNetworkCallback()

        lastDetectedIp = null
        settingsManager.clearConnectionState()
        onStateChanged?.invoke(ZeroTierState.STOPPED)
    }

    /**
     * Check if a ZeroTier interface is currently active.
     */
    fun isOnline(): Boolean {
        return lastDetectedIp != null
    }

    /**
     * Get the detected ZeroTier virtual IP.
     */
    fun getAssignedIp(): String? = lastDetectedIp

    fun getState(): ZeroTierState {
        return when {
            !isRunning.get() -> ZeroTierState.STOPPED
            lastDetectedIp != null -> ZeroTierState.CONNECTED
            else -> ZeroTierState.STARTING
        }
    }

    fun getNodeId(): String = settingsManager.nodeId

    fun getSettings(): ZeroTierSettingsManager = settingsManager

    fun joinNetwork(networkId: String) {
        settingsManager.networkId = networkId
    }

    fun leaveNetwork(networkId: String) {
        settingsManager.clearConnectionState()
    }

    fun isNetworkReady(networkId: String): Boolean = isOnline()

    // ── Internal Implementation ───────────────────────────────────

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private fun registerNetworkCallback() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    Log.d(TAG, "VPN network available, scanning for ZeroTier interface...")
                    scanForZeroTierInterface()
                }

                override fun onLost(network: android.net.Network) {
                    Log.d(TAG, "VPN network lost")
                    scanForZeroTierInterface()
                }
            }

            cm.registerNetworkCallback(request, networkCallback!!)
            Log.d(TAG, "Network callback registered for VPN transport")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            networkCallback?.let { cm.unregisterNetworkCallback(it) }
            networkCallback = null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister network callback: ${e.message}")
        }
    }

    /**
     * Scan network interfaces for ZeroTier virtual interfaces.
     * Looks for interfaces with ZeroTier-typical names or addresses.
     */
    private fun scanForZeroTierInterface() {
        try {
            val ztIp = findZeroTierInterface() ?: findZeroTierByAddress()

            if (ztIp != null && ztIp != lastDetectedIp) {
                Log.i(TAG, "ZeroTier interface detected: $ztIp")
                lastDetectedIp = ztIp
                settingsManager.assignedIp = ztIp
                onIpAssigned?.invoke(ztIp)
                onOnlineChanged?.invoke(true)
                onStateChanged?.invoke(ZeroTierState.CONNECTED)
            } else if (ztIp == null && lastDetectedIp != null) {
                Log.w(TAG, "ZeroTier interface lost")
                lastDetectedIp = null
                settingsManager.clearConnectionState()
                onOnlineChanged?.invoke(false)
                onStateChanged?.invoke(ZeroTierState.STARTING)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning for ZeroTier interface: ${e.message}")
        }
    }

    /**
     * Find a ZeroTier interface by known interface name patterns.
     */
    private fun findZeroTierInterface(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue

                val name = intf.name.lowercase()
                val isZtInterface = ZT_IFACE_PREFIXES.any { prefix -> name.startsWith(prefix) }

                if (isZtInterface) {
                    // Find the first IPv4 address
                    for (address in intf.inetAddresses) {
                        if (address is Inet4Address && !address.isLoopbackAddress) {
                            return address.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding ZeroTier interface: ${e.message}")
        }
        return null
    }

    /**
     * Find a ZeroTier interface by checking all addresses for ZeroTier-typical ranges.
     * ZeroTier typically assigns addresses in the 10.x.x.x range.
     * This is a heuristic — we also check that the interface name contains "zt" or "tun".
     */
    private fun findZeroTierByAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue

                val name = intf.name.lowercase()
                // Only consider tun/zt interfaces, not the main wlan/eth interfaces
                if (!name.contains("tun") && !name.contains("zt")) continue

                for (address in intf.inetAddresses) {
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val addrStr = address.hostAddress ?: continue
                        // ZeroTier commonly uses 10.x.x.x addresses
                        if (addrStr.startsWith("10.")) {
                            return addrStr
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding ZeroTier by address: ${e.message}")
        }
        return null
    }
}

/**
 * Represents the state of ZeroTier connectivity.
 */
enum class ZeroTierState {
    /** Not monitoring */
    STOPPED,
    /** Monitoring but no interface detected yet */
    STARTING,
    /** ZeroTier interface detected (matches name pattern) */
    ONLINE,
    /** Joining a network (unused in monitor mode) */
    JOINING,
    /** ZeroTier interface active with assigned IP */
    CONNECTED,
    /** An error occurred */
    ERROR
}
