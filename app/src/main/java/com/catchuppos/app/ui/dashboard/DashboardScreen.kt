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
import com.catchuppos.app.theme.*
import com.catchuppos.app.update.UpdateChecker
import com.catchuppos.app.update.UpdateInfo
import com.catchuppos.app.ui.update.UpdateDialog
import kotlinx.coroutines.launch
import org.json.JSONArray

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

private fun saveCupCount(context: Context, count: Int) {
    val prefs = context.getSharedPreferences("catchup_pos_prefs", Context.MODE_PRIVATE)
    prefs.edit().putInt("cups_available", count).apply()
}

private fun loadCupCount(context: Context): Int {
    val prefs = context.getSharedPreferences("catchup_pos_prefs", Context.MODE_PRIVATE)
    return prefs.getInt("cups_available", 0)
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
    var cupsAvailable by remember { mutableIntStateOf(loadCupCount(context)) }
    var showCupsDialog by remember { mutableStateOf(false) }
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

    // Load real data from repository
    LaunchedEffect(Unit) {
        totalOrders = repository.getTransactionCount()
        todaySales = repository.getTodaySales()
        totalDrinksAvailable = repository.getProductCount()
        drinksSold = repository.getTotalItemsSold()
        cupsAvailable = loadCupCount(context)
    }

    // Refresh data when returning to dashboard
    LaunchedEffect(activeNavItem) {
        if (activeNavItem == NavItem.DASHBOARD) {
            totalOrders = repository.getTransactionCount()
            todaySales = repository.getTodaySales()
            totalDrinksAvailable = repository.getProductCount()
            drinksSold = repository.getTotalItemsSold()
            cupsAvailable = loadCupCount(context)
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
                                cupsAvailable = cupsAvailable,
                                onCupsClick = { showCupsDialog = true }
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

                    NavItem.SETTINGS -> {
                        SettingsScreen()
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

    // Cups Dialog
    if (showCupsDialog) {
        CupsDialog(
            currentCups = cupsAvailable,
            onCupSelected = { count ->
                cupsAvailable = count
                saveCupCount(context, count)
                showCupsDialog = false
            },
            onDismiss = { showCupsDialog = false }
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// Cups Dialog
// ════════════════════════════════════════════════════════════════════

@Composable
private fun CupsDialog(
    currentCups: Int,
    onCupSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var cupOptions by remember { mutableStateOf(loadCupOptions(context)) }
    var customInput by remember { mutableStateOf("") }
    var showAddNew by remember { mutableStateOf(false) }
    var newOptionQuantity by remember { mutableStateOf("") }

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
                            text = "Set Cups Available",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select or enter the number of cups",
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

                // Current cups display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Coffee,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Current: $currentCups cups",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Predefined options
                Text(
                    text = "QUICK SELECT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val icons = listOf(Icons.Default.LooksOne, Icons.Default.LooksTwo, Icons.Default.Looks3, Icons.Default.Looks4)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    cupOptions.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { option ->
                                val quantity = option.quantity
                                val isSelected = currentCups == quantity
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) OrangeAccent.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable { onCupSelected(quantity) }
                                        .padding(12.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(OrangeAccent.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = option.icon,
                                                contentDescription = null,
                                                tint = OrangeAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "${quantity}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextWhite,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "cups",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                        }
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = OrangeAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // Fill empty space if odd number
                            if (row.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Custom input
                Text(
                    text = "CUSTOM AMOUNT",
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
                        value = customInput,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) customInput = it },
                        placeholder = { Text("Enter number", color = InputPlaceholder) },
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
                            val count = customInput.toIntOrNull() ?: 0
                            if (count > 0) {
                                onCupSelected(count)
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeAccent,
                            contentColor = TextWhite
                        ),
                        enabled = customInput.toIntOrNull()?.let { it > 0 } == true
                    ) {
                        Text("Set", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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

                // Close button
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
