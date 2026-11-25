package com.example.restredder.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.restredder.data.model.PlaceResult
import com.example.restredder.ui.auth.ForgotPasswordScreen
import com.example.restredder.ui.auth.LoginScreen
import com.example.restredder.ui.auth.RegisterScreen
import com.example.restredder.ui.home.HomeScreen
import com.example.restredder.ui.location.LocationPickerScreen
import com.example.restredder.ui.location.LocationPickerViewModel.Companion.ARG_ADDRESS
import com.example.restredder.ui.location.LocationPickerViewModel.Companion.ARG_LATITUDE
import com.example.restredder.ui.location.LocationPickerViewModel.Companion.ARG_LONGITUDE
import com.example.restredder.ui.packages.AvailablePackagesScreen
import com.example.restredder.ui.store.CreateStoreScreen
import com.example.restredder.ui.store.CreateStoreViewModel
import com.example.restredder.ui.store.MyStoresScreen
import com.example.restredder.ui.store.MyStoresViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val AVAILABLE_PACKAGES = "available_packages"
    const val MY_STORES = "my_stores"
    const val CREATE_STORE = "create_store"
    const val LOCATION_PICKER =
        "location_picker?$ARG_LATITUDE={$ARG_LATITUDE}&$ARG_LONGITUDE={$ARG_LONGITUDE}&$ARG_ADDRESS={$ARG_ADDRESS}"

    fun locationPicker(current: PlaceResult?): String =
        if (current == null) {
            "location_picker"
        } else {
            "location_picker?$ARG_LATITUDE=${current.latitude}" +
                    "&$ARG_LONGITUDE=${current.longitude}" +
                    "&$ARG_ADDRESS=${Uri.encode(current.address)}"
        }
}

// Keys used to send the picked location back to the previous screen.
private const val RESULT_LATITUDE = "picked_latitude"
private const val RESULT_LONGITUDE = "picked_longitude"
private const val RESULT_ADDRESS = "picked_address"

/** Reads and removes the location sent back by the location picker, if there is one. */
private fun SavedStateHandle.takePickedLocation(): PlaceResult? {
    val latitude = remove<Double>(RESULT_LATITUDE)
    val longitude = remove<Double>(RESULT_LONGITUDE)
    val address = remove<String>(RESULT_ADDRESS).orEmpty()
    return if (latitude != null && longitude != null) {
        PlaceResult(latitude, longitude, address)
    } else {
        null
    }
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
                onMyStoresClick = {
                    navController.navigate(Routes.MY_STORES)
                },
                onAddStoreClick = {
                    navController.navigate(Routes.CREATE_STORE)
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

        composable(Routes.MY_STORES) { entry ->
            val myStoresViewModel: MyStoresViewModel = viewModel()

            // Runs again when coming back from the location picker.
            LaunchedEffect(entry) {
                entry.savedStateHandle.takePickedLocation()?.let { place ->
                    myStoresViewModel.onLocationPicked(place)
                }
            }

            MyStoresScreen(
                onBack = { navController.popBackStack() },
                onAddStore = { navController.navigate(Routes.CREATE_STORE) },
                onChangeLocation = { store ->
                    navController.navigate(
                        Routes.locationPicker(
                            PlaceResult(store.latitude, store.longitude, store.address)
                        )
                    )
                },
                viewModel = myStoresViewModel
            )
        }

        composable(Routes.CREATE_STORE) { entry ->
            val createStoreViewModel: CreateStoreViewModel = viewModel()

            // Runs again when coming back from the location picker.
            LaunchedEffect(entry) {
                entry.savedStateHandle.takePickedLocation()?.let { place ->
                    createStoreViewModel.onLocationPicked(place)
                }
            }

            CreateStoreScreen(
                onBack = { navController.popBackStack() },
                onChooseLocation = { current ->
                    navController.navigate(Routes.locationPicker(current))
                },
                onStoreCreated = { navController.popBackStack() },
                viewModel = createStoreViewModel
            )
        }

        composable(
            route = Routes.LOCATION_PICKER,
            arguments = listOf(ARG_LATITUDE, ARG_LONGITUDE, ARG_ADDRESS).map { name ->
                navArgument(name) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            }
        ) {
            LocationPickerScreen(
                onBack = { navController.popBackStack() },
                onLocationConfirmed = { place ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set(RESULT_LATITUDE, place.latitude)
                        set(RESULT_LONGITUDE, place.longitude)
                        set(RESULT_ADDRESS, place.address)
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}
