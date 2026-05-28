package com.adbstudio.desktop.feature.apps.data

import java.io.File

object PinnedAppsManager {
    private val pinsFile = File(System.getProperty("user.home"), ".ADBStudio/pinned_apps.txt")

    fun load(): Set<String> {
        return try {
            if (pinsFile.exists()) {
                pinsFile.readLines().toSet()
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun save(packages: Set<String>) {
        try {
            pinsFile.parentFile?.mkdirs()
            pinsFile.writeText(packages.joinToString("\n"))
        } catch (e: Exception) {
            // Silently fail
        }
    }
}
