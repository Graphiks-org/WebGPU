package org.graphiks.webgpu.generator.lm

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.*
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class LLMClient(
    private val baseUrl: String = "http://127.0.0.1:1234/v1",
    private val apiKey: String? = null
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    suspend fun chatCompletion(
        messages: List<Message>,
        model: String = "mistral-small-3.1-24b-instruct-2503",
        maxTokens: Int = 8192,
        temperature: Double = 0.1,
        topP: Double = 1.0,
        n: Int = 1,
        stop: List<String>? = null,
        presencePenalty: Double = 0.0,
        frequencyPenalty: Double = 0.0,
        user: String? = null
    ): ChatCompletionResponse {
        val request = ChatCompletionRequest(
            model = model,
            messages = messages,
            max_tokens = maxTokens,
            temperature = temperature,
            top_p = topP,
            n = n,
            stream = false,
            stop = stop,
            presence_penalty = presencePenalty,
            frequency_penalty = frequencyPenalty,
            user = user
        )

        return client.post("$baseUrl/chat/completions") {
            timeout { HttpTimeoutConfig(requestTimeoutMillis = 60_000) }
            contentType(ContentType.Application.Json)
            apiKey?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }.body()
    }

}
