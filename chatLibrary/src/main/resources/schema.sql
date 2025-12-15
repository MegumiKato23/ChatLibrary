-- Set database to support full unicode (emojis)
-- ALTER DATABASE chat_library_test_db CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    password_hash VARCHAR(255),
    total_documents INT DEFAULT 0,
    last_login_at DATETIME,
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document (
    id VARCHAR(36) PRIMARY KEY,
    document_name VARCHAR(255),
    original_filename VARCHAR(255),
    file_path VARCHAR(512),
    file_type VARCHAR(50),
    file_size BIGINT,
    content_type VARCHAR(100),
    status TINYINT,
    error_message TEXT,
    total_chunks INT,
    total_pages INT,
    embedding_model VARCHAR(100),
    uploaded_at DATETIME,
    processed_at DATETIME,
    user_id VARCHAR(36),
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    INDEX idx_user_id (user_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_chunk (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    chunk_index INT NOT NULL,
    content LONGTEXT,
    token_count INT,
    metadata JSON,
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    INDEX idx_document_id (document_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_history (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(36),
    title VARCHAR(255),
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    UNIQUE KEY uk_session_id (session_id),
    INDEX idx_user_id (user_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(50),
    content TEXT,
    create_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    INDEX idx_session_id (session_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
