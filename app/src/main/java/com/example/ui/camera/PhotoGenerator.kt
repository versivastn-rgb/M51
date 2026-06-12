package com.example.ui.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader

object PhotoGenerator {

    /**
     * Generates a high-quality artistic base bitmap representing different Samsung Galaxy M51 lenses.
     * This provides perfect mock scenes when real camera permission is bypassed or when run on emulators.
     * The designs contain rich colors and details to test the "Colorful Tuning" and "Wide HDR" filters.
     */
    fun generateMockM51Photo(lens: M51Lens, sceneName: String): Bitmap {
        val width = 1080
        val height = when (lens) {
            M51Lens.ULTRA_WIDE -> 720  // wider 16:10 ratio for panoramic 123° effect
            M51Lens.SELFIE -> 1350    // portrait 4:5 ratio for selfies
            else -> 1080              // square / standard 4:3 crop
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (lens) {
            M51Lens.ULTRA_WIDE -> {
                // Design: Epic wide landscape with slightly curved 123° panoramic horizon
                // Cobalt Sky
                val skyGrad = LinearGradient(0f, 0f, 0f, height * 0.6f, Color.parseColor("#0F2027"), Color.parseColor("#203A43"), Shader.TileMode.CLAMP)
                paint.shader = skyGrad
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Sun rays
                paint.shader = null
                paint.color = Color.parseColor("#22FFFFE0")
                for (i in 0..12) {
                    canvas.drawCircle(width * 0.8f, height * 0.2f, (100 * i).toFloat(), paint)
                }

                // Curved wide-angle Horizon / Ocean
                val oceanGrad = LinearGradient(0f, height * 0.5f, 0f, height.toFloat(), Color.parseColor("#2C5364"), Color.parseColor("#0F2027"), Shader.TileMode.CLAMP)
                paint.shader = oceanGrad
                val curvePath = RectF(-200f, height * 0.45f, width.toFloat() + 200f, height.toFloat() * 1.6f)
                canvas.drawOval(curvePath, paint)

                // Majestic Distant Mountains (curved perspective)
                paint.shader = null
                paint.color = Color.parseColor("#1C2A38")
                canvas.drawOval(RectF(-100f, height * 0.48f, width * 0.6f, height * 0.9f), paint)
                paint.color = Color.parseColor("#15202B")
                canvas.drawOval(RectF(width * 0.4f, height * 0.46f, width + 150f, height * 0.95f), paint)

                // Tiny sailing boat to show enormous 123° scale
                paint.color = Color.WHITE
                canvas.drawRect(width * 0.3f, height * 0.55f, width * 0.31f, height * 0.57f, paint)
                paint.color = Color.parseColor("#E53935")
                canvas.drawCircle(width * 0.305f, height * 0.55f, 6f, paint)
            }

            M51Lens.WIDE -> {
                // Design: Tropical Sunset - tests "Vivid Saturation" and "Shadow Highlight Compression"
                // Rich Warm Sky
                val skyGrad = LinearGradient(0f, 0f, 0f, height * 0.6f, Color.parseColor("#E65100"), Color.parseColor("#FFB74D"), Shader.TileMode.CLAMP)
                paint.shader = skyGrad
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Giant Glowing Sun (HDR Highlighting)
                paint.shader = null
                paint.color = Color.parseColor("#FFE082")
                canvas.drawCircle(width * 0.5f, height * 0.45f, 150f, paint)
                paint.color = Color.parseColor("#FFFFFF")
                canvas.drawCircle(width * 0.5f, height * 0.45f, 100f, paint)

                // Sea with Sunset golden reflections
                val seaGrad = LinearGradient(0f, height * 0.55f, 0f, height.toFloat(), Color.parseColor("#311B92"), Color.parseColor("#0D47A1"), Shader.TileMode.CLAMP)
                paint.shader = seaGrad
                canvas.drawRect(0f, height * 0.55f, width.toFloat(), height.toFloat(), paint)

                // Rich Sun Reflection paths (tests highlights recovery)
                paint.shader = null
                paint.color = Color.parseColor("#33FFE082")
                canvas.drawOval(RectF(width * 0.35f, height * 0.56f, width * 0.65f, height * 0.82f), paint)
                paint.color = Color.parseColor("#99FFFFFF")
                canvas.drawOval(RectF(width * 0.45f, height * 0.62f, width * 0.55f, height * 0.70f), paint)

                // Lush Palm Tree Silhouette on side (tests Shadow Recovery)
                paint.color = Color.parseColor("#1B003A")
                // Trunk
                canvas.drawRect(0f, height * 0.4f, 50f, height.toFloat(), paint)
                // Leaves (drawn as simple arcs)
                val leaves = RectF(-150f, height * 0.15f, 250f, height * 0.55f)
                canvas.drawOval(leaves, paint)
            }

            M51Lens.DEPTH -> {
                // Design: Professional Face Portrait with Depth Layers (tests Bokeh Slider)
                // Colorful Studio Ambient Background
                val bgGrad = RadialGradient(width * 0.5f, height * 0.5f, width * 0.8f, Color.parseColor("#3F2B96"), Color.parseColor("#A8C0FF"), Shader.TileMode.CLAMP)
                paint.shader = bgGrad
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Glow Orbs / Blur Bokeh Lights in background (drawn separately to demonstrate blur)
                paint.shader = null
                val bokehColors = listOf("#FF4081", "#E040FB", "#00E5FF", "#FFEB3B")
                val bokehProps = listOf(
                    Triple(0.2f, 0.3f, 120f),
                    Triple(0.8f, 0.25f, 160f),
                    Triple(0.15f, 0.7f, 140f),
                    Triple(0.85f, 0.75f, 180f)
                )
                for (i in bokehProps.indices) {
                    val prop = bokehProps[i]
                    paint.color = Color.parseColor(bokehColors[i % bokehColors.size])
                    paint.alpha = 140
                    canvas.drawCircle(width * prop.first, height * prop.second, prop.third, paint)
                }
                paint.alpha = 255 // restore alpha

                // Main Subject: Sharp Portrait silhouette in foreground (remains sharp as background blurs)
                paint.color = Color.parseColor("#1A1A24")
                // Core head-and-shoulders avatar
                canvas.drawCircle(width * 0.5f, height * 0.42f, 200f, paint) // Head
                val bodyOval = RectF(width * 0.25f, height * 0.68f, width * 0.75f, height * 1.5f)
                canvas.drawOval(bodyOval, paint) // Shoulders

                // Hair / Accent glows to emphasize "Studio M51 lighting"
                val glowGrad = LinearGradient(0f, height * 0.2f, width.toFloat(), height * 0.4f, Color.parseColor("#FFFF4081"), Color.parseColor("#FF00E5FF"), Shader.TileMode.CLAMP)
                paint.shader = glowGrad
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 14f
                canvas.drawCircle(width * 0.5f, height * 0.42f, 208f, paint)
                paint.style = Paint.Style.FILL
                paint.shader = null
            }

            M51Lens.MACRO -> {
                // Design: Emerald Close-up of a Flower with Ladybug (tests "Macro Saturated Greens & Reds")
                // Deep Foresty Green blurred Background
                val forestGrad = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), Color.parseColor("#11998e"), Color.parseColor("#38ef7d"), Shader.TileMode.CLAMP)
                paint.shader = forestGrad
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Flower Core (Super Macro Focus)
                paint.shader = null
                paint.color = Color.parseColor("#FFEB3B") // bright yellow chamomile ring
                canvas.drawCircle(width * 0.3f, height * 0.6f, 400f, paint)
                paint.color = Color.parseColor("#F57F17") // deep orange core details
                canvas.drawCircle(width * 0.3f, height * 0.6f, 250f, paint)

                // Floral details (concentric rings for microscopic texture)
                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#FFD54F")
                paint.strokeWidth = 8f
                for (r in 1..4) {
                    canvas.drawCircle(width * 0.3f, height * 0.6f, (70 * r).toFloat(), paint)
                }
                paint.style = Paint.Style.FILL

                // Focal Ladybug (Ultra High-Res focus subject)
                // Red body
                paint.color = Color.parseColor("#D50000")
                val ladybug = RectF(width * 0.52f, height * 0.42f, width * 0.72f, height * 0.58f)
                canvas.drawOval(ladybug, paint)
                // Black head
                paint.color = Color.BLACK
                canvas.drawCircle(width * 0.53f, height * 0.5f, 45f, paint)
                // Dots
                canvas.drawCircle(width * 0.6f, height * 0.46f, 15f, paint)
                canvas.drawCircle(width * 0.66f, height * 0.47f, 12f, paint)
                canvas.drawCircle(width * 0.61f, height * 0.54f, 15f, paint)
                canvas.drawCircle(width * 0.67f, height * 0.53f, 12f, paint)
                // Line dividing back
                paint.strokeWidth = 6f
                canvas.drawLine(width * 0.53f, height * 0.5f, width * 0.72f, height * 0.5f, paint)
            }

            M51Lens.SELFIE -> {
                // Design: Front-facing 32MP Beauty Selfie
                // Soft Pink-Purple Selfie studio Backdrop
                val studioGrad = RadialGradient(width * 0.5f, height * 0.4f, width * 0.7f, Color.parseColor("#F4C4F3"), Color.parseColor("#FC67FA"), Shader.TileMode.CLAMP)
                paint.shader = studioGrad
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Face / Avatar frame (Sony IMX616 Sensor details)
                paint.shader = null
                // Hair Backdrop
                paint.color = Color.parseColor("#263238")
                canvas.drawRoundRect(width * 0.28f, height * 0.25f, width * 0.72f, height * 0.75f, 100f, 100f, paint)

                // Face skin tone (warm glow)
                paint.color = Color.parseColor("#FFD54F")
                paint.color = Color.parseColor("#FFE0B2")
                canvas.drawOval(RectF(width * 0.33f, height * 0.3f, width * 0.67f, height * 0.65f), paint)

                // Cool sunglasses (reflecting beautiful pink glow)
                paint.color = Color.parseColor("#1A1A1A")
                canvas.drawRoundRect(width * 0.36f, height * 0.42f, width * 0.49f, height * 0.49f, 20f, 20f, paint)
                canvas.drawRoundRect(width * 0.51f, height * 0.42f, width * 0.64f, height * 0.49f, 20f, 20f, paint)
                paint.color = Color.WHITE
                paint.strokeWidth = 5f
                canvas.drawLine(width * 0.49f, height * 0.45f, width * 0.51f, height * 0.45f, paint)

                // Lipstick smiley smile
                paint.color = Color.parseColor("#E91E63")
                canvas.drawOval(RectF(width * 0.44f, height * 0.55f, width * 0.56f, height * 0.59f), paint)

                // Glowing Neon "Vibes" text in background
                paint.color = Color.parseColor("#FFFF00E5")
                paint.textSize = 50f
                paint.isFakeBoldText = true
                canvas.drawText("M51 SELFIE", width * 0.1f, height * 0.12f, paint)
                canvas.drawText("32MP HDR", width * 0.68f, height * 0.12f, paint)
            }
        }

        return bitmap
    }
}
