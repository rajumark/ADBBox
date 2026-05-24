package com.adbstudio.desktop.util

data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val depth: Int,
    val children: List<FileEntry> = emptyList(),
)

expect fun listDirectoryTree(path: String, maxDepth: Int = 3): List<FileEntry>
