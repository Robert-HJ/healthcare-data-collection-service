CREATE TABLE IF NOT EXISTS member
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50)  NOT NULL,
    nickname   VARCHAR(50)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    record_key VARCHAR(36)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_member PRIMARY KEY (id),
    CONSTRAINT uk_member_email UNIQUE (email),
    CONSTRAINT uk_member_record_key UNIQUE (record_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS health_data_collection_request
(
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    member_id     BIGINT        NOT NULL,
    data_type     VARCHAR(30)   NOT NULL,
    source        VARCHAR(50)   NOT NULL,
    payload       JSON          NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    retry_count   INT           NOT NULL DEFAULT 0,
    error_message VARCHAR(1000) NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    CONSTRAINT pk_health_data_collection_request PRIMARY KEY (id),
    INDEX idx_health_data_collection_request_status_updated_at (status, updated_at),
    INDEX idx_health_data_collection_request_member_id (member_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
