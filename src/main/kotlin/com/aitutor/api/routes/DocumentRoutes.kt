package com.aitutor.api.routes

import com.aitutor.api.dto.ApiResponse
import com.aitutor.api.dto.DocumentListResponse
import com.aitutor.api.middleware.extractLlmConfig
import com.aitutor.common.config.AppConfig
import com.aitutor.common.util.toUUID
import com.aitutor.ingestion.repository.ChunkRepository
import com.aitutor.ingestion.repository.DocumentRepository
import com.aitutor.ingestion.service.ChunkingService
import com.aitutor.ingestion.service.EmbeddingService
import com.aitutor.ingestion.service.PdfExtractionService
import com.aitutor.llm.provider.LlmProviderFactory
import com.aitutor.rag.repository.VectorRepository
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

fun Route.documentRoutes(
    appConfig: AppConfig,
    documentRepository: DocumentRepository,
    chunkRepository: ChunkRepository,
    pdfExtractionService: PdfExtractionService,
    chunkingService: ChunkingService,
    embeddingService: EmbeddingService,
    vectorRepository: VectorRepository,
    providerFactory: LlmProviderFactory
) {
    val logger = LoggerFactory.getLogger("DocumentRoutes")
    val uploadsDir = File(appConfig.uploadsDirectory).also { it.mkdirs() }

    route("/documents") {

        post("/upload") {
            val multipart = call.receiveMultipart()
            var fileName: String? = null
            var savedFile: File? = null
            var title: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        fileName = part.originalFileName ?: "upload.pdf"
                        val fileId = UUID.randomUUID().toString()
                        savedFile = File(uploadsDir, "$fileId-$fileName").also { file ->
                            part.streamProvider().use { input ->
                                file.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                    is PartData.FormItem -> {
                        if (part.name == "title") title = part.value
                    }
                    else -> {}
                }
                part.dispose()
            }

            val file = savedFile ?: throw IllegalArgumentException("No file uploaded")
            val doc = documentRepository.create(
                userId = null,
                title = title,
                fileName = fileName!!
            )

            application.launch(Dispatchers.IO) {
                try {
                    val extracted = pdfExtractionService.extract(file)
                    val chunks = chunkingService.chunk(extracted.text)

                    val llmConfig = call.extractLlmConfig(appConfig.defaults)
                    val provider = providerFactory.create(llmConfig)

                    val docId = doc.id.toUUID()
                    for (chunk in chunks) {
                        val chunkId = chunkRepository.insert(
                            documentId = docId,
                            content = chunk.content,
                            pageNumber = chunk.pageNumber,
                            chapter = null,
                            chunkIndex = chunk.chunkIndex
                        )
                        val embedding = embeddingService.generateEmbedding(provider, chunk.content)
                        vectorRepository.storeEmbedding(chunkId, embedding)
                    }

                    documentRepository.updateStatus(
                        id = docId,
                        status = "ready",
                        totalPages = extracted.pageCount,
                        totalChunks = chunks.size
                    )
                    logger.info("Document ${doc.id} processed: ${chunks.size} chunks")
                } catch (e: Exception) {
                    logger.error("Failed to process document ${doc.id}", e)
                    documentRepository.updateStatus(
                        id = doc.id.toUUID(),
                        status = "failed",
                        totalPages = null,
                        totalChunks = null
                    )
                }
            }

            call.respond(HttpStatusCode.Accepted, ApiResponse(success = true, data = doc))
        }

        get {
            val documents = documentRepository.findAllByUserId(null)
            call.respond(ApiResponse(success = true, data = DocumentListResponse(documents)))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toUUID()
                ?: throw IllegalArgumentException("Invalid document ID")
            val doc = documentRepository.findById(id)
                ?: throw NoSuchElementException("Document not found")
            call.respond(ApiResponse(success = true, data = doc))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toUUID()
                ?: throw IllegalArgumentException("Invalid document ID")
            documentRepository.delete(id)
            call.respond(ApiResponse<Unit>(success = true))
        }
    }
}
