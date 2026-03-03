package com.aitutor.ingestion.service

import com.aitutor.common.config.AppConfig
import org.slf4j.LoggerFactory

data class TextChunk(
    val content: String,
    val chunkIndex: Int,
    val pageNumber: Int?
)

class ChunkingService(private val config: AppConfig) {

    private val logger = LoggerFactory.getLogger(ChunkingService::class.java)

    fun chunk(text: String): List<TextChunk> {
        val maxTokens = config.chunkingMaxTokens
        val overlapPercent = config.chunkingOverlapPercent
        val overlapTokens = (maxTokens * overlapPercent / 100)

        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val chunks = mutableListOf<TextChunk>()
        var start = 0
        var chunkIndex = 0

        while (start < words.size) {
            val end = minOf(start + maxTokens, words.size)
            val chunkText = words.subList(start, end).joinToString(" ")

            chunks.add(
                TextChunk(
                    content = chunkText,
                    chunkIndex = chunkIndex,
                    pageNumber = null
                )
            )

            chunkIndex++
            start += (maxTokens - overlapTokens)
        }

        logger.info("Split text into ${chunks.size} chunks (maxTokens=$maxTokens, overlap=$overlapTokens)")
        return chunks
    }
}
