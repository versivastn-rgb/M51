package com.example.ui.camera

enum class M51Lens(
    val key: String,
    val title: String,
    val description: String,
    val resolution: String,
    val megapixels: Double,
    val sensor: String,
    val aperture: String,
    val fov: String?,
    val label: String,           // text label on toggle button "0.5x", "1x", "2x", etc.
    val defaultScene: String,
    val zoomFactor: Float
) {
    ULTRA_WIDE(
        "ULTRA_WIDE",
        "Ultra-Wide Camera",
        "Kamera Ultra-Wide 12 MP f/2.2 dengan sudut lebar 123° untuk pemandangan epik.",
        "12 MP",
        12.0,
        "Samsung S5K3L6 (1/3.1\")",
        "f/2.2",
        "123°",
        "0.5x",
        "Epic Mountain Landscape",
        0.5f
    ),
    WIDE(
        "WIDE",
        "Main Camera (Wide)",
        "Kamera Utama 64 MP Sony IMX682 f/1.8 dengan autofocus PDAF & dynamic range luas.",
        "64 MP (Super HD)",
        64.0,
        "Sony IMX682 (1/1.73\", 0.8µm)",
        "f/1.8",
        null,
        "1.0x (64MP)",
        "Vibrant Sunny Skies",
        1.0f
    ),
    DEPTH(
        "DEPTH",
        "Live Focus Lens",
        "Kamera Depth 5 MP f/2.4 untuk mendeteksi latar belakang secara 3D dan bokeh artistik.",
        "5 MP (Depth Sensor)",
        5.0,
        "Samsung S5K5E9 (1/5\")",
        "f/2.4",
        null,
        "Live Focus",
        "Human Portrait bokeh",
        2.0f
    ),
    MACRO(
        "MACRO",
        "Macro Focus Lens",
        "Kamera Macro 5 MP f/2.4 untuk detail super dekat (jarak fokus optimal 4cm).",
        "5 MP (Macro)",
        5.0,
        "Focus Macro (1/5\")",
        "f/2.4",
        null,
        "Macro (4cm)",
        "Detailed Flower Closeup",
        3.0f
    ),
    SELFIE(
        "SELFIE",
        "Front Selfie Camera",
        "Kamera Depan 32 MP Sony IMX616 f/2.0 dengan sensor murni & dynamic Beauty HDR.",
        "32 MP",
        32.0,
        "Sony IMX616 (1/2.8\", 0.8µm)",
        "f/2.0",
        null,
        "Selfie (Front)",
        "Vibrant Beauty Portrait",
        1.0f
    );

    fun getDisplayName(isIndonesian: Boolean = true): String {
        return if (isIndonesian) {
            when (this) {
                ULTRA_WIDE -> "Sangat Lebar"
                WIDE -> "Kamera Utama"
                DEPTH -> "Fokus Live (Depth)"
                MACRO -> "Kamera Makro"
                SELFIE -> "Kamera Selfie"
            }
        } else title
    }
}
