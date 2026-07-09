package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.catchuppos.app.CatchUpApp
import com.catchuppos.app.data.OrderItemEntity
import com.catchuppos.app.data.TransactionEntity
import com.catchuppos.app.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Main Transactions Screen ──

@Composable
fun TransactionsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val repository = app.productRepository
    val scope = rememberCoroutineScope()

    var selectedDateRange by remember { mutableStateOf("Today") }
    var selectedType by remember { mutableStateOf("All Types") }
    var selectedStatus by remember { mutableStateOf("All Status") }
    var currentPage by remember { mutableIntStateOf(1) }
    var allTransactions by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    val itemsPerPage = 8

    // Load transactions from database
    LaunchedEffect(Unit) {
        allTransactions = repository.getAllTransactionsOnce()
    }

    // Filter logic
    val filteredTransactions = remember(allTransactions, selectedType, selectedStatus, selectedDateRange) {
        val now = System.currentTimeMillis()
        val startOfDay = getStartOfDay(now)

        allTransactions.filter { txn ->
            // Date filter
            val dateMatch = when (selectedDateRange) {
                "Today" -> txn.createdAt >= startOfDay
                "This Week" -> txn.createdAt >= startOfDay - 7 * 86400000L
                "This Month" -> txn.createdAt >= startOfDay - 30 * 86400000L
                else -> true
            }
            // Type filter
            val typeMatch = selectedType == "All Types" ||
                txn.paymentMethod.equals(selectedType, ignoreCase = true)
            // Status filter
            val statusMatch = selectedStatus == "All Status" ||
                txn.status.equals(selectedStatus, ignoreCase = true)

            dateMatch && typeMatch && statusMatch
        }
    }

    val totalPages = maxOf((filteredTransactions.size + itemsPerPage - 1) / itemsPerPage, 1)
    val safePage = minOf(currentPage, totalPages)
    val paginatedTransactions = filteredTransactions.drop((safePage - 1) * itemsPerPage).take(itemsPerPage)

    // Metrics
    val totalOrders = filteredTransactions.size
    val totalSales = filteredTransactions.sumOf { it.total }
    val totalItemsSold = filteredTransactions.sumOf { it.itemCount }
    val completedOrders = filteredTransactions.count { it.status == "Completed" }
    val pendingOrders = filteredTransactions.count { it.status == "Pending" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(28.dp)
    ) {
        // ── Section Title ──
        Text(
            text = "TRANSACTIONS",
            style = MaterialTheme.typography.headlineSmall,
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Filters Row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterDropdown(
                label = selectedDateRange,
                icon = Icons.Default.DateRange,
                options = listOf("Today", "This Week", "This Month", "All Time"),
                onSelected = { selectedDateRange = it; currentPage = 1 },
                modifier = Modifier.weight(1.2f)
            )

            FilterDropdown(
                label = selectedType,
                icon = Icons.Default.Payment,
                options = listOf("All Types", "Cash", "GCash"),
                onSelected = { selectedType = it; currentPage = 1 },
                modifier = Modifier.weight(1f)
            )

            FilterDropdown(
                label = selectedStatus,
                icon = Icons.Default.Flag,
                options = listOf("All Status", "Completed", "Pending", "Canceled"),
                onSelected = { selectedStatus = it; currentPage = 1 },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = { /* Export logic */ },
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, OrangeAccent),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = OrangeAccent
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Export",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Metric Cards ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                icon = Icons.Default.Description,
                iconBg = OrangeAccent,
                value = "$totalOrders",
                label = "Total Orders",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.AttachMoney,
                iconBg = Color(0xFFE53935),
                value = "₱${String.format("%,.2f", totalSales)}",
                label = "Total Sales",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.Coffee,
                iconBg = OrangeAccent,
                value = "$totalItemsSold",
                label = "Total Items Sold",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.CheckCircle,
                iconBg = StatusGreen,
                value = "$completedOrders",
                label = "Completed Orders",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.Schedule,
                iconBg = Color(0xFFFFC107),
                value = "$pendingOrders",
                label = "Pending Orders",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Transactions Table ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0D0D0D),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ORDER #", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.labelMedium, color = TextMuted, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    Text("CUSTOMER", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium, color = TextMuted, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    Text("ITEMS", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelMedium, color = TextMuted, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    Text("TOTAL", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = TextMuted, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    Text("PAYMENT", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = TextMuted, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    Text("STATUS", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.labelMedium, color = TextMuted, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    Text("TIME", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.labelMedium, color = TextMuted, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.weight(0.3f))
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                if (paginatedTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No transactions found.", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
                            Text("Complete a checkout to see transactions here.", color = TextGray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    paginatedTransactions.forEach { txn ->
                        TransactionRow(txn, onClick = { selectedTransaction = txn })
                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                // ── Pagination Footer ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val from = if (filteredTransactions.isEmpty()) 0 else (safePage - 1) * itemsPerPage + 1
                    val to = minOf(safePage * itemsPerPage, filteredTransactions.size)
                    Text(
                        text = "Showing $from to $to of ${filteredTransactions.size} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaginationButton(
                            label = "<",
                            isActive = false,
                            enabled = safePage > 1,
                            onClick = { if (currentPage > 1) currentPage-- }
                        )
                        for (page in 1..totalPages) {
                            PaginationButton(
                                label = "$page",
                                isActive = page == safePage,
                                enabled = true,
                                onClick = { currentPage = page }
                            )
                        }
                        PaginationButton(
                            label = ">",
                            isActive = false,
                            enabled = safePage < totalPages,
                            onClick = { if (currentPage < totalPages) currentPage++ }
                        )
                    }
                }
            }
        }
    }

    // Transaction Detail Dialog
    if (selectedTransaction != null) {
        TransactionDetailDialog(
            transaction = selectedTransaction!!,
            onDismiss = { selectedTransaction = null }
        )
    }
}

private fun getStartOfDay(now: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

// ════════════════════════════════════════════════════════════════════
// Filter Dropdown
// ════════════════════════════════════════════════════════════════════

@Composable
private fun FilterDropdown(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, DarkBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = DarkCard,
                contentColor = TextWhite
            ),
            contentPadding = PaddingValues(horizontal = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == label) OrangeAccent else TextWhite,
                            fontWeight = if (option == label) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    leadingIcon = if (option == label) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Metric Card
// ════════════════════════════════════════════════════════════════════

@Composable
private fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0D0D0D),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBg,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Transaction Row
// ════════════════════════════════════════════════════════════════════

@Composable
private fun TransactionRow(txn: TransactionEntity, onClick: () -> Unit = {}) {
    val timeFormatted = remember(txn.createdAt) {
        SimpleDateFormat("hh:mm a", Locale.US).format(Date(txn.createdAt))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ORDER #
        Text(
            text = "#${String.format("%05d", txn.id)}",
            style = MaterialTheme.typography.bodyMedium,
            color = OrangeAccent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.9f)
        )

        // CUSTOMER
        Text(
            text = txn.customerName,
            style = MaterialTheme.typography.bodyMedium,
            color = TextWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f)
        )

        // ITEMS
        Text(
            text = txn.itemsJson,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f)
        )

        // TOTAL
        Text(
            text = "₱${String.format("%.2f", txn.total)}",
            style = MaterialTheme.typography.bodyMedium,
            color = OrangeAccent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        // PAYMENT
        Text(
            text = txn.paymentMethod,
            style = MaterialTheme.typography.bodySmall,
            color = TextWhite,
            modifier = Modifier.weight(1f)
        )

        // STATUS
        Box(modifier = Modifier.weight(1.1f)) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    when (txn.status) {
                        "Completed" -> StatusGreen
                        "Pending" -> Color(0xFFFFC107)
                        "Canceled" -> MutedRed
                        else -> DarkBorder
                    }
                )
            ) {
                Text(
                    text = txn.status,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (txn.status) {
                        "Completed" -> StatusGreen
                        "Pending" -> Color(0xFFFFC107)
                        "Canceled" -> MutedRed
                        else -> TextMuted
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // TIME
        Text(
            text = timeFormatted,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray,
            modifier = Modifier.weight(0.9f)
        )

        // Chevron
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "View details",
            tint = TextGray,
            modifier = Modifier
                .weight(0.3f)
                .size(18.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// Transaction Detail Dialog
// ════════════════════════════════════════════════════════════════════

@Composable
private fun TransactionDetailDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val repository = app.productRepository

    val dateFormatted = remember(transaction.createdAt) {
        SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.US).format(Date(transaction.createdAt))
    }

    var orderItems by remember { mutableStateOf<List<OrderItemEntity>>(emptyList()) }

    LaunchedEffect(transaction.id) {
        orderItems = repository.getOrderItemsByTransactionId(transaction.id)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 420.dp, max = 520.dp)
                .wrapContentHeight()
                .heightIn(max = 600.dp),
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
                            text = "Order #${String.format("%05d", transaction.id)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = OrangeAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        when (transaction.status) {
                            "Completed" -> StatusGreen
                            "Pending" -> Color(0xFFFFC107)
                            "Canceled" -> MutedRed
                            else -> DarkBorder
                        }
                    )
                ) {
                    Text(
                        text = transaction.status,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = when (transaction.status) {
                            "Completed" -> StatusGreen
                            "Pending" -> Color(0xFFFFC107)
                            "Canceled" -> MutedRed
                            else -> TextMuted
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Rows
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailInfoRow(label = "Customer", value = transaction.customerName)
                    DetailInfoRow(label = "Cashier", value = transaction.cashierName.ifBlank { "N/A" })
                    DetailInfoRow(label = "Payment", value = transaction.paymentMethod)
                    DetailInfoRow(label = "Items Sold", value = "${transaction.itemCount}")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Items List
                Text(
                    text = "ORDER ITEMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (orderItems.isEmpty()) {
                    // Fallback to itemsJson for old transactions
                    val fallbackItems = remember(transaction.itemsJson) {
                        transaction.itemsJson.split(", ").mapNotNull { item ->
                            val trimmed = item.trim()
                            if (trimmed.isNotBlank()) trimmed else null
                        }
                    }
                    if (fallbackItems.isEmpty()) {
                        Text(
                            text = "No item details available",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            fallbackItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkCard)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = OrangeAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextWhite,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        orderItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkCard)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = OrangeAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.quantity}x ${item.productName} (${item.size})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = "₱${String.format("%.2f", item.unitPrice)} each",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGray
                                    )
                                }
                                Text(
                                    text = "₱${String.format("%.2f", item.subtotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OrangeAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Totals
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoRow(label = "Total", value = "₱${String.format("%.2f", transaction.total)}", valueColor = OrangeAccent, bold = true)
                    DetailInfoRow(label = "Amount Tendered", value = "₱${String.format("%.2f", transaction.amountTendered)}")
                    DetailInfoRow(label = "Change", value = "₱${String.format("%.2f", transaction.changeReturned)}")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Close",
                            color = OrangeAccent,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String, valueColor: Color = TextWhite, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// Pagination Button
// ════════════════════════════════════════════════════════════════════

@Composable
private fun PaginationButton(
    label: String,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) OrangeAccent else Color.Transparent,
        border = if (isActive) null else BorderStroke(1.dp, DarkBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) TextWhite else if (enabled) TextWhite else TextGray,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
