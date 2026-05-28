package com.adbstudio.desktop.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage

actual fun ByteArray.toImageBitmapOrNull(): ImageBitmap? {
    return try {
        SkiaImage.makeFromEncoded(this).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}
