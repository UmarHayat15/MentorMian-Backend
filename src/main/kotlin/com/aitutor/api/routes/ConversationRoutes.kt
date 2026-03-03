package com.aitutor.api.routes

import com.aitutor.api.dto.*
import com.aitutor.common.model.ConversationTable
import com.aitutor.common.model.MessageTable
import com.aitutor.common.util.dbQuery
import com.aitutor.common.util.toUUID
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll

fun Route.conversationRoutes() {

    route("/conversations") {

        get {
            val conversations = dbQuery {
                ConversationTable.selectAll()
                    .orderBy(ConversationTable.createdAt, SortOrder.DESC)
                    .map { row ->
                        ConversationResponse(
                            id = row[ConversationTable.id].toString(),
                            title = row[ConversationTable.title],
                            createdAt = row[ConversationTable.createdAt].toString()
                        )
                    }
            }
            call.respond(ApiResponse(success = true, data = ConversationListResponse(conversations)))
        }

        get("/{id}/messages") {
            val id = call.parameters["id"]?.toUUID()
                ?: throw IllegalArgumentException("Invalid conversation ID")

            val messages = dbQuery {
                MessageTable.selectAll()
                    .where { MessageTable.conversationId eq id }
                    .orderBy(MessageTable.createdAt, SortOrder.ASC)
                    .map { row ->
                        MessageResponse(
                            id = row[MessageTable.id].toString(),
                            role = row[MessageTable.role],
                            content = row[MessageTable.content],
                            sources = row[MessageTable.sources]?.toString(),
                            createdAt = row[MessageTable.createdAt].toString()
                        )
                    }
            }
            call.respond(ApiResponse(success = true, data = MessageListResponse(messages)))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toUUID()
                ?: throw IllegalArgumentException("Invalid conversation ID")

            dbQuery {
                MessageTable.deleteWhere { MessageTable.conversationId eq id }
                ConversationTable.deleteWhere { ConversationTable.id eq id }
            }
            call.respond(ApiResponse<Unit>(success = true))
        }
    }
}
