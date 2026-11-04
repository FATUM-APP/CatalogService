CREATE TABLE client_favorites (
    id          VARCHAR(36)   NOT NULL PRIMARY KEY,
    client_id   VARCHAR(36)   NOT NULL,
    service_id  VARCHAR(36)   NOT NULL,
    created_at  TIMESTAMP     NOT NULL,
    CONSTRAINT fk_favorite_service FOREIGN KEY (service_id) REFERENCES services (id),
    CONSTRAINT uk_favorites_client_service UNIQUE (client_id, service_id)
);
