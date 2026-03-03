package com.aitutor.llm.provider

import io.ktor.client.*
import io.ktor.client.engine.cio.*
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

class OpenAiProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1"
) : LlmProvider {

    override val providerName = "openai"

    private val logger = LoggerFactory.getLogger(OpenAiProvider::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    override suspend fun chatStream(messages: List<ChatMessage>, model: String): Flow<String> = flow {
        val messagesJson = buildJsonArray {
            messages.forEach { msg ->
                addJsonObject {
                    put("role", msg.role)
                    put("content", msg.content)
                }
            }
        }

        val requestBody = buildJsonObject {
            put("model", model)
            put("messages", messagesJson)
            put("stream", true)
        }

        val response: HttpResponse = client.post("$baseUrl/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(requestBody.toString())
        }

        val channel: ByteReadChannel = response.bodyAsChannel()

        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break

            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break

                try {
                    val jsonObj = json.parseToJsonElement(data).jsonObject
                    val delta = jsonObj["choices"]?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("delta")?.jsonObject
                    val content = delta?.get("content")?.jsonPrimitive?.contentOrNull
                    if (content != null) {
                        emit(content)
                    }
                } catch (e: Exception) {
                    logger.debug("Skipping SSE line: $data")
                }
            }
        }
    }

    override suspend fun chat(messages: List<ChatMessage>, model: String): String {
        val messagesJson = buildJsonArray {
            messages.forEach { msg ->
                addJsonObject {
                    put("role", msg.role)
                    put("content", msg.content)
                }
            }
        }

        val requestBody = buildJsonObject {
            put("model", model)
            put("messages", messagesJson)
            put("stream", false)
        }

        val response: HttpResponse = client.post("$baseUrl/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(requestBody.toString())
        }

        val responseBody = response.bodyAsText()
        val jsonObj = json.parseToJsonElement(responseBody).jsonObject
        return jsonObj["choices"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content
            ?: throw RuntimeException("No content in OpenAI response")
    }

    override suspend fun embed(text: String): List<Float> {
        val requestBody = buildJsonObject {
            put("model", "text-embedding-3-small")
            put("input", text)
        }

        val response: HttpResponse = client.post("$baseUrl/embeddings") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(requestBody.toString())
        }

        val responseBody = response.bodyAsText()
        val jsonObj = json.parseToJsonElement(responseBody).jsonObject
        return jsonObj["data"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("embedding")?.jsonArray
            ?.map { it.jsonPrimitive.float }
            ?: throw RuntimeException("No embedding in OpenAI response")
    }

    override suspend fun embedBatch(texts: List<String>): List<List<Float>> {
        val requestBody = buildJsonObject {
            put("model", "text-embedding-3-small")
            put("input", buildJsonArray { texts.forEach { add(it) } })
        }

        val response: HttpResponse = client.post("$baseUrl/embeddings") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(requestBody.toString())
        }

        val responseBody = response.bodyAsText()
        val jsonObj = json.parseToJsonElement(responseBody).jsonObject
        return jsonObj["data"]?.jsonArray
            ?.map { item ->
                item.jsonObject["embedding"]?.jsonArray
                    ?.map { it.jsonPrimitive.float }
                    ?: emptyList()
            }
            ?: throw RuntimeException("No embeddings in OpenAI response")
    }
}
