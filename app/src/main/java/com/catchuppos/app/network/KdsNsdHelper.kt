package com.catchuppos.app.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import android.net.wifi.WifiManager

/**
 * Registers the KDS WebSocket server as an NSD (Network Service Discovery) service
 * on the local network so the companion app can find it automatically.
 *
 * Service type: _catchuppos._tcp
 * Service name: "CatchUpPOS"  (Android auto-appends a suffix if duplicate)
 */
class KdsNsdHelper(private val context: Context) {

    companion object {
        private const val TAG = "KdsNsdHelper"
        const val SERVICE_TYPE = "_catchuppos._tcp"
        const val SERVICE_NAME = "CatchUpPOS"
    }

    private val nsdManager: NsdManager? by lazy {
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    }

    private var multicastLock: WifiManager.MulticastLock? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRegistered = false

    /**
     * Register the POS KDS server as an NSD service.
     * Should be called when the WebSocket server starts.
     */
    fun registerService(port: Int) {
        if (isRegistered) {
            Log.d(TAG, "NSD service already registered, skipping")
            return
        }

        val manager = nsdManager ?: run {
            Log.e(TAG, "NsdManager not available on this device")
            return
        }

        // Acquire multicast lock so NSD broadcasts are received
        acquireMulticastLock()

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            this.port = port
        }

        try {
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(registeredService: NsdServiceInfo) {
                    isRegistered = true
                    Log.d(TAG, "NSD service registered: ${registeredService.serviceName} on port ${registeredService.port}")
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "NSD registration failed: errorCode=$errorCode")
                    registrationListener = null
                    releaseMulticastLock()
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    isRegistered = false
                    registrationListener = null
                    Log.d(TAG, "NSD service unregistered: ${serviceInfo.serviceName}")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "NSD unregistration failed: errorCode=$errorCode")
                    registrationListener = null
                }
            }

            registrationListener = listener
            manager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                listener,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register NSD service: ${e.message}", e)
            registrationListener = null
            releaseMulticastLock()
        }
    }

    /**
     * Unregister the NSD service. Must pass the exact same listener that was used during registration.
     * Should be called when the WebSocket server stops.
     */
    fun unregisterService() {
        val listener = registrationListener ?: return

        try {
            nsdManager?.unregisterService(listener)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering NSD service: ${e.message}", e)
            registrationListener = null
            isRegistered = false
            releaseMulticastLock()
        }
    }

    /**
     * Acquire Wi-Fi multicast lock so the device can receive NSD discovery packets.
     */
    private fun acquireMulticastLock() {
        try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock(TAG)
            multicastLock?.acquire()
            Log.d(TAG, "Multicast lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire multicast lock: ${e.message}", e)
        }
    }

    /**
     * Release the Wi-Fi multicast lock.
     */
    private fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Multicast lock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release multicast lock: ${e.message}", e)
        }
        multicastLock = null
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        unregisterService()
    }
}
