package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.catchuppos.app.CatchUpApp
import com.catchuppos.app.auth.AuthState
import com.catchuppos.app.theme.*
import com.catchuppos.app.update.UpdateChecker
import com.catchuppos.app.update.UpdateInfo
import com.catchuppos.app.ui.update.UpdateDialog
import kotlinx.coroutines.launch
@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val repository = app.productRepository
    var activeNavItem by remember { mutableStateOf(NavItem.DASHBOARD) }
    var productCount by remember { mutableIntStateOf(0) }
    var todaySales by remember { mutableDoubleStateOf(0.0) }
    var customersServed by remember { mutableIntStateOf(0) }
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
        productCount = repository.getProductCount()
        todaySales = repository.getTodaySales()
        customersServed = repository.getTodayCustomersServed()
    }

    // Refresh data when returning to dashboard
    LaunchedEffect(activeNavItem) {
        if (activeNavItem == NavItem.DASHBOARD) {
            productCount = repository.getProductCount()
            todaySales = repository.getTodaySales()
            customersServed = repository.getTodayCustomersServed()
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
                                productCount = productCount,
                                drinksCount = productCount,
                                todaySales = todaySales,
                                customersServed = customersServed
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
                        OrdersScreen()
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
}
