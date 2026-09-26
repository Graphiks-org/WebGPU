package io.ygdrasil.webgpu.fetcher

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.io.path.readText
import kotlin.io.path.writeText

class WebGpuSpecificationFetcherPluginTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private lateinit var server: HttpServer
    private lateinit var htmlUrl: String
    private lateinit var idlUrl: String
    private val htmlRequests = AtomicInteger()
    private val idlRequests = AtomicInteger()
    private val htmlBody = "local WebGPU HTML".toByteArray()
    private val idlBody = "interface GPU {};".toByteArray()

    @Before
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/webgpu.html") { exchange ->
            htmlRequests.incrementAndGet()
            exchange.sendResponseHeaders(200, htmlBody.size.toLong())
            exchange.responseBody.use { it.write(htmlBody) }
        }
        server.createContext("/webgpu.idl") { exchange ->
            idlRequests.incrementAndGet()
            exchange.sendResponseHeaders(200, idlBody.size.toLong())
            exchange.responseBody.use { it.write(idlBody) }
        }
        server.start()
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        htmlUrl = "$baseUrl/webgpu.html"
        idlUrl = "$baseUrl/webgpu.idl"
    }

    @After
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun pluginRegistersCheckCacheWithDefaultResourceDirectory() {
        val projectDir = temporaryFolder.newFolder("default-output-project").toPath()
        writeProject(projectDir)

        val result = runCheckCache(projectDir)

        val resourceDirectory = projectDir.resolve("webgpu-ktypes-specifications/src/jvmMain/resources")
        assertTrue(result.output.contains("check-cache"))
        assertTrue(result.output.contains("webgpu.html: changed"))
        assertTrue(result.output.contains("webgpu.idl: changed"))
        assertEquals("local WebGPU HTML", resourceDirectory.resolve("webgpu.html").readText())
        assertEquals("interface GPU {};", resourceDirectory.resolve("webgpu.idl").readText())
        assertTrue(resourceDirectory.resolve("cache.json").toFile().isFile)
        assertEquals(1, htmlRequests.get())
        assertEquals(1, idlRequests.get())
    }

    @Test
    fun checkCacheAlwaysFetchesBothConfiguredSources() {
        val projectDir = temporaryFolder.newFolder("repeat-project").toPath()
        writeProject(projectDir, "build/specification-resources")

        runCheckCache(projectDir)
        val secondResult = runCheckCache(projectDir)

        assertEquals(2, htmlRequests.get())
        assertEquals(2, idlRequests.get())
        assertTrue(secondResult.output.contains("webgpu.html: unchanged"))
        assertTrue(secondResult.output.contains("webgpu.idl: unchanged"))
        val resourceDirectory = projectDir.resolve("build/specification-resources")
        assertEquals("local WebGPU HTML", resourceDirectory.resolve("webgpu.html").readText())
        assertEquals("interface GPU {};", resourceDirectory.resolve("webgpu.idl").readText())
    }

    private fun writeProject(projectDirectory: Path, resourceDirectory: String? = null) {
        projectDirectory.resolve("settings.gradle").writeText("rootProject.name = 'plugin-test-project'\n")
        val resourceConfiguration = resourceDirectory?.let {
            "resourceDirectory.set(layout.projectDirectory.dir('$it'))"
        }.orEmpty()
        projectDirectory.resolve("build.gradle").writeText(
            """
            plugins {
                id 'io.ygdrasil.webgpu-specification-fetcher'
            }

            webGpuSpecificationFetcher {
                htmlUrl.set('$htmlUrl')
                idlUrl.set('$idlUrl')
                $resourceConfiguration
            }
            """.trimIndent() + "\n",
        )
    }

    private fun runCheckCache(projectDirectory: Path): BuildResult = GradleRunner.create()
        .withProjectDir(projectDirectory.toFile())
        .withArguments("check-cache", "--stacktrace")
        .withPluginClasspath()
        .build()
}
