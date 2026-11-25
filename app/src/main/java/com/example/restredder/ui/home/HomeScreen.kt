package com.example.restredder.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.restredder.R

@Composable
fun HomeScreen(
    onAvailablePackagesClick: () -> Unit,
    onCreateStoreClick: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            if (uiState.role == "merchant") {
                Button(
                    onClick = onCreateStoreClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.create_store_button))
                }
            } else {
                Button(
                    onClick = onAvailablePackagesClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.available_packages_button))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    onLoggedOut()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.logout_button))
            }
        }
    }
}
