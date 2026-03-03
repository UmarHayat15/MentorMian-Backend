package com.aitutor.common.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.javatime.timestamp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.time.Instant

object MessageTable : Table("messages") {
    val id = uuid("id").autoGenerate()
    val conversationId = uuid("conversation_id").references(ConversationTable.id)
    val role = varchar("role", 20)
    val content = text("content")
    val sources = jsonb<JsonElement>("sources", Json.Default).nullable()
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}
