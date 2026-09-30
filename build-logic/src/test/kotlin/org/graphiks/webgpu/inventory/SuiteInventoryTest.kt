package org.graphiks.webgpu.inventory

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SuiteInventoryTest {

    private fun tempDir(): File = createTempDir(prefix = "acid-gen-")

    private fun fileIn(dir: File, name: String, body: String): File =
        File(dir, name).apply {
            parentFile.mkdirs()
            writeText(body)
        }

    private fun caseDir(vararg files: Pair<String, String>): File {
        val dir = tempDir()
        files.forEach { (name, body) -> fileIn(dir, name, body) }
        return dir
    }

    private fun enumFile(vararg entries: String): File =
        fileIn(
            tempDir(),
            "Ids.kt",
            "package p\nenum class AcidCaseId(val id: String) {\n${entries.joinToString("\n") { "    $it," }}\n}\n",
        )

    private fun familyFile(vararg entries: String): File =
        fileIn(
            tempDir(),
            "Families.kt",
            "package p\nenum class AcidFamily(val id: String, val packageName: String) {\n${entries.joinToString("\n") { "    $it," }}\n}\n",
        )

    private val symbols = mapOf("API_x" to "GPUDevice.createBuffer")
    private val families = mapOf("F" to EnumIds.Family("f", "p"))

    private fun case(
        idEntry: String,
        familyEntry: String,
        function: String,
        contract: String = "ApiSymbols.API_x",
    ) = """
        package p
        @AcidTest(id = AcidCaseId.$idEntry, family = AcidFamily.$familyEntry, contract = [$contract])
        suspend fun $function(device: Any) {}
    """.trimIndent()

    private fun messages(failure: Throwable): String =
        generateSequence(failure) { it.cause }.joinToString("\n") { it.message ?: "" }

    @Test
    fun `duplicate enum entry is rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> { EnumIds.parseStrict(enumFile("A(\"a\")", "A(\"a\")")) }
        assertContains(messages(failure), "Duplicate AcidCaseId entry 'A'")
    }

    @Test
    fun `duplicate enum dotted id is rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> { EnumIds.parseStrict(enumFile("A(\"a\")", "B(\"a\")")) }
        assertContains(messages(failure), "Duplicate AcidCaseId id 'a'")
    }

    @Test
    fun `duplicate case id is rejected`() {
        val dir = caseDir(
            "One.kt" to case("A", "F", "one"),
            "Two.kt" to case("B", "F", "two"),
        )
        val cases = AcidTestParser.parse(dir, symbols)
        val failure = assertFailsWith<IllegalArgumentException> {
            CatalogueValidator.validate(cases, mapOf("A" to "a", "B" to "a"), families)
        }
        assertContains(messages(failure), "Duplicate case id 'a'")
    }

    @Test
    fun `unused enum entry is rejected`() {
        val dir = caseDir("One.kt" to case("A", "F", "one"))
        val failure = assertFailsWith<IllegalArgumentException> {
            CatalogueValidator.validate(AcidTestParser.parse(dir, symbols), mapOf("A" to "a", "B" to "b"), families)
        }
        assertContains(messages(failure), "no case")
        assertContains(messages(failure), "B")
    }

    @Test
    fun `unknown enum entry is rejected`() {
        val dir = caseDir("One.kt" to case("zzz", "F", "one"))
        val failure = assertFailsWith<IllegalArgumentException> {
            CatalogueValidator.validate(AcidTestParser.parse(dir, symbols), mapOf("A" to "a"), families)
        }
        assertContains(messages(failure), "zzz")
    }

    @Test
    fun `duplicate function is rejected`() {
        val dir = caseDir("One.kt" to case("A", "F", "dup"))
        val failure = assertFailsWith<IllegalArgumentException> {
            CatalogueValidator.validate(
                AcidTestParser.parse(dir, symbols) + AcidTestParser.parse(dir, symbols),
                mapOf("A" to "a"),
                families,
            )
        }
        assertContains(messages(failure), "Duplicate @AcidTest function 'dup'")
    }

    @Test
    fun `empty contract is rejected`() {
        val dir = caseDir("One.kt" to case("A", "F", "one", contract = ""))
        val failure = assertFailsWith<IllegalArgumentException> {
            CatalogueValidator.validate(AcidTestParser.parse(dir, symbols), mapOf("A" to "a"), families)
        }
        assertContains(messages(failure), "empty contract")
    }

    @Test
    fun `non-adjacent function is rejected`() {
        val dir = caseDir(
            "Bad.kt" to """
                package p
                @AcidTest(id = AcidCaseId.A, family = AcidFamily.F, contract = [ApiSymbols.API_x])
                private fun helper() {}
                suspend fun real(device: Any) {}
            """.trimIndent(),
        )
        assertFailsWith<IllegalStateException> { AcidTestParser.parse(dir, symbols) }
    }

    @Test
    fun `absent function is rejected`() {
        val dir = caseDir(
            "Bad.kt" to """
                package p
                @AcidTest(id = AcidCaseId.A, family = AcidFamily.F, contract = [ApiSymbols.API_x])
                val notAFunction = 1
            """.trimIndent(),
        )
        assertFailsWith<IllegalStateException> { AcidTestParser.parse(dir, symbols) }
    }

    @Test
    fun `a complete catalogue validates`() {
        val dir = caseDir("One.kt" to case("A", "F", "one"))
        assertEquals(1, CatalogueValidator.validate(AcidTestParser.parse(dir, symbols), mapOf("A" to "a"), families).size)
    }

    @Test
    fun `the three generated outputs contain exactly the validated catalogue`() {
        val cases = listOf(
            ParsedCase(
                idEntry = "A", familyEntry = "F", contract = listOf("API_x"), requiredFeatures = emptyList(),
                functionName = "one", packageName = "p", input = "Device",
                sourceFile = "suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/one/One.kt",
            ),
            ParsedCase(
                idEntry = "B", familyEntry = "F", contract = listOf("API_x"), requiredFeatures = emptyList(),
                functionName = "two", packageName = "p", input = "Device",
                sourceFile = "suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/two/Two.kt",
            ),
        )
        val ids = mapOf("A" to "a", "B" to "b")
        val idList = foundationCaseIdsJson(cases, ids)
        assertEquals(listOf("a", "b"), Regex("\"([^\"]+)\"").findAll(idList).map { it.groupValues[1] }.toList())
        val source = foundationCasesSource(cases)
        assertEquals(2, Regex("AcidCase\\(").findAll(source).count())
        val manifest = casesJson(cases, ids, families, symbols)
        assertEquals(listOf("a", "b"), Regex("\"id\": \"([^\"]+)\"").findAll(manifest).map { it.groupValues[1] }.toList())
    }

    @Suppress("unused")
    private fun familyFileIsAvailable() {
        familyFile("F(\"f\", \"p\")")
    }
}
