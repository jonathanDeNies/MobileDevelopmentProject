package com.example.restredder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.restredder.ui.auth.RegisterScreen
import com.example.restredder.ui.home.HomeScreen
import com.example.restredder.ui.packages.AvailablePackagesScreen

object Routes {
    const val REGISTER = "register"
    const val HOME = "home"
    const val AVAILABLE_PACKAGES = "available_packages"
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.REGISTER,
        modifier = modifier
    ) {
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onAvailablePackagesClick = {
                    navController.navigate(Routes.AVAILABLE_PACKAGES)
                }
            )
        }

        composable(Routes.AVAILABLE_PACKAGES) {
            AvailablePackagesScreen()
        }
    }
}
