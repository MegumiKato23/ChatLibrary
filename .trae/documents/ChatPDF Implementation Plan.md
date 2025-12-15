# Project Implementation Plan: Spring AI ChatPDF (Nuxt + Spring Boot)

This plan outlines the steps to complete the ChatPDF-like application, including backend enhancements, frontend development, and API integration.

## 1. Backend Enhancements (`chatLibrary`)

We will refine the existing Spring Boot application to provide necessary endpoints and ensure robust streaming and file management.

### Dependencies & Configuration
- **Add Swagger/OpenAPI**: Integrate `springdoc-openapi-starter-webmvc-ui` to generate API documentation automatically.
- **Fix CORS**: Update `MvcConfiguration` to use `allowedOriginPatterns` instead of `allowedOrigins` with credentials, ensuring the frontend (port 3000) can communicate with the backend (port 8080).

### API Development
- **Document Management (`DocumentController`)**:
    - `GET /document/list`: Retrieve a list of uploaded documents.
    - `DELETE /document/{id}`: Delete a document and its vectors.
- **Chat Interface (`ChatController`)**:
    - Update `chat` endpoint to use `MediaType.TEXT_EVENT_STREAM_VALUE` for proper Server-Sent Events (SSE) streaming.
    - `GET /ai/history/{sessionId}`: Retrieve chat history for a specific session.
- **Service Layer**:
    - Implement `getList` and `delete` methods in `DocumentService`.
    - Implement `getHistory` in `ChatService`.

## 2. Frontend Development (`chat-library-frontend`)

We will build a modern, responsive UI using Nuxt 3 (or 4), Vue 3, and Tailwind CSS + Element Plus.

### Setup & Dependencies
- **Install Packages**: `element-plus`, `tailwindcss`, `axios`, `sass`, `@element-plus/icons-vue`.
- **Configuration**: Configure `nuxt.config.ts` to load Element Plus and Tailwind CSS.

### UI Components & Pages
- **Layout**: A responsive layout with a sidebar for document management and a main area for chat.
- **Components**:
    - `FileUploader`: Drag-and-drop area for PDF uploads.
    - `DocumentList`: List of processed documents with status indicators and delete options.
    - `ChatWindow`: Message history display (User vs. AI) with auto-scroll.
    - `ChatInput`: Text area for entering prompts.
- **State Management**: Use Vue `ref` / `reactive` for simple state (current chat session, document list).

### Integration
- **API Client**: Configure `axios` with base URL and interceptors.
- **Streaming**: Use `fetch` API or `EventSource` (via `fetch-event-source` or native) to handle the streaming response from the chat endpoint.

## 3. API Documentation
- **Swagger UI**: Once implemented, API documentation will be accessible at `http://localhost:8080/swagger-ui/index.html`.
- **Manual Summary**: I will also provide a markdown summary of the endpoints.

## Execution Steps
1.  **Modify Backend `pom.xml`**: Add OpenAPI dependency.
2.  **Refactor Backend Code**: Update Controllers and Services.
3.  **Setup Frontend**: Install dependencies and configure Nuxt.
4.  **Implement Frontend UI**: Create pages and components.
5.  **Verify & Test**: Upload a PDF, chat with it, and verify persistence.
