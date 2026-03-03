package com.aitutor.rag.service

import com.aitutor.common.config.AppConfig
import com.aitutor.rag.repository.SimilarChunk

class ContextAssembler(private val appConfig: AppConfig) {

    fun assemble(chunks: List<SimilarChunk>, maxTokens: Int = appConfig.rag.maxContextTokens): String {
        val builder = StringBuilder()
        var estimatedTokens = 0

        for (chunk in chunks) {
            val chunkTokens = chunk.content.split(" ").size
            if (estimatedTokens + chunkTokens > maxTokens) break

            val source = buildString {
                chunk.chapter?.let { append("Chapter: $it") }
                chunk.pageNumber?.let {
                    if (isNotEmpty()) append(", ")
                    append("Page: $it")
                }
            }

            if (source.isNotEmpty()) {
                builder.appendLine("[$source]")
            }
            builder.appendLine(chunk.content)
            builder.appendLine()

            estimatedTokens += chunkTokens
        }

        return builder.toString().trim()
    }
}
