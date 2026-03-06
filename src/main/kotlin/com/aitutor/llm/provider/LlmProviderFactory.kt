package com.aitutor.llm.provider

import com.aitutor.api.middleware.LlmConfig
import com.aitutor.common.config.AppConfig

class LlmProviderFactory(private val appConfig: AppConfig) {

    fun create(llmConfig: LlmConfig): LlmProvider {
        return when (llmConfig.provider) {
            "openai" -> {
                val apiKey = llmConfig.apiKey
                    ?: throw IllegalArgumentException("API key required for OpenAI provider")
                OpenAiProvider(
                    apiKey = apiKey,
                    baseUrl = llmConfig.baseUrl ?: appConfig.openai.baseUrl,
                    embeddingModel = appConfig.openai.embedModel
                )
            }
            "ollama" -> {
                OllamaProvider(
                    baseUrl = llmConfig.baseUrl ?: appConfig.ollama.baseUrl,
                    embeddingModel = appConfig.ollama.embedModel
                )
            }
            "gemini" -> {
                val apiKey = llmConfig.apiKey
                    ?: throw IllegalArgumentException("API key required for Gemini provider")
                GeminiProvider(
                    apiKey = apiKey,
                    baseUrl = llmConfig.baseUrl ?: appConfig.gemini.baseUrl,
                    embeddingModel = appConfig.gemini.embedModel
                )
            }
            "custom" -> {
                val apiKey = llmConfig.apiKey
                    ?: throw IllegalArgumentException("API key required for custom provider")
                val baseUrl = llmConfig.baseUrl
                    ?: throw IllegalArgumentException("Base URL required for custom provider")
                CustomProvider(
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    embeddingModel = appConfig.openai.embedModel
                )
            }
            else -> throw IllegalArgumentException("Unknown provider: ${llmConfig.provider}")
        }
    }
}
