package com.aitutor.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val conversationId: String? = null,
    val message: String,
    val documentIds: List<String>? = null
)

@Serializable
data class ChatSource(
    val chunkId: String,
    val documentTitle: String?,
    val pageNumber: Int?,
    val snippet: String
)

@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val provider: String
)

@Serializable
data class ModelsResponse(
    val models: List<ModelInfo>
)
