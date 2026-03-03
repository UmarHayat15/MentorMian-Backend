package com.aitutor.api.routes

import com.aitutor.api.dto.ApiResponse
import com.aitutor.api.dto.ModelInfo
import com.aitutor.api.dto.ModelsResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.modelRoutes() {

    get("/models/available") {
        val models = listOf(
            ModelInfo(id = "gpt-4o", name = "GPT-4o", provider = "openai"),
            ModelInfo(id = "gpt-4o-mini", name = "GPT-4o Mini", provider = "openai"),
            ModelInfo(id = "gpt-4-turbo", name = "GPT-4 Turbo", provider = "openai"),
            ModelInfo(id = "llama3", name = "Llama 3", provider = "ollama"),
            ModelInfo(id = "mistral", name = "Mistral", provider = "ollama"),
            ModelInfo(id = "qwen2", name = "Qwen 2", provider = "ollama"),
            ModelInfo(id = "gemma2", name = "Gemma 2", provider = "ollama")
        )
        call.respond(ApiResponse(success = true, data = ModelsResponse(models)))
    }
}
