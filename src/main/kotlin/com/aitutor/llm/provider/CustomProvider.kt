package com.aitutor.llm.provider

class CustomProvider(
    apiKey: String,
    baseUrl: String,
    embeddingModel: String = "text-embedding-3-small"
) : LlmProvider by OpenAiProvider(apiKey = apiKey, baseUrl = baseUrl, embeddingModel = embeddingModel) {
    override val providerName = "custom"
}
