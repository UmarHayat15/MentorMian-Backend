package com.aitutor.common.config

import io.ktor.server.application.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val driver: String,
    val maxPoolSize: Int
)

data class OllamaConfig(
    val baseUrl: String,
    val embedModel: String
)

data class OpenAiConfig(
    val baseUrl: String,
    val embedModel: String
)

data class GeminiConfig(
    val baseUrl: String,
    val embedModel: String
)

data class DefaultsConfig(
    val provider: String,
    val model: String
)

data class RagConfig(
    val topK: Int,
    val maxContextTokens: Int
)

data class ChatConfig(
    val maxHistory: Int,
    val snippetLength: Int,
    val titleLength: Int
)

data class AppMetaConfig(
    val version: String,
    val corsAllowedOrigins: String
)

data class AppConfig(
    val database: DatabaseConfig,
    val uploadsDirectory: String,
    val ollamaBaseUrl: String,
    val ollama: OllamaConfig,
    val openai: OpenAiConfig,
    val gemini: GeminiConfig,
    val defaults: DefaultsConfig,
    val chunkingMaxTokens: Int,
    val chunkingOverlapPercent: Int,
    val rag: RagConfig,
    val chat: ChatConfig,
    val appMeta: AppMetaConfig
)

fun Application.loadAppConfig(): AppConfig {
    val config = environment.config
    val dotEnv = loadDotEnv()

    fun resolve(envKey: String, configKey: String): String {
        return System.getenv(envKey)
            ?: dotEnv[envKey]
            ?: config.propertyOrNull(configKey)?.getString()
            ?: throw IllegalArgumentException("Missing config for $envKey / $configKey")
    }

    fun resolveInt(envKey: String, configKey: String): Int {
        return resolve(envKey, configKey).toInt()
    }

    val ollamaBaseUrl = resolve("AITUTOR_OLLAMA_BASE_URL", "aitutor.ollama.baseUrl")

    return AppConfig(
        database = DatabaseConfig(
            url = resolve("AITUTOR_DB_URL", "aitutor.database.url"),
            user = resolve("AITUTOR_DB_USER", "aitutor.database.user"),
            password = resolve("AITUTOR_DB_PASSWORD", "aitutor.database.password"),
            driver = resolve("AITUTOR_DB_DRIVER", "aitutor.database.driver"),
            maxPoolSize = resolveInt("AITUTOR_DB_MAX_POOL_SIZE", "aitutor.database.maxPoolSize")
        ),
        uploadsDirectory = resolve("AITUTOR_UPLOADS_DIR", "aitutor.uploads.directory"),
        ollamaBaseUrl = ollamaBaseUrl,
        ollama = OllamaConfig(
            baseUrl = ollamaBaseUrl,
            embedModel = resolve("AITUTOR_OLLAMA_EMBED_MODEL", "aitutor.ollama.embedModel")
        ),
        openai = OpenAiConfig(
            baseUrl = resolve("AITUTOR_OPENAI_BASE_URL", "aitutor.openai.baseUrl"),
            embedModel = resolve("AITUTOR_OPENAI_EMBED_MODEL", "aitutor.openai.embedModel")
        ),
        gemini = GeminiConfig(
            baseUrl = resolve("AITUTOR_GEMINI_BASE_URL", "aitutor.gemini.baseUrl"),
            embedModel = resolve("AITUTOR_GEMINI_EMBED_MODEL", "aitutor.gemini.embedModel")
        ),
        defaults = DefaultsConfig(
            provider = resolve("AITUTOR_DEFAULT_PROVIDER", "aitutor.defaults.provider"),
            model = resolve("AITUTOR_DEFAULT_MODEL", "aitutor.defaults.model")
        ),
        chunkingMaxTokens = resolveInt("AITUTOR_CHUNKING_MAX_TOKENS", "aitutor.chunking.maxTokens"),
        chunkingOverlapPercent = resolveInt("AITUTOR_CHUNKING_OVERLAP_PERCENT", "aitutor.chunking.overlapPercent"),
        rag = RagConfig(
            topK = resolveInt("AITUTOR_RAG_TOP_K", "aitutor.rag.topK"),
            maxContextTokens = resolveInt("AITUTOR_RAG_MAX_CONTEXT_TOKENS", "aitutor.rag.maxContextTokens")
        ),
        chat = ChatConfig(
            maxHistory = resolveInt("AITUTOR_CHAT_MAX_HISTORY", "aitutor.chat.maxHistory"),
            snippetLength = resolveInt("AITUTOR_CHAT_SNIPPET_LENGTH", "aitutor.chat.snippetLength"),
            titleLength = resolveInt("AITUTOR_CHAT_TITLE_LENGTH", "aitutor.chat.titleLength")
        ),
        appMeta = AppMetaConfig(
            version = resolve("APP_VERSION", "aitutor.app.version"),
            corsAllowedOrigins = resolve("CORS_ALLOWED_ORIGINS", "aitutor.app.corsAllowedOrigins")
        )
    )
}

private fun loadDotEnv(path: Path = Path.of(".env")): Map<String, String> {
    if (!path.exists()) return emptyMap()

    return Files.readAllLines(path)
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .map { line ->
            val idx = line.indexOf('=')
            val key = line.substring(0, idx).trim()
            var value = line.substring(idx + 1).trim()
            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))
            ) {
                value = value.substring(1, value.length - 1)
            }
            key to value
        }
        .toMap()
}
