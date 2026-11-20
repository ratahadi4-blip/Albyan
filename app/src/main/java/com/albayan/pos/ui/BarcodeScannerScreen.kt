package com.albayan.pos.ui

import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scannedOnce by remember { mutableStateOf(false) }
    var captureRequested by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("وجّه الكاميرا نحو الباركود أو اضغط تصوير") }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val scanner = BarcodeScanning.getClient()
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !scannedOnce) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    val rawValue = barcodes.firstOrNull()?.rawValue
                                    if (!rawValue.isNullOrBlank() && !scannedOnce) {
                                        scannedOnce = true
                                        onBarcodeScanned(rawValue.trim())
                                    } else if (captureRequested) {
                                        captureRequested = false
                                        statusMessage = "لم يظهر باركود واضح، قرّب الكاميرا وأعد المحاولة"
                                    }
                                }
                                .addOnFailureListener {
                                    if (captureRequested) {
                                        captureRequested = false
                                        statusMessage = "تعذرت القراءة، جرّب إضاءة أفضل"
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Button(
                onClick = {
                    captureRequested = true
                    statusMessage = "جاري قراءة الباركود..."
                }
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("تصوير ومسح")
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "إغلاق الكاميرا",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
