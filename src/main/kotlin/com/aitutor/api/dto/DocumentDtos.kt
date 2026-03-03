package com.aitutor.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class DocumentResponse(
    val id: String,
    val title: String?,
    val fileName: String,
    val totalPages: Int?,
    val totalChunks: Int,
    val status: String,
    val createdAt: String
)

@Serializable
data class DocumentListResponse(
    val documents: List<DocumentResponse>
)
