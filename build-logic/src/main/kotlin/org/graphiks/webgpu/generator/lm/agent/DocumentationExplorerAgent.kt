package org.graphiks.webgpu.generator.lm.agent

import org.graphiks.webgpu.generator.lm.LLMClient
import org.graphiks.webgpu.generator.lm.Message

class DocumentationExplorerAgent(private val client: LLMClient) {

    suspend fun isRelevant(source: String, nextNode: String, subject: String): Result<String> = runCatching {
        val prompt = """
            Given this kotlin code :
            
            ```kotlin
            $subject
            ```
            
            We have already selected this part of the documentation to talk about it : 
            
            ```html
            $source
            ```
            
            Is this the following part is relevant to create Kotlin documentation on the same topic ?
            
            ```html
            $nextNode
            ```
        """.trimIndent()

        val messages = listOf(
            Message("system", systemPrompt),
            Message("user", prompt)
        )

        val response = client.chatCompletion(messages = messages)
        val rawJson = response.choices.firstOrNull()?.message?.content ?: error("fail to get response")
        println("Réponse: ${rawJson}")
        println("Tokens utilisés: ${response.usage.total_tokens}")
        rawJson.substringAfter("```json").substringBeforeLast("```")
    }

    val systemPrompt = """
        You are an expert in Kotlin development and graphics APIs, specialized in generating high-quality technical documentation. 
        Your task is to explore the WebGPU API based on the official W3C specification available at https://www.w3.org/TR/webgpu/.
        
        We will give you part of the documentation and ask you to respond by "yes" or "no" if the selected piece of HTML is related to another one.
    """.trimIndent()
}

