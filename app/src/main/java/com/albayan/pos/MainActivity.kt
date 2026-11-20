package com.albayan.pos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.albayan.pos.data.AppDatabase
import com.albayan.pos.ui.BarcodeScannerScreen
import com.albayan.pos.ui.InventoryApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.getInstance(this).productDao()

        setContent {
            MaterialTheme {
                val context = LocalContext.current
                var isScanning by remember { mutableStateOf(false) }
                var scanCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        isScanning = true
                    }
                }

                fun startScan(onResult: (String) -> Unit) {
                    scanCallback = onResult
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        isScanning = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                if (isScanning) {
                    BarcodeScannerScreen(
                        onBarcodeScanned = { code ->
                            isScanning = false
                            scanCallback?.invoke(code)
                        },
                        onClose = { isScanning = false }
                    )
                } else {
                    InventoryApp(
                        dao = dao,
                        onRequestCameraScan = { callback -> startScan(callback) }
                    )
                }
            }
        }
    }
}
