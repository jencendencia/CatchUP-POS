package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catchuppos.app.theme.*

// ── Data Model ──

data class TransactionRecord(
    val orderId: Int,
    val customer: String,
    val items: String,
    val total: Double,
    val payment: String,
    val status: String,
    val time: String
)

// ── Sample Data ──

private val sampleTransactions = listOf(
    TransactionRecord(15, "Master Joel", "2 × Macchiato (16oz)", 170.00, "Cash", "Completed", "04:35 PM"),
    TransactionRecord(14, "Maria", "1 × Matcha Strawberry (16oz)", 95.00, "GCash", "Completed", "04:30 PM"),
    TransactionRecord(13, "John", "1 × Cappuccino (16oz)", 85.00, "Cash", "Completed", "04:28 PM"),
    TransactionRecord(12, "Kevin", "1 × Spanish Latte (16oz)", 85.00, "Cash", "Completed", "04:25 PM"),
    TransactionRecord(11, "Anna", "2 × Okinawa (16oz)", 170.00, "GCash", "Completed", "04:20 PM"),
    TransactionRecord(10, "Chris", "1 × Americano (16oz)", 85.00, "Cash", "Completed", "04:18 PM"),
    TransactionRecord(9, "Rose", "1 × Chocolate (16oz)", 85.00, "Cash", "Completed", "04:15 PM"),
    TransactionRecord(8, "Mark", "1 × Cookies & Cream (16oz)", 85.00, "GCash", "Completed", "04:10 PM"),
    TransactionRecord(7, "Sofia", "1 × Caramel Macchiato (16oz)", 95.00, "Cash", "Completed", "03:55 PM"),
    TransactionRecord(6, "Daniel", "3 × Latte (16oz)", 255.00, "GCash", "Completed", "03:40 PM"),
    TransactionRecord(5, "Ella", "1 × Mocha (16oz)", 90.00, "Cash", "Completed", "03:25 PM"),
    TransactionRecord(4, "Brian", "2 × Americano (16oz)", 170.00, "Cash", "Completed", "03:10 PM"),
    TransactionRecord(3, "Liza", "1 × Vanilla Latte (16oz)", 95.00, "GCash", "Completed", "02:50 PM"),
    TransactionRecord(2, "Rico", "1 × Hazelnut Latte (16oz)", 95.00, "Cash", "Completed", "02:30 PM"),
    TransactionRecord(1, "Clara", "1 × Brewed Coffee (16oz)", 75.00, "Cash", "Completed", "02:00 PM")
)

// ── Main Transactions Screen ──

@Composable
fun TransactionsScreen() {
    var selectedDateRange by remember { mutableStateOf("July 9, 2026 - July 9, 2026") }
    var selectedType by remember { mutableStateOf("All Types") }
    var selectedStatus by remember { mutableStateOf("All Status") }
    var currentPage by remember { mutableIntStateOf(1) }
    val itemsPerPage = 8

    // Filter logic
    val filteredTransactions = remember(selectedType, selectedStatus) {
        sampleTransactions.filter { txn ->
            val typeMatch = selectedType == "All Types" ||
                txn.payment.equals(selectedType, ignoreCase = true)
            val statusMatch = selectedStatus == "All Status" ||
                txn.status.equals(selectedStatus, ignoreCase = true)
            typeMatch && statusMatch
        }
    }

    val totalPages = (filteredTransactions.size + itemsPerPage - 1) / itemsPerPage
    val paginatedTransactions = filteredTransactions.drop((currentPage - 1) * itemsPerPage).take(itemsPerPage)

    // Metrics
    val totalOrders = filteredTransactions.size
    val totalSales = filteredTransactions.sumOf { it.total }
    val totalItemsSold = filteredTransactions.sumOf { it.items.split("×").first().trim().toIntOrNull() ?: 0 }
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
            // Date Range Dropdown
            FilterDropdown(
                label = selectedDateRange,
                icon = Icons.Default.DateRange,
                options = listOf(
                    "July 9, 2026 - July 9, 2026",
                    "July 1, 2026 - July 9, 2026",
                    "June 1, 2026 - June 30, 2026"
                ),
                onSelected = { selectedDateRange = it },
                modifier = Modifier.weight(1.2f)
            )

            // Transaction Type Dropdown
            FilterDropdown(
                label = selectedType,
                icon = Icons.Default.Payment,
                options = listOf("All Types", "Cash", "GCash"),
                onSelected = { selectedType = it; currentPage = 1 },
                modifier = Modifier.weight(1f)
            )

            // Transaction Status Dropdown
            FilterDropdown(
                label = selectedStatus,
                icon = Icons.Default.Flag,
                options = listOf("All Status", "Completed", "Pending", "Canceled"),
                onSelected = { selectedStatus = it; currentPage = 1 },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Export Button
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

                // Table Rows
                if (paginatedTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions found.", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    paginatedTransactions.forEach { txn ->
                        TransactionRow(txn)
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
                    Text(
                        text = "Showing ${(currentPage - 1) * itemsPerPage + 1} to ${minOf(currentPage * itemsPerPage, filteredTransactions.size)} of ${filteredTransactions.size} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Previous
                        PaginationButton(
                            label = "<",
                            isActive = false,
                            enabled = currentPage > 1,
                            onClick = { if (currentPage > 1) currentPage-- }
                        )
                        // Page numbers
                        for (page in 1..totalPages) {
                            PaginationButton(
                                label = "$page",
                                isActive = page == currentPage,
                                enabled = true,
                                onClick = { currentPage = page }
                            )
                        }
                        // Next
                        PaginationButton(
                            label = ">",
                            isActive = false,
                            enabled = currentPage < totalPages,
                            onClick = { if (currentPage < totalPages) currentPage++ }
                        )
                    }
                }
            }
        }
    }
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
private fun TransactionRow(txn: TransactionRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Open detail */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ORDER #
        Text(
            text = "#${String.format("%05d", txn.orderId)}",
            style = MaterialTheme.typography.bodyMedium,
            color = OrangeAccent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.9f)
        )

        // CUSTOMER
        Text(
            text = txn.customer,
            style = MaterialTheme.typography.bodyMedium,
            color = TextWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f)
        )

        // ITEMS
        Text(
            text = txn.items,
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
            text = txn.payment,
            style = MaterialTheme.typography.bodySmall,
            color = TextWhite,
            modifier = Modifier.weight(1f)
        )

        // STATUS
        Box(
            modifier = Modifier.weight(1.1f)
        ) {
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
            text = txn.time,
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
