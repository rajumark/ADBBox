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

    var windowX: String
        get() = props.getProperty("windowX", "")
        set(value) {
            props.setProperty("windowX", value)
            save()
        }

    var windowY: String
        get() = props.getProperty("windowY", "")
        set(value) {
            props.setProperty("windowY", value)
            save()
        }

    var windowWidth: String
        get() = props.getProperty("windowWidth", "1200")
        set(value) {
            props.setProperty("windowWidth", value)
            save()
        }

    var windowHeight: String
        get() = props.getProperty("windowHeight", "800")
        set(value) {
            props.setProperty("windowHeight", value)
            save()
        }

    var askBeforeUninstall: Boolean
        get() = props.getProperty("askBeforeUninstall", "true").toBoolean()
        set(value) {
            props.setProperty("askBeforeUninstall", value.toString())
            save()
        }

    var askBeforeClearData: Boolean
        get() = props.getProperty("askBeforeClearData", "true").toBoolean()
        set(value) {
            props.setProperty("askBeforeClearData", value.toString())
            save()
        }
}
