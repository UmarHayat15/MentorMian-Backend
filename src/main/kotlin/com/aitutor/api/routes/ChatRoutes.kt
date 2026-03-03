package com.aitutor.api.routes

import com.aitutor.api.dto.ChatRequest
import com.aitutor.api.middleware.extractLlmConfig
import com.aitutor.common.util.toUUID
import com.aitutor.llm.service.ChatService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

fun Route.chatRoutes(chatService: ChatService) {
    val logger = LoggerFactory.getLogger("ChatRoutes")

    route("/chat") {

        post {
            val llmConfig = call.extractLlmConfig()
            val request = call.receive<ChatRequest>()

            val conversationId = request.conversationId?.toUUID()
            val documentIds = request.documentIds?.map { it.toUUID() }

            val (activeConversationId, tokenFlow) = chatService.chatStream(
                llmConfig = llmConfig,
                conversationId = conversationId,
                userMessage = request.message,
                documentIds = documentIds
            )

            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                write("data: {\"conversation_id\": \"$activeConversationId\"}\n\n")
                flush()

                tokenFlow.collect { token ->
                    val escaped = token.replace("\\", "\\\\").replace("\"", "\\\"")
                        .replace("\n", "\\n")
                    write("data: {\"token\": \"$escaped\"}\n\n")
                    flush()
                }

                write("data: [DONE]\n\n")
                flush()
            }
        }
    }
}
