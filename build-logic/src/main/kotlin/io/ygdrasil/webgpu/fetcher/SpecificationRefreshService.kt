package io.ygdrasil.webgpu.fetcher

import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

data class SpecificationSource(val fileName: String, val url: URI)

data class RefreshResult(val changedFiles: Set<String>, val hashes: Map<String, String>)

class SpecificationRefreshException(message: String, cause: Throwable) : RuntimeException(message, cause)

fun interface FileMoveOperation {
    fun move(source: Path, target: Path, atomic: Boolean)
}

class NioFileMoveOperation : FileMoveOperation {
    override fun move(source: Path, target: Path, atomic: Boolean) {
        if (atomic) Files.move(source, target, ATOMIC_MOVE, REPLACE_EXISTING)
        else Files.move(source, target, REPLACE_EXISTING)
    }
}

class SpecificationRefreshService(
    private val clock: Clock = Clock.systemUTC(),
    move: FileMoveOperation = NioFileMoveOperation(),
) {
    private val resourceReplacer = AtomicFileReplacer(move)
    private val cacheWriter = AtomicCacheWriter(move)
    private val json = Json

    fun refresh(resourceDirectory: Path, sources: List<SpecificationSource>): RefreshResult {
        val allUrls = sources.joinToString(", ") { it.url.toString() }
        try {
            Files.createDirectories(resourceDirectory)
        } catch (failure: Exception) {
            throw SpecificationRefreshException(
                "create resource directory $resourceDirectory for $allUrls failed",
                failure,
            )
        }
        val cacheFile = resourceDirectory.resolve(CACHE_FILE_NAME)
        try {
            if (Files.exists(cacheFile)) {
                json.decodeFromString<SpecificationFileCache>(Files.readString(cacheFile))
            }
        } catch (failure: Exception) {
            throw SpecificationRefreshException("read cache $cacheFile while checking $allUrls", failure)
        }

        val staged = mutableListOf<StagedResource>()
        var stagedCache: Path? = null
        try {
            for (source in sources) {
                val stage = try {
                    Files.createTempFile(resourceDirectory, ".${source.fileName}.", ".download.tmp")
                } catch (failure: Exception) {
                    throw refreshFailure("stage download", source.url, failure)
                }
                staged += StagedResource(source, stage, "")
                try {
                    download(source, stage)
                    staged[staged.lastIndex] = staged.last().copy(hash = sha256(Files.readAllBytes(stage)))
                } catch (failure: SpecificationRefreshException) {
                    throw failure
                } catch (failure: Exception) {
                    throw refreshFailure("download and validate", source.url, failure)
                }
            }

            val changedFiles = linkedSetOf<String>()
            val actualHashes = mutableMapOf<String, String>()
            for (resource in staged) {
                val target = resourceDirectory.resolve(resource.source.fileName)
                val targetHash = try {
                    if (Files.isRegularFile(target)) sha256(Files.readAllBytes(target)) else null
                } catch (failure: Exception) {
                    throw refreshFailure("hash existing target", resource.source.url, failure)
                }
                if (targetHash != resource.hash) {
                    try {
                        resourceReplacer.replaceResource(resource.path, target, "replace ${resource.source.url}")
                        changedFiles += resource.source.fileName
                    } catch (failure: Exception) {
                        throw refreshFailure("replace ${resource.source.fileName}", resource.source.url, failure)
                    }
                }
                actualHashes[resource.source.fileName] = resource.hash
            }

            val checkedAt = LocalDateTime.ofInstant(clock.instant(), clock.zone)
            val entriesByName = linkedMapOf<String, CachedSpecification>()
            for (resource in staged) {
                entriesByName[resource.source.fileName] = CachedSpecification(resource.source.fileName, resource.hash, checkedAt)
            }
            val nextCache = SpecificationFileCache(entriesByName.values.toList())
            stagedCache = try {
                Files.createTempFile(resourceDirectory, ".cache.", ".tmp")
            } catch (failure: Exception) {
                throw SpecificationRefreshException("stage cache for $allUrls", failure)
            }
            try {
                Files.writeString(stagedCache, json.encodeToString(nextCache))
                cacheWriter.replace(stagedCache, cacheFile, "commit cache for $allUrls")
            } catch (failure: Exception) {
                throw SpecificationRefreshException("commit cache for $allUrls", failure)
            }
            return RefreshResult(changedFiles, actualHashes)
        } finally {
            for (resource in staged) runCatching { Files.deleteIfExists(resource.path) }
            stagedCache?.let { runCatching { Files.deleteIfExists(it) } }
        }
    }

    private fun download(source: SpecificationSource, destination: Path) {
        val connection = source.url.toURL().openConnection() as? HttpURLConnection
            ?: error("unsupported connection for ${source.url}")
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            val status = connection.responseCode
            if (status !in 200..299) error("HTTP status $status")
            val body = connection.inputStream.use { it.readBytes() }
            if (body.isEmpty()) error("empty response body")
            Files.write(destination, body)
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private fun refreshFailure(operation: String, url: URI, cause: Throwable) =
        SpecificationRefreshException("$operation for $url failed", cause)

    private data class StagedResource(val source: SpecificationSource, val path: Path, val hash: String)

    companion object {
        private const val CACHE_FILE_NAME = "cache.json"
    }
}

@Serializable
private data class SpecificationFileCache(val cachedFiles: List<CachedSpecification>)

@Serializable
private data class CachedSpecification(
    val name: String,
    val hash: String,
    @Serializable(with = LocalDateTimeIsoSerializer::class)
    val updateDate: LocalDateTime,
)

private object LocalDateTimeIsoSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime =
        LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}
