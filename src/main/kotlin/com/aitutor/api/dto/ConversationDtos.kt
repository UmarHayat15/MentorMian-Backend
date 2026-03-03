package com.aitutor.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ConversationResponse(
    val id: String,
    val title: String?,
    val createdAt: String
)

@Serializable
data class ConversationListResponse(
    val conversations: List<ConversationResponse>
)

@Serializable
data class MessageResponse(
    val id: String,
    val role: String,
    val content: String,
    val sources: String?,
    val createdAt: String
)

@Serializable
data class MessageListResponse(
    val messages: List<MessageResponse>
)
