package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextAlign
import com.catchuppos.app.CatchUpApp
import com.catchuppos.app.data.ProductEntity
import com.catchuppos.app.data.ProductVariantEntity
import com.catchuppos.app.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- Data Models ---

data class ProductVariantUI(
    val sizeName: String,
    val sellingPrice: Double,
    val costPrice: Double? = null,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

enum class ProductCategory(val displayName: String) {
    ALL("All"),
    COFFEE("Coffee"),
    NON_COFFEE("Non Coffee"),
    FOOD("Food")
}

data class Product(
    val id: Int,
    val title: String,
    val description: String? = null,
    val price: Double,
    val category: ProductCategory,
    val isActive: Boolean = true,
    val section: ProductSection,
    val imagePath: String? = null,
    val variants: List<ProductVariantUI> = emptyList()
)

enum class ProductSection(val displayName: String) {
    DRINKS("DRINKS"),
    FOOD("FOOD")
}

// --- Mapper ---

private fun ProductEntity.toUiProduct(): Product {
    val category = when (category) {
        "Coffee" -> ProductCategory.COFFEE
        "Non Coffee" -> ProductCategory.NON_COFFEE
        else -> ProductCategory.FOOD
    }
    val section = when (type) {
        "FOOD" -> ProductSection.FOOD
        else -> ProductSection.DRINKS
    }
    return Product(
        id = id,
        title = title,
        description = description,
        price = sellingPrice,
        category = category,
        isActive = isActive,
        section = section,
        imagePath = imagePath
    )
}

// --- Main Screen ---

@Composable
fun ProductsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val repository = app.productRepository
    val scope = rememberCoroutineScope()

    var showAddForm by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedCategory by remember { mutableStateOf(ProductCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableStateOf(0) }
    var dbProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    val pageSize = 8

    suspend fun refreshProducts() {
        val entities = repository.allProductsOnce()
        dbProducts = entities.map { entity ->
            val variants = repository.getVariantsByProductIdOnce(entity.id).map {
                ProductVariantUI(it.sizeName, it.sellingPrice, it.costPrice, it.isDefault, it.sortOrder)
            }
            entity.toUiProduct().copy(variants = variants)
        }
    }

    LaunchedEffect(Unit) {
        refreshProducts()
    }

    if (showAddForm || editingProduct != null) {
        AddProductScreen(
            editingProduct = editingProduct,
            onBack = {
                showAddForm = false
                editingProduct = null
            },
            onSave = { entity, variants ->
                scope.launch {
                    val productId = repository.insertProduct(entity).toInt()
                    if (variants.isNotEmpty()) {
                        repository.insertVariants(variants.map { v ->
                            ProductVariantEntity(
                                productId = productId,
                                sizeName = v.sizeName,
                                sellingPrice = v.sellingPrice,
                                costPrice = v.costPrice,
                                isDefault = v.isDefault,
                                sortOrder = v.sortOrder
                            )
                        })
                    }
                    refreshProducts()
                    showAddForm = false
                }
            },
            onUpdate = { entity, variants ->
                scope.launch {
                    repository.updateProduct(entity)
                    repository.deleteVariantsByProductId(entity.id)
                    if (variants.isNotEmpty()) {
                        repository.insertVariants(variants.map { v ->
                            ProductVariantEntity(
                                productId = entity.id,
                                sizeName = v.sizeName,
                                sellingPrice = v.sellingPrice,
                                costPrice = v.costPrice,
                                isDefault = v.isDefault,
                                sortOrder = v.sortOrder
                            )
                        })
                    }
                    refreshProducts()
                    editingProduct = null
                }
            },
            onCopyVariants = { productId, variants ->
                scope.launch {
                    repository.deleteVariantsByProductId(productId)
                    repository.insertVariants(variants.map { v ->
                        ProductVariantEntity(
                            productId = productId,
                            sizeName = v.sizeName,
                            sellingPrice = v.sellingPrice,
                            costPrice = v.costPrice,
                            isDefault = v.isDefault,
                            sortOrder = v.sortOrder
                        )
                    })
                    refreshProducts()
                    editingProduct = null
                }
            }
        )
    } else {
        val filteredProducts = remember(selectedCategory, searchQuery, dbProducts) {
            dbProducts.filter { product ->
                val categoryMatch = selectedCategory == ProductCategory.ALL || product.category == selectedCategory
                val searchMatch = searchQuery.isBlank() ||
                        product.title.contains(searchQuery, ignoreCase = true) ||
                        (product.description?.contains(searchQuery, ignoreCase = true) == true)
                categoryMatch && searchMatch
            }
        }

        val totalPages = (filteredProducts.size + pageSize - 1) / pageSize
        val pagedProducts = filteredProducts
            .drop(currentPage * pageSize)
            .take(pageSize)

        Column(
            modifier = modifier
                .fillMaxSize()
        ) {
            // --- Top Controls: Category Pills + Search + Add Button ---
            ProductsTopBar(
                selectedCategory = selectedCategory,
                onCategorySelected = {
                    selectedCategory = it
                    currentPage = 0
                },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onAddProduct = { showAddForm = true }
            )

            // --- Product Grid Area ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            ) {
                if (pagedProducts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No products found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Group products by section
                        val sections = pagedProducts.groupBy { it.section }
                        sections.forEach { (section, products) ->
                            // Section Header
                            Text(
                                text = section.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Product Cards Grid (5 columns)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                products.chunked(5).forEach { rowProducts ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowProducts.forEach { product ->
                                            ProductCard(
                                                product = product,
                                                modifier = Modifier.weight(1f),
                                                onDelete = {
                                                    scope.launch {
                                                        repository.deleteProductById(product.id)
                                                        refreshProducts()
                                                    }
                                                },
                                                onEdit = {
                                                    scope.launch {
                                                        val entity = repository.getProductById(product.id)
                                                        if (entity != null) {
                                                            editingProduct = entity
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        // Fill empty space if less than 5
                                        repeat(5 - rowProducts.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // --- Footer Pagination ---
            if (filteredProducts.isNotEmpty()) {
                ProductPagination(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    pageSize = pageSize,
                    totalItems = filteredProducts.size,
                    onPageChange = { currentPage = it }
                )
            }
        }
    }
}

// --- Top Bar: Category Pills + Search + Add Button ---

@Composable
private fun ProductsTopBar(
    selectedCategory: ProductCategory,
    onCategorySelected: (ProductCategory) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddProduct: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Row 1: Category Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ProductCategory.entries) { category ->
                CategoryPill(
                    label = category.displayName,
                    isSelected = category == selectedCategory,
                    onClick = { onCategorySelected(category) }
                )
            }
        }

        // Row 2: Search + Add Product
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search product...",
                        color = InputPlaceholder
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeAccent,
                    unfocusedBorderColor = InputBorder,
                    cursorColor = OrangeAccent,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            // Add Product Button
            Button(
                onClick = onAddProduct,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = TextWhite
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add Product",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- Category Pill ---

@Composable
private fun CategoryPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) OrangeAccent else Color(0xFF1A1A1A),
        border = if (isSelected) null else BorderStroke(1.dp, DarkBorder)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) TextWhite else TextMuted,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// --- Product Card ---

@Composable
private fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box {
            // More Actions Menu Button (⋮)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .padding(top = 2.dp, end = 2.dp)
                    .clickable { showMenu = true }
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More actions",
                    tint = TextMuted,
                    modifier = Modifier.size(12.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit", color = TextWhite) },
                    onClick = {
                        showMenu = false
                        onEdit()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MutedRed) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MutedRed, modifier = Modifier.size(18.dp))
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Product Image or Initials
                if (product.imagePath != null) {
                    val imageBitmap = remember(product.imagePath) {
                        android.graphics.BitmapFactory.decodeFile(product.imagePath)
                    }
                    if (imageBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = imageBitmap.asImageBitmap(),
                            contentDescription = product.title,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
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

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                // Description (conditional)
                if (product.description != null) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Price
                val priceText = if (product.variants.size > 1) {
                    val minPrice = product.variants.minOf { it.sellingPrice }
                    val maxPrice = product.variants.maxOf { it.sellingPrice }
                    "₱${String.format("%.0f", minPrice)}-${String.format("%.0f", maxPrice)}"
                } else {
                    "₱${String.format("%.2f", product.price)}"
                }
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.labelMedium,
                    color = OrangeAccent,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(1.dp))

                // Status
                Text(
                    text = if (product.isActive) "Active" else "Inactive",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.isActive) StatusGreen else TextGray,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// --- Footer Pagination ---

@Composable
private fun ProductPagination(
    currentPage: Int,
    totalPages: Int,
    pageSize: Int,
    totalItems: Int,
    onPageChange: (Int) -> Unit
) {
    val startIndex = currentPage * pageSize + 1
    val endIndex = minOf((currentPage + 1) * pageSize, totalItems)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0D0D0D)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status Readout
            Text(
                text = "Showing $startIndex to $endIndex of $totalItems products",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            // Navigation Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Arrow
                IconButton(
                    onClick = { if (currentPage > 0) onPageChange(currentPage - 1) },
                    enabled = currentPage > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous",
                        tint = if (currentPage > 0) TextWhite else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Page Numbers
                val visiblePages = generateVisiblePages(currentPage, totalPages)
                visiblePages.forEach { page ->
                    val isCurrent = page == currentPage
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isCurrent) OrangeAccent else Color.Transparent
                            )
                            .clickable { onPageChange(page) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${page + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCurrent) TextWhite else TextMuted,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Next Arrow
                IconButton(
                    onClick = { if (currentPage < totalPages - 1) onPageChange(currentPage + 1) },
                    enabled = currentPage < totalPages - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = if (currentPage < totalPages - 1) TextWhite else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun generateVisiblePages(currentPage: Int, totalPages: Int): List<Int> {
    if (totalPages <= 3) return (0 until totalPages).toList()
    return when (currentPage) {
        0 -> listOf(0, 1, 2)
        totalPages - 1 -> listOf(totalPages - 3, totalPages - 2, totalPages - 1)
        else -> listOf(currentPage - 1, currentPage, currentPage + 1)
    }
}

// ════════════════════════════════════════════════════════════════════
// Category Selection Dialog
// ════════════════════════════════════════════════════════════════════

private data class CategoryOption(
    val label: String,
    val productCategory: ProductCategory,
    val iconColor: Color,
    val iconChar: String,
    val autoType: ProductType
)

private val categoryOptions = listOf(
    CategoryOption("Coffee", ProductCategory.COFFEE, Color(0xFF6D4C41), "☕", ProductType.DRINK),
    CategoryOption("Non Coffee", ProductCategory.NON_COFFEE, Color(0xFFF48FB1), "🧋", ProductType.DRINK),
    CategoryOption("Food", ProductCategory.FOOD, Color(0xFFFF6600), "🛎️", ProductType.FOOD),
    CategoryOption("Add-Ons", ProductCategory.ALL, Color(0xFF9C27B0), "+", ProductType.DRINK),
    CategoryOption("Merchandise", ProductCategory.ALL, Color(0xFF388E3C), "🛍️", ProductType.DRINK)
)

@Composable
private fun CategorySelectionDialog(
    selectedLabel: String,
    onCategorySelected: (ProductCategory, ProductType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 380.dp, max = 420.dp)
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
                            text = "Choose a category for this product",
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

                // ── Category Options ──
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categoryOptions.forEach { option ->
                        val isSelected = option.label == selectedLabel
                        val bgColor = if (isSelected) option.iconColor.copy(alpha = 0.12f) else Color.Transparent

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .clickable {
                                    onCategorySelected(option.productCategory, option.autoType)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon Box
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(option.iconColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.iconChar,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Label
                            Text(
                                text = option.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextWhite,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )

                            // Arrow
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // ── Add New Category Link ──
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = { /* TODO: Add new category */ }) {
                        Text(
                            text = "+ Add New Category",
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
// Add Item Dialog (reusable for sizes & add-ons)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun AddListItemDialog(
    title: String,
    placeholder: String,
    existingItems: List<String>,
    onAddItem: (String) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 360.dp, max = 400.dp)
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
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
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

                // ── Input Field ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(placeholder, color = InputPlaceholder) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (inputText.isNotBlank()) {
                                    onAddItem(inputText.trim())
                                    inputText = ""
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        colors = formFieldColors()
                    )

                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onAddItem(inputText.trim())
                                inputText = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeAccent,
                            contentColor = TextWhite
                        ),
                        enabled = inputText.isNotBlank()
                    ) {
                        Text(
                            text = "Add",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Added Items List ──
                if (existingItems.isEmpty()) {
                    Text(
                        text = "No items added yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        existingItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkCard)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhite
                                )
                                IconButton(
                                    onClick = { onRemoveItem(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MutedRed,
                                        modifier = Modifier.size(16.dp)
                                    )
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

// ── Added Items Chips Row ──

@Composable
private fun AddedItemsChips(items: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkCard
            ) {
                Text(
                    text = item,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextWhite
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Size Variant Editor Dialog
// ════════════════════════════════════════════════════════════════════

private val defaultSizes = listOf("12oz", "16oz", "22oz", "Large")

@Composable
private fun SizeVariantEditorDialog(
    selectedVariants: List<ProductVariantUI>,
    onVariantsSelected: (List<ProductVariantUI>) -> Unit,
    onDismiss: () -> Unit
) {
    var customSizeInput by remember { mutableStateOf("") }
    var customPriceInput by remember { mutableStateOf("") }
    var tempVariants by remember { mutableStateOf(selectedVariants.toMutableList()) }
    var editingIndex by remember { mutableIntStateOf(-1) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 440.dp, max = 500.dp)
                .wrapContentHeight()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sizes & Prices", style = MaterialTheme.typography.titleLarge, color = TextWhite, fontWeight = FontWeight.Bold)
                        Text("Add sizes and set the price for each", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick-add predefined sizes
                Text("QUICK ADD", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    defaultSizes.forEach { size ->
                        val exists = tempVariants.any { it.sizeName == size }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !exists) {
                                    val basePrice = tempVariants.firstOrNull()?.sellingPrice ?: 85.0
                                    tempVariants = tempVariants + ProductVariantUI(
                                        sizeName = size,
                                        sellingPrice = basePrice + when (size) {
                                            "16oz" -> 10.0
                                            "22oz" -> 20.0
                                            "Large" -> 15.0
                                            else -> 0.0
                                        },
                                        isDefault = tempVariants.isEmpty(),
                                        sortOrder = tempVariants.size
                                    )
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (exists) DarkCard.copy(alpha = 0.5f) else DarkCard,
                            border = BorderStroke(1.dp, if (exists) DarkBorder else DarkBorder)
                        ) {
                            Text(
                                text = size,
                                modifier = Modifier.padding(vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (exists) TextGray else TextMuted,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Custom size input with price
                Text("ADD CUSTOM SIZE", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customSizeInput,
                        onValueChange = { customSizeInput = it },
                        placeholder = { Text("Size name", color = InputPlaceholder) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = formFieldColors()
                    )
                    OutlinedTextField(
                        value = customPriceInput,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) customPriceInput = it },
                        placeholder = { Text("₱ Price", color = InputPlaceholder) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        prefix = { Text("₱", color = TextWhite, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = formFieldColors()
                    )
                    Button(
                        onClick = {
                            val price = customPriceInput.toDoubleOrNull() ?: 0.0
                            if (customSizeInput.isNotBlank() && tempVariants.none { it.sizeName == customSizeInput.trim() }) {
                                tempVariants = tempVariants + ProductVariantUI(
                                    sizeName = customSizeInput.trim(),
                                    sellingPrice = price,
                                    isDefault = tempVariants.isEmpty(),
                                    sortOrder = tempVariants.size
                                )
                                customSizeInput = ""
                                customPriceInput = ""
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = TextWhite),
                        enabled = customSizeInput.isNotBlank()
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Variants list with editable prices
                if (tempVariants.isNotEmpty()) {
                    Text("VARIANTS (${tempVariants.size})", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tempVariants.forEachIndexed { index, variant ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkCard)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Default indicator
                                if (variant.isDefault) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = OrangeAccent) {
                                        Text("DEF", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = TextWhite, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Size name
                                Text(variant.sizeName, style = MaterialTheme.typography.bodyMedium, color = TextWhite, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))

                                // Editable price
                                if (editingIndex == index) {
                                    OutlinedTextField(
                                        value = String.format("%.2f", variant.sellingPrice),
                                        onValueChange = { newVal ->
                                            if (newVal.isEmpty() || newVal.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                                tempVariants = tempVariants.toMutableList().apply {
                                                    this[index] = this[index].copy(sellingPrice = newVal.toDoubleOrNull() ?: 0.0)
                                                }
                                            }
                                        },
                                        modifier = Modifier.width(100.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true,
                                        prefix = { Text("₱", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(color = TextWhite),
                                        colors = formFieldColors()
                                    )
                                    IconButton(onClick = { editingIndex = -1 }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Check, contentDescription = "Done", tint = StatusGreen, modifier = Modifier.size(16.dp))
                                    }
                                } else {
                                    Surface(
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { editingIndex = index },
                                        shape = RoundedCornerShape(6.dp),
                                        color = DarkCard
                                    ) {
                                        Text(
                                            text = "₱${String.format("%.2f", variant.sellingPrice)}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = OrangeAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Set as default
                                IconButton(onClick = {
                                    tempVariants = tempVariants.toMutableList().apply {
                                        for (i in indices) { this[i] = this[i].copy(isDefault = i == index) }
                                    }
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = if (variant.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Set default",
                                        tint = if (variant.isDefault) OrangeAccent else TextGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Remove
                                IconButton(onClick = {
                                    tempVariants = tempVariants.toMutableList().apply { removeAt(index) }
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MutedRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = {
                        onVariantsSelected(tempVariants.toList())
                        onDismiss()
                    }) {
                        Text("Done", color = OrangeAccent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Add-on Selection Dialog
// ════════════════════════════════════════════════════════════════════

private val defaultAddOns = listOf("Extra Shot", "Pearl", "Cheese", "Whipped Cream", "Vanilla Syrup", "Caramel Syrup")

@Composable
private fun AddOnSelectionDialog(
    selectedAddOns: List<String>,
    onAddOnsSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var customAddOnInput by remember { mutableStateOf("") }
    var tempSelected by remember { mutableStateOf(selectedAddOns.toList()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 380.dp, max = 420.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Select Add-ons", style = MaterialTheme.typography.titleLarge, color = TextWhite, fontWeight = FontWeight.Bold)
                        Text("Tap to select add-ons for this product", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("PREDEFINED ADD-ONS", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    defaultAddOns.take(3).forEach { addOn ->
                        val isSelected = addOn in tempSelected
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    tempSelected = if (isSelected) tempSelected.filter { it != addOn }
                                    else tempSelected + addOn
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF9C27B0) else DarkCard,
                            border = if (isSelected) null else BorderStroke(1.dp, DarkBorder)
                        ) {
                            Text(
                                text = addOn,
                                modifier = Modifier.padding(vertical = 10.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) TextWhite else TextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    defaultAddOns.drop(3).forEach { addOn ->
                        val isSelected = addOn in tempSelected
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    tempSelected = if (isSelected) tempSelected.filter { it != addOn }
                                    else tempSelected + addOn
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF9C27B0) else DarkCard,
                            border = if (isSelected) null else BorderStroke(1.dp, DarkBorder)
                        ) {
                            Text(
                                text = addOn,
                                modifier = Modifier.padding(vertical = 10.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) TextWhite else TextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text("ADD CUSTOM ADD-ON", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customAddOnInput,
                        onValueChange = { customAddOnInput = it },
                        placeholder = { Text("e.g. Oat Milk, Honey", color = InputPlaceholder) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = formFieldColors()
                    )
                    Button(
                        onClick = {
                            if (customAddOnInput.isNotBlank() && customAddOnInput !in tempSelected) {
                                tempSelected = tempSelected + customAddOnInput.trim()
                                customAddOnInput = ""
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0), contentColor = TextWhite),
                        enabled = customAddOnInput.isNotBlank()
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (tempSelected.isNotEmpty()) {
                    Text("SELECTED (${tempSelected.size})", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tempSelected.forEach { addOn ->
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF9C27B0).copy(alpha = 0.15f), border = BorderStroke(1.dp, Color(0xFF9C27B0))) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(addOn, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9C27B0), fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF9C27B0), modifier = Modifier.size(14.dp).clickable { tempSelected = tempSelected.filter { it != addOn } })
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = {
                        onAddOnsSelected(tempSelected)
                        onDismiss()
                    }) {
                        Text("Done", color = OrangeAccent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Add Product Screen
// ════════════════════════════════════════════════════════════════════

private enum class ProductType { DRINK, FOOD }

@Composable
private fun AddProductScreen(
    editingProduct: ProductEntity? = null,
    onBack: () -> Unit,
    onSave: (ProductEntity, List<ProductVariantUI>) -> Unit = { _, _ -> },
    onUpdate: (ProductEntity, List<ProductVariantUI>) -> Unit = { _, _ -> },
    onCopyVariants: (Int, List<ProductVariantUI>) -> Unit = { _, _ -> }
) {
    val isEditing = editingProduct != null

    // ── Map editing entity to initial values ──
    val initialCategory = when (editingProduct?.category) {
        "Coffee" -> ProductCategory.COFFEE
        "Non Coffee" -> ProductCategory.NON_COFFEE
        else -> ProductCategory.FOOD
    }
    val initialType = if (editingProduct?.type == "FOOD") ProductType.FOOD else ProductType.DRINK

    // ── Form State (keyed by editingProduct to reset when switching products) ──
    var productName by remember(editingProduct) { mutableStateOf(editingProduct?.title ?: "") }
    var selectedCategory by remember(editingProduct) { mutableStateOf(initialCategory) }
    var productType by remember(editingProduct) { mutableStateOf(initialType) }
    var description by remember(editingProduct) { mutableStateOf(editingProduct?.description ?: "") }
    var isActive by remember(editingProduct) { mutableStateOf(editingProduct?.isActive ?: true) }

    var sellingPrice by remember(editingProduct) { mutableStateOf(editingProduct?.sellingPrice?.let { String.format("%.2f", it) } ?: "0.00") }
    var costPrice by remember(editingProduct) { mutableStateOf(editingProduct?.costPrice?.let { String.format("%.2f", it) } ?: "0.00") }

    var trackInventory by remember(editingProduct) { mutableStateOf(editingProduct?.trackInventory ?: false) }
    var quantity by remember(editingProduct) { mutableStateOf((editingProduct?.quantity ?: 0).toString()) }
    var lowStockThreshold by remember(editingProduct) { mutableStateOf((editingProduct?.lowStockThreshold ?: 5).toString()) }
    var selectedUnit by remember(editingProduct) { mutableStateOf(editingProduct?.unit ?: "pcs") }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    var variants by remember(editingProduct) { mutableStateOf<List<ProductVariantUI>>(emptyList()) }
    var addOns by remember(editingProduct) {
        mutableStateOf(
            editingProduct?.addOnsJson?.let { json ->
                try { json.removePrefix("[").removeSuffix("]").split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotBlank() } }
                catch (e: Exception) { emptyList() }
            } ?: emptyList()
        )
    }
    var showVariantDialog by remember { mutableStateOf(false) }
    var showAddOnDialog by remember { mutableStateOf(false) }

    // Load variants from DB when editing
    val addProductContext = LocalContext.current
    val addProductApp = addProductContext.applicationContext as CatchUpApp
    val addProductRepository = addProductApp.productRepository
    LaunchedEffect(editingProduct) {
        if (editingProduct != null) {
            variants = addProductRepository.getVariantsByProductIdOnce(editingProduct!!.id).map {
                ProductVariantUI(it.sizeName, it.sellingPrice, it.costPrice, it.isDefault, it.sortOrder)
            }
        }
    }

    var imageUri by remember(editingProduct) { mutableStateOf<android.net.Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
    }

    val saveScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Back Link ──
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = OrangeAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Back to Products",
                color = OrangeAccent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }

        // ── Header ──
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isEditing) "Edit Product" else "Add New Product",
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isEditing) "Update the details of your product below." else "Fill in the details below to add a new product to your menu.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Two-Column Form Layout ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Left Column ──
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Product Information
                FormSection(
                    sectionNumber = 1,
                    title = "Product Information"
                ) {
                    // Product Name
                    FormFieldLabel(
                        label = "Product Name",
                        required = true
                    )
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        placeholder = { Text("Enter product name", color = InputPlaceholder) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = formFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category
                    FormFieldLabel(
                        label = "Category",
                        required = true
                    )
                    Box {
                        OutlinedTextField(
                            value = selectedCategory.displayName,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Select category", color = InputPlaceholder) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = formFieldColors(),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select category",
                                    tint = TextMuted
                                )
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showCategoryDialog = true }
                        )
                    }

                    // Category Dialog
                    if (showCategoryDialog) {
                        CategorySelectionDialog(
                            selectedLabel = selectedCategory.displayName,
                            onCategorySelected = { category, type ->
                                selectedCategory = category
                                productType = type
                                showCategoryDialog = false
                            },
                            onDismiss = { showCategoryDialog = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Type
                    FormFieldLabel(
                        label = "Type",
                        required = true
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = productType == ProductType.DRINK,
                                onClick = { productType = ProductType.DRINK },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = OrangeAccent,
                                    unselectedColor = TextMuted
                                )
                            )
                            Text("Drink", color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = productType == ProductType.FOOD,
                                onClick = { productType = ProductType.FOOD },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = OrangeAccent,
                                    unselectedColor = TextMuted
                                )
                            )
                            Text("Food", color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description (Optional)
                    FormFieldLabel(label = "Description (Optional)")
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Enter product description (optional)", color = InputPlaceholder) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 3,
                        maxLines = 5,
                        colors = formFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status
                    FormFieldLabel(label = "Status")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isActive) "Active" else "Inactive",
                            color = if (isActive) StatusGreen else TextGray,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextWhite,
                                checkedTrackColor = OrangeAccent,
                                uncheckedThumbColor = TextGray,
                                uncheckedTrackColor = DarkBorder
                            )
                        )
                    }
                }

                // Section 4: Additional Options
                FormSection(
                    sectionNumber = 4,
                    title = "Additional Options (Optional)"
                ) {
                    // Size / Variant
                    FormFieldLabel(label = "Sizes & Prices")
                    TextButton(onClick = { showVariantDialog = true }) {
                        Text(
                            text = if (variants.isEmpty()) "+ Add Sizes & Prices" else "Edit Sizes (${variants.size})",
                            color = OrangeAccent,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    if (variants.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            variants.forEach { v ->
                                Surface(shape = RoundedCornerShape(8.dp), color = OrangeAccent.copy(alpha = 0.15f), border = BorderStroke(1.dp, OrangeAccent)) {
                                    Text(
                                        text = "${v.sizeName} ₱${String.format("%.0f", v.sellingPrice)}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OrangeAccent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "e.g. 12oz ₱85, 16oz ₱95, 22oz ₱105",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Variant Editor Dialog
                    if (showVariantDialog) {
                        SizeVariantEditorDialog(
                            selectedVariants = variants,
                            onVariantsSelected = { variants = it },
                            onDismiss = { showVariantDialog = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Add-ons
                    FormFieldLabel(label = "Add-ons")
                    TextButton(onClick = { showAddOnDialog = true }) {
                        Text(
                            text = if (addOns.isEmpty()) "+ Add Add-on" else "Edit Add-ons (${addOns.size})",
                            color = OrangeAccent,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    if (addOns.isNotEmpty()) {
                        AddedItemsChips(items = addOns)
                    } else {
                        Text(
                            text = "e.g. Extra Shot, Pearl, Cheese",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Add-on Dialog
                    if (showAddOnDialog) {
                        AddOnSelectionDialog(
                            selectedAddOns = addOns,
                            onAddOnsSelected = { addOns = it },
                            onDismiss = { showAddOnDialog = false }
                        )
                    }
                }
            }

            // ── Right Column ──
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 2: Pricing & Cost
                FormSection(
                    sectionNumber = 2,
                    title = "Pricing & Cost"
                ) {
                    // Selling Price
                    FormFieldLabel(
                        label = "Selling Price (₱)",
                        required = true
                    )
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { newVal ->
                            if (newVal.isEmpty() || newVal.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                sellingPrice = newVal
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        prefix = { Text("₱", color = TextWhite, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = formFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cost Price
                    FormFieldLabel(label = "Cost Price (₱) (Optional)")
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { newVal ->
                            if (newVal.isEmpty() || newVal.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                costPrice = newVal
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        prefix = { Text("₱", color = TextWhite, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = formFieldColors()
                    )
                }

                // Section 3: Product Image
                FormSection(
                    sectionNumber = 3,
                    title = "Product Image"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Dropzone
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkCard)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = "Upload",
                                    tint = TextMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Click to upload or drag & drop",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "800x800px | Max 2MB (JPG, PNG)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
                            }
                        }

                        // Preview
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkCard),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null) {
                                val context = LocalContext.current
                                val bitmap = remember(imageUri) {
                                    try {
                                        context.contentResolver.openInputStream(imageUri!!)?.use { stream ->
                                            android.graphics.BitmapFactory.decodeStream(stream)
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = "Preview",
                                        tint = TextGray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = "Preview",
                                    tint = TextGray,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                // Section 5: Inventory
                FormSection(
                    sectionNumber = 5,
                    title = "Inventory"
                ) {
                    // Track Inventory
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Track Inventory",
                            color = TextWhite,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (trackInventory) "Yes" else "No",
                                color = if (trackInventory) StatusGreen else TextGray,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Switch(
                                checked = trackInventory,
                                onCheckedChange = { trackInventory = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextWhite,
                                    checkedTrackColor = OrangeAccent,
                                    uncheckedThumbColor = TextGray,
                                    uncheckedTrackColor = DarkBorder
                                )
                            )
                        }
                    }

                    if (trackInventory) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                FormFieldLabel(label = "Quantity")
                                OutlinedTextField(
                                    value = quantity,
                                    onValueChange = { newVal ->
                                        if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                                            quantity = newVal
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = formFieldColors()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                FormFieldLabel(label = "Low Stock Threshold")
                                OutlinedTextField(
                                    value = lowStockThreshold,
                                    onValueChange = { newVal ->
                                        if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                                            lowStockThreshold = newVal
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = formFieldColors()
                                )
                            }
                        }

                        Text(
                            text = "You'll be notified when stock reaches this number",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Unit
                        FormFieldLabel(label = "Unit")
                        Box {
                            OutlinedTextField(
                                value = selectedUnit,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (unitExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Select unit",
                                        tint = TextMuted
                                    )
                                },
                                colors = formFieldColors()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { unitExpanded = true }
                            )
                            DropdownMenu(
                                expanded = unitExpanded,
                                onDismissRequest = { unitExpanded = false }
                            ) {
                                listOf("pcs", "cups", "plates", "bowls", "bottles").forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit, color = TextWhite) },
                                        onClick = {
                                            selectedUnit = unit
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Text(
                            text = "e.g. pcs, cups, plates",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = DarkBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Submission Actions Footer ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel Button
            Button(
                onClick = onBack,
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A1A),
                    contentColor = TextWhite
                ),
                border = BorderStroke(1.dp, DarkBorder),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Save Product Button
            var isSaving by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    isSaving = true
                    saveScope.launch {
                        val savedImagePath = if (imageUri != null) {
                            withContext(Dispatchers.IO) {
                                saveImageToInternalStorage(context, imageUri!!)
                            }
                        } else if (isEditing) {
                            editingProduct?.imagePath
                        } else null

                        val defaultVariantPrice = variants.firstOrNull { it.isDefault }?.sellingPrice
                            ?: variants.firstOrNull()?.sellingPrice
                            ?: (sellingPrice.toDoubleOrNull() ?: 0.0)

                        val entity = ProductEntity(
                            id = editingProduct?.id ?: 0,
                            title = productName.ifBlank { "New Product" },
                            description = description.ifBlank { null },
                            category = selectedCategory.displayName,
                            type = if (productType == ProductType.DRINK) "DRINK" else "FOOD",
                            sellingPrice = defaultVariantPrice,
                            costPrice = costPrice.toDoubleOrNull()?.takeIf { it > 0 },
                            isActive = isActive,
                            trackInventory = trackInventory,
                            quantity = quantity.toIntOrNull() ?: 0,
                            lowStockThreshold = lowStockThreshold.toIntOrNull() ?: 5,
                            unit = selectedUnit,
                            imagePath = savedImagePath,
                            addOnsJson = if (addOns.isNotEmpty()) "[${addOns.joinToString(",") { "\"$it\"" }}]" else null
                        )
                        if (isEditing) {
                            onUpdate(entity, variants)
                        } else {
                            onSave(entity, variants)
                        }
                        isSaving = false
                    }
                },
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = TextWhite
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = TextWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEditing) "Update Product" else "Save Product",
                    fontWeight = FontWeight.Bold
                )
            }

            // Save Variants Button (only when editing)
            if (isEditing && variants.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                var isSavingVariants by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = {
                        isSavingVariants = true
                        saveScope.launch {
                            onCopyVariants(editingProduct!!.id, variants)
                            isSavingVariants = false
                        }
                    },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, OrangeAccent),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OrangeAccent
                    ),
                    enabled = !isSavingVariants
                ) {
                    if (isSavingVariants) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = OrangeAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save Variants",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Form Section Wrapper ──

@Composable
private fun FormSection(
    sectionNumber: Int,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section Header with badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(OrangeAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$sectionNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

// ── Form Field Label ──

@Composable
private fun FormFieldLabel(
    label: String,
    required: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextWhite,
            fontWeight = FontWeight.Medium
        )
        if (required) {
            Text(
                text = " *",
                color = Color(0xFFE53935),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}

// ── Image Save Helper ──

private fun saveImageToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val imagesDir = java.io.File(context.filesDir, "product_images")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val fileName = "product_${System.currentTimeMillis()}.jpg"
        val file = java.io.File(imagesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

// ── Shared Form Field Colors ──

@Composable
private fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OrangeAccent,
    unfocusedBorderColor = InputBorder,
    cursorColor = OrangeAccent,
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedLabelColor = OrangeAccent,
    unfocusedLabelColor = TextMuted
)
