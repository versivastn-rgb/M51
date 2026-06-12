package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "captured_photos")
data class CapturedPhoto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,                 // File path where image is saved (can be relative if inside app storage)
    val lensType: String,                 // "Wide (1x)", "Ultra-Wide (0.5x)", "Macro", "Live Focus", "Selfie"
    val sensorModel: String,              // "Sony IMX682", "Sony IMX616", "M51 UW Sensor", "M51 Macro Sensor", etc.
    val resolutionMegaPixels: Double,     // 64.0, 12.0, 5.0, 32.0, etc.
    val aperture: String,                 // "f/1.8", "f/2.2", "f/2.4", "f/2.0"
    val fieldOfView: String?,             // "123°" for Ultra-Wide, or null
    val targetApertureValue: String?,     // simulated bokeh aperture (e.g. "f/1.4", "f/2.8", "f/5.6") if using Live Focus
    var tunedSaturation: Float = 1.4f,    // Default colorful tuning as requested (e.g., 1.4x)
    var tunedHdrStrength: Float = 1.5f,   // Default rich HDR strength (1.5x)
    var shadowRecovery: Float = 0.7f,     // 0.0 to 1.0
    var highlightCompression: Float = 0.6f, // 0.0 to 1.0
    var bokehLevel: Float = 0.0f,          // 0.0 to 1.0 (for depth lens)
    var isAiOptimizerEnabled: Boolean = true,
    var sceneDetected: String = "Sky/Nature", // Auto optimized scene (Greenery, Sky, Landscape, Portrait, Macro)
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
