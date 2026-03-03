package com.aitutor.llm.provider

import com.aitutor.ingestion.service.EmbeddingProvider
import kotlinx.coroutines.flow.Flow

data class ChatMessage(
    val role: String,
    val content: String
)

interface LlmProvider : EmbeddingProvider {
    val providerName: String
    suspend fun chatStream(messages: List<ChatMessage>, model: String): Flow<String>
    suspend fun chat(messages: List<ChatMessage>, model: String): String
}
