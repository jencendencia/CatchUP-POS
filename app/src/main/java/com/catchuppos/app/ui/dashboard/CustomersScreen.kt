package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catchuppos.app.CatchUpApp
import com.catchuppos.app.data.*
import com.catchuppos.app.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomersScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val repository = app.productRepository

    // Data states
    var topCustomers by remember { mutableStateOf<List<CustomerSummary>>(emptyList()) }
    var allTransactions by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }

    // Load data
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        topCustomers = repository.getTopCustomers(todayStart, now, 10)
        allTransactions = repository.getTransactionsByDateRange(todayStart, now)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(28.dp)
    ) {
        // Title
        Text(
            text = "Customers",
            style = MaterialTheme.typography.headlineMedium,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "View your top customers and their spending history.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(20.dp))

        CustomersTabContent(
            topCustomers = topCustomers,
            totalCustomers = allTransactions.map { it.customerName }.distinct().size,
            allTransactions = allTransactions
        )
    }
}
