package com.aitutor.ingestion.repository

import com.aitutor.common.util.dbQuery
import com.aitutor.ingestion.model.ChunkTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

data class ChunkRecord(
    val id: UUID,
    val documentId: UUID,
    val content: String,
    val pageNumber: Int?,
    val chapter: String?,
    val chunkIndex: Int
)

class ChunkRepository {

    suspend fun insert(documentId: UUID, content: String, pageNumber: Int?, chapter: String?, chunkIndex: Int): UUID = dbQuery {
        val row = ChunkTable.insert {
            it[ChunkTable.documentId] = documentId
            it[ChunkTable.content] = content
            it[ChunkTable.pageNumber] = pageNumber
            it[ChunkTable.chapter] = chapter
            it[ChunkTable.chunkIndex] = chunkIndex
        }.resultedValues!!.first()
        row[ChunkTable.id]
    }

    suspend fun findByDocumentId(documentId: UUID): List<ChunkRecord> = dbQuery {
        ChunkTable.selectAll()
            .where { ChunkTable.documentId eq documentId }
            .orderBy(ChunkTable.chunkIndex)
            .map { row ->
                ChunkRecord(
                    id = row[ChunkTable.id],
                    documentId = row[ChunkTable.documentId],
                    content = row[ChunkTable.content],
                    pageNumber = row[ChunkTable.pageNumber],
                    chapter = row[ChunkTable.chapter],
                    chunkIndex = row[ChunkTable.chunkIndex]
                )
            }
    }

    suspend fun deleteByDocumentId(documentId: UUID) = dbQuery {
        ChunkTable.deleteWhere { ChunkTable.documentId eq documentId }
    }
}
