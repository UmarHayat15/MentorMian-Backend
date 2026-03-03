package com.aitutor.ingestion.repository

import com.aitutor.api.dto.DocumentResponse
import com.aitutor.common.util.dbQuery
import com.aitutor.ingestion.model.DocumentTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

class DocumentRepository {

    suspend fun create(userId: UUID?, title: String?, fileName: String): DocumentResponse = dbQuery {
        val row = DocumentTable.insert {
            it[DocumentTable.userId] = userId
            it[DocumentTable.title] = title
            it[DocumentTable.fileName] = fileName
            it[DocumentTable.status] = "processing"
        }.resultedValues!!.first()
        row.toResponse()
    }

    suspend fun findById(id: UUID): DocumentResponse? = dbQuery {
        DocumentTable.selectAll()
            .where { DocumentTable.id eq id }
            .map { it.toResponse() }
            .singleOrNull()
    }

    suspend fun findAllByUserId(userId: UUID?): List<DocumentResponse> = dbQuery {
        val query = if (userId != null) {
            DocumentTable.selectAll().where { DocumentTable.userId eq userId }
        } else {
            DocumentTable.selectAll()
        }
        query.orderBy(DocumentTable.createdAt, SortOrder.DESC)
            .map { it.toResponse() }
    }

    suspend fun updateStatus(id: UUID, status: String, totalPages: Int?, totalChunks: Int?) = dbQuery {
        DocumentTable.update({ DocumentTable.id eq id }) {
            it[DocumentTable.status] = status
            if (totalPages != null) it[DocumentTable.totalPages] = totalPages
            if (totalChunks != null) it[DocumentTable.totalChunks] = totalChunks
        }
    }

    suspend fun delete(id: UUID) = dbQuery {
        DocumentTable.deleteWhere { DocumentTable.id eq id }
    }

    private fun ResultRow.toResponse() = DocumentResponse(
        id = this[DocumentTable.id].toString(),
        title = this[DocumentTable.title],
        fileName = this[DocumentTable.fileName],
        totalPages = this[DocumentTable.totalPages],
        totalChunks = this[DocumentTable.totalChunks],
        status = this[DocumentTable.status],
        createdAt = this[DocumentTable.createdAt].toString()
    )
}
