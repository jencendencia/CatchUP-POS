package com.catchuppos.app.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.catchuppos.app.CatchUpApp
import com.catchuppos.app.data.UserEntity
import com.catchuppos.app.data.UserRole
import com.catchuppos.app.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UsersScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val userRepository = app.userRepository
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<UserEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        users = userRepository.getAllUsersOnce()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Users",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage user accounts and roles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.height(48.dp),
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
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add User",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

        // Users List
        if (users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No users found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                users.forEach { user ->
                    UserCard(
                        user = user,
                        onEdit = { editingUser = user },
                        onDelete = { showDeleteConfirm = user }
                    )
                }
            }
        }
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Add User Dialog
    if (showAddDialog) {
        UserFormDialog(
            title = "Add New User",
            onDismiss = { showAddDialog = false },
            onSave = { username, email, password, role, profileImagePath ->
                scope.launch {
                    userRepository.insertUser(
                        UserEntity(
                            username = username,
                            email = email,
                            password = password,
                            role = role,
                            isActive = true,
                            profileImagePath = profileImagePath
                        )
                    )
                    users = userRepository.getAllUsersOnce()
                    showAddDialog = false
                }
            }
        )
    }

    // Edit User Dialog
    if (editingUser != null) {
        UserFormDialog(
            title = "Edit User",
            existingUser = editingUser!!,
            onDismiss = { editingUser = null },
            onSave = { username, email, password, role, profileImagePath ->
                scope.launch {
                    val passwordChanged = password.isNotBlank()
                    val updated = editingUser!!.copy(
                        username = username,
                        email = email,
                        password = if (passwordChanged) password else editingUser!!.password,
                        role = role,
                        profileImagePath = profileImagePath
                    )
                    userRepository.updateUser(updated)
                    users = userRepository.getAllUsersOnce()
                    editingUser = null
                    if (passwordChanged) {
                        snackbarHostState.showSnackbar("Password updated successfully for ${updated.username}")
                    }
                }
            }
        )
    }

    // Delete Confirmation
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = {
                Text("Delete User", color = TextWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${showDeleteConfirm!!.username}\"? This action cannot be undone.",
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            userRepository.deleteUserById(showDeleteConfirm!!.id)
                            users = userRepository.getAllUsersOnce()
                            showDeleteConfirm = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedRed)
                ) {
                    Text("Delete", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}

@Composable
private fun UserCard(
    user: UserEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isAdmin = user.role == UserRole.ADMIN.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with profile image
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(OrangeAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (user.profileImagePath != null) {
                    val bitmap = remember(user.profileImagePath) {
                        com.catchuppos.app.util.loadAndFixBitmap(user.profileImagePath)
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                            contentDescription = null,
                            tint = OrangeAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Role Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isAdmin) OrangeAccent.copy(alpha = 0.15f) else DarkCard
            ) {
                Text(
                    text = if (isAdmin) "Admin" else "User",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAdmin) OrangeAccent else TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Status
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (user.isActive) StatusGreenSoft else Color.Transparent
            ) {
                Text(
                    text = if (user.isActive) "Active" else "Inactive",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.isActive) StatusGreen else TextGray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Edit Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(OrangeAccent.copy(alpha = 0.12f))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = OrangeAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MutedRed.copy(alpha = 0.12f))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MutedRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserFormDialog(
    title: String,
    existingUser: UserEntity? = null,
    onDismiss: () -> Unit,
    onSave: (username: String, email: String, password: String, role: String, profileImagePath: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf(existingUser?.username ?: "") }
    var email by remember { mutableStateOf(existingUser?.email ?: "") }
    var oldPassword by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(existingUser?.role ?: UserRole.USER.name) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var profileImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var savedProfilePath by remember { mutableStateOf<String?>(existingUser?.profileImagePath) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        profileImageUri = uri
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 400.dp, max = 460.dp)
                .wrapContentHeight()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Profile Picture Section
                Text("PROFILE PICTURE", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
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
                        if (profileImageUri != null) {
                            val bitmap = remember(profileImageUri) {
                                try {
                                    context.contentResolver.openInputStream(profileImageUri!!)?.use { stream ->
                                        android.graphics.BitmapFactory.decodeStream(stream)
                                    }
                                } catch (_: Exception) { null }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(28.dp))
                            }
                        } else if (savedProfilePath != null) {
                            val bitmap = remember(savedProfilePath) {
                                com.catchuppos.app.util.loadAndFixBitmap(savedProfilePath!!)
                            }
                            if (bitmap != null) {
                                Image(
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

                Spacer(modifier = Modifier.height(16.dp))

                // Username
                Text("USERNAME", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Enter username", color = InputPlaceholder) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = InputBorder,
                        cursorColor = OrangeAccent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Email
                Text("EMAIL", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Enter email", color = InputPlaceholder) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = InputBorder,
                        cursorColor = OrangeAccent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Old Password (only when editing and entering new password)
                if (existingUser != null && password.isNotBlank()) {
                    Text("CURRENT PASSWORD", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        placeholder = { Text("Enter current password to confirm change", color = InputPlaceholder) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAccent,
                            unfocusedBorderColor = InputBorder,
                            cursorColor = OrangeAccent,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Password
                Text("PASSWORD", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = {
                        Text(
                            if (existingUser != null) "New password (leave blank to keep)" else "Enter password",
                            color = InputPlaceholder
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = InputBorder,
                        cursorColor = OrangeAccent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Confirm Password
                Text("CONFIRM PASSWORD", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text("Confirm password", color = InputPlaceholder) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = InputBorder,
                        cursorColor = OrangeAccent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Role Selection
                Text("ROLE", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UserRole.entries.forEach { role ->
                        val isSelected = selectedRole == role.name
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedRole = role.name },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) OrangeAccent.copy(alpha = 0.15f) else DarkCard,
                            border = if (isSelected) BorderStroke(1.dp, OrangeAccent) else BorderStroke(1.dp, DarkBorder)
                        ) {
                            Text(
                                text = role.displayName,
                                modifier = Modifier.padding(vertical = 12.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) OrangeAccent else TextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Error Message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MutedRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = {
                        when {
                            username.isBlank() -> errorMessage = "Username is required"
                            email.isBlank() -> errorMessage = "Email is required"
                            password.isBlank() && existingUser == null -> errorMessage = "Password is required"
                            password != confirmPassword -> errorMessage = "Passwords do not match"
                            password.isNotBlank() && existingUser != null && oldPassword != existingUser!!.password -> errorMessage = "Current password is incorrect"
                            else -> {
                                errorMessage = null
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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = TextWhite
                    )
                ) {
                    Text(
                        text = if (existingUser != null) "Update User" else "Add User",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
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
