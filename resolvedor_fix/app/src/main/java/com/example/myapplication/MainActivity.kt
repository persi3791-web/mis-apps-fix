package com.example.myapplication

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.MainScreen
import com.example.myapplication.ui.MessageDialog
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.viewmodel.MainViewModel
import com.example.myapplication.util.RequestStoragePermission

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var permissionGranted by remember { mutableStateOf(false) }
                    var showPermissionDialog by remember { mutableStateOf(false) }
                    
                    val viewModel: MainViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return MainViewModel(application) as T
                            }
                        }
                    )
                    
                    RequestStoragePermission(
                        onPermissionGranted = {
                            permissionGranted = true
                            showPermissionDialog = false
                        },
                        onPermissionDenied = {
                            permissionGranted = false
                            showPermissionDialog = true
                        }
                    )
                    
                    MainScreen(
                        viewModel = viewModel,
                        onPermissionDenied = {
                            showPermissionDialog = true
                        }
                    )
                    
                    if (showPermissionDialog) {
                        MessageDialog(
                            title = "Permiso necesario",
                            message = "Se necesita permiso de almacenamiento para guardar el archivo CSV. Por favor, otorga el permiso en la configuración de la aplicación.",
                            onDismiss = { showPermissionDialog = false }
                        )
                    }
                }
            }
        }
    }
}