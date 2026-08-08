package com.catchuppos.app.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.catchuppos.app.auth.AuthState
import com.catchuppos.app.license.LicenseManager
import com.catchuppos.app.ui.activation.ActivationScreen
import com.catchuppos.app.ui.dashboard.DashboardScreen
import com.catchuppos.app.ui.login.LoginScreen

object Routes {
    const val ACTIVATION = "activation"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
}

@Composable
fun CatchUpNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    // Never let a license-check failure (e.g. broken keystore / encrypted prefs) crash the
    // app on launch. Worst case the user is sent to the activation screen to re-enter their key.
    val isActivated = try {
        LicenseManager.isActivated(context)
    } catch (e: Exception) {
        Log.w("CatchUpNavGraph", "License check failed, showing activation: ${e.message}")
        false
    }
    val startDestination = if (isActivated) Routes.LOGIN else Routes.ACTIVATION

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.ACTIVATION) {
            ActivationScreen(
                onActivationSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ACTIVATION) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onLogout = {
                    AuthState.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }
    }
}
