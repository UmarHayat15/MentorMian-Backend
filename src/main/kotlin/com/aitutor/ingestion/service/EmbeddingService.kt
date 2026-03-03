package com.aitutor.ingestion.service

interface EmbeddingProvider {
    suspend fun embed(text: String): List<Float>
    suspend fun embedBatch(texts: List<String>): List<List<Float>>
}

class EmbeddingService {

    suspend fun generateEmbedding(provider: EmbeddingProvider, text: String): List<Float> {
        return provider.embed(text)
    }

    suspend fun generateEmbeddings(provider: EmbeddingProvider, texts: List<String>): List<List<Float>> {
        return provider.embedBatch(texts)
    }
}
