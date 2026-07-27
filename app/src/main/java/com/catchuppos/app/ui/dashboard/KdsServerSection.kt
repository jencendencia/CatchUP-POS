package com.catchuppos.app.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catchuppos.app.CatchUpApp
import com.catchuppos.app.theme.*

/**
 * KDS (Kitchen Display System) server management section for the POS Settings screen.
 *
 * Allows the user to:
 * - Start/stop the KDS WebSocket server
 * - See the local IP address and port
 * - Monitor connected KDS client count
 * - Configure the server port
 */
@Composable
fun KdsServerSection(
    app: CatchUpApp,
    isAdmin: Boolean
) {
    val context = LocalContext.current
    var showPortDialog by remember { mutableStateOf(false) }
    var portInput by remember { mutableStateOf(app.kdsSettingsManager.port.toString()) }

    // Derived state from the app's server instance
    val isServerRunning = app.kdsServer != null
    val connectedClients = app.kdsServer?.clientCount ?: 0
    val localIp = app.kdsSettingsManager.getLocalIpAddress(context)
    val currentPort = app.kdsSettingsManager.port

    // Animate status indicator color
    val statusColor by animateColorAsState(
        targetValue = if (isServerRunning) StatusGreen else MutedRed,
        label = "statusColor"
    )

    SectionHeader(title = "KITCHEN DISPLAY SYSTEM")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Server Status & Toggle ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isServerRunning) "Server Running" else "Server Stopped",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isServerRunning) "$connectedClients client(s) connected"
                                   else "KDS companion cannot connect",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                // On/Off toggle
                Switch(
                    checked = isServerRunning,
                    onCheckedChange = { shouldRun ->
                        if (shouldRun) {
                            app.startKdsServer(currentPort)
                        } else {
                            app.stopKdsServer()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = OrangeAccent,
                        checkedThumbColor = TextWhite,
                        uncheckedTrackColor = DarkBorder,
                        uncheckedThumbColor = TextGray
                    )
                )
            }

            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

            // ── Network Details ──
            NetworkDetailRow(
                icon = Icons.Default.Computer,
                label = "IP Address",
                value = localIp
            )

            NetworkDetailRow(
                icon = Icons.Default.Tag,
                label = "Port",
                value = currentPort.toString(),
                trailingContent = {
                    if (isAdmin) {
                        TextButton(onClick = {
                            portInput = currentPort.toString()
                            showPortDialog = true
                        }) {
                            Text(
                                text = "Edit",
                                color = OrangeAccent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            )

            if (isServerRunning && connectedClients > 0) {
                NetworkDetailRow(
                    icon = Icons.Default.Devices,
                    label = "Connected Clients",
                    value = connectedClients.toString(),
                    valueColor = StatusGreen
                )
            }

            // ── Connection Info ──
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OrangeAccent.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = OrangeAccent.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Connection Details",
                            style = MaterialTheme.typography.labelMedium,
                            color = OrangeAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect your KDS companion app using:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkBackground
                        ) {
                            Text(
                                text = "ws://$localIp:$currentPort/ws/kds",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = if (isServerRunning) StatusGreen else TextGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Both devices must be on the same Wi-Fi network.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
            }
        }
    }

    // ── Port Configuration Dialog ──
    if (showPortDialog) {
        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text(
                    text = "Configure Port",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a port number between 1024 and 65535:",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { newVal ->
                            if (newVal.all { it.isDigit() } && newVal.length <= 5) {
                                portInput = newVal
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("8080", color = InputPlaceholder) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAccent,
                            unfocusedBorderColor = InputBorder,
                            cursorColor = OrangeAccent,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = InputBackground,
                            unfocusedContainerColor = InputBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Server will restart automatically with the new port.",
                        color = TextGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPort = portInput.toIntOrNull() ?: 8080
                        if (newPort in 1024..65535) {
                            app.kdsSettingsManager.port = newPort
                            if (isServerRunning) {
                                app.startKdsServer(newPort)
                            }
                            showPortDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPortDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun NetworkDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = TextWhite,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = valueColor,
                fontWeight = FontWeight.SemiBold
            )
            trailingContent?.invoke()
        }
    }
}
