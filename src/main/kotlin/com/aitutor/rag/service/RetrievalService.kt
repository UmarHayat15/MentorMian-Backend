package com.aitutor.rag.service

import com.aitutor.ingestion.service.EmbeddingProvider
import com.aitutor.rag.repository.SimilarChunk
import com.aitutor.rag.repository.VectorRepository
import org.slf4j.LoggerFactory
import java.util.UUID

class RetrievalService(
    private val vectorRepository: VectorRepository,
    private val contextAssembler: ContextAssembler
) {
    private val logger = LoggerFactory.getLogger(RetrievalService::class.java)

    suspend fun retrieve(
        query: String,
        embeddingProvider: EmbeddingProvider,
        topK: Int = 10,
        documentIds: List<UUID>? = null
    ): RetrievalResult {
        val queryEmbedding = embeddingProvider.embed(query)
        val similarChunks = vectorRepository.findSimilar(queryEmbedding, topK, documentIds)

        logger.info("Retrieved ${similarChunks.size} chunks for query: ${query.take(50)}...")

        val context = contextAssembler.assemble(similarChunks)
        return RetrievalResult(
            context = context,
            chunks = similarChunks
        )
    }
}

data class RetrievalResult(
    val context: String,
    val chunks: List<SimilarChunk>
)
