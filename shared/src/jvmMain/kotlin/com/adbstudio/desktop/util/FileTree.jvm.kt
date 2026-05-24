package com.adbstudio.desktop.util

import java.io.File

actual fun listDirectoryTree(path: String, maxDepth: Int): List<FileEntry> {
    val dir = File(path)
    if (!dir.exists() || !dir.isDirectory) return emptyList()
    return listFiles(dir, depth = 0, maxDepth = maxDepth)
}

private fun listFiles(dir: File, depth: Int, maxDepth: Int): List<FileEntry> {
    if (depth > maxDepth) return emptyList()
    return dir.listFiles()?.map { file ->
        val children = if (file.isDirectory && depth < maxDepth) {
            listFiles(file, depth + 1, maxDepth)
        } else emptyList()
        FileEntry(
            name = file.name,
            path = file.absolutePath,
            isDirectory = file.isDirectory,
            size = if (file.isFile) file.length() else 0,
            depth = depth,
            children = children,
        )
    }?.sortedWith(compareByDescending<FileEntry> { it.isDirectory }.thenBy { it.name })
        ?: emptyList()
}
