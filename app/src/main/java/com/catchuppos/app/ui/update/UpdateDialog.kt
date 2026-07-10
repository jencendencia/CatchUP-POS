package com.catchuppos.app.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.catchuppos.app.theme.*
import com.catchuppos.app.update.UpdateChecker
import com.catchuppos.app.update.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

enum class UpdateState {
    AVAILABLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    ERROR
}

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(UpdateState.AVAILABLE) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun downloadAndInstall() {
        state = UpdateState.DOWNLOADING
        scope.launch {
            try {
                val apkFile = withContext(Dispatchers.IO) {
                    downloadApk(context, updateInfo.downloadUrl) { progress ->
                        downloadProgress = progress
                    }
                }

                if (apkFile != null && apkFile.exists()) {
                    state = UpdateState.READY_TO_INSTALL
                    installApk(context, apkFile)
                } else {
                    errorMessage = "Download failed"
                    state = UpdateState.ERROR
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Download failed"
                state = UpdateState.ERROR
            }
        }
    }

    Dialog(
        onDismissRequest = { if (state != UpdateState.DOWNLOADING) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 380.dp, max = 460.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (state) {
                                UpdateState.AVAILABLE -> "Update Available"
                                UpdateState.DOWNLOADING -> "Downloading Update"
                                UpdateState.READY_TO_INSTALL -> "Ready to Install"
                                UpdateState.ERROR -> "Update Failed"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version ${updateInfo.versionName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OrangeAccent,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (state != UpdateState.DOWNLOADING) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                when (state) {
                    UpdateState.AVAILABLE -> {
                        // Changelog
                        if (updateInfo.changelog.isNotBlank()) {
                            Text(
                                text = "WHAT'S NEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkCard
                            ) {
                                Text(
                                    text = updateInfo.changelog,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 150.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Update Now Button
                        Button(
                            onClick = { downloadAndInstall() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangeAccent,
                                contentColor = TextWhite
                            )
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Update Now", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Later Button
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Later", color = TextMuted)
                        }
                    }

                    UpdateState.DOWNLOADING -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            CircularProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier.size(64.dp),
                                color = OrangeAccent,
                                trackColor = DarkCard,
                                strokeWidth = 6.dp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Downloading... $downloadProgress%",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextWhite,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Please do not close the app",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    UpdateState.READY_TO_INSTALL -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StatusGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Update downloaded successfully",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextWhite,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Tap Install to complete the update",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val apkFile = File(context.cacheDir, "updates/app-update.apk")
                                    if (apkFile.exists()) installApk(context, apkFile)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusGreen,
                                    contentColor = TextWhite
                                )
                            ) {
                                Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Install", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Later", color = TextMuted)
                            }
                        }
                    }

                    UpdateState.ERROR -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MutedRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MutedRed,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = errorMessage ?: "Something went wrong",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkCard,
                                    contentColor = TextWhite
                                )
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun downloadApk(
    context: Context,
    url: String,
    onProgress: (Int) -> Unit
): File? = withContext(Dispatchers.IO) {
    try {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) updatesDir.mkdirs()

        val apkFile = File(updatesDir, "app-update.apk")

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) return@withContext null

        val body = response.body ?: return@withContext null
        val contentLength = body.contentLength()
        var bytesRead = 0L

        body.byteStream().use { input ->
            apkFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesRead += read
                    if (contentLength > 0) {
                        withContext(Dispatchers.Main) {
                            onProgress((bytesRead * 100 / contentLength).toInt())
                        }
                    }
                }
            }
        }

        apkFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun installApk(context: Context, apkFile: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    context.startActivity(intent)
}
