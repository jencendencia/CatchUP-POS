package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.catchuppos.app.CatchUpApp
import com.catchuppos.app.auth.AuthState
import com.catchuppos.app.data.UserEntity
import com.catchuppos.app.data.UserRole
import com.catchuppos.app.theme.*
import com.catchuppos.app.update.UpdateChecker
import com.catchuppos.app.update.UpdateInfo
import com.catchuppos.app.ui.update.UpdateDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    onRestoreComplete: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val userRepository = app.userRepository
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<UserEntity?>(null) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }

    // Backup launcher
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            com.catchuppos.app.data.BackupHelper.backup(
                                context, output,
                                database = app.database.openHelper.writableDatabase
                            )
                        }
                        true
                    } catch (e: Exception) {
                        android.util.Log.e("Settings", "Backup failed", e)
                        false
                    }
                }
                Toast.makeText(
                    context,
                    if (success) "Backup created successfully" else "Backup failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Restore launcher
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirmation = true
        }
    }

    // Load users
    LaunchedEffect(Unit) {
        users = userRepository.getAllUsersOnce()
    }

    fun checkForUpdate() {
        scope.launch {
            checkingUpdate = true
            val update = UpdateChecker.checkForUpdate(context)
            checkingUpdate = false
            if (update != null) {
                updateInfo = update
            } else {
                Toast.makeText(context, "You're up to date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage users and system settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }

            if (AuthState.isAdmin) {
                Button(
                    onClick = { showAddUserDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = TextWhite
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add User", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Current User Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrangeAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AuthState.currentUser?.username ?: "Guest",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = AuthState.currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (AuthState.isAdmin) OrangeAccent.copy(alpha = 0.15f) else Color(0xFF1A1A1A)
                ) {
                    Text(
                        text = AuthState.currentUser?.role ?: "GUEST",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (AuthState.isAdmin) OrangeAccent else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Users List
        Text(
            text = "REGISTERED USERS",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Users
            items(users) { user ->
                UserCard(
                    user = user,
                    isCurrentUser = user.id == AuthState.currentUser?.id,
                    isAdmin = AuthState.isAdmin,
                    onEdit = { editingUser = user },
                    onDelete = { showDeleteConfirmation = user }
                )
            }

            // Empty users state
            if (users.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No users registered",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                    }
                }
            }

            // ── KDS Server Section ──
            item {
                Spacer(modifier = Modifier.height(32.dp))
                KdsServerSection(
                    app = app,
                    isAdmin = AuthState.isAdmin
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── App Updates Section ──
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "APP UPDATES",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { checkForUpdate() },
                            enabled = !checkingUpdate,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF1A1A1A),
                                contentColor = TextWhite,
                                disabledContainerColor = Color(0xFF1A1A1A),
                                disabledContentColor = TextMuted
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (checkingUpdate) "Checking for Updates..." else "Check for Updates",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Check if a new version is available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            if (checkingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF2196F3)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Database Management Section ──
            if (AuthState.isAdmin) {
                item {
                    Text(
                        text = "DATABASE MANAGEMENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Backup Button
                            OutlinedButton(
                                onClick = {
                                    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                                    val fileName = "catchup_pos_backup_${dateFormat.format(Date())}.zip"
                                    backupLauncher.launch(fileName)
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF1A1A1A),
                                    contentColor = TextWhite
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backup,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = StatusGreen
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Backup Database & Images",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Save a copy of your data and images",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Restore Button
                            OutlinedButton(
                                onClick = {
                                    restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF1A1A1A),
                                    contentColor = TextWhite
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = OrangeAccent
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Restore Database",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Load data and images from a backup file",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add User Dialog
    if (showAddUserDialog) {
        AddEditUserDialog(
            title = "Add New User",
            onSave = { username, email, password, role, profileImagePath ->
                scope.launch {
                    userRepository.insertUser(
                        UserEntity(
                            username = username,
                            email = email,
                            password = password,
                            role = role.name,
                            profileImagePath = profileImagePath
                        )
                    )
                    users = userRepository.getAllUsersOnce()
                    showAddUserDialog = false
                }
            },
            onDismiss = { showAddUserDialog = false }
        )
    }

    // Edit User Dialog
    if (editingUser != null) {
        AddEditUserDialog(
            title = "Edit User",
            existingUser = editingUser!!,
            onSave = { username, email, password, role, profileImagePath ->
                scope.launch {
                    val updatedUser = editingUser!!.copy(
                        username = username,
                        email = email,
                        password = if (password.isNotBlank()) password else editingUser!!.password,
                        role = role.name,
                        profileImagePath = profileImagePath
                    )
                    userRepository.updateUser(updatedUser)
                    users = userRepository.getAllUsersOnce()
                    editingUser = null
                }
            },
            onDismiss = { editingUser = null }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text("Delete User", color = TextWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete ${showDeleteConfirmation!!.username}? This action cannot be undone.",
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            userRepository.deleteUserById(showDeleteConfirmation!!.id)
                            users = userRepository.getAllUsersOnce()
                            showDeleteConfirmation = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Update Dialog
    if (updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss = { updateInfo = null }
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmation && pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false; pendingRestoreUri = null },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text("Restore Database", color = TextWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will replace all current data and images with the backup. This action cannot be undone. Continue?",
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = withContext(Dispatchers.IO) {
                                try {
                                    // Close existing connections
                                    app.closeDatabase()
                                    // Restore database and images from zip
                                    context.contentResolver.openInputStream(pendingRestoreUri!!)?.use { input ->
                                        com.catchuppos.app.data.BackupHelper.restore(context, input)
                                    } ?: false
                                } catch (e: Exception) {
                                    android.util.Log.e("Settings", "Restore failed", e)
                                    false
                                }
                            }
                            showRestoreConfirmation = false
                            pendingRestoreUri = null
                            if (success) {
                                // Re-seed admin so login works with the restored database
                                app.userRepository.seedDefaultAdmin()
                                AuthState.logout()
                                Toast.makeText(context, "Database restored successfully", Toast.LENGTH_LONG).show()
                                onRestoreComplete()
                            } else {
                                Toast.makeText(context, "Restore failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) {
                    Text("Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmation = false; pendingRestoreUri = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

}

@Composable
private fun UserCard(
    user: UserEntity,
    isCurrentUser: Boolean,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (user.role == UserRole.ADMIN.name)
                            OrangeAccent.copy(alpha = 0.15f)
                        else
                            Color(0xFF1A1A1A)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (user.profileImagePath != null) {
                    val bitmap = remember(user.profileImagePath) {
                        com.catchuppos.app.util.loadAndFixBitmap(user.profileImagePath)
                    }
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (user.role == UserRole.ADMIN.name) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (user.role == UserRole.ADMIN.name) OrangeAccent else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = if (user.role == UserRole.ADMIN.name) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (user.role == UserRole.ADMIN.name) OrangeAccent else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StatusGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "YOU",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            // Role Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (user.role == UserRole.ADMIN.name)
                    OrangeAccent.copy(alpha = 0.12f)
                else
                    Color(0xFF1A1A1A)
            ) {
                Text(
                    text = user.role,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.role == UserRole.ADMIN.name) OrangeAccent else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }

            // Actions (Admin only, not for current user)
            if (isAdmin && !isCurrentUser) {
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MutedRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditUserDialog(
    title: String,
    existingUser: UserEntity? = null,
    onSave: (username: String, email: String, password: String, role: UserRole, profileImagePath: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(existingUser?.username ?: "") }
    var email by remember { mutableStateOf(existingUser?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(
        if (existingUser?.role == UserRole.ADMIN.name) UserRole.ADMIN else UserRole.USER
    ) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var profileImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var savedProfilePath by remember { mutableStateOf<String?>(existingUser?.profileImagePath) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        profileImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = {
            Text(title, color = TextWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = InputBorder,
                        cursorColor = OrangeAccent,
                        focusedLabelColor = OrangeAccent,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = InputBorder,
                        cursorColor = OrangeAccent,
                        focusedLabelColor = OrangeAccent,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (existingUser != null) "New Password (leave blank to keep)" else "Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = InputBorder,
                        cursorColor = OrangeAccent,
                        focusedLabelColor = OrangeAccent,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = InputBorder,
                        cursorColor = OrangeAccent,
                        focusedLabelColor = OrangeAccent,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )

                // Profile Picture
                Text("Profile Picture", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile picture preview
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(OrangeAccent.copy(alpha = 0.15f))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val displayPath = profileImageUri?.let { null } ?: savedProfilePath
                        if (profileImageUri != null) {
                            val bitmap = remember(profileImageUri) {
                                try {
                                    context.contentResolver.openInputStream(profileImageUri!!)?.use { stream ->
                                        android.graphics.BitmapFactory.decodeStream(stream)
                                    }
                                } catch (_: Exception) { null }
                            }
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(28.dp))
                            }
                        } else if (displayPath != null) {
                            val bitmap = remember(displayPath) {
                                com.catchuppos.app.util.loadAndFixBitmap(displayPath)
                            }
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(28.dp))
                            }
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(28.dp))
                        }
                    }
                    TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text("Upload Photo", color = OrangeAccent, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Role Selection
                Text("Role", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UserRole.entries.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(role.displayName) },
                            leadingIcon = if (selectedRole == role) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangeAccent,
                                selectedLabelColor = TextWhite,
                                containerColor = Color(0xFF0D0D0D),
                                labelColor = TextMuted
                            )
                        )
                    }
                }

                // Error Message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MutedRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        username.isBlank() -> errorMessage = "Username is required"
                        email.isBlank() -> errorMessage = "Email is required"
                        password.isBlank() && existingUser == null -> errorMessage = "Password is required"
                        password != confirmPassword -> errorMessage = "Passwords do not match"
                        else -> {
                            scope.launch {
                                val imagePath = if (profileImageUri != null) {
                                    withContext(Dispatchers.IO) {
                                        saveProfileImageToInternalStorage(context, profileImageUri!!)
                                    }
                                } else {
                                    savedProfilePath
                                }
                                onSave(username.trim(), email.trim(), password, selectedRole, imagePath)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

// ── Profile Image Save Helper ──

private fun saveProfileImageToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val imagesDir = java.io.File(context.filesDir, "profile_images")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val fileName = "profile_${System.currentTimeMillis()}.jpg"
        val file = java.io.File(imagesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (_: Exception) {
        null
    }
}
