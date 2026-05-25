package com.adbstudio.desktop.util

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.skia.Image
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.File

actual fun loadIconPainter(path: String?): Painter? {
    if (path == null) return null
    return try {
        val bytes = File(path).readBytes()
        val skiaImage = Image.makeFromEncoded(bytes)
        BitmapPainter(skiaImage.toComposeImageBitmap())
    } catch (_: Exception) { null }
}
