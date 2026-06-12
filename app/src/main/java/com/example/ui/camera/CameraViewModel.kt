package com.example.ui.camera

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CapturedPhoto
import com.example.data.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = PhotoRepository(database.capturedPhotoDao())

    // List of all captured photos from Room Database
    val photosList: StateFlow<List<CapturedPhoto>> = repository.allPhotos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Camera states
    private val _activeLens = MutableStateFlow(M51Lens.WIDE)
    val activeLens: StateFlow<M51Lens> = _activeLens.asStateFlow()

    private val _cameraFlashMode = MutableStateFlow("OFF") // "OFF", "ON", "AUTO"
    val cameraFlashMode: StateFlow<String> = _cameraFlashMode.asStateFlow()

    // Real-time Tuning sliders (applied directly during preview or capture)
    val saturation = MutableStateFlow(1.4f)         // Colorful default
    val hdrStrength = MutableStateFlow(1.5f)        // Rich HDR default
    val shadowRecovery = MutableStateFlow(0.7f)      // High dynamic shadow default
    val highlightCompression = MutableStateFlow(0.6f) // High highlight protection default
    val bokehLevel = MutableStateFlow(0.0f)          // 3D live focus bokeh strength
    val isAiOptimizerEnabled = MutableStateFlow(true)
    val sceneDetected = MutableStateFlow("Sky/Nature")

    // Active photo selected for post-editing / detail-viewing
    private val _selectedPhotoForTuning = MutableStateFlow<CapturedPhoto?>(null)
    val selectedPhotoForTuning: StateFlow<CapturedPhoto?> = _selectedPhotoForTuning.asStateFlow()

    // Status UI states
    private val _captureAnimationTrigger = MutableStateFlow(false)
    val captureAnimationTrigger: StateFlow<Boolean> = _captureAnimationTrigger.asStateFlow()

    private val _showTuningSheet = MutableStateFlow(false)
    val showTuningSheet: StateFlow<Boolean> = _showTuningSheet.asStateFlow()

    init {
        // Automatically sync active lens changes with recommended default scene optimizer settings
        viewModelScope.launch {
            _activeLens.collect { lens ->
                sceneDetected.value = when (lens) {
                    M51Lens.ULTRA_WIDE -> "Sangat Lebar / Landscape"
                    M51Lens.WIDE -> "Pemandangan / Blue Sky"
                    M51Lens.DEPTH -> "Fokus Live / Portrait"
                    M51Lens.MACRO -> "Makro Detail / Bunga"
                    M51Lens.SELFIE -> "Selfie / Glow Beauty"
                }
                // Automatically activate mock bokeh level if Depth Lens is picked
                if (lens == M51Lens.DEPTH) {
                    if (bokehLevel.value <= 0.05f) {
                        bokehLevel.value = 0.6f // default pleasing 3D portrait blur
                    }
                } else {
                    bokehLevel.value = 0.0f
                }
            }
        }
    }

    fun selectLens(lens: M51Lens) {
        _activeLens.value = lens
    }

    fun toggleFlash() {
        _cameraFlashMode.value = when (_cameraFlashMode.value) {
            "OFF" -> "ON"
            "ON" -> "AUTO"
            else -> "OFF"
        }
    }

    fun selectPhotoForTuning(photo: CapturedPhoto?) {
        _selectedPhotoForTuning.value = photo
        if (photo != null) {
            // Load photo's specific values to the active sliders for customization
            saturation.value = photo.tunedSaturation
            hdrStrength.value = photo.tunedHdrStrength
            shadowRecovery.value = photo.shadowRecovery
            highlightCompression.value = photo.highlightCompression
            bokehLevel.value = photo.bokehLevel
            isAiOptimizerEnabled.value = photo.isAiOptimizerEnabled
            _activeLens.value = M51Lens.values().firstOrNull { it.key == photo.lensType } ?: M51Lens.WIDE
        }
    }

    fun setShowTuningSheet(show: Boolean) {
        _showTuningSheet.value = show
    }

    /**
     * Captures a photo in simulation mode by writing a pre-rendered high-res
     * bitmap to local disk and registering it into the Room DB.
     * This provides perfect execution in any environment.
     */
    fun captureSimulationPhoto() {
        viewModelScope.launch {
            _captureAnimationTrigger.value = true
            
            withContext(Dispatchers.IO) {
                try {
                    val lens = _activeLens.value
                    // Create base beautiful geometric art canvas
                    val bitmap = PhotoGenerator.generateMockM51Photo(lens, sceneDetected.value)
                    
                    // Create output file inside directories
                    val app = getApplication<Application>()
                    val directory = File(app.filesDir, "m51_captures")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    
                    val file = File(directory, "M51_${lens.key}_${UUID.randomUUID().toString().take(6)}.jpg")
                    val out = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    out.flush()
                    out.close()

                    // Insert photo record with the active spectacular colorful tuning defaults
                    val newPhoto = CapturedPhoto(
                        filePath = file.absolutePath,
                        lensType = lens.key,
                        sensorModel = lens.sensor,
                        resolutionMegaPixels = lens.megapixels,
                        aperture = lens.aperture,
                        fieldOfView = lens.fov,
                        targetApertureValue = if (lens == M51Lens.DEPTH) "f/1.4" else null,
                        tunedSaturation = saturation.value,
                        tunedHdrStrength = hdrStrength.value,
                        shadowRecovery = shadowRecovery.value,
                        highlightCompression = highlightCompression.value,
                        bokehLevel = bokehLevel.value,
                        isAiOptimizerEnabled = isAiOptimizerEnabled.value,
                        sceneDetected = sceneDetected.value,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    repository.savePhoto(newPhoto)
                    
                    // Automatically select this newly captured photo to show the custom tuning page!
                    withContext(Dispatchers.Main) {
                        selectPhotoForTuning(newPhoto)
                        _showTuningSheet.value = true
                    }
                } catch (e: Exception) {
                    Log.e("CameraViewModel", "Gagal menyimpan foto mock: ", e)
                }
            }
            
            // Retract capture trigger
            kotlinx.coroutines.delay(100)
            _captureAnimationTrigger.value = false
        }
    }

    /**
     * Saves a photo taken by real CameraX. We receive the raw file path,
     * read/process it if needed, and register it in Room DB.
     */
    fun saveRealCameraXPhoto(filePath: String) {
        viewModelScope.launch {
            _captureAnimationTrigger.value = true
            
            withContext(Dispatchers.IO) {
                try {
                    val lens = _activeLens.value
                    val currentFile = File(filePath)
                    
                    // Register into Room with default Samsung dynamic color configs!
                    val newPhoto = CapturedPhoto(
                        filePath = currentFile.absolutePath,
                        lensType = lens.key,
                        sensorModel = lens.sensor,
                        resolutionMegaPixels = lens.megapixels,
                        aperture = lens.aperture,
                        fieldOfView = lens.fov,
                        targetApertureValue = if (lens == M51Lens.DEPTH) "f/1.8" else null,
                        tunedSaturation = saturation.value,
                        tunedHdrStrength = hdrStrength.value,
                        shadowRecovery = shadowRecovery.value,
                        highlightCompression = highlightCompression.value,
                        bokehLevel = bokehLevel.value,
                        isAiOptimizerEnabled = isAiOptimizerEnabled.value,
                        sceneDetected = "Auto Dynamic HDR",
                        timestamp = System.currentTimeMillis()
                    )

                    val savedId = repository.savePhoto(newPhoto)
                    val insertedPhoto = newPhoto.copy(id = savedId.toInt())
                    
                    withContext(Dispatchers.Main) {
                        selectPhotoForTuning(insertedPhoto)
                        _showTuningSheet.value = true
                    }
                } catch (e: Exception) {
                    Log.e("CameraViewModel", "Gagal mendaftarkan real camera capture: ", e)
                }
            }
            
            kotlinx.coroutines.delay(100)
            _captureAnimationTrigger.value = false
        }
    }

    /**
     * Commits adjusted tuning values of the selected photo back into local Room DB.
     * This makes changes persistent!
     */
    fun commitCurrentPhotoTuning() {
        val activePhoto = _selectedPhotoForTuning.value ?: return
        viewModelScope.launch {
            val updatedPhoto = activePhoto.copy(
                tunedSaturation = saturation.value,
                tunedHdrStrength = hdrStrength.value,
                shadowRecovery = shadowRecovery.value,
                highlightCompression = highlightCompression.value,
                bokehLevel = bokehLevel.value,
                isAiOptimizerEnabled = isAiOptimizerEnabled.value
            )
            repository.updatePhoto(updatedPhoto)
            _selectedPhotoForTuning.value = updatedPhoto
        }
    }

    /**
     * Reset tuning parameters back to recommended colorful HDR values
     */
    fun resetActiveTuningValues() {
        saturation.value = 1.4f
        hdrStrength.value = 1.5f
        shadowRecovery.value = 0.7f
        highlightCompression.value = 0.6f
        bokehLevel.value = if (_activeLens.value == M51Lens.DEPTH) 0.6f else 0.0f
        isAiOptimizerEnabled.value = true
        commitCurrentPhotoTuning()
    }

    /**
     * Delete a photo completely (file + Room entity)
     */
    fun deletePhoto(photo: CapturedPhoto) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Delete physical file
                val file = File(photo.filePath)
                if (file.exists()) {
                    file.delete()
                }
                // Delete DB record
                repository.deletePhoto(photo)
                
                // Clear active selected photo if deleted
                if (_selectedPhotoForTuning.value?.id == photo.id) {
                    withContext(Dispatchers.Main) {
                        selectPhotoForTuning(null)
                        _showTuningSheet.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Gagal menghapus file foto", e)
            }
        }
    }
}
