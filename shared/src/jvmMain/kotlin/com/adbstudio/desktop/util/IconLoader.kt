package com.adbstudio.desktop.util

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.skia.Color
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.File

actual fun loadIconPainter(path: String?): Painter {
    if (path == null) return defaultAppIcon()
    return try {
        val bytes = File(path).readBytes()
        val skiaImage = Image.makeFromEncoded(bytes)
        BitmapPainter(skiaImage.toComposeImageBitmap())
    } catch (_: Exception) { defaultAppIcon() }
}

private val defaultAppIcon by lazy {
    val s = 72
    val surface = Surface.makeRasterN32Premul(s, s)
    val canvas = surface.canvas
    val bg = Paint().apply { color = Color.makeRGB(230, 230, 230) }
    canvas.drawRRect(RRect.fromRectAndRadius(Rect(0f, 0f, s.toFloat(), s.toFloat()), 12f, 12f), bg)
    val fg = Paint().apply { color = Color.makeRGB(180, 180, 180) }
    canvas.drawCircle(s / 2f, s * 0.35f, s * 0.18f, fg)
    canvas.drawRoundRect(Rect(s * 0.25f, s * 0.55f, s * 0.75f, s * 0.85f), 4f, 4f, fg)
    BitmapPainter(surface.makeImageSnapshot().toComposeImageBitmap())
}
