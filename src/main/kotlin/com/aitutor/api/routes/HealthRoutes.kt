package com.aitutor.api.routes

import com.aitutor.api.dto.HealthResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.healthRoutes() {
    get("/health") {
        call.respond(HealthResponse(status = "ok", version = "0.1.0"))
    }
}
