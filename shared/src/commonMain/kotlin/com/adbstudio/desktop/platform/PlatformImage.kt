package com.adbstudio.desktop.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.toImageBitmapOrNull(): ImageBitmap?
