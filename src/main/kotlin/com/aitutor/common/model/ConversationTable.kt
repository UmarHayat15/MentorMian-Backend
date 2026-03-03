package com.aitutor.common.model

import com.aitutor.user.model.UserTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object ConversationTable : Table("conversations") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id").references(UserTable.id).nullable()
    val title = varchar("title", 500).nullable()
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}
