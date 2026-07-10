package com.catchuppos.app.license

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object LicenseManager {

    private const val SERVER_URL = "https://dtr-license-server.jencendencia.workers.dev"
    private const val PREFS_NAME = "license_prefs"
    private const val KEY_LICENSE_KEY = "license_key"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_ACTIVATED = "activated"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        PREFS_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isActivated(context: Context): Boolean {
        val prefs = getEncryptedPrefs(context)
        val activated = prefs.getBoolean(KEY_ACTIVATED, false)
        if (!activated) return false

        // Verify device binding
        val storedDeviceId = prefs.getString(KEY_DEVICE_ID, null)
        val currentDeviceId = getDeviceId(context)
        return storedDeviceId == currentDeviceId
    }

    suspend fun activate(context: Context, key: String): ActivateResult = withContext(Dispatchers.IO) {
        val cleanKey = key.trim().uppercase()

        // Validate key format: DTR-XXXX-XXXX-XXXX-XXXX
        if (!isValidKeyFormat(cleanKey)) {
            return@withContext ActivateResult.INVALID_FORMAT
        }

        val deviceId = getDeviceId(context)

        try {
            val jsonBody = JSONObject().apply {
                put("key", cleanKey)
                put("machineId", deviceId)
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$SERVER_URL/validate")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext ActivateResult.ServerError()

            val result = JSONObject(body)
            val valid = result.optBoolean("valid", false)
            val message = result.optString("message", "Unknown error")

            if (valid) {
                // Store activation locally
                val prefs = getEncryptedPrefs(context)
                prefs.edit().apply {
                    putBoolean(KEY_ACTIVATED, true)
                    putString(KEY_LICENSE_KEY, cleanKey)
                    putString(KEY_DEVICE_ID, deviceId)
                    apply()
                }
                ActivateResult.SUCCESS
            } else {
                when {
                    message.contains("already activated", ignoreCase = true) -> ActivateResult.KEY_USED_ON_ANOTHER_DEVICE
                    message.contains("not found", ignoreCase = true) -> ActivateResult.INVALID_KEY
                    else -> ActivateResult.ServerError(message)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ActivateResult.NETWORK_ERROR
        }
    }

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    private fun isValidKeyFormat(key: String): Boolean {
        // DTR-XXXX-XXXX-XXXX-XXXX (4 groups of 4 alphanumeric chars)
        return key.matches(Regex("^DTR-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$"))
    }
}

sealed class ActivateResult {
    data object SUCCESS : ActivateResult()
    data object INVALID_FORMAT : ActivateResult()
    data object INVALID_KEY : ActivateResult()
    data object KEY_USED_ON_ANOTHER_DEVICE : ActivateResult()
    data object NETWORK_ERROR : ActivateResult()
    data class ServerError(val serverMessage: String = "Server error") : ActivateResult()

    val message: String
        get() = when (this) {
            is SUCCESS -> "Activation successful!"
            is INVALID_FORMAT -> "Invalid key format. Expected: DTR-XXXX-XXXX-XXXX-XXXX"
            is INVALID_KEY -> "Invalid license key."
            is KEY_USED_ON_ANOTHER_DEVICE -> "This key is already registered to another device."
            is NETWORK_ERROR -> "Network error. Please check your internet connection."
            is ServerError -> this.serverMessage
        }
}
