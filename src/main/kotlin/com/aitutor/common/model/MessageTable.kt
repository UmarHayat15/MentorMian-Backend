package com.aitutor.common.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object MessageTable : Table("messages") {
    val id = uuid("id").autoGenerate()
    val conversationId = uuid("conversation_id").references(ConversationTable.id)
    val role = varchar("role", 20)
    val content = text("content")
    val sources = text("sources").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}
