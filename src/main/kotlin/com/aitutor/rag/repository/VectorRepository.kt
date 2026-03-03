package com.aitutor.rag.repository

import com.aitutor.common.util.dbQuery
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.util.UUID

data class SimilarChunk(
    val chunkId: UUID,
    val documentId: UUID,
    val content: String,
    val pageNumber: Int?,
    val chapter: String?,
    val similarity: Double
)

class VectorRepository {

    suspend fun storeEmbedding(chunkId: UUID, embedding: List<Float>) = dbQuery {
        val vectorStr = embedding.joinToString(",", prefix = "[", postfix = "]")
        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(
            "UPDATE chunks SET embedding = ?::vector WHERE id = ?::uuid",
            false
        )
        stmt.set(1, vectorStr)
        stmt.set(2, chunkId.toString())
        stmt.executeUpdate()
    }

    suspend fun findSimilar(
        queryEmbedding: List<Float>,
        topK: Int = 10,
        documentIds: List<UUID>? = null
    ): List<SimilarChunk> = dbQuery {
        val vectorStr = queryEmbedding.joinToString(",", prefix = "[", postfix = "]")

        val documentFilter = if (!documentIds.isNullOrEmpty()) {
            val ids = documentIds.joinToString(",") { "'$it'" }
            "AND c.document_id IN ($ids)"
        } else {
            ""
        }

        val sql = """
            SELECT c.id, c.document_id, c.content, c.page_number, c.chapter,
                   1 - (c.embedding <=> '$vectorStr'::vector) AS similarity
            FROM chunks c
            WHERE c.embedding IS NOT NULL
            $documentFilter
            ORDER BY c.embedding <=> '$vectorStr'::vector
            LIMIT $topK
        """.trimIndent()

        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(sql, false)
        val rs = stmt.executeQuery()

        val results = mutableListOf<SimilarChunk>()
        while (rs.next()) {
            results.add(
                SimilarChunk(
                    chunkId = UUID.fromString(rs.getString(1)),
                    documentId = UUID.fromString(rs.getString(2)),
                    content = rs.getString(3),
                    pageNumber = rs.getObject(4) as? Int,
                    chapter = rs.getString(5),
                    similarity = rs.getDouble(6)
                )
            )
        }
        results
    }
}
