package com.soywiz.util

import java.io.*

fun File.getOrNull(vararg folders: String, returnFirst: Boolean = false): File? {
    for (folder in folders) {
        val dir = File(this, folder).takeIf { it.exists() }
        if (dir != null) return dir
    }
    if (returnFirst && folders.isNotEmpty()) return File(this, folders.first())
    return null
}

operator fun File.get(vararg folders: String): File =
    getOrNull(*folders, returnFirst = true) ?: error("Can't find any of $folders in $this")
