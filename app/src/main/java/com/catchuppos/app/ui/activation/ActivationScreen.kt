package com.catchuppos.app.ui.activation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catchuppos.app.R
import com.catchuppos.app.license.ActivateResult
import com.catchuppos.app.license.LicenseManager
import com.catchuppos.app.theme.*
import kotlinx.coroutines.launch

@Composable
fun ActivationScreen(
    onActivationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var keyPart1 by remember { mutableStateOf("") }
    var keyPart2 by remember { mutableStateOf("") }
    var keyPart3 by remember { mutableStateOf("") }
    var keyPart4 by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun attemptActivation() {
        val fullKey = "DTR-$keyPart1-$keyPart2-$keyPart3-$keyPart4"
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = LicenseManager.activate(context, fullKey)
            if (result is ActivateResult.SUCCESS) {
                onActivationSuccess()
            } else {
                errorMessage = result.message
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkSurface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            OrangeAccent.copy(alpha = 0.03f),
                            DarkBackground.copy(alpha = 0f)
                        ),
                        radius = 800f
                    )
                )
        )

        Card(
            modifier = Modifier
                .widthIn(min = 420.dp, max = 520.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "CatchUP POS Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = "Activate Your License",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your license key to start using CatchUP POS",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                Spacer(modifier = Modifier.height(24.dp))

                // Key Input Fields
                Text(
                    text = "LICENSE KEY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prefix
                    Text(
                        text = "DTR",
                        style = MaterialTheme.typography.titleMedium,
                        color = OrangeAccent,
                        fontWeight = FontWeight.Bold
                    )

                    Text("-", color = TextMuted, fontWeight = FontWeight.Bold)

                    // Part 1
                    OutlinedTextField(
                        value = keyPart1,
                        onValueChange = {
                            keyPart1 = it.uppercase().take(4)
                            errorMessage = null
                            if (it.length == 4) focusManager.moveFocus(FocusDirection.Right)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Right) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAccent,
                            unfocusedBorderColor = InputBorder,
                            cursorColor = OrangeAccent,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    Text("-", color = TextMuted, fontWeight = FontWeight.Bold)

                    // Part 2
                    OutlinedTextField(
                        value = keyPart2,
                        onValueChange = {
                            keyPart2 = it.uppercase().take(4)
                            errorMessage = null
                            if (it.length == 4) focusManager.moveFocus(FocusDirection.Right)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Right) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAccent,
                            unfocusedBorderColor = InputBorder,
                            cursorColor = OrangeAccent,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    Text("-", color = TextMuted, fontWeight = FontWeight.Bold)

                    // Part 3
                    OutlinedTextField(
                        value = keyPart3,
                        onValueChange = {
                            keyPart3 = it.uppercase().take(4)
                            errorMessage = null
                            if (it.length == 4) focusManager.moveFocus(FocusDirection.Right)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Right) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAccent,
                            unfocusedBorderColor = InputBorder,
                            cursorColor = OrangeAccent,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    Text("-", color = TextMuted, fontWeight = FontWeight.Bold)

                    // Part 4
                    OutlinedTextField(
                        value = keyPart4,
                        onValueChange = {
                            keyPart4 = it.uppercase().take(4)
                            errorMessage = null
                            if (it.length == 4) focusManager.clearFocus()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (keyPart1.length == 4 && keyPart2.length == 4 && keyPart3.length == 4 && keyPart4.length == 4) {
                                    attemptActivation()
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAccent,
                            unfocusedBorderColor = InputBorder,
                            cursorColor = OrangeAccent,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )
                }

                // Error message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = MutedRed,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Activate Button
                Button(
                    onClick = {
                        if (keyPart1.isBlank() || keyPart2.isBlank() || keyPart3.isBlank() || keyPart4.isBlank()) {
                            errorMessage = "Please enter the full license key"
                        } else {
                            attemptActivation()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = TextWhite,
                        disabledContainerColor = OrangeAccent.copy(alpha = 0.5f)
                    ),
                    enabled = !isLoading,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = TextWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Activate",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Help text
                Text(
                    text = "Contact your seller if you don't have a license key",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
