package com.example.ui.camera

import androidx.compose.ui.graphics.ColorMatrix

object ImageProcessor {
    
    /**
     * Generates a custom 4x5 color matrix representing the Samsung Galaxy M51's signature camera tuning:
     * - "Colorful tuning": Saturated green foliage, vibrant blue sky boost, warm skin tones.
     * - "Wide HDR": Boosted shadow details (high dynamic range) and compressed bright highlights.
     */
    fun createTuningMatrix(
        saturation: Float,          // Range: 0.0f (B&W) to 2.5f (Super Vivid)
        hdrStrength: Float,         // Range: 0.5f to 2.5f (dynamic range boost)
        shadowRecovery: Float,       // Range: 0.0f (default) to 1.0f (highly lifted dark parts)
        highlightCompression: Float, // Range: 0.0f (bright highlight) to 1.0f (highlight containment)
        isAiOptimizerEnabled: Boolean,
        sceneType: String = ""
    ): ColorMatrix {
        val matrix = ColorMatrix()
        
        // Apply base saturation using Compose's built-in algorithm
        matrix.setToSaturation(saturation)
        val array = matrix.values
        
        // Apply signature Samsung tuning boosts
        // Samsung's IMX682 and image processing pipeline famously boosts Cyan/Sky Blues and Emerald Greens.
        var blueBoost = 1.0f
        var greenBoost = 1.0f
        var redBoost = 1.0f
        
        if (isAiOptimizerEnabled) {
            when {
                sceneType.contains("Sky", ignoreCase = true) -> {
                    // Deep lush skies and slightly warm sunset tints
                    blueBoost += 0.35f
                    redBoost += 0.15f
                }
                sceneType.contains("Nature", ignoreCase = true) || sceneType.contains("Green", ignoreCase = true) -> {
                    // Striking neon-like natural foliage (signature Samsung greens)
                    greenBoost += 0.40f
                    blueBoost += 0.10f
                }
                sceneType.contains("Portrait", ignoreCase = true) || sceneType.contains("Selfie", ignoreCase = true) -> {
                    // Gentle skin warming and highlight compression to prevent flash burnouts
                    redBoost += 0.20f
                    greenBoost -= 0.05f
                }
                sceneType.contains("Macro", ignoreCase = true) -> {
                    // Intensely vivid close-up macros with microscopic contrast details
                    redBoost += 0.25f
                    greenBoost += 0.25f
                    blueBoost += 0.15f
                }
                else -> {
                    // Default AI Scene Optimizer: general popping colors
                    blueBoost += 0.20f
                    greenBoost += 0.20f
                    redBoost += 0.10f
                }
            }
        } else {
            // Standard non-AI Colorful mode
            val saturationDelta = maxOf(0.0f, saturation - 1.0f)
            blueBoost += saturationDelta * 0.25f
            greenBoost += saturationDelta * 0.20f
            redBoost += saturationDelta * 0.10f
        }

        // Apply channel adjustments directly to the color matrix rows
        array[0] = array[0] * redBoost     // Red row
        array[6] = array[6] * greenBoost   // Green row
        array[12] = array[12] * blueBoost   // Blue row
        
        // wide HDR Shadow Recovery & Highlight Compression
        // Lift shadows: increases brightness offset primarily for shadow values
        // Highlight compression: offsets darkwards to restore highlights
        val liftFactor = (shadowRecovery * 45.0f)      // Positive offset
        val compressionFactor = (highlightCompression * 35.0f) // Negative offset
        
        val netOffset = liftFactor - compressionFactor
        array[4] += netOffset  // red channel offset
        array[9] += netOffset  // green channel offset
        array[14] += netOffset // blue channel offset
        
        // HDR Dynamic Contrast adjustment:
        // Combines contrast widening with highlight protections
        if (hdrStrength > 1.0f) {
            val contrastFactor = 1.0f + (hdrStrength - 1.0f) * 0.22f
            
            // Adjust matrix scale values
            array[0] *= contrastFactor
            array[6] *= contrastFactor
            array[12] *= contrastFactor
            
            // Shift mid-point anchor slightly upwards to recover crushed low-lights
            val dynamicMidpoint = 128f * (1.0f - contrastFactor) + (shadowRecovery * 15.0f)
            array[4] += dynamicMidpoint
            array[9] += dynamicMidpoint
            array[14] += dynamicMidpoint
        }
        
        return matrix
    }
}
