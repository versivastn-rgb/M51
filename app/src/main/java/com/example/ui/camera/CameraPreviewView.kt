package com.example.ui.camera

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    activeLens: M51Lens,
    flashMode: String,
    onImageCaptureCreated: (ImageCapture) -> Unit,
    onCameraBindingFailed: (Throwable) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    LaunchedEffect(imageCapture) {
        onImageCaptureCreated(imageCapture)
    }

    LaunchedEffect(activeLens, flashMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Decide front or back sensor
                val cameraSelector = if (activeLens == M51Lens.SELFIE) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                // Check if device actually has the required camera
                if (!cameraProvider.hasCamera(cameraSelector)) {
                    throw IllegalStateException("Sensor kamera tidak tersedia pada perangkat ini.")
                }

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                // Map flash
                val flashConfig = when (flashMode) {
                    "ON" -> ImageCapture.FLASH_MODE_ON
                    "AUTO" -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                }
                imageCapture.flashMode = flashConfig

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                // Adjust zoom to emulate Galaxy M51 perspective factors
                val zoomState = camera.cameraInfo.zoomState.value
                val minZoom = zoomState?.minZoomRatio ?: 1.0f
                val maxZoom = zoomState?.maxZoomRatio ?: 4.0f

                val targetZoom = when (activeLens) {
                    M51Lens.ULTRA_WIDE -> minZoom // Wide angle
                    M51Lens.WIDE -> 1.0f          // standard Sony IMX682
                    M51Lens.DEPTH -> minOf(2.0f, maxZoom) // simulated Live Focus crop
                    M51Lens.MACRO -> minOf(3.0f, maxZoom) // simulated macro focusing crop
                    M51Lens.SELFIE -> 1.0f        // main IMX616 front selfie
                }
                camera.cameraControl.setZoomRatio(targetZoom)

            } catch (exc: Exception) {
                Log.e("CameraPreviewView", "Gagal mengikat CameraX use cases", exc)
                onCameraBindingFailed(exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
