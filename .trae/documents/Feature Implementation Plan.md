# Feature Implementation Plan: Authentication, Preview, and Enhanced Upload

This plan outlines the steps to implement user authentication, document preview (supporting PDF, TXT, MD, DOCX), and enhanced upload functionality.

## 1. Backend Implementation (`chatLibrary`)

### 1.1 Authentication Module
-   **Dependencies**: Add `spring-boot-starter-validation`.
-   **Components**: Create `UserMapper`, `UserService`, and `UserController`.
    -   `POST /user/login`: Verify credentials.
    -   `POST /user/register`: Create account.

### 1.2 Document Preview (Enhanced)
-   **Endpoint**: `GET /document/preview/{id}`.
-   **Format Handling**:
    -   **PDF/TXT/MD**: Serve raw content with appropriate `MediaType` (`application/pdf`, `text/plain`, `text/markdown`). Browser renders these natively or via extensions.
    -   **DOCX**: Convert to HTML or PDF on the fly, OR simpler for MVP: Serve as download or use a frontend renderer. *Strategy: For MVP, we will serve the raw file. Frontend will handle rendering. For DOCX, frontend can use a library like `docx-preview` or `mammoth.js`.*
-   **CORS**: Ensure headers allow frontend to fetch these blobs.

### 1.3 Upload Updates
-   **Update Controller**: Accept `userId` in upload request.

## 2. Frontend Implementation (`chat-library-frontend`)

### 2.1 Authentication UI
-   **State Management**: Create a simple composable `useAuth` to handle user state and localStorage.
-   **Login/Register Modal**: Integrated into the main page or separate route.

### 2.2 Document Preview UI
-   **Preview Dialog**: A modal dialog to show document content.
-   **File Handlers**:
    -   **PDF**: Use `<iframe src="blob_url">`.
    -   **TXT/MD**: Fetch text and render in `<pre>` or a Markdown renderer (`markdown-it` is already common in Nuxt context or we can add it).
    -   **DOCX**: Use `renderAsync` from `docx-preview` npm package to render DOCX into a container div.
-   **Dependencies**: Install `docx-preview` (or similar), `markdown-it`.

### 2.3 Enhanced Upload
-   **UI**: Progress bar, drag & drop.
-   **Logic**: Attach `userId` to upload request.

## Execution Steps
1.  **Backend**: Implement User Auth (Entity, Service, Controller).
2.  **Backend**: Add `preview` endpoint to `DocumentController`.
3.  **Frontend**: Install dependencies (`docx-preview`, `markdown-it`).
4.  **Frontend**: Implement Login/Register logic and UI.
5.  **Frontend**: Implement Document Preview Modal with type switching logic.
6.  **Frontend**: Update Upload to include `userId` and show progress.
