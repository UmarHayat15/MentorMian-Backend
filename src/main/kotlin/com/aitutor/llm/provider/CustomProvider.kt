package com.aitutor.llm.provider

class CustomProvider(
    apiKey: String,
    baseUrl: String
) : LlmProvider by OpenAiProvider(apiKey = apiKey, baseUrl = baseUrl) {
    override val providerName = "custom"
}
