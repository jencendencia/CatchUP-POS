package com.catchuppos.app

import android.app.Application
import com.catchuppos.app.data.AppDatabase
import com.catchuppos.app.data.ProductRepository
import com.catchuppos.app.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CatchUpApp : Application() {

    // Not using by lazy so that closeInstance() + subsequent access creates a fresh DB
    val database: AppDatabase
        get() = AppDatabase.getInstance(this)
    // Also not lazy so that after close+restore, we get fresh DAOs from the new database instance
    val productRepository: ProductRepository
        get() = ProductRepository(database.productDao(), database.categoryDao(), database.transactionDao(), database.productVariantDao(), database.orderItemDao(), database.expenseDao())
    val userRepository: UserRepository
        get() = UserRepository(database.userDao())

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Seed admin synchronously so login works immediately
        runBlocking {
            try {
                userRepository.seedDefaultAdmin()
            } catch (e: Exception) {
                android.util.Log.e("CatchUpApp", "Failed to seed admin: ${e.message}", e)
            }
        }
        // Seed sample data asynchronously
        applicationScope.launch {
            try { productRepository.seedSampleData() } catch (_: Exception) {}
        }
    }

    fun closeDatabase() {
        AppDatabase.closeInstance()
    }
}
