# AI Tutor Backend

Kotlin + Ktor backend for the AI Tutor application. Provides RAG-powered tutoring from PDF textbooks with multilingual support (English, Urdu, Roman Urdu).

## Tech Stack

- **Kotlin** + **Ktor** — Server framework with coroutines
- **PostgreSQL** + **pgvector** — Database with vector similarity search
- **Apache Tika** — PDF text extraction (with OCR fallback)
- **Exposed** — Kotlin SQL framework
- **Flyway** — Database migrations
- **BYOK** — Bring Your Own API Key (OpenAI, Ollama, custom endpoints)

## Prerequisites

- JDK 21+
- Docker & Docker Compose

## Quick Start

### 0. Configure `.env`

```bash
cp .env.example .env
```

Edit `.env` as needed. Runtime config loading priority is:
1. OS environment variables
2. `.env` file
3. `application.yaml`

### 1. Start the database

```bash
docker compose up -d postgres
```

To also start Ollama for open-source models:

```bash
docker compose --profile ollama up -d
```

### 2. Run the backend

```bash
./gradlew run
```

The server starts at `http://localhost:8080`.

### 3. Test the health endpoint

```bash
curl http://localhost:8080/api/v1/health
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/health` | Health check |
| POST | `/api/v1/documents/upload` | Upload PDF (multipart) |
| GET | `/api/v1/documents` | List all documents |
| GET | `/api/v1/documents/{id}` | Get document details |
| DELETE | `/api/v1/documents/{id}` | Delete document |
| POST | `/api/v1/chat` | Chat with SSE streaming |
| GET | `/api/v1/conversations` | List conversations |
| GET | `/api/v1/conversations/{id}/messages` | Get messages |
| DELETE | `/api/v1/conversations/{id}` | Delete conversation |
| GET | `/api/v1/models/available` | List available models |

## BYOK Headers

Pass LLM configuration via request headers:

```
X-LLM-Provider: openai | ollama | custom
X-API-Key: <your-api-key>
X-Model: gpt-4o | llama3 | etc.
X-Base-URL: <custom-endpoint-url>
```

## Project Structure

```
src/main/kotlin/com/aitutor/
├── Application.kt          — Entry point & wiring
├── api/
│   ├── routes/             — REST endpoint handlers
│   ├── dto/                — Request/response data classes
│   └── middleware/         — BYOK extraction, error handling
├── ingestion/
│   ├── service/            — PDF extraction, chunking, embeddings
│   ├── model/              — Database table definitions
│   └── repository/         — Database operations
├── rag/
│   ├── service/            — Retrieval, context assembly
│   └── repository/         — Vector similarity search
├── llm/
│   ├── provider/           — LLM abstraction (OpenAI, Ollama, Custom)
│   ├── service/            — Chat orchestration
│   └── config/             — System prompts
├── user/
│   ├── model/              — User table
│   ├── service/            — User operations
│   └── repository/         — User database ops
└── common/
    ├── config/             — App & database configuration
    ├── model/              — Shared tables (conversations, messages)
    └── util/               — Extension functions
```
