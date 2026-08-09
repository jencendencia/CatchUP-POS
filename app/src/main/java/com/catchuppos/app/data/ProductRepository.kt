package com.catchuppos.app.data

import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val variantDao: ProductVariantDao,
    private val orderItemDao: OrderItemDao,
    private val expenseDao: ExpenseDao
) {

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    // ── Products ──

    suspend fun getProductById(id: Int): ProductEntity? = productDao.getProductById(id)

    fun getProductByIdFlow(id: Int): Flow<ProductEntity?> = productDao.getProductByIdFlow(id)

    suspend fun insertProduct(product: ProductEntity): Long = productDao.insertProduct(product)

    suspend fun updateProduct(product: ProductEntity) = productDao.updateProduct(product)

    suspend fun deleteProduct(product: ProductEntity) = productDao.deleteProduct(product)

    suspend fun deleteProductById(id: Int) = productDao.deleteProductById(id)

    suspend fun searchProducts(category: String? = null, searchQuery: String? = null): List<ProductEntity> =
        productDao.searchProducts(category, searchQuery)

    suspend fun allProductsOnce(): List<ProductEntity> = productDao.getAllProductsOnce()

    suspend fun getProductCount(): Int = productDao.getProductCount()

    suspend fun getTotalCupsAvailable(): Int = productDao.getTotalCupsAvailable()

    // ── Categories ──

    suspend fun allCategoriesOnce(): List<CategoryEntity> = categoryDao.getAllCategoriesOnce()

    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)

    suspend fun deleteCategoryById(id: Int) = categoryDao.deleteCategoryById(id)

    // ── Product Variants ──

    suspend fun getVariantsByProductIdOnce(productId: Int): List<ProductVariantEntity> =
        variantDao.getVariantsByProductIdOnce(productId)

    fun getVariantsByProductId(productId: Int): Flow<List<ProductVariantEntity>> =
        variantDao.getVariantsByProductId(productId)

    suspend fun insertVariants(variants: List<ProductVariantEntity>) =
        variantDao.insertVariants(variants)

    suspend fun deleteVariantsByProductId(productId: Int) =
        variantDao.deleteVariantsByProductId(productId)

    suspend fun updateVariant(variant: ProductVariantEntity) =
        variantDao.updateVariant(variant)

    // ── Seeding ──

    /**
     * One-time repair (runs every launch, idempotent): Add-Ons / Merchandise / legacy "All"
     * products stored with type FOOD get corrected to DRINK so they never appear under the
     * Food section in the Products page.
     */
    suspend fun repairProductCategoryTypes() = productDao.fixNonDrinkCategoryTypes()

    suspend fun seedSampleData() {
        // Seed categories
        if (categoryDao.getCategoryCount() == 0) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Coffee", iconColor = "#6D4C41", iconChar = "☕", autoType = "DRINK"),
                CategoryEntity(name = "Non Coffee", iconColor = "#F48FB1", iconChar = "🧋", autoType = "DRINK"),
                CategoryEntity(name = "Food", iconColor = "#FF6600", iconChar = "🛎️", autoType = "FOOD"),
                CategoryEntity(name = "Add-Ons", iconColor = "#9C27B0", iconChar = "+", autoType = "DRINK"),
                CategoryEntity(name = "Merchandise", iconColor = "#388E3C", iconChar = "🛍️", autoType = "DRINK")
            )
            categoryDao.insertCategories(defaultCategories)
        }

        // Seed products with variants
        if (productDao.getProductCount() == 0) {
            val sampleProducts = listOf(
                ProductEntity(title = "Macchiato", category = "Coffee", type = "DRINK", temperature = "HOT", sellingPrice = 85.0),
                ProductEntity(title = "Latte", category = "Coffee", type = "DRINK", temperature = "HOT", sellingPrice = 95.0),
                ProductEntity(title = "Cold Brew", category = "Coffee", description = "Cold Brew", type = "DRINK", temperature = "COLD", sellingPrice = 80.0),
                ProductEntity(title = "Spanish Latte", category = "Coffee", type = "DRINK", temperature = "HOT", sellingPrice = 110.0),
                ProductEntity(title = "Matcha Latte", category = "Non Coffee", type = "DRINK", temperature = "HOT", sellingPrice = 95.0),
                ProductEntity(title = "Strawberry Smoothie", category = "Non Coffee", type = "DRINK", temperature = "COLD", sellingPrice = 90.0),
                ProductEntity(title = "Iced Chocolate", category = "Non Coffee", type = "DRINK", temperature = "COLD", sellingPrice = 85.0),
                ProductEntity(title = "Tocilog", category = "Food", description = "Tocino, Egg, Rice", type = "FOOD", sellingPrice = 130.0),
                ProductEntity(title = "Bacon Silog", category = "Food", description = "Bacon, Egg, Rice", type = "FOOD", sellingPrice = 120.0),
                ProductEntity(title = "Chicken Sandwich", category = "Food", description = "Grilled Chicken, Lettuce, Mayo", type = "FOOD", sellingPrice = 95.0),
                ProductEntity(title = "Pancakes", category = "Food", description = "With Maple Syrup & Butter", type = "FOOD", sellingPrice = 110.0),
                ProductEntity(title = "French Toast", category = "Food", type = "FOOD", sellingPrice = 105.0),
                ProductEntity(title = "Americano", category = "Coffee", type = "DRINK", temperature = "HOT", sellingPrice = 75.0),
                ProductEntity(title = "Cappuccino", category = "Coffee", type = "DRINK", temperature = "HOT", sellingPrice = 90.0),
                ProductEntity(title = "Hot Tea", category = "Non Coffee", description = "Assorted Premium Teas", type = "DRINK", temperature = "HOT", sellingPrice = 65.0),
                ProductEntity(title = "Lemonade", category = "Non Coffee", description = "Fresh Squeezed", type = "DRINK", temperature = "COLD", sellingPrice = 70.0),
                ProductEntity(title = "Clubhouse Sandwich", category = "Food", description = "Triple-decker with Fries", type = "FOOD", sellingPrice = 145.0),
                ProductEntity(title = "Caesar Salad", category = "Food", type = "FOOD", sellingPrice = 85.0),
                ProductEntity(title = "Mocha", category = "Coffee", type = "DRINK", temperature = "HOT", sellingPrice = 100.0),
                ProductEntity(title = "Caramel Latte", category = "Coffee", type = "DRINK", temperature = "HOT", sellingPrice = 105.0),
                ProductEntity(title = "Iced Tea", category = "Non Coffee", description = "Peach or Lemon", type = "DRINK", temperature = "COLD", sellingPrice = 65.0),
                ProductEntity(title = "Nachos", category = "Food", description = "With Cheese & Salsa", type = "FOOD", sellingPrice = 120.0)
            )

            // Insert products one by one to get IDs
            for (product in sampleProducts) {
                val productId = productDao.insertProduct(product).toInt()
                if (product.type == "DRINK") {
                    val basePrice = product.sellingPrice
                    variantDao.insertVariants(listOf(
                        ProductVariantEntity(productId = productId, sizeName = "12oz", sellingPrice = basePrice, isDefault = true, sortOrder = 0),
                        ProductVariantEntity(productId = productId, sizeName = "16oz", sellingPrice = basePrice + 10, sortOrder = 1),
                        ProductVariantEntity(productId = productId, sizeName = "22oz", sellingPrice = basePrice + 20, sortOrder = 2),
                    ))
                } else {
                    variantDao.insertVariants(listOf(
                        ProductVariantEntity(productId = productId, sizeName = "Regular", sellingPrice = product.sellingPrice, isDefault = true, sortOrder = 0),
                    ))
                }
            }
        }
    }

    // ── Transactions ──

    suspend fun insertTransaction(transaction: TransactionEntity): Long = transactionDao.insertTransaction(transaction)

    /**
     * Get active (non-completed) transactions with their order items, ready for KDS dispatch.
     * Returns pairs of (transaction, items).
     */
    suspend fun getActiveTransactionsWithItems(): List<Pair<TransactionEntity, List<OrderItemEntity>>> {
        val activeTransactions = transactionDao.getTransactionsByStatuses(listOf("Preparing", "Ready"))
        return activeTransactions.map { txn ->
            val items = orderItemDao.getOrderItemsByTransactionId(txn.id)
            txn to items
        }
    }

    suspend fun getTransactionByTransactionId(transactionId: String): TransactionEntity? =
        transactionDao.getTransactionByTransactionId(transactionId)

    suspend fun updateTransactionStatus(transactionId: Int, newStatus: String) = transactionDao.updateTransactionStatus(transactionId, newStatus)

    suspend fun getAllTransactionsOnce(): List<TransactionEntity> = transactionDao.getAllTransactionsOnce()

    suspend fun getTransactionCount(): Int = transactionDao.getTransactionCount()

    suspend fun getTransactionCountByStatus(status: String): Int = transactionDao.getTransactionCountByStatus(status)

    suspend fun getTotalSales(): Double = transactionDao.getTotalSales() ?: 0.0

    suspend fun getTotalItemsSold(): Int = transactionDao.getTotalItemsSold() ?: 0

    suspend fun getTodaySales(): Double {
        val startOfDay = getStartOfDay()
        return transactionDao.getTodaySales(startOfDay) ?: 0.0
    }

    suspend fun getTodayOrdersCount(): Int {
        val startOfDay = getStartOfDay()
        val endOfDay = startOfDay + 86_400_000 // 24 hours in millis
        return transactionDao.getOrdersCountByDateRange(startOfDay, endOfDay)
    }

    suspend fun getTodayItemsSold(): Int {
        val startOfDay = getStartOfDay()
        val endOfDay = startOfDay + 86_400_000 // 24 hours in millis
        return transactionDao.getItemsSoldByDateRange(startOfDay, endOfDay) ?: 0
    }

    suspend fun getTodayCustomersServed(): Int {
        val startOfDay = getStartOfDay()
        return transactionDao.getTodayCustomersServed(startOfDay)
    }

    private fun getStartOfDay(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    // ── Order Items ──

    suspend fun insertOrderItems(items: List<OrderItemEntity>) = orderItemDao.insertOrderItems(items)

    suspend fun getOrderItemsByTransactionId(transactionId: Int): List<OrderItemEntity> =
        orderItemDao.getOrderItemsByTransactionId(transactionId)

    fun getOrderItemsByTransactionIdFlow(transactionId: Int): Flow<List<OrderItemEntity>> =
        orderItemDao.getOrderItemsByTransactionIdFlow(transactionId)

    suspend fun getProductSalesSummary(): List<ProductSalesSummary> = orderItemDao.getProductSalesSummary()

    suspend fun getTotalSalesByProduct(productName: String): Double? = orderItemDao.getTotalSalesByProduct(productName)

    // ── Reports ──

    suspend fun getCategorySalesSummary(startTime: Long, endTime: Long): List<CategorySalesSummary> =
        orderItemDao.getCategorySalesSummary(startTime, endTime)

    suspend fun getTopSellingProducts(startTime: Long, endTime: Long): List<TopSellingProduct> =
        orderItemDao.getTopSellingProducts(startTime, endTime)

    suspend fun getOrderItemsByCategory(startTime: Long, endTime: Long): List<CategoryOrderItem> =
        orderItemDao.getOrderItemsByCategory(startTime, endTime)

    suspend fun getSalesByPaymentMethod(startTime: Long, endTime: Long): List<PaymentMethodSales> =
        transactionDao.getSalesByPaymentMethod(startTime, endTime)

    suspend fun getSalesTotalByDateRange(startTime: Long, endTime: Long): Double =
        transactionDao.getSalesTotalByDateRange(startTime, endTime) ?: 0.0

    suspend fun getOrdersCountByDateRange(startTime: Long, endTime: Long): Int =
        transactionDao.getOrdersCountByDateRange(startTime, endTime)

    suspend fun getItemsSoldByDateRange(startTime: Long, endTime: Long): Int =
        transactionDao.getItemsSoldByDateRange(startTime, endTime) ?: 0

    suspend fun getHourlySales(startTime: Long, endTime: Long): List<HourlySalesSummary> =
        transactionDao.getHourlySales(startTime, endTime)

    suspend fun getTransactionsByDateRange(startTime: Long, endTime: Long): List<TransactionEntity> =
        transactionDao.getTransactionsByDateRange(startTime, endTime)

    suspend fun getDailySales(startTime: Long, endTime: Long): List<DailySalesSummary> =
        transactionDao.getDailySales(startTime, endTime)

    suspend fun getDailyItemsSold(startTime: Long, endTime: Long): List<DailyItemsSold> =
        transactionDao.getDailyItemsSold(startTime, endTime)

    suspend fun getTopCustomers(startTime: Long, endTime: Long, limit: Int = 10): List<CustomerSummary> =
        transactionDao.getTopCustomers(startTime, endTime, limit)

    suspend fun getCashierPerformance(startTime: Long, endTime: Long): List<CashierSummary> =
        transactionDao.getCashierPerformance(startTime, endTime)

    suspend fun getOrderStatusCounts(startTime: Long, endTime: Long): List<OrderStatusSummary> =
        transactionDao.getOrderStatusCounts(startTime, endTime)

    suspend fun getOrderTypeCounts(startTime: Long, endTime: Long): List<OrderTypeSummary> =
        transactionDao.getOrderTypeCounts(startTime, endTime)

    suspend fun getCustomerOrderCount(startTime: Long, endTime: Long, customerName: String): Int =
        transactionDao.getCustomerOrderCount(startTime, endTime, customerName)

    // ── Reports: aggregated helpers ──

    suspend fun getCustomerCount(startTime: Long, endTime: Long): Int {
        return transactionDao.getTransactionsByDateRange(startTime, endTime)
            .map { it.customerName }
            .distinct()
            .size
    }

    // ── Expenses ──

    suspend fun getExpensesByDateRange(startTime: Long, endTime: Long): List<ExpenseEntity> =
        expenseDao.getExpensesByDateRange(startTime, endTime)

    suspend fun getTotalExpensesByDateRange(startTime: Long, endTime: Long): Double {
        val query = SimpleSQLiteQuery(
            "SELECT SUM(syrups + sauce + milk + ice + others) FROM expenses WHERE date BETWEEN $startTime AND $endTime"
        )
        return expenseDao.getTotalExpensesByDateRangeRaw(query) ?: 0.0
    }

    suspend fun getExpenseCategoryTotals(startTime: Long, endTime: Long): ExpenseCategoryTotals? {
        val query = SimpleSQLiteQuery(
            "SELECT SUM(syrups) AS syrups, SUM(sauce) AS sauce, SUM(milk) AS milk, SUM(ice) AS ice, SUM(others) AS others FROM expenses WHERE date BETWEEN $startTime AND $endTime"
        )
        return expenseDao.getExpenseCategoryTotalsRaw(query)
    }

    suspend fun getExpensesByDateRangeAsc(startTime: Long, endTime: Long): List<ExpenseEntity> =
        expenseDao.getExpensesByDateRangeAsc(startTime, endTime)

    suspend fun getExpenseCountByDateRange(startTime: Long, endTime: Long): Int =
        expenseDao.getExpenseCountByDateRange(startTime, endTime)

    suspend fun insertExpense(expense: ExpenseEntity): Long =
        expenseDao.insertExpense(expense)

    suspend fun deleteExpenseById(id: Int) =
        expenseDao.deleteExpenseById(id)
}

