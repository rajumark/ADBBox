package com.adbstudio.desktop.adb.model.base

/**
 * Minimal typed command model (see project_rules.md §3).
 *
 * Each command defines:
 * - CLI args (excluding the adb binary path)
 * - output parsing into a typed result
 */
interface AdbCommand<out T> {
    val serial: String?
    val id: String
    fun toCliArgs(): List<String>
    fun parse(output: String): T
}
