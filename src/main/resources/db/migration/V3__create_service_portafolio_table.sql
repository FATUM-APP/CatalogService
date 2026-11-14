CREATE TABLE service_portafolio (
    id                  VARCHAR(36)   NOT NULL PRIMARY KEY,
    service_id          VARCHAR(36)   NOT NULL,
    image_key           VARCHAR(512)  NOT NULL,
    original_filename   VARCHAR(255)  NOT NULL,
    content_type        VARCHAR(100)  NOT NULL,
    file_size           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_portafolio_service FOREIGN KEY (service_id) REFERENCES services (id)
);
