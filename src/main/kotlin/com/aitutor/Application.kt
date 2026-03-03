package com.aitutor

import com.aitutor.api.middleware.configureErrorHandling
import com.aitutor.api.routes.*
import com.aitutor.common.config.AppConfig
import com.aitutor.common.config.DatabaseFactory
import com.aitutor.common.config.loadAppConfig
import com.aitutor.ingestion.repository.ChunkRepository
import com.aitutor.ingestion.repository.DocumentRepository
import com.aitutor.ingestion.service.ChunkingService
import com.aitutor.ingestion.service.EmbeddingService
import com.aitutor.ingestion.service.PdfExtractionService
import com.aitutor.llm.provider.LlmProviderFactory
import com.aitutor.llm.service.ChatService
import com.aitutor.rag.repository.VectorRepository
import com.aitutor.rag.service.ContextAssembler
import com.aitutor.rag.service.RetrievalService
import com.aitutor.user.repository.UserRepository

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")
    val appConfig = loadAppConfig()

    DatabaseFactory.init(appConfig)
    logger.info("Database initialized")

    configureSerialization()
    configureCors(appConfig)
    configureErrorHandling()
    configureHeaders()

    val documentRepository = DocumentRepository()
    val chunkRepository = ChunkRepository()
    val userRepository = UserRepository()
    val vectorRepository = VectorRepository()

    val pdfExtractionService = PdfExtractionService()
    val chunkingService = ChunkingService(appConfig)
    val embeddingService = EmbeddingService()
    val contextAssembler = ContextAssembler(appConfig)
    val retrievalService = RetrievalService(vectorRepository, contextAssembler, appConfig)
    val providerFactory = LlmProviderFactory(appConfig)
    val chatService = ChatService(providerFactory, retrievalService, appConfig)

    configureRouting(
        appConfig = appConfig,
        documentRepository = documentRepository,
        chunkRepository = chunkRepository,
        pdfExtractionService = pdfExtractionService,
        chunkingService = chunkingService,
        embeddingService = embeddingService,
        vectorRepository = vectorRepository,
        providerFactory = providerFactory,
        chatService = chatService
    )

    logger.info("AI Tutor Backend started on port ${environment.config.property("ktor.deployment.port").getString()}")
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

fun Application.configureCors(appConfig: AppConfig) {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-LLM-Provider")
        allowHeader("X-API-Key")
        allowHeader("X-Model")
        allowHeader("X-Base-URL")

        val origins = appConfig.appMeta.corsAllowedOrigins
        if (origins == "*") {
            anyHost()
        } else {
            origins.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { origin ->
                allowHost(origin.removePrefix("https://").removePrefix("http://"),
                    schemes = listOf("http", "https"))
            }
        }
    }
}

fun Application.configureHeaders() {
    install(DefaultHeaders) {
        header("X-Engine", "AI-Tutor-Backend")
    }
}

fun Application.configureRouting(
    appConfig: AppConfig,
    documentRepository: DocumentRepository,
    chunkRepository: ChunkRepository,
    pdfExtractionService: PdfExtractionService,
    chunkingService: ChunkingService,
    embeddingService: EmbeddingService,
    vectorRepository: VectorRepository,
    providerFactory: LlmProviderFactory,
    chatService: ChatService
) {
    routing {
        route("/api/v1") {
            healthRoutes(appConfig)
            documentRoutes(
                appConfig = appConfig,
                documentRepository = documentRepository,
                chunkRepository = chunkRepository,
                pdfExtractionService = pdfExtractionService,
                chunkingService = chunkingService,
                embeddingService = embeddingService,
                vectorRepository = vectorRepository,
                providerFactory = providerFactory
            )
            chatRoutes(appConfig, chatService)
            conversationRoutes()
            modelRoutes()
        }
    }
}
