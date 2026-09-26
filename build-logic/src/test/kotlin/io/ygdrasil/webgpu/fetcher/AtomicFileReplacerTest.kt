package io.ygdrasil.webgpu.fetcher

import java.nio.file.AccessDeniedException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AtomicFileReplacerTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private val sourceUrl = "https://example.test/webgpu.idl"

    @Test
    fun resourceMoveFallsBackOnlyWhenAtomicMoveIsUnsupported() {
        val (staged, target, stagedBytes) = files()
        var atomicAttempts = 0
        var nonAtomicAttempts = 0
        val replacer = AtomicFileReplacer(FileMoveOperation { source, destination, atomic ->
            if (atomic) {
                atomicAttempts++
                throw AtomicMoveNotSupportedException(source.toString(), destination.toString(), "unsupported")
            }
            nonAtomicAttempts++
            Files.move(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        })

        replacer.replaceResource(staged, target, sourceUrl)

        assertEquals(1, atomicAttempts)
        assertEquals(1, nonAtomicAttempts)
        assertContentEquals(stagedBytes, target.toFile().readBytes())
    }

    @Test
    fun resourceMovePropagatesOtherFailures() {
        val (staged, target) = files()
        var atomicAttempts = 0
        var nonAtomicAttempts = 0
        val replacer = AtomicFileReplacer(FileMoveOperation { _, _, atomic ->
            if (atomic) atomicAttempts++ else nonAtomicAttempts++
            throw AccessDeniedException("denied")
        })

        assertFailsWith<AccessDeniedException> {
            replacer.replaceResource(staged, target, sourceUrl)
        }

        assertEquals(1, atomicAttempts)
        assertEquals(0, nonAtomicAttempts)
        assertEquals("old", target.toFile().readText())
    }

    @Test
    fun cacheMoveHasNoNonAtomicFallbackAndPreservesOldBytes() {
        val (staged, cache, _) = files()
        cache.toFile().writeBytes("old cache".toByteArray())
        val oldCache = cache.toFile().readBytes()
        var atomicAttempts = 0
        var nonAtomicAttempts = 0
        val writer = AtomicCacheWriter(FileMoveOperation { source, destination, atomic ->
            if (atomic) {
                atomicAttempts++
                throw AtomicMoveNotSupportedException(source.toString(), destination.toString(), "unsupported")
            }
            nonAtomicAttempts++
            Files.move(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        })

        assertFailsWith<AtomicMoveNotSupportedException> {
            writer.replace(staged, cache, "checking $sourceUrl and https://example.test/webgpu.html")
        }

        assertEquals(1, atomicAttempts)
        assertEquals(0, nonAtomicAttempts)
        assertContentEquals(oldCache, cache.toFile().readBytes())
    }

    private fun files(): Triple<Path, Path, ByteArray> {
        val directory = temporaryFolder.newFolder().toPath()
        val staged = directory.resolve("staged")
        val target = directory.resolve("target")
        val bytes = "new bytes".toByteArray()
        staged.toFile().writeBytes(bytes)
        target.toFile().writeText("old")
        return Triple(staged, target, bytes)
    }
}
