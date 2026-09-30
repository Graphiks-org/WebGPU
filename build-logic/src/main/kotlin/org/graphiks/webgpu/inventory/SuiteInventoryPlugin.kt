package org.graphiks.webgpu.inventory

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Generates the suite artefacts that are derived from the annotated cases and the public API:
 *
 * - `ApiSymbols.kt`, the typed `const val` references used by `@AcidTest(contract = …)`.
 * - `FoundationCases.kt`, the executable catalogue assembled from the annotations.
 * - `cases.json` / `foundation-case-ids.json`, the data the site builder and the browser runner read.
 *
 * The generated sources and manifests are build outputs, never versioned.
 */
class SuiteInventoryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register("generateSuiteInventory", GenerateSuiteInventoryTask::class.java).configure {
            apiSourceDir.set(project.rootProject.layout.projectDirectory.dir("webgpu-api/src/commonMain/kotlin"))
            caseSourceDir.set(project.layout.projectDirectory.dir("src/commonMain/kotlin/org/graphiks/webgpu/suite/acid"))
            caseIdFile.set(project.rootProject.layout.projectDirectory.file("suite-core/src/commonMain/kotlin/org/graphiks/webgpu/suite/AcidCaseId.kt"))
            familyFile.set(project.rootProject.layout.projectDirectory.file("suite-core/src/commonMain/kotlin/org/graphiks/webgpu/suite/AcidFamily.kt"))
            generatedSourceDir.set(project.layout.buildDirectory.dir("generated/suite/commonMain/kotlin"))
            inventoryDir.set(project.layout.buildDirectory.dir("suite-inventory"))
            suiteVersion.set(project.provider { project.version.toString() })
        }
    }
}

abstract class GenerateSuiteInventoryTask : DefaultTask() {

    @get:InputDirectory
    abstract val apiSourceDir: DirectoryProperty

    @get:InputDirectory
    abstract val caseSourceDir: DirectoryProperty

    @get:InputFile
    abstract val caseIdFile: RegularFileProperty

    @get:InputFile
    abstract val familyFile: RegularFileProperty

    @get:OutputDirectory
    abstract val generatedSourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val inventoryDir: DirectoryProperty

    @get:org.gradle.api.tasks.Input
    abstract val suiteVersion: org.gradle.api.provider.Property<String>

    @TaskAction
    fun generate() {
        val apiSymbols = ApiSymbolParser.parse(apiSourceDir.get().asFile)
        val nameBySymbol = ApiSymbolParser.mangleAll(apiSymbols)
        val symbolByMangled = nameBySymbol.entries.associate { (symbol, name) -> name to symbol }
        val caseIds = EnumIds.parseStrict(caseIdFile.get().asFile)
        val families = EnumIds.parseFamilies(familyFile.get().asFile)
        val cases = AcidTestParser.parse(caseSourceDir.get().asFile, symbolByMangled)
        CatalogueValidator.validate(cases, caseIds, families)
        cases.forEach { case ->
            val expected = "org.graphiks.webgpu.suite.acid.${families.getValue(case.familyEntry).packageName}"
            require(case.packageName == expected) {
                "Case ${case.functionName} is in package ${case.packageName} but family ${case.familyEntry} expects $expected"
            }
        }

        writeApiSymbols(nameBySymbol)
        writeFoundationCases(cases)
        writeManifests(cases, caseIds, families, symbolByMangled)
        writeBaseline()
    }

    private fun writeBaseline() {
        val apiDir = apiSourceDir.get().asFile
        val files = apiDir.listFiles { file -> file.extension == "kt" }?.sortedBy { it.name } ?: emptyList()
        val commit = runCatching {
            ProcessBuilder("git", "rev-parse", "HEAD")
                .directory(apiDir)
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .readText()
                .trim()
        }.getOrDefault("unknown")
        val version = suiteVersion.get()
        inventoryDir.get().asFile.resolve("baseline.json").writeText(buildString {
            appendLine("{")
            appendLine("  \"repository\": \"https://github.com/Graphiks-org/WebGPU\",")
            appendLine("  \"commit\": \"$commit\",")
            appendLine("  \"apiVersion\": \"$version\",")
            appendLine("  \"suiteVersion\": \"$version\",")
            appendLine("  \"sourceFiles\": [")
            files.forEachIndexed { index, file ->
                val hash = java.security.MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                    .joinToString("") { "%02x".format(it) }
                appendLine("    { \"path\": \"webgpu-api/src/commonMain/kotlin/${file.name}\", \"sha256\": \"$hash\" }${if (index == files.lastIndex) "" else ","}")
            }
            appendLine("  ]")
            appendLine("}")
        })
    }

    private fun writeApiSymbols(nameBySymbol: Map<String, String>) {
        val file = generatedSourceDir.get().asFile.resolve("org/graphiks/webgpu/suite/acid/ApiSymbols.kt")
        file.parentFile.mkdirs()
        file.writeText(buildString {
            appendLine("package org.graphiks.webgpu.suite.acid")
            appendLine()
            appendLine("// Generated by :suite-acid-tests:generateSuiteInventory. Do not edit.")
            appendLine("object ApiSymbols {")
            for ((symbol, name) in nameBySymbol) {
                appendLine("    const val $name: String = \"${symbol.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
            }
            appendLine("}")
        })
    }

    private fun writeFoundationCases(cases: List<ParsedCase>) {
        val file = generatedSourceDir.get().asFile.resolve("org/graphiks/webgpu/suite/acid/FoundationCases.kt")
        file.parentFile.mkdirs()
        file.writeText(foundationCasesSource(cases))
    }

    private fun writeManifests(cases: List<ParsedCase>, caseIds: Map<String, String>, families: Map<String, EnumIds.Family>, symbolByMangled: Map<String, String>) {
        val dir = inventoryDir.get().asFile
        dir.mkdirs()
        dir.resolve("foundation-case-ids.json").writeText(foundationCaseIdsJson(cases, caseIds))
        dir.resolve("cases.json").writeText(casesJson(cases, caseIds, families, symbolByMangled))
    }
}

/** The executable catalogue source. Pure so the validator and its tests share one definition. */
internal fun foundationCasesSource(cases: List<ParsedCase>): String = buildString {
    appendLine("package org.graphiks.webgpu.suite.acid")
    appendLine()
    appendLine("import org.graphiks.webgpu.GPUFeatureName")
    appendLine("import org.graphiks.webgpu.suite.AcidCase")
    appendLine("import org.graphiks.webgpu.suite.AcidCaseId")
    appendLine("import org.graphiks.webgpu.suite.AcidFamily")
    cases.map { "${it.packageName}.${it.functionName}" }.distinct().sorted().forEach { appendLine("import $it") }
    appendLine()
    appendLine("// Generated by :suite-acid-tests:generateSuiteInventory. Do not edit.")
    appendLine("fun foundationCases(): List<AcidCase> = listOf(")
    for (case in cases) {
        appendLine("    AcidCase(")
        appendLine("        id = AcidCaseId.${case.idEntry},")
        appendLine("        family = AcidFamily.${case.familyEntry},")
        appendLine("        contract = listOf(${case.contract.joinToString(", ") { "ApiSymbols.$it" }}),")
        if (case.requiredFeatures.isEmpty()) {
            appendLine("        requiredFeatures = emptySet(),")
        } else {
            appendLine("        requiredFeatures = setOf(${case.requiredFeatures.joinToString(", ") { "GPUFeatureName.$it" }}),")
        }
        when (case.input) {
            "Context" -> appendLine("        run = ::${case.functionName},")
            "Device" -> appendLine("        run = { context -> ${case.functionName}(context.device) },")
            else -> error("Unknown AcidInput '${case.input}' on ${case.functionName}")
        }
        appendLine("    ),")
    }
    appendLine(")")
}

/** The dotted id list manifest, in catalogue order, exactly once per case. */
internal fun foundationCaseIdsJson(cases: List<ParsedCase>, caseIds: Map<String, String>): String =
    cases.map { caseIds.getValue(it.idEntry) }
        .joinToString(prefix = "[\n", postfix = "\n]\n", separator = ",\n") { "  \"$it\"" }

/** The full case manifest, including the repo-relative `sourcePath`. */
internal fun casesJson(
    cases: List<ParsedCase>,
    caseIds: Map<String, String>,
    families: Map<String, EnumIds.Family>,
    symbolByMangled: Map<String, String>,
): String = buildString {
    appendLine("[")
    cases.forEachIndexed { index, case ->
        appendLine("  {")
        appendLine("    \"id\": \"${caseIds.getValue(case.idEntry)}\",")
        appendLine("    \"idEntry\": \"${case.idEntry}\",")
        appendLine("    \"family\": \"${families.getValue(case.familyEntry).id}\",")
        appendLine("    \"familyEntry\": \"${case.familyEntry}\",")
        appendLine("    \"function\": \"${case.functionName}\",")
        appendLine("    \"sourcePath\": \"${case.sourceFile}\",")
        appendLine("    \"contract\": [${case.contract.joinToString(", ") { "\"${symbolByMangled.getValue(it)}\"" }}],")
        appendLine("    \"requiredFeatures\": [${case.requiredFeatures.joinToString(", ") { "\"$it\"" }}]")
        append("  }${if (index == cases.lastIndex) "" else ","}\n")
    }
    appendLine("]")
}

data class ParsedCase(
    val idEntry: String,
    val familyEntry: String,
    val contract: List<String>,
    val requiredFeatures: List<String>,
    val functionName: String,
    val packageName: String,
    /** `Device` when the annotation omits `input`; otherwise the declared [AcidInput] entry. */
    val input: String = "Device",
    /** Repo-relative source path of the case, used to link an inventory entry to its file. */
    val sourceFile: String = "",
)

object EnumIds {

    data class Family(val id: String, val packageName: String)

    /**
     * Parses `Entry("dotted.id")`, rejecting a duplicate entry or a duplicate dotted id. The
     * diagnostic names the previous owner so a copy-paste id collision can be found.
     */
    fun parseStrict(file: File): Map<String, String> {
        val pattern = Regex("""(\w+)\("([^"]+)"\)""")
        val entries = LinkedHashMap<String, String>()
        val ownerById = LinkedHashMap<String, String>()
        pattern.findAll(file.readText()).forEach {
            val entry = it.groupValues[1]
            val id = it.groupValues[2]
            val previousEntry = entries.put(entry, id)
            require(previousEntry == null) { "Duplicate AcidCaseId entry '$entry' in ${file.name}" }
            val previousOwner = ownerById.put(id, entry)
            require(previousOwner == null) {
                "Duplicate AcidCaseId id '$id' in ${file.name} (entries $previousOwner and $entry)"
            }
        }
        return entries
    }

    /** Parses `Entry("dotted.id")` lines of an enum with a String constructor parameter. */
    fun parse(file: File): Map<String, String> = parseStrict(file)

    /** Parses `Entry("dotted.id", "package")` lines of the family enum. */
    fun parseFamilies(file: File): Map<String, Family> {
        val pattern = Regex("""(\w+)\("([^"]+)"\s*,\s*"([^"]+)"\)""")
        return pattern.findAll(file.readText()).associate {
            it.groupValues[1] to Family(it.groupValues[2], it.groupValues[3])
        }
    }
}

/**
 * The catalogue invariants the generator must uphold before it writes any artefact: every enum
 * entry is used exactly once, every case id is unique, every function is unique, every family
 * exists and every contract is non-empty. All offenders are collected and reported together.
 */
object CatalogueValidator {

    fun validate(
        cases: List<ParsedCase>,
        caseIds: Map<String, String>,
        families: Map<String, EnumIds.Family>,
    ): List<ParsedCase> {
        val problems = mutableListOf<String>()
        val seenFunctions = mutableSetOf<String>()
        val seenIds = mutableSetOf<String>()
        cases.forEach { case ->
            if (!seenFunctions.add(case.functionName)) {
                problems += "Duplicate @AcidTest function '${case.functionName}'"
            }
            val id = caseIds[case.idEntry]
            if (id == null) {
                problems += "Unknown AcidCaseId entry '${case.idEntry}' on ${case.functionName}"
            } else if (!seenIds.add(id)) {
                problems += "Duplicate case id '$id' used by ${case.functionName}"
            }
            if (!families.containsKey(case.familyEntry)) {
                problems += "Unknown AcidFamily entry '${case.familyEntry}' on ${case.functionName}"
            }
            if (case.contract.isEmpty()) {
                problems += "Case ${case.functionName} declares an empty contract"
            }
        }
        val used = cases.mapTo(mutableSetOf()) { it.idEntry }
        val unused = caseIds.keys - used
        if (unused.isNotEmpty()) {
            problems += "AcidCaseId entries declared with no case: ${unused.sorted().joinToString(", ")}"
        }
        require(problems.isEmpty()) {
            "Invalid acid catalogue:\n" + problems.joinToString("\n") { " - $it" }
        }
        return cases
    }
}

object ApiSymbolParser {

    fun parse(apiDir: File): List<String> {
        val symbols = LinkedHashSet<String>()
        apiDir.listFiles { file -> file.extension == "kt" }?.sortedBy { it.name }?.forEach { file ->
            when (file.name) {
                "interfaces.kt" -> parseInterfaces(file, symbols)
                "enumerations.kt" -> parseEnumerations(file, symbols)
                "bitflags.kt" -> parseBitflags(file, symbols)
                "typealiases.kt" -> parseTypealiases(file, symbols)
                "ArrayBuffer.kt" -> parseArrayBuffer(file, symbols)
                "FlagEnumeration.kt" -> parseFlagEnumeration(file, symbols)
                "GPUTextureSwizzle.kt" -> parseSwizzle(file, symbols)
            }
        }
        return symbols.toList()
    }

    fun mangleAll(symbols: List<String>): Map<String, String> {
        val used = mutableSetOf<String>()
        val result = LinkedHashMap<String, String>()
        for (symbol in symbols) {
            var name = symbol.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
            if (name.firstOrNull()?.isDigit() == true) name = "_$name"
            var candidate = name
            var suffix = 2
            while (!used.add(candidate)) {
                candidate = "${name}_$suffix"
                suffix += 1
            }
            result[symbol] = candidate
        }
        return result
    }

    private fun parseInterfaces(file: File, symbols: MutableSet<String>) {
        var owner: String? = null
        file.forEachLine { raw ->
            val line = raw.trimEnd()
            val declaration = Regex("""^(?:sealed |fun )?interface (\w+)(.*)$""").find(line)
            if (declaration != null) {
                owner = declaration.groupValues[1]
                symbols.add(owner!!)
                val superTypes = declaration.groupValues[2]
                if ("AutoCloseable" in superTypes) symbols.add("$owner.close")
                if ("GPUPipelineBase" in superTypes) symbols.add("$owner.getBindGroupLayout")
                if ("GPUBindingCommandsMixin" in superTypes) {
                    symbols.add("$owner.setBindGroup")
                    symbols.add("$owner.setImmediates")
                }
                if ("GPUDebugCommandsMixin" in superTypes) {
                    symbols.add("$owner.pushDebugGroup")
                    symbols.add("$owner.popDebugGroup")
                    symbols.add("$owner.insertDebugMarker")
                }
                if ("GPURenderCommandsMixin" in superTypes) {
                    listOf("setPipeline", "setIndexBuffer", "setVertexBuffer", "draw", "drawIndexed", "drawIndirect", "drawIndexedIndirect")
                        .forEach { symbols.add("$owner.$it") }
                }
                return@forEachLine
            }
            if (line.startsWith("}")) {
                owner = null
                return@forEachLine
            }
            val current = owner ?: return@forEachLine
            Regex("""^\t(?:suspend )?(?:val|var|fun) (\w+)""").find(line)?.let {
                symbols.add("$current.${it.groupValues[1]}")
            }
        }
    }

    private fun parseEnumerations(file: File, symbols: MutableSet<String>) {
        var owner: String? = null
        var depth = 0
        file.forEachLine { raw ->
            val line = raw.trimEnd()
            if (owner == null) {
                Regex("""^expect enum class (\w+)""").find(line)?.let {
                    owner = it.groupValues[1]
                    symbols.add(owner!!)
                    depth = line.count { c -> c == '{' } - line.count { c -> c == '}' }
                }
                return@forEachLine
            }
            depth += line.count { c -> c == '{' } - line.count { c -> c == '}' }
            val stripped = line.trim()
            if (stripped.startsWith("}") || depth <= 0) {
                if ("}" in line) {
                    owner = null
                    depth = 0
                }
                return@forEachLine
            }
            if (stripped.isEmpty() || stripped.startsWith("/") || stripped.startsWith("*") || stripped.startsWith("@")) return@forEachLine
            Regex("""^([A-Za-z_]\w*)\s*[,;]?$""").find(stripped)?.let {
                symbols.add("$owner.${it.groupValues[1]}")
            }
        }
    }

    private fun parseBitflags(file: File, symbols: MutableSet<String>) {
        var owner: String? = null
        file.forEachLine { raw ->
            val line = raw.trimEnd()
            Regex("""^public value class (\w+)""").find(line)?.let {
                owner = it.groupValues[1]
                symbols.add(owner!!)
                return@forEachLine
            }
            val current = owner ?: return@forEachLine
            Regex("""^\s+public val\s+`?(\w+)`?:""").find(line)?.let { symbols.add("$current.${it.groupValues[1]}") }
            Regex("""^\s+public infix fun\s+(\w+)\(""").find(line)?.let { symbols.add("$current.${it.groupValues[1]}") }
            if (line.startsWith("}")) owner = null
        }
    }

    private fun parseTypealiases(file: File, symbols: MutableSet<String>) {
        file.forEachLine { line ->
            Regex("""^typealias\s+(\w+)""").find(line)?.let { symbols.add(it.groupValues[1]) }
        }
    }

    private fun parseArrayBuffer(file: File, symbols: MutableSet<String>) {
        var owner: String? = null
        file.forEachLine { raw ->
            val line = raw.trimEnd()
            if (Regex("""^expect sealed interface\s+ArrayBuffer""").containsMatchIn(line)) {
                owner = "ArrayBuffer"
                symbols.add(owner!!)
                return@forEachLine
            }
            val current = owner ?: return@forEachLine
            Regex("""^    (?:val|var|fun)\s+(\w+)""").find(line)?.let { symbols.add("$current.${it.groupValues[1]}") }
            Regex("""^        (?:actual )?fun\s+(\w+)\(""").find(line)?.let { symbols.add("$current.${it.groupValues[1]}") }
        }
    }

    private fun parseFlagEnumeration(file: File, symbols: MutableSet<String>) {
        var owner: String? = null
        file.forEachLine { raw ->
            val line = raw.trimEnd()
            if (owner == null) {
                if (Regex("""^interface\s+FlagEnumeration""").containsMatchIn(line)) {
                    owner = "FlagEnumeration"
                    symbols.add(owner!!)
                    return@forEachLine
                }
                Regex("""^fun\s+Set<FlagEnumeration>\.(\w+)""").find(line)?.let { symbols.add("Set<FlagEnumeration>.${it.groupValues[1]}") }
                return@forEachLine
            }
            Regex("""^\s+val\s+(\w+)""").find(line)?.let { symbols.add("FlagEnumeration.${it.groupValues[1]}") }
            if (line.startsWith("}")) owner = null
        }
    }

    private fun parseSwizzle(file: File, symbols: MutableSet<String>) {
        var owner: String? = null
        var inSourceEnum = false
        file.forEachLine { raw ->
            val line = raw.trimEnd()
            Regex("""^enum class\s+(\w+)""").find(line)?.let {
                owner = it.groupValues[1]
                symbols.add(owner!!)
                inSourceEnum = true
                return@forEachLine
            }
            if (inSourceEnum) {
                Regex("""^    ([A-Za-z_]\w*)\s*\(""").find(line)?.let { symbols.add("$owner.${it.groupValues[1]}") }
                if (line.startsWith("}")) {
                    inSourceEnum = false
                    owner = null
                }
                return@forEachLine
            }
            Regex("""^data class\s+(\w+)""").find(line)?.let {
                owner = it.groupValues[1]
                symbols.add(owner!!)
                return@forEachLine
            }
            val current = owner ?: return@forEachLine
            Regex("""^    (?:val|fun)\s+(\w+)""").find(line)?.let { symbols.add("$current.${it.groupValues[1]}") }
        }
    }
}

object AcidTestParser {

    fun parse(caseDir: File, symbolByName: Map<String, String>): List<ParsedCase> {
        val cases = mutableListOf<ParsedCase>()
        caseDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .forEach { file ->
            val text = file.readText()
            val packageName = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE).find(text)?.groupValues?.get(1)
                ?: error("Missing package declaration in ${file.name}")
            var index = 0
            while (true) {
                val annotationStart = text.indexOf("@AcidTest(", index)
                if (annotationStart < 0) break
                val open = text.indexOf('(', annotationStart)
                val close = matchingParen(text, open)
                val annotation = text.substring(open + 1, close)
                val following = text.substring(close + 1).trimStart()
                val functionName = Regex("""^suspend fun\s+(\w+)\s*\(""").find(following)?.groupValues?.get(1)
                    ?: error("An @AcidTest annotation in ${file.name} is not immediately followed by a suspend fun")
                val idEntry = Regex("""id\s*=\s*AcidCaseId\.(\w+)""").find(annotation)?.groupValues?.get(1)
                val familyEntry = Regex("""family\s*=\s*AcidFamily\.(\w+)""").find(annotation)?.groupValues?.get(1)
                val contractBlock = Regex("""contract\s*=\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL).find(annotation)?.groupValues?.get(1) ?: ""
                val contract = Regex("""ApiSymbols\.(\w+)""").findAll(contractBlock).map { it.groupValues[1] }.toList()
                val featuresBlock = Regex("""requiredFeatures\s*=\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL).find(annotation)?.groupValues?.get(1) ?: ""
                val features = Regex("""GPUFeatureName\.(\w+)""").findAll(featuresBlock).map { it.groupValues[1] }.toList()
                val input = Regex("""input\s*=\s*AcidInput\.(\w+)""").find(annotation)?.groupValues?.get(1) ?: "Device"
                require(idEntry != null) { "Missing @AcidTest id in ${file.name}" }
                require(familyEntry != null) { "Missing @AcidTest family in ${file.name}" }
                require(input == "Device" || input == "Context") {
                    "Unknown @AcidTest input '$input' in ${file.name}: expected AcidInput.Device or AcidInput.Context"
                }
                contract.forEach { require(symbolByName.containsKey(it)) { "Unknown ApiSymbols.$it in ${file.name}" } }
                val sourceFile = "suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/" +
                    file.relativeTo(caseDir).invariantSeparatorsPath
                cases.add(ParsedCase(idEntry, familyEntry, contract, features, functionName, packageName, input, sourceFile))
                index = close + 1
            }
        }
        return cases
    }

    private fun matchingParen(text: String, open: Int): Int {
        var depth = 0
        for (i in open until text.length) {
            when (text[i]) {
                '(' -> depth += 1
                ')' -> {
                    depth -= 1
                    if (depth == 0) return i
                }
            }
        }
        error("Unbalanced parentheses in annotation")
    }
}
