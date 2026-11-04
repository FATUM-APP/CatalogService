CREATE TABLE services (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    provider_id         VARCHAR(36)     NOT NULL,
    title               VARCHAR(40)     NOT NULL,
    provider_name       VARCHAR(255)    NOT NULL,
    provider_username   VARCHAR(15)     NOT NULL,
    description         TEXT,
    base_price          NUMERIC(10,2)   NOT NULL DEFAULT 0,
    price_type          VARCHAR(12)     NOT NULL,
    is_negotiable       BOOLEAN         NOT NULL DEFAULT FALSE,
    default_time        INTEGER,
    job_type            VARCHAR(10)     NOT NULL,
    status              VARCHAR(12)     NOT NULL DEFAULT 'ACTIVE',
    category            VARCHAR(30)     NOT NULL,
    fixed_questions     JSONB,
    CONSTRAINT fk_service_category FOREIGN KEY (category) REFERENCES categories (name),
    CONSTRAINT uk_services_provider_category UNIQUE (provider_id, category),
    CONSTRAINT uk_services_provider_title UNIQUE (provider_id, title)
);
