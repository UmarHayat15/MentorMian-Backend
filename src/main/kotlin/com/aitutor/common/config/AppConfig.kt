package com.aitutor.common.config

import io.ktor.server.application.*

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val driver: String,
    val maxPoolSize: Int
)

data class AppConfig(
    val database: DatabaseConfig,
    val uploadsDirectory: String,
    val ollamaBaseUrl: String,
    val chunkingMaxTokens: Int,
    val chunkingOverlapPercent: Int
)

fun Application.loadAppConfig(): AppConfig {
    val config = environment.config
    return AppConfig(
        database = DatabaseConfig(
            url = config.property("aitutor.database.url").getString(),
            user = config.property("aitutor.database.user").getString(),
            password = config.property("aitutor.database.password").getString(),
            driver = config.property("aitutor.database.driver").getString(),
            maxPoolSize = config.property("aitutor.database.maxPoolSize").getString().toInt()
        ),
        uploadsDirectory = config.property("aitutor.uploads.directory").getString(),
        ollamaBaseUrl = config.property("aitutor.ollama.baseUrl").getString(),
        chunkingMaxTokens = config.property("aitutor.chunking.maxTokens").getString().toInt(),
        chunkingOverlapPercent = config.property("aitutor.chunking.overlapPercent").getString().toInt()
    )
}
