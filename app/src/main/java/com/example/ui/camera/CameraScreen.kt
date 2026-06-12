package com.example.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.CapturedPhoto
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State bindings from ViewModel
    val activeLens by viewModel.activeLens.collectAsStateWithLifecycle()
    val flashMode by viewModel.cameraFlashMode.collectAsStateWithLifecycle()
    val saturation by viewModel.saturation.collectAsStateWithLifecycle()
    val hdrStrength by viewModel.hdrStrength.collectAsStateWithLifecycle()
    val shadowRecovery by viewModel.shadowRecovery.collectAsStateWithLifecycle()
    val highlightCompression by viewModel.highlightCompression.collectAsStateWithLifecycle()
    val bokehLevel by viewModel.bokehLevel.collectAsStateWithLifecycle()
    val isAiOptimizerEnabled by viewModel.isAiOptimizerEnabled.collectAsStateWithLifecycle()
    val sceneDetected by viewModel.sceneDetected.collectAsStateWithLifecycle()
    val photosList by viewModel.photosList.collectAsStateWithLifecycle()
    val selectedPhotoForTuning by viewModel.selectedPhotoForTuning.collectAsStateWithLifecycle()
    val isTuningSheetOpen by viewModel.showTuningSheet.collectAsStateWithLifecycle()
    val captureAnimationTrigger by viewModel.captureAnimationTrigger.collectAsStateWithLifecycle()

    // Camera permission and simulator states
    var isCameraPermissionGranted by remember { mutableStateOf(false) }
    var forceSimulatorMode by remember { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    // Grid guide & level settings
    var showGridLineGuide by remember { mutableStateOf(true) }
    var currentDeviceTilt by remember { mutableStateOf(0f) } // Simulated tilt to show M51 level meter
    var showM51SpecsInfoDialog by remember { mutableStateOf(false) }

    // Real Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isCameraPermissionGranted = isGranted
        if (!isGranted) {
            forceSimulatorMode = true
        }
    }

    // Trigger permission request on startup
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
        // Keep Level Meter slightly moving to show real-time gyroscope simulation
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(1200)
                currentDeviceTilt = (-2..2).random().toFloat()
            }
        }
    }

    // Helper for real CameraX snap capture
    val capturePhotoWithRealCamera: () -> Unit = {
        val currentImageCapture = imageCapture
        if (currentImageCapture != null && isCameraPermissionGranted && !forceSimulatorMode) {
            val directory = File(context.filesDir, "m51_captures")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val photoFile = File(directory, "M51_REAL_${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            currentImageCapture.takePicture(
                outputOptions,
                androidx.core.content.ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        viewModel.saveRealCameraXPhoto(photoFile.absolutePath)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        // If hardware error, fall back to high-res simulation gracefully
                        viewModel.captureSimulationPhoto()
                    }
                }
            )
        } else {
            // Simulator capture
            viewModel.captureSimulationPhoto()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- VIEWPORT / SHOOTING VIEW ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 180.dp) // Leave clean space at bottom for switcher and shutter
        ) {
            if (isCameraPermissionGranted && !forceSimulatorMode) {
                CameraPreviewView(
                    modifier = Modifier.fillMaxSize(),
                    activeLens = activeLens,
                    flashMode = flashMode,
                    onImageCaptureCreated = { imageCapture = it },
                    onCameraBindingFailed = {
                        forceSimulatorMode = true
                    }
                )
            } else {
                // PREMIUM SIMULATOR VIEWPORT INTRO
                Box(modifier = Modifier.fillMaxSize()) {
                    // Generate a preview of the active mock lens before taking photo
                    val previewBitmap = remember(activeLens, sceneDetected) {
                        PhotoGenerator.generateMockM51Photo(activeLens, sceneDetected)
                    }

                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Simulated Viewfinder",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(
                            ImageProcessor.createTuningMatrix(
                                saturation = saturation,
                                hdrStrength = hdrStrength,
                                shadowRecovery = shadowRecovery,
                                highlightCompression = highlightCompression,
                                isAiOptimizerEnabled = isAiOptimizerEnabled,
                                sceneType = sceneDetected
                            )
                        )
                    )

                    // Overlay watermark warning
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 90.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.Yellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Modul Simulator Kamera M51 Aktif",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // --- CAMERA VIEWPORT GRID LINES (RULE OF THIRDS) ---
            if (showGridLineGuide) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Vertical Grid Lines
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(w / 3f, 0f),
                        end = Offset(w / 3f, h),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(2f * w / 3f, 0f),
                        end = Offset(2f * w / 3f, h),
                        strokeWidth = 1.5f
                    )

                    // Horizontal Grid Lines
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(0f, h / 3f),
                        end = Offset(w, h / 3f),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(0f, 2f * h / 3f),
                        end = Offset(w, 2f * h / 3f),
                        strokeWidth = 1.5f
                    )
                }
            }

            // --- GYROSCOPIC LEVEL METER ---
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(180.dp)
                    .height(40.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val isBalanced = currentDeviceTilt in -0.5f..0.5f

                    // Draw center level marks
                    drawCircle(
                        color = if (isBalanced) Color.Green else Color.White.copy(alpha = 0.6f),
                        radius = 6f,
                        center = Offset(w / 2f, h / 2f)
                    )

                    // Draw dynamic tilting needle line
                    val lineLength = 60f
                    val angleRad = Math.toRadians(currentDeviceTilt.toDouble())
                    val startX = (w / 2f) - (lineLength * Math.cos(angleRad)).toFloat()
                    val startY = (h / 2f) - (lineLength * Math.sin(angleRad)).toFloat()
                    val endX = (w / 2f) + (lineLength * Math.cos(angleRad)).toFloat()
                    val endY = (h / 2f) + (lineLength * Math.sin(angleRad)).toFloat()

                    drawLine(
                        color = if (isBalanced) Color.Green else Color.Yellow,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }

                Text(
                    text = "${currentDeviceTilt}°",
                    color = if (currentDeviceTilt in -0.5f..0.5f) Color.Green else Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // --- SHUTTER WHITE FLASH FEEDBACK ---
            AnimatedVisibility(
                visible = captureAnimationTrigger,
                enter = fadeIn(animationSpec = tween(50)),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            }

            // --- TOP CONTROL BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Flash Toggle
                IconButton(
                    onClick = { viewModel.toggleFlash() },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .size(42.dp)
                ) {
                    val icon = when (flashMode) {
                        "ON" -> Icons.Default.FlashOn
                        "AUTO" -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    }
                    val tint = if (flashMode == "OFF") Color.White else Color.Yellow
                    Icon(imageVector = icon, contentDescription = "Flash", tint = tint, modifier = Modifier.size(20.dp))
                }

                // 2. Active Scene Indicator (AI Scene Optimizer Button)
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.isAiOptimizerEnabled.value = !isAiOptimizerEnabled }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Scene Optimizer",
                            tint = if (isAiOptimizerEnabled) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isAiOptimizerEnabled) "AI: ${sceneDetected}" else "AI Mati",
                            color = if (isAiOptimizerEnabled) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. Grid Guide Switch
                IconButton(
                    onClick = { showGridLineGuide = !showGridLineGuide },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .size(42.dp)
                ) {
                    Icon(
                        imageVector = if (showGridLineGuide) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "Grid",
                        tint = if (showGridLineGuide) Color(0xFF00E5FF) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 4. Camera Spec Hardware Info dialog button
                IconButton(
                    onClick = { showM51SpecsInfoDialog = true },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Specs",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // --- LENS CHASSIS SPECIFICATION OVERLAY ---
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 135.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .widthIn(max = 300.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE50914), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "LENS ${activeLens.label}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = activeLens.resolution,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${activeLens.sensor} | Aperture ${activeLens.aperture}",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (activeLens.fov != null) {
                        Text(
                            text = "Sudut Lebar Panoramis: ${activeLens.fov}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // --- HARDWARE SIMULATION SELECTORS (if not using real camera) ---
            if (forceSimulatorMode || !isCameraPermissionGranted) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Simulasi Objek Foto (Ubah Scene):",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            val scenes = listOf(
                                "Sky/Nature",
                                "Vibrant Blue Ocean",
                                "Warm Beach Sunset",
                                "Bunga Macro Merah",
                                "Studio Portrait Headshot"
                            )
                            scenes.forEach { scene ->
                                val selected = sceneDetected == scene
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (selected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.sceneDetected.value = scene }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = scene,
                                        color = if (selected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- BOTTOM SHUTTER AND LENS DIAL PANELS ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                    )
                )
                .navigationBarsPadding() // Respect safe areas beautifully!
                .padding(bottom = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. GALAXY DIAL LENS SWITCHER
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    M51Lens.values().forEach { lens ->
                        val selected = activeLens == lens
                        val pulseScale by animateFloatAsState(
                            targetValue = if (selected) 1.15f else 1.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessHigh)
                        )

                        Surface(
                            modifier = Modifier
                                .scale(pulseScale)
                                .clickable {
                                    viewModel.selectLens(lens)
                                    // Switch simulator or CameraX source automatically
                                    if (lens == M51Lens.SELFIE) {
                                        // Auto adjust back plate
                                    }
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = lens.label,
                                color = if (selected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. MAIN CAPTURE BAR (GALLERY THUMBNAIL | SHUTTER BUTTON | FILTER TUNE SWITCHER)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Latest Photo Thumbnail Link to Gallery
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .background(Color.DarkGray)
                            .clickable {
                                if (photosList.isNotEmpty()) {
                                    viewModel.selectPhotoForTuning(photosList.first())
                                    viewModel.setShowTuningSheet(true)
                                }
                            }
                    ) {
                        if (photosList.isNotEmpty()) {
                            AsyncImage(
                                model = File(photosList.first().filePath),
                                contentDescription = "Buka Galeri",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    // MAIN SHUTTER BUTTON (WHITE DOUBLE CIRCLE)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { capturePhotoWithRealCamera() }
                            .padding(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, Color.Black, CircleShape)
                        )
                    }

                    // Real-time Tuning sliders toggler
                    IconButton(
                        onClick = { viewModel.setShowTuningSheet(true) },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Tuning Panel",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // --- SAMSUNG M51 LENS DIAGNOSTICS DIALOG ---
        if (showM51SpecsInfoDialog) {
            AlertDialog(
                onDismissRequest = { showM51SpecsInfoDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF)
                        )
                        Text(
                            text = "Samsung M51 Camera System",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Aplikasi ini dirancang khusus dengan kustomisasi color palette dan dinamika HDR berdasarkan tuning sensor Galaxy M51 asli.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        val specItems = listOf(
                            Triple("Utama (Wide) 64MP", "Sony IMX682 | f/1.8 | 1/1.73\" | PDAF\nTuning: Sangat colorful, dynamic recovery optimal.", Color(0xFF00E5FF)),
                            Triple("Ultra-Wide 12MP", "Samsung S5K3L6 | f/2.2 | 123° FOV\nTuning: Koreksi lengkungan distorsi, saturasi langit biru tajam.", Color(0xFF90CAF9)),
                            Triple("Macro 5MP", "Galaxy Close-up f/2.4 | Fokus optimal 4cm\nTuning: Peningkatan kontras detail mikro, saturasi tanaman hijau.", Color(0xFFA5D6A7)),
                            Triple("Bokeh / Depth 5MP", "Samsung S5K5E9 | f/2.4\nTuning: Kalkulasi segmentasi 3D & render lingkaran bokeh lembut.", Color(0xFFF48FB1)),
                            Triple("Depan (Selfie) 32MP", "Sony IMX616 | f/2.0\nTuning: Skin-tone warming, reduksi highlight backlight matahari.", Color(0xFFCE93D8))
                        )

                        specItems.forEach { (name, details, color) ->
                            Column {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                                Text(
                                    text = details,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Checkbox(
                                checked = !forceSimulatorMode,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                        forceSimulatorMode = false
                                    } else {
                                        forceSimulatorMode = true
                                    }
                                }
                            )
                            Text(
                                text = "Gunakan Hardware Kamera Asli jika tersedia",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showM51SpecsInfoDialog = false }) {
                        Text("Tutup", color = Color(0xFF00E5FF))
                    }
                }
            )
        }

        // --- FLOATING DETAILED GALLERY & ADJUSTMENT HDR SHEET ---
        if (isTuningSheetOpen) {
            TuningAndGallerySheet(
                viewModel = viewModel,
                onDismiss = { viewModel.setShowTuningSheet(false) }
            )
        }
    }
}

/**
 * High-fidelity sub-sheet that implements interactive "Before/After Slider Comparison"
 * and real Room DB persistence. Users can tweak saturation and shadow recovery on their photos.
 */
@Composable
fun TuningAndGallerySheet(
    viewModel: CameraViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photosList by viewModel.photosList.collectAsStateWithLifecycle()
    val selectedPhoto by viewModel.selectedPhotoForTuning.collectAsStateWithLifecycle()

    val saturation by viewModel.saturation.collectAsStateWithLifecycle()
    val hdrStrength by viewModel.hdrStrength.collectAsStateWithLifecycle()
    val shadowRecovery by viewModel.shadowRecovery.collectAsStateWithLifecycle()
    val highlightCompression by viewModel.highlightCompression.collectAsStateWithLifecycle()
    val bokehLevel by viewModel.bokehLevel.collectAsStateWithLifecycle()
    val isAiOptimizerEnabled by viewModel.isAiOptimizerEnabled.collectAsStateWithLifecycle()

    var showOriginalBeforeComparison by remember { mutableStateOf(false) }
    var currentDisplayLens by remember { mutableStateOf<M51Lens?>(null) }

    LaunchedEffect(selectedPhoto) {
        selectedPhoto?.let { photo ->
            currentDisplayLens = M51Lens.values().firstOrNull { it.key == photo.lensType }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(enabled = false) {} // block click propagation
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }

                Text(
                    text = "M51 COLOR & HDR TUNER",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = {
                        viewModel.resetActiveTuningValues()
                    }
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Color.LightGray)
                }
            }

            // Preview viewport showing currently selected image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                if (selectedPhoto != null) {
                    val resolvedFile = File(selectedPhoto!!.filePath)
                    if (resolvedFile.exists()) {
                        // Display either original or customized version
                        val activeSaturation = if (showOriginalBeforeComparison) 1.0f else saturation
                        val activeHdr = if (showOriginalBeforeComparison) 1.0f else hdrStrength
                        val activeShadows = if (showOriginalBeforeComparison) 0.0f else shadowRecovery
                        val activeHighlights = if (showOriginalBeforeComparison) 0.0f else highlightCompression

                        AsyncImage(
                            model = resolvedFile,
                            contentDescription = "Active captured photo view",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.colorMatrix(
                                ImageProcessor.createTuningMatrix(
                                    saturation = activeSaturation,
                                    hdrStrength = activeHdr,
                                    shadowRecovery = activeShadows,
                                    highlightCompression = activeHighlights,
                                    isAiOptimizerEnabled = if (showOriginalBeforeComparison) false else isAiOptimizerEnabled,
                                    sceneType = selectedPhoto?.sceneDetected ?: ""
                                )
                            )
                        )
                    } else {
                        // Missing file error placeholder
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("File Gambar Tidak Ditemukan", color = Color.Red, fontSize = 14.sp)
                        }
                    }

                    // Speclist info overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "SENSOR M51: ${selectedPhoto!!.sensorModel}",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Resolusi: ${selectedPhoto!!.resolutionMegaPixels} MP | Aperture: ${selectedPhoto!!.aperture}",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Scene: ${selectedPhoto!!.sceneDetected}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }

                    // COMPARE HOLD BUTTON
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        showOriginalBeforeComparison = true
                                        awaitRelease()
                                        showOriginalBeforeComparison = false
                                    }
                                )
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = if (showOriginalBeforeComparison) Color.Yellow else Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = null,
                                tint = if (showOriginalBeforeComparison) Color.Black else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (showOriginalBeforeComparison) "Menampilkan Asli" else "Tahan untuk Bandingkan",
                                color = if (showOriginalBeforeComparison) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Empty list state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Belum Ada Foto yang Ditangkap",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Sliders and controls (tuning options)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212)),
                color = Color(0xFF141414),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    if (selectedPhoto != null) {
                        // Slider - COLOR SATURATION (Saturasi - Colorful)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Saturation (Tuning Colorful)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format("%.1f x", saturation),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = saturation,
                                onValueChange = {
                                    viewModel.saturation.value = it
                                    viewModel.commitCurrentPhotoTuning()
                                },
                                valueRange = 0.0f..2.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF)
                                )
                            )
                        }

                        // Slider - HDR STRENGTH (Kontras Dinamis HDR)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "HDR Dynamic Strength (Tuning Lebar)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format("%.1f x", hdrStrength),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = hdrStrength,
                                onValueChange = {
                                    viewModel.hdrStrength.value = it
                                    viewModel.commitCurrentPhotoTuning()
                                },
                                valueRange = 0.5f..2.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF)
                                )
                            )
                        }

                        // Expandable shadow recovery sliders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Shadow recovery column
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Shadow Recover",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                                Slider(
                                    value = shadowRecovery,
                                    onValueChange = {
                                        viewModel.shadowRecovery.value = it
                                        viewModel.commitCurrentPhotoTuning()
                                    },
                                    valueRange = 0.0f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38EF7D), activeTrackColor = Color(0xFF38EF7D))
                                )
                            }

                            // Highlight compression column
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Highlight Compress",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                                Slider(
                                    value = highlightCompression,
                                    onValueChange = {
                                        viewModel.highlightCompression.value = it
                                        viewModel.commitCurrentPhotoTuning()
                                    },
                                    valueRange = 0.0f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFE50914), activeTrackColor = Color(0xFFE50914))
                                )
                            }
                        }

                        // Live Focus Bokeh Intensity Slider - only visible for Depth Lens photos
                        if (selectedPhoto!!.lensType == "DEPTH") {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Live Focus 3D Bokeh Depth (f/Aperture)",
                                        color = Color(0xFFF48FB1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = String.format("Aperture f/%.1f", 1.4f + (1.0f - bokehLevel) * 14f),
                                        color = Color(0xFFF48FB1),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                        value = bokehLevel,
                                        onValueChange = {
                                            viewModel.bokehLevel.value = it
                                            viewModel.commitCurrentPhotoTuning()
                                        },
                                        valueRange = 0.0f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFF48FB1),
                                        activeTrackColor = Color(0xFFF48FB1)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Actions Line: Delete specific item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Opsi Foto",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )

                            Button(
                                onClick = { viewModel.deletePhoto(selectedPhoto!!) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hapus Foto", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // horizontal slide list of all photos
                    Text(
                        text = "LIHAT SEMUA FOTO (${photosList.size}):",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        photosList.forEach { photo ->
                            val active = selectedPhoto?.id == photo.id
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (active) 3.dp else 1.dp,
                                        color = if (active) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.selectPhotoForTuning(photo)
                                    }
                            ) {
                                AsyncImage(
                                    model = File(photo.filePath),
                                    contentDescription = "Thumb",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Micro indicator of the lens used
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(topStart = 4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    val lensCode = when (photo.lensType) {
                                        "ULTRA_WIDE" -> "UW"
                                        "WIDE" -> "W"
                                        "DEPTH" -> "LF"
                                        "MACRO" -> "MC"
                                        else -> "SF"
                                    }
                                    Text(
                                        text = lensCode,
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
