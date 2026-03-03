package com.aitutor.api.middleware

import io.ktor.server.application.*

data class LlmConfig(
    val provider: String,
    val apiKey: String?,
    val model: String,
    val baseUrl: String?
)

fun ApplicationCall.extractLlmConfig(): LlmConfig {
    val provider = request.headers["X-LLM-Provider"] ?: "openai"
    val apiKey = request.headers["X-API-Key"]
    val model = request.headers["X-Model"] ?: "gpt-4o"
    val baseUrl = request.headers["X-Base-URL"]
    return LlmConfig(
        provider = provider.lowercase(),
        apiKey = apiKey,
        model = model,
        baseUrl = baseUrl
    )
}
