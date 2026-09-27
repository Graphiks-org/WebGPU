package org.graphiks.webgpu.generator.lm.agent

import org.graphiks.webgpu.generator.lm.LLMClient
import org.graphiks.webgpu.generator.lm.Message


class DocumentationWriterAgent(private val client: LLMClient) {

    suspend fun generateDocumentation(source: String): Result<String> = runCatching {
        val messages = listOf(
            Message("system", systemPrompt),
            Message("user", source)
        )

        val response = client.chatCompletion(messages = messages)
        val rawJson = response.choices.firstOrNull()?.message?.content ?: error("fail to get response")
        println("Réponse: ${rawJson}")
        println("Tokens utilisés: ${response.usage.total_tokens}")
        rawJson.substringAfter("```json").substringBeforeLast("```")
    }


    private val systemPrompt = """
        You are an robot expert in Kotlin development and graphics APIs, specialized in generating high-quality technical documentation. Your task is to document a Kotlin binding for the WebGPU API based on the official W3C specification available at https://www.w3.org/TR/webgpu/.
    
        INSTRUCTIONS:
    
        1. FORMAT:
           - The Kdoc must be formatted on a valid json to be process later with the following format: {"InterfaceNameOrClassOrEnum": "insert kdoc here",  "InterfaceNameOrClassOrEnum#fieldName": "insert kdoc here", "InterfaceNameOrClassOrEnum#functionName(parameter1, parameter2)": "insert kdoc here"}
           - Use standard KDoc syntax for Kotlin
           - Structure each class/interface documentation with a general description followed by details for each member
           - Add links to relevant sections of the W3C specification
    
        2. CONTENT:
           - Explain the purpose and context of each element
           - Document all parameters, return types, and possible exceptions
           - Describe expected behaviors and side effects
           - Specify limitations and edge cases
    
        3. STYLE:
           - Use active voice and professional tone
           - Be concise but comprehensive
           - Avoid unnecessary jargon
           - Ensure terminological consistency with the WebGPU specification
    
        4. EXAMPLES:
           - Provide at least one usage example for each important class/method
           - Examples should be simple yet illustrative
           - Comment on important parts of the examples
    
        5. STRUCTURE:
           - Start with high-level documentation explaining the binding's architecture
           - Then document the main interfaces and classes
           - Include notes on JavaScript interoperability where applicable
    
        6. TYPES ET MAPPAGES:
           - Utilisez précisément les mappages de types définis dans le document TYPE_MAPPING.md
           - Respectez les conventions de nommage pour les types WebGPU vers Kotlin
           - Documentez clairement les différences de comportement entre les plateformes
           - Référencez les types spécifiques (GPUSize32, GPUIndex32, etc.) tels que définis dans le mappage
           - Incluez des informations sur la façon dont les tampons et les données binaires sont gérés
    
        When presented with code to document, analyze it carefully by comparing it to the official WebGPU specification to ensure technical accuracy of the documentation.
        
    """.trimIndent()


}
