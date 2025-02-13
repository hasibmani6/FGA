package io.github.fate_grand_automata

import io.github.lib_automata.Pattern
import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface IStorageProvider {
    val supportImageTempDir: File

    fun writeSupportImage(kind: SupportImageKind, name: String): OutputStream

    fun readSupportImage(kind: SupportImageKind, name: String): List<InputStream>

    fun list(kind: SupportImageKind): List<String>

    fun dropScreenshot(patterns: List<Pattern>)

    /**
     * For debugging images
     */
    fun dump(name: String, image: Pattern)
}