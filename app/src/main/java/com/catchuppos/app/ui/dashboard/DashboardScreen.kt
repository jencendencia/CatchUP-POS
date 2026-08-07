package com.catchuppos.app.ui.dashboard

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.catchuppos.app.CatchUpApp
import com.catchuppos.app.auth.AuthState
import com.catchuppos.app.data.PaymentMethodSales
import com.catchuppos.app.theme.*
import com.catchuppos.app.update.UpdateChecker
import com.catchuppos.app.update.UpdateInfo
import com.catchuppos.app.ui.update.UpdateDialog
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── Cup size option data model ──
private data class CupSizeOption(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val quantity: Int
)

private val defaultCupOptions = listOf(
    CupSizeOption("50 cups", Icons.Default.LooksOne, 50),
    CupSizeOption("100 cups", Icons.Default.LooksTwo, 100),
    CupSizeOption("200 cups", Icons.Default.Looks3, 200),
    CupSizeOption("500 cups", Icons.Default.Looks4, 500)
)

// ── SharedPreferences helpers ──
private fun saveCupOptions(context: Context, options: List<CupSizeOption>) {
    val prefs = context.getSharedPreferences("catchup_pos_prefs", Context.MODE_PRIVATE)
    val jsonArray = JSONArray()
    options.forEach { opt ->
        val obj = org.json.JSONObject()
        obj.put("name", opt.name)
        obj.put("quantity", opt.quantity)
        jsonArray.put(obj)
    }
    prefs.edit().putString("cup_options", jsonArray.toString()).apply()
}

private fun loadCupOptions(context: Context): List<CupSizeOption> {
    val prefs = context.getSharedPreferences("catchup_pos_prefs", Context.MODE_PRIVATE)
    val json = prefs.getString("cup_options", null) ?: return defaultCupOptions
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            CupSizeOption(
                name = obj.getString("name"),
                icon = Icons.Default.Coffee,
                quantity = obj.getInt("quantity")
            )
        }
    } catch (e: Exception) {
        defaultCupOptions
    }
}

private fun saveCupCount(context: Context, type: String, count: Int) {
    val prefs = context.getSharedPreferences("catchup_pos_prefs", Context.MODE_PRIVATE)
    prefs.edit().putInt("cups_${type}_available", count).apply()
}

private fun loadCupCount(context: Context, type: String): Int {
    val prefs = context.getSharedPreferences("catchup_pos_prefs", Context.MODE_PRIVATE)
    return prefs.getInt("cups_${type}_available", 0)
}
@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val repository = app.productRepository
    var activeNavItem by remember { mutableStateOf(NavItem.DASHBOARD) }
    var totalOrders by remember { mutableIntStateOf(0) }
    var drinksSold by remember { mutableIntStateOf(0) }
    var todaySales by remember { mutableDoubleStateOf(0.0) }
    var totalDrinksAvailable by remember { mutableIntStateOf(0) }
    var cupsAvailable by remember { mutableIntStateOf(loadCupCount(context, "hot") + loadCupCount(context, "cold")) }
    var hotCupsAvailable by remember { mutableIntStateOf(loadCupCount(context, "hot")) }
    var coldCupsAvailable by remember { mutableIntStateOf(loadCupCount(context, "cold")) }
    var showCupsDialog by remember { mutableStateOf(false) }
    var showSalesTodayDialog by remember { mutableStateOf(false) }
    var todayPaymentMethods by remember { mutableStateOf<List<PaymentMethodSales>>(emptyList()) }
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    // Check for updates on first launch
    LaunchedEffect(Unit) {
        val update = UpdateChecker.checkForUpdate(context)
        if (update != null) {
            updateInfo = update
        }
    }

    // Load real data from repository (today-specific data)
    LaunchedEffect(Unit) {
        totalOrders = repository.getTodayOrdersCount()
        todaySales = repository.getTodaySales()
        totalDrinksAvailable = repository.getProductCount()
        drinksSold = repository.getTodayItemsSold()
        hotCupsAvailable = loadCupCount(context, "hot")
        coldCupsAvailable = loadCupCount(context, "cold")
        cupsAvailable = hotCupsAvailable + coldCupsAvailable
    }

    // Load today's payment-method breakdown when the Sales Today dialog opens
    LaunchedEffect(showSalesTodayDialog) {
        if (showSalesTodayDialog) {
            todayPaymentMethods = repository.getSalesByPaymentMethod(getTodayStartMillis(), System.currentTimeMillis())
        }
    }

    // Refresh data when returning to dashboard
    LaunchedEffect(activeNavItem) {
        if (activeNavItem == NavItem.DASHBOARD) {
            totalOrders = repository.getTodayOrdersCount()
            todaySales = repository.getTodaySales()
            totalDrinksAvailable = repository.getProductCount()
            drinksSold = repository.getTodayItemsSold()
            hotCupsAvailable = loadCupCount(context, "hot")
            coldCupsAvailable = loadCupCount(context, "cold")
            cupsAvailable = hotCupsAvailable + coldCupsAvailable
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Sidebar Navigation
        Sidebar(
            activeItem = activeNavItem,
            onNavItemClick = { activeNavItem = it },
            onLogout = onLogout
        )

        // Main Workspace
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Header Bar
            HeaderBar()

            // Content area based on active nav item
            Box(modifier = Modifier.fillMaxSize()) {
                when (activeNavItem) {
                    NavItem.DASHBOARD -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))

                            // KPI Dashboard Grid (4-column)
                            KPICardsGrid(
                                totalOrders = totalOrders,
                                drinksSold = drinksSold,
                                todaySales = todaySales,
                                totalDrinksAvailable = totalDrinksAvailable,
                                hotCupsAvailable = hotCupsAvailable,
                                coldCupsAvailable = coldCupsAvailable,
                                onCupsClick = { showCupsDialog = true },
                                onSalesTodayClick = { showSalesTodayDialog = true }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Quick Actions Row
                            QuickActionsSection(
                                onNewOrderClick = { activeNavItem = NavItem.ORDERS },
                                onViewOrdersClick = { activeNavItem = NavItem.ORDERS },
                                onProductsClick = { activeNavItem = NavItem.PRODUCTS },
                                onReportsClick = { /* Reports page - not implemented yet */ }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Divider
                            HorizontalDivider(
                                color = DarkBorder,
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 28.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Empty State (Main Content Display Area)
                            EmptyState(
                                onNewOrderClick = { activeNavItem = NavItem.ORDERS }
                            )

                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    NavItem.ORDERS -> {
                        OrdersScreen(
                            onOrderComplete = { activeNavItem = NavItem.DASHBOARD }
                        )
                    }

                    NavItem.TRANSACTIONS -> {
                        TransactionsScreen()
                    }

                    NavItem.PRODUCTS -> {
                        ProductsScreen()
                    }

                    NavItem.REPORTS -> {
                        ReportsScreen(
                            onNavigate = { navItem -> activeNavItem = navItem }
                        )
                    }

                    NavItem.PROFIT -> {
                        ProfitScreen()
                    }

                    NavItem.EXPENSES -> {
                        ExpensesScreen()
                    }

                    NavItem.CUSTOMERS -> {
                        CustomersScreen()
                    }

                    NavItem.USERS -> {
                        UsersScreen()
                    }

                    NavItem.SETTINGS -> {
                        SettingsScreen(
                            onRestoreComplete = {
                                // AuthState.logout() already called inside SettingsScreen
                                onLogout()
                            }
                        )
                    }

                    else -> {
                        // Placeholder for other tabs
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeNavItem.label,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    // Update Dialog
    if (updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss = { updateInfo = null }
        )
    }

    // Sales Today Dialog
    if (showSalesTodayDialog) {
        SalesTodayDialog(
            totalSales = todaySales,
            paymentMethods = todayPaymentMethods,
            onDismiss = { showSalesTodayDialog = false }
        )
    }

    // Cups Dialog
    if (showCupsDialog) {
        CupsDialog(
            currentHotCups = hotCupsAvailable,
            currentColdCups = coldCupsAvailable,
            onHotCupsChanged = { count ->
                hotCupsAvailable = count
                saveCupCount(context, "hot", count)
                cupsAvailable = hotCupsAvailable + coldCupsAvailable
            },
            onColdCupsChanged = { count ->
                coldCupsAvailable = count
                saveCupCount(context, "cold", count)
                cupsAvailable = hotCupsAvailable + coldCupsAvailable
            },
            onDismiss = { showCupsDialog = false }
        )
    }
}

private fun getTodayStartMillis(): Long {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

// ════════════════════════════════════════════════════════════════════
// Sales Today Dialog (Cash / GCash breakdown)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun SalesTodayDialog(
    totalSales: Double,
    paymentMethods: List<PaymentMethodSales>,
    onDismiss: () -> Unit
) {
    val dateText = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date()) }
    val cash = paymentMethods.firstOrNull { it.method.equals("Cash", ignoreCase = true) }?.totalSales ?: 0.0
    val gcash = paymentMethods.firstOrNull { it.method.equals("GCash", ignoreCase = true) }?.totalSales ?: 0.0
    val others = paymentMethods.filter {
        !it.method.equals("Cash", ignoreCase = true) && !it.method.equals("GCash", ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 380.dp, max = 440.dp)
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
                            text = "Today's Sales Report",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateText,
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
                Spacer(modifier = Modifier.height(20.dp))

                // Total Sales
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "₱${String.format(Locale.US, "%,.2f", totalSales)}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total Sales Today",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cash breakdown
                PaymentBreakdownRow(
                    icon = Icons.Default.Payments,
                    color = StatusGreen,
                    label = "Cash",
                    amount = cash,
                    total = totalSales
                )

                Spacer(modifier = Modifier.height(10.dp))

                // GCash breakdown
                PaymentBreakdownRow(
                    icon = Icons.Default.PhoneAndroid,
                    color = Color(0xFF2196F3),
                    label = "GCash",
                    amount = gcash,
                    total = totalSales
                )

                // Other payment methods (if any)
                others.forEach { pm ->
                    Spacer(modifier = Modifier.height(10.dp))
                    PaymentBreakdownRow(
                        icon = Icons.Default.MoreHoriz,
                        color = Color(0xFF9C27B0),
                        label = pm.method,
                        amount = pm.totalSales,
                        total = totalSales
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
private fun PaymentBreakdownRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    label: String,
    amount: Double,
    total: Double
) {
    val pct = if (total > 0) (amount / total * 100) else 0.0
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkCard
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "₱${String.format(Locale.US, "%,.2f", amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${String.format(Locale.US, "%.1f", pct)}%",
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Cups Dialog (separate Hot / Cold inputs)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun CupsDialog(
    currentHotCups: Int,
    currentColdCups: Int,
    onHotCupsChanged: (Int) -> Unit,
    onColdCupsChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var cupOptions by remember { mutableStateOf(loadCupOptions(context)) }
    var hotInput by remember { mutableStateOf("") }
    var coldInput by remember { mutableStateOf("") }
    var showAddNew by remember { mutableStateOf(false) }
    var newOptionQuantity by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 380.dp, max = 460.dp)
                .heightIn(max = 700.dp)
                .verticalScroll(rememberScrollState()),
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
                            text = "Set Cups Available",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter the number of hot and cold cups",
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

                // ── Hot Cups Section ──
                CupAmountSection(
                    title = "HOT CUPS",
                    icon = Icons.Default.LocalFireDepartment,
                    color = Color(0xFFFF9800),
                    current = currentHotCups,
                    options = cupOptions,
                    input = hotInput,
                    onInputChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) hotInput = it },
                    onQuickSelect = { count -> onHotCupsChanged(count) },
                    onSet = {
                        val count = hotInput.toIntOrNull() ?: 0
                        if (count >= 0) {
                            onHotCupsChanged(count)
                            hotInput = ""
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // ── Cold Cups Section ──
                CupAmountSection(
                    title = "COLD CUPS",
                    icon = Icons.Default.AcUnit,
                    color = Color(0xFF2196F3),
                    current = currentColdCups,
                    options = cupOptions,
                    input = coldInput,
                    onInputChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) coldInput = it },
                    onQuickSelect = { count -> onColdCupsChanged(count) },
                    onSet = {
                        val count = coldInput.toIntOrNull() ?: 0
                        if (count >= 0) {
                            onColdCupsChanged(count)
                            coldInput = ""
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Add New Option
                if (!showAddNew) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = { showAddNew = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = OrangeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add New Option",
                                color = OrangeAccent,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                } else {
                    Text(
                        text = "ADD NEW OPTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newOptionQuantity,
                            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) newOptionQuantity = it },
                            placeholder = { Text("Number of cups", color = InputPlaceholder) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeAccent,
                                unfocusedBorderColor = InputBorder,
                                cursorColor = OrangeAccent,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )
                        Button(
                            onClick = {
                                val qty = newOptionQuantity.toIntOrNull() ?: 0
                                if (qty > 0) {
                                    val newOption = CupSizeOption("${qty} cups", Icons.Default.Coffee, qty)
                                    cupOptions = cupOptions + newOption
                                    saveCupOptions(context, cupOptions)
                                    newOptionQuantity = ""
                                    showAddNew = false
                                }
                            },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangeAccent,
                                contentColor = TextWhite
                            ),
                            enabled = newOptionQuantity.toIntOrNull()?.let { it > 0 } == true
                        ) {
                            Text("Add", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Done button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Done",
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

// ════════════════════════════════════════════════════════════════════
// Cup Amount Section (shared by Hot / Cold)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun CupAmountSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    current: Int,
    options: List<CupSizeOption>,
    input: String,
    onInputChange: (String) -> Unit,
    onQuickSelect: (Int) -> Unit,
    onSet: () -> Unit
) {
    Column {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Current: $current cups",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick select options
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { option ->
                        val isSelected = current == option.quantity
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { onQuickSelect(option.quantity) }
                                .padding(vertical = 10.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${option.quantity}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "cups",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom input + Set
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("Custom amount", color = InputPlaceholder) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = color,
                    unfocusedBorderColor = InputBorder,
                    cursorColor = color,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                )
            )
            Button(
                onClick = onSet,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = color,
                    contentColor = TextWhite
                ),
                enabled = input.toIntOrNull() != null
            ) {
                Text("Set", fontWeight = FontWeight.Bold)
            }
        }
    }
}
