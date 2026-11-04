CREATE TABLE provider_unavailable_dates (
    id            VARCHAR(36)   NOT NULL PRIMARY KEY,
    provider_id   VARCHAR(36)   NOT NULL,
    reason        TEXT,
    start_date    TIMESTAMP     NOT NULL,
    final_date    TIMESTAMP     NOT NULL,
    CONSTRAINT uk_unavailable_dates_provider_date UNIQUE (provider_id, start_date)
);
