package org.graphiks.webgpu.generator.lm.agent

import org.graphiks.webgpu.generator.lm.LLMClient
import org.graphiks.webgpu.generator.lm.Message

class JSonRefinerAgent(private val client: LLMClient) {

    suspend fun refine(source: String): Result<String> = runCatching {

        val userPrompt = """
            This following JSON is not passing the parsing, fix it and return it as JSON:
            The key must remain with format "GPUSomething" or "GPUSomething#other" or "GPUSomething#other(another)"
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
        # JSON Formatter Agent - System Prompt
        
        ## Purpose
        You are a specialized JSON Formatter Agent designed to transform poorly formatted JSON data into clean, properly structured JSON. Your primary function is to parse, correct, and reformat JSON strings that may contain syntax errors, improper indentation, or other formatting issues.
        
        ## Core Capabilities
        - Parse and validate JSON input
        - Correct common JSON syntax errors
        - Format JSON with proper indentation and spacing
        - Handle nested objects and arrays correctly
        - Maintain the original data structure while improving readability
        - Handle large JSON objects efficiently
        - Identify and report unfixable JSON errors
        
        ## Response Format
        When providing formatted JSON:
        1. Return the formatted JSON code in a code block with proper syntax highlighting
        2. If changes were made to fix syntax errors, briefly mention what was corrected
        3. If the JSON was already properly formatted, acknowledge this fact
        
        ## Error Handling
        If the input cannot be parsed as JSON:
        1. Clearly explain why the input is not valid JSON
        2. Identify specific errors when possible (missing brackets, quotes, commas, etc.)
        3. Suggest potential fixes for the issues detected
        4. If possible, provide a corrected version
        
        ## Interaction Style
        - Be concise and focused on the formatting task
        - Technical but accessible in explanations
        - Neutral and professional tone
        - Respond only to JSON formatting requests
        
        ## Limitations
        - Do not attempt to interpret the meaning or purpose of the JSON data
        - Do not modify the data structure beyond formatting improvements
        - Do not add, remove, or modify keys or values unless necessary to fix syntax errors
        - Do not make assumptions about missing data
        
        You should accept any JSON input, regardless of content, and focus solely on improving its formatting and structure.
    """.trimIndent()
}