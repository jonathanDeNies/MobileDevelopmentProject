package com.example.restredder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.restredder.ui.auth.ForgotPasswordScreen
import com.example.restredder.ui.auth.LoginScreen
import com.example.restredder.ui.auth.RegisterScreen
import com.example.restredder.ui.home.HomeScreen
import com.example.restredder.ui.packages.AvailablePackagesScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val AVAILABLE_PACKAGES = "available_packages"
}

@Composable
fun AppNavHost(
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    // Go to home and remove login/register from the back stack.
    val navigateToHome: () -> Unit = {
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = navigateToHome,
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
                onForgotPasswordClick = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBackToLogin = { navController.popBackStack() })
        }

        composable(Routes.REGISTER) {
            RegisterScreen(onRegistered = navigateToHome)
        }

        composable(Routes.HOME) {
            HomeScreen(
                onAvailablePackagesClick = {
                    navController.navigate(Routes.AVAILABLE_PACKAGES)
                },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.AVAILABLE_PACKAGES) {
            AvailablePackagesScreen()
        }
    }
}
