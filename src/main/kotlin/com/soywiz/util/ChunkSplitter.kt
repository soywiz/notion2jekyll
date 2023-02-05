package com.soywiz.util


private val SPACES_REGEX = Regex("\\s+")

fun String.splitBySpaces(): List<String> {
    val out = arrayListOf<String>()
    val spaces = SPACES_REGEX.findAll(this)
    var lastPos = 0
    for (space in spaces) {
        this.substring(lastPos, space.range.first).let { if (it.isNotEmpty()) out.add(it) }
        this.substring(space.range).let { if (it.isNotEmpty()) out.add(it) }
        lastPos = space.range.last + 1
    }
    this.substring(lastPos, this.length).let { if (it.isNotEmpty()) out.add(it) }
    return out
}

fun String.splitInSeparatedChunks(maxSize: Int = 80): List<String> {
    val out = arrayListOf<String>()
    var line = ""
    val chunks = splitBySpaces()
    fun flush() {
        if (line.isNotEmpty()) {
            out.add(line)
            line = ""
        }
    }
    for (n in 0 until chunks.size) {
        val current = chunks[n]
        if (line.length + current.length > maxSize) {
            flush()
        }
        line += current
    }
    flush()
    return out
}
