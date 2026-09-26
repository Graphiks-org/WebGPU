package generator.lm

import generator.domain.Enumeration
import generator.domain.Interface
import generator.domain.MapperContext
import generator.files.SpecificationResources
import generator.lm.agent.DocumentationExplorerAgent
import generator.lm.agent.DocumentationRefinerAgent
import generator.lm.agent.DocumentationWriterAgent
import generator.lm.agent.JSonRefinerAgent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.gradle.api.logging.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.nio.file.Path

internal val prettyJson = Json {
    prettyPrint = true
}

class DocumentGeneratorManager(
    private val context: MapperContext,
    private val specificationResources: SpecificationResources,
    htmlDocumentation: Path,
    private val logger: Logger
) {
    private val body = Jsoup.parse(htmlDocumentation.toFile(), "UTF-8")
        ?: error("fail to parse html")

    private var currentDocumentation = emptyMap<String, String>()

    private val llmClient = LLMClient()
    private val documentationExplorerAgent = DocumentationExplorerAgent(llmClient)
    private val documentationWriterAgent = DocumentationWriterAgent(llmClient)
    private val jSonRefinerAgent = JSonRefinerAgent(llmClient)
    private val documentationRefinerAgent = DocumentationRefinerAgent(llmClient)

    val documentationFile = specificationResources.specificationsSourcePath.resolve(SpecificationResources.Files.documentationJson)

    fun inferHtmlDocumentation() = runBlocking {
        logger.info("Start inferHtmlDocumentation")
        currentDocumentation = getActualDocumentation()
        (context.commonEnumerations
            .map { Triple(it.toString(), it.name.lowercase() ,it.getDocumentationKeys()) }
            .filter { (_, _, expectedKeys) -> expectedKeys.filter { it in currentDocumentation.keys }.size != expectedKeys.size} +
        context.interfaces
            .map { Triple(it.toString(), it.name.lowercase() ,it.getDocumentationKeys()) }
            .filter { (_, _, expectedKeys) -> expectedKeys.filter { it in currentDocumentation.keys }.size != expectedKeys.size})
            .forEach { (kInterface, name, expectedKeys) ->
                val missingKeys = expectedKeys.filter { it !in currentDocumentation.keys }
                logger.info("Infer for $kInterface")
                runCatching {
                    val htmlNode = findRootNode(name) ?: error("fail to find root node for declaration $name")
                    val htmlDocumentation = inferHtmlDocumentation(htmlNode, name)
                    val kdocDocumentation = inferKdocDocumentation(kInterface, htmlDocumentation, missingKeys)
                        .filterKeys { it in missingKeys }
                    currentDocumentation += kdocDocumentation
                    val jsonString = prettyJson.encodeToString(currentDocumentation)
                    java.nio.file.Files.write(documentationFile, jsonString.toByteArray())
                }.onFailure {
                    logger.error("fail to infer for $kInterface", it)
                }
            }
    }

    private fun getActualDocumentation(): Map<String, String> = getActualDocumentation(documentationFile)

    private suspend fun inferKdocDocumentation(
        kotlinDefinition: String,
        htmlDocumentation: MutableList<Element>,
        expectedKeys: List<String>
    ): Map<String, String> {
        val userPrompt = """
            Provide only the documentation for the kotlin code and skip the rest.
            This is the kotlin code :
            
            ```kotlin
            $kotlinDefinition
            ```
            
            We expect the following keys, only provide the documentation for this keys : 
            ```json
            {
                ${expectedKeys.joinToString(", ") { "\"$it\" : \"insert documentation here\"" }}
            }
            ```
                    
            This is the HTML specification
            $htmlDocumentation
                       
            """.trimIndent()

        println("infer: $kotlinDefinition")


        var responseAsJson = documentationWriterAgent.generateDocumentation(userPrompt)
            .map { documentationRefinerAgent.refine(it).getOrThrow() }
            .getOrThrow()

        var remainingTry = 5
        while (remainingTry > 0) {
            remainingTry--
            runCatching {
                Json.Default.decodeFromString<Map<String, String>>(responseAsJson)
                    .let { return it }
            }
            responseAsJson = jSonRefinerAgent.refine(responseAsJson)
                .getOrThrow()
            println("refined json: $responseAsJson")
        }


        error("fail to get response")
    }

    private suspend fun inferHtmlDocumentation(htmlNode: Element, subject: String): MutableList<Element> {
        val selectedDocumentation = mutableListOf(htmlNode)

        // Parse before
        var shouldContinue = true
        var currentElement = htmlNode.nextElementSibling()
        while (shouldContinue && currentElement != null) {
            shouldContinue = documentationExplorerAgent.isRelevant(
                selectedDocumentation.toString(),
                currentElement.toString(),
                subject
            ).map { it.lowercase() == "yes" }.getOrElse { false }

            if (shouldContinue) {
                selectedDocumentation.add(currentElement)
                currentElement = currentElement.nextElementSibling()
            }
            println("shouldContinue: $shouldContinue")
        }

        return selectedDocumentation
    }

    private fun findRootNode(name: String): Element? = (body.select("dfn[id=dictdef-$name]").first()
        ?: body.select("dfn[id=typedefdef-$name]").first()
        ?: body.select("dfn[id=enumdef-$name]").first()
        ?: body.select("a[href=#$name]").select("a[class=self-link]").first()
        ?: body.select("dfn[id=$name]").first()
            )?.findRootNode()
}

private fun Element.findRootNode(): Element? = parent().let {
    when (it?.tagName()) {
        null, "main" -> this
        else -> it.findRootNode()
    }
}

internal fun getActualDocumentation(documentationFile: Path): Map<String, String> = runCatching {
    java.nio.file.Files.readString(documentationFile)
        .let { prettyJson.decodeFromString<Map<String, String>>(it) }
}.getOrElse { emptyMap<String, String>() }

internal fun Interface.getDocumentationKeys(): List<String> {
    val prefix = name
    return listOf(name) +
            attributes.map { "$prefix#${it.name}" } +
            methods.map { "$prefix#${it.name}(${it.parameters.joinToString(", ") { it.name }})" }
}

internal fun Enumeration.getDocumentationKeys(): List<String> {
    val prefix = name
    return listOf(name) +
            values.map { "$prefix#$it" }
}
