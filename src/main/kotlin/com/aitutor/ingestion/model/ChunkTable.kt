package com.aitutor.ingestion.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object ChunkTable : Table("chunks") {
    val id = uuid("id").autoGenerate()
    val documentId = uuid("document_id").references(DocumentTable.id)
    val content = text("content")
    val pageNumber = integer("page_number").nullable()
    val chapter = varchar("chapter", 500).nullable()
    val chunkIndex = integer("chunk_index")
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}
