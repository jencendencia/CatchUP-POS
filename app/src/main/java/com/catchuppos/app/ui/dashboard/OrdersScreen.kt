package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import com.catchuppos.app.CatchUpApp
import com.catchuppos.app.data.ProductEntity
import com.catchuppos.app.theme.*
import kotlinx.coroutines.launch

// ── Cart Data Model ──

data class CartItem(
    val product: ProductEntity,
    val quantity: Int = 1,
    val size: String = "12oz",
    val sugarLevel: String = "100%",
    val iceLevel: String = "Regular Ice",
    val specialInstructions: String = ""
)

// ── Main Orders Screen ──

@Composable
fun OrdersScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val repository = app.productRepository
    val scope = rememberCoroutineScope()

    var drinkProducts by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var dbCategories by remember { mutableStateOf<List<com.catchuppos.app.data.CategoryEntity>>(emptyList()) }
    var heldOrders by remember { mutableStateOf<List<List<CartItem>>>(emptyList()) }
    var showHeldOrdersDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var checkoutData by remember { mutableStateOf<CheckoutData?>(null) }

    fun holdOrder() {
        if (cartItems.isNotEmpty()) {
            heldOrders = heldOrders + listOf(cartItems)
            cartItems = emptyList()
        }
    }

    fun restoreHeldOrder(index: Int) {
        cartItems = heldOrders[index]
        heldOrders = heldOrders.toMutableList().apply { removeAt(index) }
    }

    fun deleteHeldOrder(index: Int) {
        heldOrders = heldOrders.toMutableList().apply { removeAt(index) }
    }

    // Load drink products and categories from DB
    LaunchedEffect(Unit) {
        val allProducts = repository.allProductsOnce()
        drinkProducts = allProducts.filter { it.type == "DRINK" }
        dbCategories = repository.allCategoriesOnce()
    }

    // Filtered products
    val filteredProducts = remember(searchQuery, drinkProducts, selectedCategoryFilter) {
        drinkProducts.filter { prod ->
            val searchMatch = searchQuery.isBlank() ||
                prod.title.contains(searchQuery, ignoreCase = true) ||
                (prod.description?.contains(searchQuery, ignoreCase = true) == true)
            val categoryMatch = selectedCategoryFilter == null || prod.category == selectedCategoryFilter
            searchMatch && categoryMatch
        }
    }

    // Group by category
    val categorizedProducts = remember(filteredProducts) {
        filteredProducts.groupBy { it.category }
    }

    // Cart totals
    val subtotal = remember(cartItems) {
        cartItems.sumOf { it.product.sellingPrice * it.quantity }
    }
    val totalItemCount = remember(cartItems) {
        cartItems.sumOf { it.quantity }
    }

    fun incrementCartItem(index: Int) {
        if (index in cartItems.indices) {
            cartItems = cartItems.toMutableList().apply {
                set(index, this[index].copy(quantity = this[index].quantity + 1))
            }
        }
    }

    fun decrementCartItem(index: Int) {
        if (index in cartItems.indices) {
            val q = cartItems[index].quantity
            if (q > 1) {
                cartItems = cartItems.toMutableList().apply {
                    set(index, this[index].copy(quantity = q - 1))
                }
            }
        }
    }

    fun removeFromCart(index: Int) {
        cartItems = cartItems.toMutableList().apply { removeAt(index) }
    }

    fun clearCart() {
        cartItems = emptyList()
    }

    // ── Checkout Success Screen ──
    if (checkoutData != null) {
        CheckoutSuccessScreen(
            checkoutData = checkoutData!!,
            onPrintReceipt = { /* TODO: Print receipt */ },
            onNewOrder = {
                cartItems = emptyList()
                checkoutData = null
            }
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ── Left: Product Catalog or Item Customization ──
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (selectedProduct != null) {
                ItemCustomizationSheet(
                    product = selectedProduct!!,
                    onBack = { selectedProduct = null },
                    onAddToOrder = { cartItem ->
                        cartItems = cartItems + cartItem
                        selectedProduct = null
                    }
                )
            } else {
                // Scrollable catalog area
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No drinks found."
                                else "No products available.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextMuted
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                        ) {
                            Spacer(modifier = Modifier.height(20.dp))

                            categorizedProducts.forEach { (category, products) ->
                                val sectionTitle = if (category == "Coffee") "COFFEE DRINKS" else "NON COFFEE DRINKS"
                                CategorySection(
                                    title = sectionTitle,
                                    products = products,
                                    onProductClick = { selectedProduct = it }
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // Global Rules Banner
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = DarkCard
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "All drinks are 12oz.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // ── Bottom Toolbar ──
                CatalogBottomToolbar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onCategoriesClick = { showCategoriesDialog = true },
                    onAddCustomItem = {
                        // Add custom item to cart
                    }
                )

                // Categories Dialog
                if (showCategoriesDialog) {
                    CategoriesDialog(
                        categories = dbCategories,
                        selectedCategory = selectedCategoryFilter,
                        onCategorySelected = { categoryName ->
                            selectedCategoryFilter = categoryName
                            showCategoriesDialog = false
                        },
                        onDismiss = { showCategoriesDialog = false }
                    )
                }
            }
        }

        // ── Right: Current Order Panel ──
        CurrentOrderPanel(
            cartItems = cartItems,
            totalItemCount = totalItemCount,
            subtotal = subtotal,
            heldOrdersCount = heldOrders.size,
            onRemoveItem = { removeFromCart(it) },
            onClearCart = { clearCart() },
            onHoldOrder = { holdOrder() },
            onViewHeldOrders = { showHeldOrdersDialog = true },
            onIncrementItem = { incrementCartItem(it) },
            onDecrementItem = { decrementCartItem(it) },
            onCheckout = { showPaymentDialog = true }
        )

        // Held Orders Dialog
        if (showHeldOrdersDialog) {
            HeldOrdersDialog(
                heldOrders = heldOrders,
                onRestoreOrder = { restoreHeldOrder(it) },
                onDeleteOrder = { deleteHeldOrder(it) },
                onDismiss = { showHeldOrdersDialog = false }
            )
        }

        // Payment Dialog
        if (showPaymentDialog) {
            PaymentDialog(
                total = subtotal,
                onConfirm = { amountTendered, customerName ->
                    showPaymentDialog = false

                    val itemsSummary = cartItems.joinToString(", ") { "${it.quantity} × ${it.product.title} (${it.size})" }
                    val totalItemCount = cartItems.sumOf { it.quantity }

                    // Save transaction to database
                    scope.launch {
                        repository.insertTransaction(
                            com.catchuppos.app.data.TransactionEntity(
                                customerName = customerName,
                                itemsJson = itemsSummary,
                                itemCount = totalItemCount,
                                total = subtotal,
                                amountTendered = amountTendered,
                                changeReturned = amountTendered - subtotal,
                                paymentMethod = "Cash",
                                status = "Completed",
                                transactionId = generateTransactionId(),
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }

                    checkoutData = CheckoutData(
                        items = cartItems.toList(),
                        subtotal = subtotal,
                        total = subtotal,
                        amountTendered = amountTendered,
                        changeReturned = amountTendered - subtotal,
                        transactionId = generateTransactionId(),
                        dateTime = formatDateTime(),
                        customerName = customerName
                    )
                },
                onDismiss = { showPaymentDialog = false }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Category Section (Coffee Drinks / Non Coffee Drinks)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun CategorySection(
    title: String,
    products: List<ProductEntity>,
    onProductClick: (ProductEntity) -> Unit
) {
    Column {
        // Section Header with orange underline
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        // Orange underline bar
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(3.dp)
                .background(OrangeAccent, RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Product grid — 5 columns for compact cards
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            products.chunked(5).forEach { rowProducts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowProducts.forEach { product ->
                        MenuItemCard(
                            product = product,
                            modifier = Modifier.weight(1f),
                            onClick = { onProductClick(product) }
                        )
                    }
                    repeat(5 - rowProducts.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Menu Item Card
// ════════════════════════════════════════════════════════════════════

@Composable
private fun MenuItemCard(
    product: ProductEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Product Thumbnail (image or plain initials)
            if (product.imagePath != null) {
                val bitmap = remember(product.imagePath) {
                    android.graphics.BitmapFactory.decodeFile(product.imagePath)
                }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = product.title,                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        text = product.title.take(2).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = OrangeAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = product.title.take(2).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = OrangeAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title (uppercase)
            Text(
                text = product.title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Price
            Text(
                text = "₱${String.format("%.0f", product.sellingPrice)}.00",
                style = MaterialTheme.typography.labelMedium,
                color = OrangeAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Catalog Bottom Toolbar
// ════════════════════════════════════════════════════════════════════

@Composable
private fun CatalogBottomToolbar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCategoriesClick: () -> Unit = {},
    onAddCustomItem: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search drink...", color = InputPlaceholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangeAccent,
                unfocusedBorderColor = InputBorder,
                cursorColor = OrangeAccent,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        // Categories Button
        OutlinedButton(
            onClick = onCategoriesClick,
            modifier = Modifier.height(46.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, DarkBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = DarkCard,
                contentColor = TextWhite
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.GridView,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = TextMuted
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Categories",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }

        // Add Custom Item Button
        OutlinedButton(
            onClick = onAddCustomItem,
            modifier = Modifier.height(46.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, DarkBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = DarkCard,
                contentColor = TextWhite
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = TextMuted
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Add Custom Item",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Current Order Panel (Right Sidebar)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun CurrentOrderPanel(
    cartItems: List<CartItem>,
    totalItemCount: Int,
    subtotal: Double,
    heldOrdersCount: Int = 0,
    onRemoveItem: (Int) -> Unit,
    onClearCart: () -> Unit,
    onHoldOrder: () -> Unit = {},
    onViewHeldOrders: () -> Unit = {},
    onIncrementItem: (Int) -> Unit = {},
    onDecrementItem: (Int) -> Unit = {},
    onCheckout: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        color = Color(0xFF0D0D0D),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CURRENT ORDER",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                // Item count badge
                if (totalItemCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MutedRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$totalItemCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

            if (cartItems.isEmpty()) {
                // Empty cart state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No items yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Text(
                            text = "Tap a product to add it",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
            } else {
                // ── Cart Items List ──
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(cartItems) { index, item ->
                        CartLineItem(
                            item = item,
                            onIncrement = { onIncrementItem(index) },
                            onDecrement = { onDecrementItem(index) },
                            onRemove = { onRemoveItem(index) }
                        )
                    }
                }
            }

            // ── Financial Summary ──
            if (cartItems.isNotEmpty()) {
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subtotal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Text(
                            text = "₱${String.format("%.2f", subtotal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.titleMedium,
                            color = OrangeAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₱${String.format("%.2f", subtotal)}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = OrangeAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
            }

            // ── Action Buttons ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Held Orders Indicator (if any)
                if (heldOrdersCount > 0) {
                    OutlinedButton(
                        onClick = onViewHeldOrders,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, OrangeAccent),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = OrangeAccent.copy(alpha = 0.08f),
                            contentColor = OrangeAccent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$heldOrdersCount Held Order${if (heldOrdersCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Hold Order
                OutlinedButton(
                    onClick = onHoldOrder,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DarkCard,
                        contentColor = TextWhite
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hold Order",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Cancel Order
                OutlinedButton(
                    onClick = onClearCart,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkRed),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DarkRed.copy(alpha = 0.2f),
                        contentColor = MutedRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancel Order",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Checkout
                Button(
                    onClick = onCheckout,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = TextWhite
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Checkout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Categories Dialog (for filter)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun CategoriesDialog(
    categories: List<com.catchuppos.app.data.CategoryEntity>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 340.dp, max = 400.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // ── Header ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Category",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Filter products by category",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── "All" option ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedCategory == null) OrangeAccent.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onCategorySelected(null) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(OrangeAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("All", fontSize = 14.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "All Items",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextWhite,
                        fontWeight = if (selectedCategory == null) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (selectedCategory == null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = OrangeAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // ── Category options from DB ──
                categories.forEach { cat ->
                    val isSelected = cat.name == selectedCategory
                    val bgColor = if (isSelected) parseColor(cat.iconColor).copy(alpha = 0.12f) else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable { onCategorySelected(cat.name) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(parseColor(cat.iconColor).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.iconChar,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = cat.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextWhite,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = OrangeAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Utility to parse hex color string ──

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFFFF6600)
    }
}

// ════════════════════════════════════════════════════════════════════
// Held Orders Dialog
// ════════════════════════════════════════════════════════════════════

@Composable
private fun HeldOrdersDialog(
    heldOrders: List<List<CartItem>>,
    onRestoreOrder: (Int) -> Unit,
    onDeleteOrder: (Int) -> Unit,
    onDismiss: () -> Unit
) {
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
                // ── Header ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Held Orders",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${heldOrders.size} order${if (heldOrders.size > 1) "s" else ""} parked",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                if (heldOrders.isEmpty()) {
                    Text(
                        text = "No held orders.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        heldOrders.forEachIndexed { index, order ->
                            val orderTotal = order.sumOf { it.product.sellingPrice * it.quantity }
                            val itemCount = order.sumOf { it.quantity }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = DarkCard
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Order details
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Order #${index + 1}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextWhite,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "$itemCount item${if (itemCount > 1) "s" else ""} • ₱${String.format("%.2f", orderTotal)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextGray
                                        )
                                        // Show first few item names
                                        val itemNames = order.take(3).joinToString(", ") { it.product.title }
                                        val more = if (order.size > 3) " +${order.size - 3} more" else ""
                                        Text(
                                            text = itemNames + more,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Restore button
                                    IconButton(
                                        onClick = { onRestoreOrder(index) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = "Restore",
                                            tint = StatusGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = { onDeleteOrder(index) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MutedRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // ── Done Button ──
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
// Item Customization Sheet
// ════════════════════════════════════════════════════════════════════

@Composable
private fun ItemCustomizationSheet(
    product: ProductEntity,
    onBack: () -> Unit,
    onAddToOrder: (CartItem) -> Unit
) {
    var quantity by remember { mutableStateOf(1) }
    var selectedSize by remember { mutableStateOf("16oz") }
    var sugarLevel by remember { mutableStateOf("100%") }
    var iceLevel by remember { mutableStateOf("Regular Ice") }
    var specialInstructions by remember { mutableStateOf("") }

    val sizes = listOf("12oz", "16oz", "22oz")
    val sugarOptions = listOf("0%", "25%", "50%", "75%", "100%")
    val iceOptions = listOf("No Ice", "Less Ice", "Regular Ice", "Extra Ice")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── Back to Menu Link ──
        TextButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = OrangeAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Back to Menu",
                color = OrangeAccent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Product Profile ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Product Image
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrangeAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (product.imagePath != null) {
                    val bitmap = remember(product.imagePath) {
                        android.graphics.BitmapFactory.decodeFile(product.imagePath)
                    }
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = product.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = product.title.take(2).uppercase(),
                            style = MaterialTheme.typography.displaySmall,
                            color = OrangeAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = product.title.take(2).uppercase(),
                        style = MaterialTheme.typography.displaySmall,
                        color = OrangeAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Product Info
            Column(modifier = Modifier.weight(1f)) {
                // Title with underline bar
                Text(
                    text = product.title.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(3.dp)
                        .padding(top = 4.dp)
                        .background(OrangeAccent, RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category
                val categoryLabel = if (product.category == "Coffee") "Coffee Drinks" else "Non Coffee Drinks"
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OrangeMuted,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Description
                Text(
                    text = product.description ?: "Rich and flavorful beverage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Price
                Text(
                    text = "₱${String.format("%.0f", product.sellingPrice)}.00",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OrangeAccent,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Size & Quantity Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Size selector
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        sizes.forEach { size ->
                            val isSelected = size == selectedSize
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedSize = size },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) DarkCard else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) OrangeAccent else DarkBorder
                                )
                            ) {
                                Text(
                                    text = size,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) OrangeAccent else TextMuted,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Quantity Controls
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCard
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "—",
                                    color = if (quantity > 1) TextWhite else TextGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Text(
                                text = "$quantity",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { quantity++ },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "+",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(20.dp))

        // ── Sugar Level ──
        Text(
            text = "⚗️ SUGAR LEVEL",
            style = MaterialTheme.typography.labelLarge,
            color = TextWhite,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sugarOptions.forEach { option ->
                val isSelected = option == sugarLevel
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { sugarLevel = option },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) OrangeAccent else DarkCard,
                    border = if (isSelected) null else BorderStroke(1.dp, DarkBorder)
                ) {
                    Text(
                        text = if (isSelected) "$option ✔" else option,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) TextWhite else TextMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Ice Level ──
        Text(
            text = "❄️ ICE LEVEL",
            style = MaterialTheme.typography.labelLarge,
            color = TextWhite,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            iceOptions.forEach { option ->
                val isSelected = option == iceLevel
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { iceLevel = option },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) OrangeAccent else DarkCard,
                    border = if (isSelected) null else BorderStroke(1.dp, DarkBorder)
                ) {
                    Text(
                        text = if (isSelected) "$option ✔" else option,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) TextWhite else TextMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Special Instructions ──
        Text(
            text = "📝 SPECIAL INSTRUCTIONS",
            style = MaterialTheme.typography.labelLarge,
            color = TextWhite,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        OutlinedTextField(
            value = specialInstructions,
            onValueChange = { specialInstructions = it },
            placeholder = { Text("Add a note... (e.g. less sweet, extra shot)", color = InputPlaceholder) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            minLines = 3,
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangeAccent,
                unfocusedBorderColor = InputBorder,
                cursorColor = OrangeAccent,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            )
        )

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Action Buttons ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = DarkCard,
                    contentColor = TextWhite
                )
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Add to Order
            Button(
                onClick = {
                    onAddToOrder(
                        CartItem(
                            product = product,
                            quantity = quantity,
                            size = selectedSize,
                            sugarLevel = sugarLevel,
                            iceLevel = iceLevel,
                            specialInstructions = specialInstructions
                        )
                    )
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = TextWhite
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add $quantity to Order",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Cart Line Item
// ════════════════════════════════════════════════════════════════════

@Composable
private fun CartLineItem(
    item: CartItem,
    onIncrement: () -> Unit = {},
    onDecrement: () -> Unit = {},
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product thumbnail
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(OrangeAccent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (item.product.imagePath != null) {
                val bitmap = remember(item.product.imagePath) {
                    android.graphics.BitmapFactory.decodeFile(item.product.imagePath)
                }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = item.product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        text = item.product.title.take(2).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OrangeAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = item.product.title.take(2).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OrangeAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Item name + customization
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.product.title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Customization details
            Text(
                text = "${item.size} • ${item.sugarLevel} Sugar • ${item.iceLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── [- 1 +] [x] Controls ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Quantity controls
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0D0D0D)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Minus
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clickable { onDecrement() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "-",
                                color = if (item.quantity > 1) TextWhite else TextGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        // Quantity
                        Text(
                            text = "${item.quantity}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        // Plus
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clickable { onIncrement() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Price
                Text(
                    text = "₱${String.format("%.2f", item.product.sellingPrice * item.quantity)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextWhite,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(4.dp))

                // X Remove button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "x",
                        color = MutedRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
