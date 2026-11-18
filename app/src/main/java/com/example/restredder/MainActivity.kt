
package com.example.restredder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.restredder.data.repository.AuthRepository
import com.example.restredder.ui.navigation.AppNavHost
import com.example.restredder.ui.navigation.Routes
import com.example.restredder.ui.theme.RestRedderTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val startDestination =
            if (AuthRepository().isLoggedIn()) Routes.HOME else Routes.LOGIN

        setContent {
            RestRedderTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        AppNavHost(startDestination = startDestination)
                    }
                }
            }
        }
    }
}
