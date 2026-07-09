package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.catchuppos.app.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HeaderBar(
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var currentDate by remember { mutableStateOf(getCurrentDate()) }

    // Update clock every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = getCurrentTime()
            currentDate = getCurrentDate()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 28.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Greeting Group - Left
        Column {
            Text(
                text = getGreeting(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
            Text(
                text = "CATCH UP!",
                style = MaterialTheme.typography.headlineSmall,
                color = OrangeAccent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Store Dashboard",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
        }

        // Live Clock - Right
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = currentTime,
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentDate,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

private fun getGreeting(): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning!"
        in 12..16 -> "Good Afternoon!"
        in 17..20 -> "Good Evening!"
        else -> "Good Night!"
    }
}

private fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date())
}

private fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    return sdf.format(Date())
}
