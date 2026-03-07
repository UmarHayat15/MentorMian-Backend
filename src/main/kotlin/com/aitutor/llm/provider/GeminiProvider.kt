package com.aitutor.llm.provider

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

class GeminiProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    private val embeddingModel: String = "text-embedding-004"
) : LlmProvider {

    override val providerName = "gemini"

    private val logger = LoggerFactory.getLogger(GeminiProvider::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    /**
     * Build the Gemini "contents" array from ChatMessages.
     * Filters out "system" role messages (handled separately via systemInstruction).
     * Gemini uses "user" and "model" roles (not "assistant").
     * Also ensures proper alternation — merges consecutive same-role messages.
     */
    private fun buildContentsJson(messages: List<ChatMessage>): JsonArray {
        // Filter out system messages and merge consecutive same-role messages
        val filtered = messages.filter { it.role != "system" }
        val merged = mutableListOf<ChatMessage>()
        for (msg in filtered) {
            val role = if (msg.role == "assistant") "model" else msg.role
            val last = merged.lastOrNull()
            if (last != null && last.role == role) {
                merged[merged.size - 1] = ChatMessage(role = role, content = last.content + "\n\n" + msg.content)
            } else {
                merged.add(ChatMessage(role = role, content = msg.content))
            }
        }

        return buildJsonArray {
            merged.forEach { msg ->
                addJsonObject {
                    put("role", msg.role)
                    put("parts", buildJsonArray {
                        addJsonObject {
                            put("text", msg.content)
                        }
                    })
                }
            }
        }
    }

    /**
     * Extract system instruction from message list for Gemini's systemInstruction field.
     */
    private fun buildSystemInstruction(messages: List<ChatMessage>): JsonObject? {
        val systemMessages = messages.filter { it.role == "system" }
        if (systemMessages.isEmpty()) return null
        val combined = systemMessages.joinToString("\n\n") { it.content }
        return buildJsonObject {
            put("parts", buildJsonArray {
                addJsonObject {
                    put("text", combined)
                }
            })
        }
    }

    /**
     * Build the full request body with optional systemInstruction.
     */
    private fun buildRequestBody(messages: List<ChatMessage>): JsonObject {
        return buildJsonObject {
            val sysInstruction = buildSystemInstruction(messages)
            if (sysInstruction != null) {
                put("systemInstruction", sysInstruction)
            }
            put("contents", buildContentsJson(messages))
        }
    }

    override suspend fun chatStream(messages: List<ChatMessage>, model: String): Flow<String> = flow {
        val requestBody = buildRequestBody(messages)

        val response: HttpResponse = client.post(
            "$baseUrl/models/$model:streamGenerateContent?alt=sse&key=$apiKey"
        ) {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            throw IllegalArgumentException("Gemini chat failed (${response.status.value}): $body")
        }

        val channel: ByteReadChannel = response.bodyAsChannel()

        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break

            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ").trim()
                if (data.isEmpty()) continue

                try {
                    val jsonObj = json.parseToJsonElement(data).jsonObject
                    val text = jsonObj["candidates"]?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
                    if (text != null) {
                        emit(text)
                    }
                } catch (e: Exception) {
                    logger.debug("Skipping Gemini SSE line: $data")
                }
            }
        }
    }

    override suspend fun chat(messages: List<ChatMessage>, model: String): String {
        val requestBody = buildRequestBody(messages)

        val response: HttpResponse = client.post(
            "$baseUrl/models/$model:generateContent?key=$apiKey"
        ) {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            throw IllegalArgumentException("Gemini chat failed (${response.status.value}): $body")
        }

        val responseBody = response.bodyAsText()
        val jsonObj = json.parseToJsonElement(responseBody).jsonObject
        return jsonObj["candidates"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            ?: throw RuntimeException("No content in Gemini response")
    }

    override suspend fun embed(text: String): List<Float> {
        val requestBody = buildJsonObject {
            put("model", "models/$embeddingModel")
            put("content", buildJsonObject {
                put("parts", buildJsonArray {
                    addJsonObject {
                        put("text", text)
                    }
                })
            })
        }

        val response: HttpResponse = client.post(
            "$baseUrl/models/$embeddingModel:embedContent?key=$apiKey"
        ) {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            throw IllegalArgumentException("Gemini embed failed (${response.status.value}): $body")
        }

        val responseBody = response.bodyAsText()
        val jsonObj = json.parseToJsonElement(responseBody).jsonObject
        return jsonObj["embedding"]?.jsonObject
            ?.get("values")?.jsonArray
            ?.map { it.jsonPrimitive.float }
            ?: throw RuntimeException("No embedding in Gemini response")
    }

    override suspend fun embedBatch(texts: List<String>): List<List<Float>> {
        val requestBody = buildJsonObject {
            put("requests", buildJsonArray {
                texts.forEach { text ->
                    addJsonObject {
                        put("model", "models/$embeddingModel")
                        put("content", buildJsonObject {
                            put("parts", buildJsonArray {
                                addJsonObject {
                                    put("text", text)
                                }
                            })
                        })
                    }
                }
            })
        }

        val response: HttpResponse = client.post(
            "$baseUrl/models/$embeddingModel:batchEmbedContents?key=$apiKey"
        ) {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            throw IllegalArgumentException("Gemini batch embed failed (${response.status.value}): $body")
        }

        val responseBody = response.bodyAsText()
        val jsonObj = json.parseToJsonElement(responseBody).jsonObject
        return jsonObj["embeddings"]?.jsonArray
            ?.map { item ->
                item.jsonObject["values"]?.jsonArray
                    ?.map { it.jsonPrimitive.float }
                    ?: emptyList()
            }
            ?: throw RuntimeException("No embeddings in Gemini response")
    }
}
