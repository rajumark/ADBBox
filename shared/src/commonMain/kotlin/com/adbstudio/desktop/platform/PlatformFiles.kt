package com.adbstudio.desktop.platform

/**
 * Returns a platform-appropriate cache/temp directory for ADBStudio.
 *
 * - macOS: ~/Library/Caches/ADBStudio
 * - Linux: ~/.cache/ADBStudio
 * - Windows: %LOCALAPPDATA%\Temp\ADBStudio
 */
expect fun getAppCacheDir(): String
