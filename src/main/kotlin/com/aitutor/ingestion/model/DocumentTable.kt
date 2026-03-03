package com.aitutor.ingestion.model

import com.aitutor.user.model.UserTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object DocumentTable : Table("documents") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id").references(UserTable.id).nullable()
    val title = varchar("title", 500).nullable()
    val fileName = varchar("file_name", 500)
    val totalPages = integer("total_pages").nullable()
    val totalChunks = integer("total_chunks").default(0)
    val status = varchar("status", 50).default("processing")
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}
