package org.graphiks.webgpu.fetcher

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

internal object SpecificationFileIO {
    private const val BUFFER_SIZE = 8 * 1024

    fun copyToFile(input: InputStream, destination: Path): Long = input.use { source ->
        Files.newOutputStream(destination).use { output ->
            source.copyTo(output, bufferSize = BUFFER_SIZE)
        }
    }

    fun sha256File(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        Files.newInputStream(path).use { input ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
