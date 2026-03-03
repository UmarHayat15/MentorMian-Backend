package com.aitutor.api.routes

import com.aitutor.api.dto.HealthResponse
import com.aitutor.common.config.AppConfig
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.healthRoutes(appConfig: AppConfig) {
    get("/health") {
        call.respond(HealthResponse(status = "ok", version = appConfig.appMeta.version))
    }
}
