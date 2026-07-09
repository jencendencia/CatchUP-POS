package com.catchuppos.app

import android.app.Application
import com.catchuppos.app.data.AppDatabase
import com.catchuppos.app.data.ProductRepository
import com.catchuppos.app.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CatchUpApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val productRepository by lazy { ProductRepository(database.productDao(), database.categoryDao(), database.transactionDao(), database.productVariantDao(), database.orderItemDao()) }
    val userRepository by lazy { UserRepository(database.userDao()) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            productRepository.seedSampleData()
            userRepository.seedDefaultAdmin()
        }
    }
}
