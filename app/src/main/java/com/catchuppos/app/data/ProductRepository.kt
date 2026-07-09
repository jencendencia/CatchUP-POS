package com.catchuppos.app.data

import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
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

    // ── Categories ──

    suspend fun allCategoriesOnce(): List<CategoryEntity> = categoryDao.getAllCategoriesOnce()

    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)

    suspend fun deleteCategoryById(id: Int) = categoryDao.deleteCategoryById(id)

    // ── Seeding ──

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

        // Seed products
        if (productDao.getProductCount() == 0) {
            val sampleProducts = listOf(
                ProductEntity(title = "Macchiato", category = "Coffee", type = "DRINK", sellingPrice = 85.0, imagePath = null),
                ProductEntity(title = "Latte", category = "Coffee", type = "DRINK", sellingPrice = 95.0),
                ProductEntity(title = "Cold Brew", category = "Coffee", description = "Cold Brew", type = "DRINK", sellingPrice = 80.0),
                ProductEntity(title = "Spanish Latte", category = "Coffee", type = "DRINK", sellingPrice = 110.0),
                ProductEntity(title = "Matcha Latte", category = "Non Coffee", type = "DRINK", sellingPrice = 95.0),
                ProductEntity(title = "Strawberry Smoothie", category = "Non Coffee", type = "DRINK", sellingPrice = 90.0),
                ProductEntity(title = "Iced Chocolate", category = "Non Coffee", type = "DRINK", sellingPrice = 85.0),
                ProductEntity(title = "Tocilog", category = "Food", description = "Tocino, Egg, Rice", type = "FOOD", sellingPrice = 130.0),
                ProductEntity(title = "Bacon Silog", category = "Food", description = "Bacon, Egg, Rice", type = "FOOD", sellingPrice = 120.0),
                ProductEntity(title = "Chicken Sandwich", category = "Food", description = "Grilled Chicken, Lettuce, Mayo", type = "FOOD", sellingPrice = 95.0),
                ProductEntity(title = "Pancakes", category = "Food", description = "With Maple Syrup & Butter", type = "FOOD", sellingPrice = 110.0),
                ProductEntity(title = "French Toast", category = "Food", type = "FOOD", sellingPrice = 105.0),
                ProductEntity(title = "Americano", category = "Coffee", type = "DRINK", sellingPrice = 75.0),
                ProductEntity(title = "Cappuccino", category = "Coffee", type = "DRINK", sellingPrice = 90.0),
                ProductEntity(title = "Hot Tea", category = "Non Coffee", description = "Assorted Premium Teas", type = "DRINK", sellingPrice = 65.0),
                ProductEntity(title = "Lemonade", category = "Non Coffee", description = "Fresh Squeezed", type = "DRINK", sellingPrice = 70.0),
                ProductEntity(title = "Clubhouse Sandwich", category = "Food", description = "Triple-decker with Fries", type = "FOOD", sellingPrice = 145.0),
                ProductEntity(title = "Caesar Salad", category = "Food", type = "FOOD", sellingPrice = 85.0),
                ProductEntity(title = "Mocha", category = "Coffee", type = "DRINK", sellingPrice = 100.0),
                ProductEntity(title = "Caramel Latte", category = "Coffee", type = "DRINK", sellingPrice = 105.0),
                ProductEntity(title = "Iced Tea", category = "Non Coffee", description = "Peach or Lemon", type = "DRINK", sellingPrice = 65.0),
                ProductEntity(title = "Nachos", category = "Food", description = "With Cheese & Salsa", type = "FOOD", sellingPrice = 120.0)
            )
            productDao.insertProducts(sampleProducts)
        }
    }

    // ── Transactions ──

    suspend fun insertTransaction(transaction: TransactionEntity): Long = transactionDao.insertTransaction(transaction)

    suspend fun getAllTransactionsOnce(): List<TransactionEntity> = transactionDao.getAllTransactionsOnce()

    suspend fun getTransactionCount(): Int = transactionDao.getTransactionCount()

    suspend fun getTransactionCountByStatus(status: String): Int = transactionDao.getTransactionCountByStatus(status)

    suspend fun getTotalSales(): Double = transactionDao.getTotalSales() ?: 0.0

    suspend fun getTotalItemsSold(): Int = transactionDao.getTotalItemsSold() ?: 0

    suspend fun getTodaySales(): Double {
        val startOfDay = getStartOfDay()
        return transactionDao.getTodaySales(startOfDay) ?: 0.0
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
}
