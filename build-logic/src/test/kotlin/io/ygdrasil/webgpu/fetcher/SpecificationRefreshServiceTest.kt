package io.ygdrasil.webgpu.fetcher

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

class SpecificationRefreshServiceTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private lateinit var directory: Path
    private lateinit var server: HttpServer
    private lateinit var baseUrl: String
    private val fixedInstant = Instant.parse("2026-09-20T11:12:13Z")
    private val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val resources = linkedMapOf<String, Response>()
    private val htmlBytes = "html body v1".toByteArray()
    private val idlBytes = "interface GPU {};".toByteArray()
    private lateinit var htmlUrl: URI
    private lateinit var idlUrl: URI
    private lateinit var sources: List<SpecificationSource>

    @Before
    fun setUp() {
        directory = temporaryFolder.newFolder().toPath()
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val response = resources[exchange.requestURI.path] ?: Response(404, "missing".toByteArray())
            exchange.sendResponseHeaders(response.status, response.body.size.toLong())
            exchange.responseBody.use { it.write(response.body) }
        }
        server.start()
        baseUrl = "http://127.0.0.1:${server.address.port}"
        htmlUrl = URI("$baseUrl/webgpu.html")
        idlUrl = URI("$baseUrl/webgpu.idl")
        resources["/webgpu.html"] = Response(200, htmlBytes)
        resources["/webgpu.idl"] = Response(200, idlBytes)
        sources = listOf(SpecificationSource("webgpu.html", htmlUrl), SpecificationSource("webgpu.idl", idlUrl))
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun refreshDownloadsMissingFilesAndCache() {
        val result = service().refresh(directory, sources)

        assertEquals(setOf("webgpu.html", "webgpu.idl"), result.changedFiles)
        assertEquals(mapOf("webgpu.html" to sha256(htmlBytes), "webgpu.idl" to sha256(idlBytes)), result.hashes)
        assertContentEquals(htmlBytes, target("webgpu.html").readBytes())
        assertContentEquals(idlBytes, target("webgpu.idl").readBytes())
        assertEquals(setOf("webgpu.html", "webgpu.idl"), cacheEntries().keys)
        assertEquals(setOf(checkedAt()), cacheEntries().values.map { it.second }.toSet())
    }

    @Test
    fun refreshKeepsUnchangedFilesAndRefreshesCheckedAt() {
        target("webgpu.html").writeBytes(htmlBytes)
        target("webgpu.idl").writeBytes(idlBytes)
        writeCache("webgpu.html" to sha256(htmlBytes), "webgpu.idl" to sha256(idlBytes), timestamp = "2026-09-19T11:12:13")
        var resourceMoves = 0
        val result = service(FileMoveOperation { source, destination, atomic ->
            if (destination.fileName.toString() != "cache.json") resourceMoves++
            Files.move(source, destination, *moveOptions(atomic))
        }).refresh(directory, sources)

        assertTrue(result.changedFiles.isEmpty())
        assertEquals(0, resourceMoves)
        assertEquals(setOf(checkedAt()), cacheEntries().values.map { it.second }.toSet())
    }

    @Test
    fun refreshReplacesChangedFilesAndStoresNewHashes() {
        target("webgpu.html").writeBytes("old html".toByteArray())
        target("webgpu.idl").writeBytes("old idl".toByteArray())
        writeCache("webgpu.html" to "old-html-hash", "webgpu.idl" to "old-idl-hash")

        val result = service().refresh(directory, sources)

        assertEquals(setOf("webgpu.html", "webgpu.idl"), result.changedFiles)
        assertContentEquals(htmlBytes, target("webgpu.html").readBytes())
        assertContentEquals(idlBytes, target("webgpu.idl").readBytes())
        assertEquals(sha256(htmlBytes), cacheEntries().getValue("webgpu.html").first)
        assertEquals(sha256(idlBytes), cacheEntries().getValue("webgpu.idl").first)
    }

    @Test
    fun refreshRepairsModifiedTargetEvenWhenCacheHashMatches() {
        writeCache("webgpu.idl" to sha256(idlBytes))
        target("webgpu.idl").writeBytes("local edit".toByteArray())

        service().refresh(directory, listOf(sources[1]))

        assertContentEquals(idlBytes, target("webgpu.idl").readBytes())
        assertEquals(sha256(idlBytes), cacheEntries().getValue("webgpu.idl").first)
    }

    @Test
    fun refreshCreatesMissingTargetEvenWhenCacheHashMatches() {
        writeCache("webgpu.idl" to sha256(idlBytes))
        assertFalse(target("webgpu.idl").toFile().exists())

        service().refresh(directory, listOf(sources[1]))

        assertContentEquals(idlBytes, target("webgpu.idl").readBytes())
    }

    @Test
    fun refreshDoesNotReplaceAnySourceWhenSecondDownloadFails() {
        target("webgpu.html").writeBytes("old html".toByteArray())
        target("webgpu.idl").writeBytes("old idl".toByteArray())
        writeCache("webgpu.html" to "old-html-hash", "webgpu.idl" to "old-idl-hash")
        val oldTargets = snapshotTargets()
        val oldCache = directory.resolve("cache.json").readBytes()
        resources["/webgpu.idl"] = Response(503, "unavailable".toByteArray())

        val error = assertFailsWith<SpecificationRefreshException> { service().refresh(directory, sources) }

        assertTrue(error.message.orEmpty().contains(idlUrl.toString()))
        assertTrue(error.message.orEmpty().contains("download"))
        assertEquals(oldTargets, snapshotTargets())
        assertContentEquals(oldCache, directory.resolve("cache.json").readBytes())
        assertTrue(temporaryFiles().isEmpty())
    }

    @Test
    fun refreshRejectsEmptyResponseAndCleansTemporaryFiles() {
        resources["/webgpu.html"] = Response(200, byteArrayOf())

        val error = assertFailsWith<SpecificationRefreshException> {
            service().refresh(directory, listOf(sources[0]))
        }

        assertTrue(error.message.orEmpty().contains(htmlUrl.toString()))
        assertTrue(error.message.orEmpty().contains("download"))
        assertTrue(temporaryFiles().isEmpty())
        assertFalse(target("webgpu.html").toFile().exists())
    }

    @Test
    fun refreshNormalizesDuplicateCacheEntries() {
        writeRawCache(listOf(
            CacheEntry("webgpu.idl", "stale-one", "2026-09-18T01:00:00"),
            CacheEntry("unconfigured.txt", "keep", "2026-09-18T02:00:00"),
            CacheEntry("webgpu.idl", "stale-two", "2026-09-18T03:00:00"),
        ))

        service().refresh(directory, listOf(sources[1]))

        assertEquals(setOf("webgpu.idl"), cacheEntries().keys)
        assertEquals(sha256(idlBytes), cacheEntries().getValue("webgpu.idl").first)
        assertEquals(checkedAt(), cacheEntries().getValue("webgpu.idl").second)
    }

    @Test
    fun refreshPreservesCacheWhenResourceReplacementFails() {
        target("webgpu.idl").writeBytes("old idl".toByteArray())
        writeCache("webgpu.idl" to "old-hash")
        val cacheFile = directory.resolve("cache.json")
        val oldCache = cacheFile.readBytes()
        val service = service(FileMoveOperation { _, _, _ -> throw java.nio.file.AccessDeniedException("resource denied") })

        val error = assertFailsWith<SpecificationRefreshException> { service.refresh(directory, listOf(sources[1])) }

        assertTrue(error.message.orEmpty().contains(idlUrl.toString()))
        assertTrue(error.message.orEmpty().contains("replace"))
        assertEquals("old idl", target("webgpu.idl").toFile().readText())
        assertContentEquals(oldCache, cacheFile.readBytes())
        assertTrue(temporaryFiles().isEmpty())
    }

    @Test
    fun refreshPreservesPreviousCacheWhenCacheCommitFailsAndNextRunReconciles() {
        target("webgpu.html").writeBytes("old html".toByteArray())
        target("webgpu.idl").writeBytes("old idl".toByteArray())
        writeCache("webgpu.html" to "old-html-hash", "webgpu.idl" to "old-idl-hash")
        val cacheFile = directory.resolve("cache.json")
        val oldCache = cacheFile.readBytes()
        val failedOnce = AtomicInteger()
        val move = FileMoveOperation { source, destination, atomic ->
            if (destination.fileName.toString() == "cache.json" && failedOnce.getAndIncrement() == 0) {
                throw AtomicMoveNotSupportedException(source.toString(), destination.toString(), "cache atomic unsupported")
            }
            Files.move(source, destination, *moveOptions(atomic))
        }

        val error = assertFailsWith<SpecificationRefreshException> { service(move).refresh(directory, sources) }

        assertTrue(error.message.orEmpty().contains(htmlUrl.toString()))
        assertTrue(error.message.orEmpty().contains(idlUrl.toString()))
        assertTrue(error.message.orEmpty().contains("cache"))
        assertContentEquals(oldCache, cacheFile.readBytes())
        assertContentEquals(htmlBytes, target("webgpu.html").readBytes())
        assertContentEquals(idlBytes, target("webgpu.idl").readBytes())
        assertTrue(temporaryFiles().isEmpty())

        service().refresh(directory, sources)

        assertEquals(sha256(htmlBytes), cacheEntries().getValue("webgpu.html").first)
        assertEquals(sha256(idlBytes), cacheEntries().getValue("webgpu.idl").first)
        assertTrue(temporaryFiles().isEmpty())
    }

    @Test
    fun refreshFailsOnMalformedCacheWithoutOverwritingIt() {
        val cacheFile = directory.resolve("cache.json")
        val malformed = "{ definitely not cache".toByteArray()
        cacheFile.writeBytes(malformed)

        val error = assertFailsWith<SpecificationRefreshException> { service().refresh(directory, sources) }

        assertTrue(error.message.orEmpty().contains("cache"))
        assertContentEquals(malformed, cacheFile.readBytes())
        assertTrue(temporaryFiles().isEmpty())
    }

    @Test
    fun refreshWrapsResourceDirectoryPreparationFailureWithSources() {
        val occupiedPath = temporaryFolder.newFile("occupied-resource-directory").toPath()

        val error = assertFailsWith<SpecificationRefreshException> {
            service().refresh(occupiedPath, sources)
        }

        assertTrue(error.message.orEmpty().contains("create resource directory"))
        assertTrue(error.message.orEmpty().contains(htmlUrl.toString()))
        assertTrue(error.message.orEmpty().contains(idlUrl.toString()))
        assertTrue(Files.list(occupiedPath.parent).use { stream ->
            stream.noneMatch { it.fileName.toString().startsWith(".") || it.fileName.toString().contains("tmp") }
        })
    }

    private fun service(move: FileMoveOperation = FileMoveOperation { source, destination, atomic ->
        Files.move(source, destination, *moveOptions(atomic))
    }) = SpecificationRefreshService(fixedClock, move)

    private fun target(fileName: String) = directory.resolve(fileName)

    private fun snapshotTargets() = listOf("webgpu.html", "webgpu.idl").associateWith { name ->
        target(name).takeIf { it.toFile().exists() }?.readBytes()?.toList()
    }

    private fun temporaryFiles() = Files.list(directory).use { stream ->
        stream.filter { it.fileName.toString().contains("tmp") || it.fileName.toString().startsWith(".") }.toList()
    }

    private fun writeCache(vararg hashes: Pair<String, String>, timestamp: String = "2026-09-19T00:00:00") {
        writeRawCache(hashes.map { CacheEntry(it.first, it.second, timestamp) })
    }

    private fun writeRawCache(entries: List<CacheEntry>) {
        val body = entries.joinToString(",") {
            "{\"name\":\"${it.name}\",\"hash\":\"${it.hash}\",\"updateDate\":\"${it.timestamp}\"}"
        }
        directory.resolve("cache.json").writeText("{\"cachedFiles\":[$body]}")
    }

    private fun cacheEntries(): Map<String, Pair<String, String>> {
        val text = directory.resolve("cache.json").toFile().readText()
        val entryRegex = Regex("\\{\\\"name\\\":\\\"([^\\\"]+)\\\",\\\"hash\\\":\\\"([^\\\"]+)\\\",\\\"updateDate\\\":\\\"([^\\\"]+)\\\"}")
        return entryRegex.findAll(text).associate { match ->
            match.groupValues[1] to (match.groupValues[2] to match.groupValues[3])
        }
    }

    private fun checkedAt() = LocalDateTime.ofInstant(fixedInstant, ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun moveOptions(atomic: Boolean): Array<java.nio.file.CopyOption> = if (atomic) {
        arrayOf(java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    } else {
        arrayOf(java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }

    private data class Response(val status: Int, val body: ByteArray)
    private data class CacheEntry(val name: String, val hash: String, val timestamp: String)
}
