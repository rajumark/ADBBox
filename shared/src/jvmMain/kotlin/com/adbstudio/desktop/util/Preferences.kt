package com.adbstudio.desktop.util

import java.io.File
import java.util.Properties

class Preferences(appDataDir: String) {
    private val file = File(appDataDir, "preferences.properties")
    private val props = Properties()

    init {
        if (file.exists()) {
            file.inputStream().use { props.load(it) }
        }
    }

    fun save() {
        file.parentFile.mkdirs()
        file.outputStream().use { props.store(it, "ADBStudio Preferences") }
    }

    var lastScreen: String
        get() = props.getProperty("lastScreen", "")
        set(value) {
            props.setProperty("lastScreen", value)
            save()
        }

    var themeMode: String
        get() = props.getProperty("themeMode", "System")
        set(value) {
            props.setProperty("themeMode", value)
            save()
        }

    var packageFilter: String
        get() = props.getProperty("packageFilter", "User")
        set(value) {
            props.setProperty("packageFilter", value)
            save()
        }
}
