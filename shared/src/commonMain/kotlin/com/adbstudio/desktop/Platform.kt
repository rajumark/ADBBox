package com.adbstudio.desktop

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform