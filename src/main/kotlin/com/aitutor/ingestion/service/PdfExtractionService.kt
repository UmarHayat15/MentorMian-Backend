package com.aitutor.ingestion.service

import org.apache.tika.Tika
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream

data class ExtractedDocument(
    val text: String,
    val title: String?,
    val pageCount: Int?
)

class PdfExtractionService {

    private val logger = LoggerFactory.getLogger(PdfExtractionService::class.java)
    private val tika = Tika()

    fun extract(file: File): ExtractedDocument {
        logger.info("Extracting text from: ${file.name}")

        val metadata = Metadata()
        val handler = BodyContentHandler(-1)
        val parser = AutoDetectParser()

        FileInputStream(file).use { stream ->
            parser.parse(stream, handler, metadata)
        }

        val text = handler.toString().trim()
        val title = metadata.get("dc:title") ?: metadata.get("title")
        val pageCount = metadata.get("xmpTPg:NPages")?.toIntOrNull()

        logger.info("Extracted ${text.length} characters, ${pageCount ?: "unknown"} pages from ${file.name}")

        return ExtractedDocument(
            text = text,
            title = title,
            pageCount = pageCount
        )
    }
}
