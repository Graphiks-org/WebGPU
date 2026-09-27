package org.graphiks.webgpu.generator.lm.agent

import org.graphiks.webgpu.generator.lm.LLMClient
import org.graphiks.webgpu.generator.lm.Message

class DocumentationRefinerAgent(private val client: LLMClient)  {

    suspend fun refine(source: String): Result<String> = runCatching {

        val userPrompt = """
            refine the following KDoc and return it as JSON:
            
            $source
        """.trimIndent()

        val messages = listOf(
            Message("system", systemPrompt),
            Message("user", userPrompt)
        )

        val response = client.chatCompletion(messages = messages)
        val rawJson = response.choices.firstOrNull()?.message?.content ?: error("fail to get response")
        println("Réponse: ${rawJson}")
        println("Tokens utilisés: ${response.usage.total_tokens}")
        rawJson.substringAfter("```json").substringBeforeLast("```")
    }

    val systemPrompt = """
        You are a specialized KDoc processor for Kotlin documentation stored in JSON format. Your primary responsibilities are:
        
        1. Review Kotlin documentation (KDoc) entries and remove all code examples while preserving the explanatory text.
        
        2. Fix invalid internal references by converting links in the format [GPUClass](#gpuclass) to direct code references in the format [GPUClass] and remove the (#gpuclass) part.
        
        3. Preserve all external links (URLs pointing to websites outside the codebase) in their original format.
        
        4. Maintain the overall structure and meaning of the documentation while making these specific modifications.
        
        5. Ensure the resulting documentation remains clear, concise, and technically accurate.
        
        When processing documentation:
        - DO remove all code blocks and examples
        - DO convert internal anchor links to direct code references
        - DO NOT modify external website URLs ONLY when they are valid links to websites outside the codebase 
        - DO preserve all technical explanations and descriptions
        - DO maintain the original formatting where appropriate
        
        Your output should be clean, well-structured Kotlin documentation ready to be stored back in JSON format.
    """.trimIndent()

}