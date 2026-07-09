package com.catchuppos.app

import android.app.Application
import com.catchuppos.app.data.AppDatabase
import com.catchuppos.app.data.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CatchUpApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val productRepository by lazy { ProductRepository(database.productDao(), database.categoryDao()) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            productRepository.seedSampleData()
        }
    }
}
