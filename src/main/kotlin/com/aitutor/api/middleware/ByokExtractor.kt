package com.aitutor.api.middleware

import com.aitutor.common.config.DefaultsConfig
import io.ktor.server.application.*

data class LlmConfig(
    val provider: String,
    val apiKey: String?,
    val model: String,
    val baseUrl: String?
)

fun ApplicationCall.extractLlmConfig(defaults: DefaultsConfig): LlmConfig {
    val provider = request.headers["X-LLM-Provider"] ?: defaults.provider
    val apiKey = request.headers["X-API-Key"]
    val model = request.headers["X-Model"] ?: defaults.model
    val baseUrl = request.headers["X-Base-URL"]
    return LlmConfig(
        provider = provider.lowercase(),
        apiKey = apiKey,
        model = model,
        baseUrl = baseUrl
    )
}
