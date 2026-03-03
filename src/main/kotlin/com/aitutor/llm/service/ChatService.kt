package com.aitutor.llm.service

import com.aitutor.api.dto.ChatSource
import com.aitutor.api.middleware.LlmConfig
import com.aitutor.common.config.AppConfig
import com.aitutor.common.model.ConversationTable
import com.aitutor.common.model.MessageTable
import com.aitutor.common.util.dbQuery
import com.aitutor.llm.config.SystemPrompts
import com.aitutor.llm.provider.ChatMessage
import com.aitutor.llm.provider.LlmProvider
import com.aitutor.llm.provider.LlmProviderFactory
import com.aitutor.rag.service.RetrievalService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.util.UUID

class ChatService(
    private val providerFactory: LlmProviderFactory,
    private val retrievalService: RetrievalService,
    private val appConfig: AppConfig
) {
    private val logger = LoggerFactory.getLogger(ChatService::class.java)
    private val json = Json { encodeDefaults = true }

    suspend fun chatStream(
        llmConfig: LlmConfig,
        conversationId: UUID?,
        userMessage: String,
        documentIds: List<UUID>?
    ): Pair<UUID, Flow<String>> {
        val provider = providerFactory.create(llmConfig)
        val activeConversationId = conversationId ?: createConversation(userMessage)

        saveMessage(activeConversationId, "user", userMessage, null)

        val retrievalResult = retrievalService.retrieve(
            query = userMessage,
            embeddingProvider = provider,
            documentIds = documentIds
        )

        val sources = retrievalResult.chunks.map { chunk ->
            ChatSource(
                chunkId = chunk.chunkId.toString(),
                documentTitle = chunk.chapter,
                pageNumber = chunk.pageNumber,
                snippet = chunk.content.take(appConfig.chat.snippetLength)
            )
        }

        val history = getConversationHistory(activeConversationId)
        val messages = buildMessageList(history, retrievalResult.context, userMessage)

        val responseBuilder = StringBuilder()
        val responseFlow = flow {
            provider.chatStream(messages, llmConfig.model).collect { token ->
                responseBuilder.append(token)
                emit(token)
            }
        }.onCompletion {
            val sourcesJson = json.encodeToString(sources)
            saveMessage(activeConversationId, "assistant", responseBuilder.toString(), sourcesJson)
        }

        return Pair(activeConversationId, responseFlow)
    }

    private fun buildMessageList(
        history: List<ChatMessage>,
        context: String,
        userMessage: String
    ): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        messages.add(ChatMessage(role = "system", content = SystemPrompts.TUTOR_SYSTEM_PROMPT))
        messages.addAll(history.takeLast(appConfig.chat.maxHistory))
        messages.add(
            ChatMessage(
                role = "user",
                content = SystemPrompts.buildRagPrompt(context, userMessage)
            )
        )
        return messages
    }

    private suspend fun createConversation(firstMessage: String): UUID = dbQuery {
        val title = firstMessage.take(appConfig.chat.titleLength)
        val row = ConversationTable.insert {
            it[ConversationTable.title] = title
        }.resultedValues!!.first()
        row[ConversationTable.id]
    }

    private suspend fun getConversationHistory(conversationId: UUID): List<ChatMessage> = dbQuery {
        MessageTable.selectAll()
            .where { MessageTable.conversationId eq conversationId }
            .orderBy(MessageTable.createdAt)
            .map { row ->
                ChatMessage(
                    role = row[MessageTable.role],
                    content = row[MessageTable.content]
                )
            }
    }

    private suspend fun saveMessage(conversationId: UUID, role: String, content: String, sources: String?) = dbQuery {
        val sourcesJson: JsonElement? = sources?.let { json.parseToJsonElement(it) }
        MessageTable.insert {
            it[MessageTable.conversationId] = conversationId
            it[MessageTable.role] = role
            it[MessageTable.content] = content
            it[MessageTable.sources] = sourcesJson
        }
    }
}
