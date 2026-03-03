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

class OllamaProvider(
    private val baseUrl: String = "http://localhost:11434"
) : LlmProvider {

    override val providerName = "ollama"

    private val logger = LoggerFactory.getLogger(OllamaProvider::class.java)
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

        val response: HttpResponse = client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        val channel: ByteReadChannel = response.bodyAsChannel()

        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            if (line.isBlank()) continue

            try {
                val jsonObj = json.parseToJsonElement(line).jsonObject
                val content = jsonObj["message"]?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
                if (content != null) {
                    emit(content)
                }
                val done = jsonObj["done"]?.jsonPrimitive?.booleanOrNull ?: false
                if (done) break
            } catch (e: Exception) {
                logger.debug("Skipping Ollama line: $line")
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

        val response: HttpResponse = client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        val responseBody = response.bodyAsText()
        val jsonObj = json.parseToJsonElement(responseBody).jsonObject
        return jsonObj["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: throw RuntimeException("No content in Ollama response")
    }

    override suspend fun embed(text: String): List<Float> {
        val requestBody = buildJsonObject {
            put("model", "nomic-embed-text")
            put("prompt", text)
        }

        val response: HttpResponse = client.post("$baseUrl/api/embeddings") {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        val responseBody = response.bodyAsText()
        val jsonObj = json.parseToJsonElement(responseBody).jsonObject
        return jsonObj["embedding"]?.jsonArray
            ?.map { it.jsonPrimitive.float }
            ?: throw RuntimeException("No embedding in Ollama response")
    }

    override suspend fun embedBatch(texts: List<String>): List<List<Float>> {
        return texts.map { embed(it) }
    }
}
