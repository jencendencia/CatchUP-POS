package com.catchuppos.app.update

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val changelog: String,
    val downloadUrl: String
)

object UpdateChecker {

    private const val REPO_OWNER = "jencendencia"
    private const val REPO_NAME = "CatchUP-POS"
    private const val RELEASES_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "")
            val changelog = json.optString("body", "")

            // Parse version from tag (e.g. "v1.1.0" -> "1.1.0")
            val versionName = tagName.removePrefix("v").trim()
            if (versionName.isBlank()) return@withContext null

            // Convert version name to version code (e.g. "1.1.0" -> 10100)
            val versionCode = versionNameToCode(versionName)

            // Get current app version
            val currentVersionCode = getCurrentVersionCode(context)
            val currentVersionName = getCurrentVersionName(context)

            // Check if remote version is newer
            if (versionCode <= currentVersionCode) return@withContext null

            // Find APK download URL from release assets
            val assets = json.getJSONArray("assets")
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk")) {
                    downloadUrl = asset.optString("browser_download_url", null)
                    break
                }
            }

            if (downloadUrl.isNullOrBlank()) return@withContext null

            UpdateInfo(
                versionName = versionName,
                versionCode = versionCode,
                changelog = changelog,
                downloadUrl = downloadUrl
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun versionNameToCode(versionName: String): Int {
        // "1.10.0" -> 1_10_00 -> 11000
        val parts = versionName.split(".")
        var code = 0
        for (part in parts) {
            val num = part.toIntOrNull() ?: 0
            code = code * 100 + num
        }
        return code
    }

    private fun getCurrentVersionCode(context: Context): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }

    private fun getCurrentVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }
}
