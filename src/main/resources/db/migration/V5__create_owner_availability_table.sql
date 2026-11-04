CREATE TABLE owner_availability (
    provider_id   VARCHAR(36)   NOT NULL,
    day           VARCHAR(10)   NOT NULL,
    initial_time  TIME          NOT NULL,
    final_time    TIME          NOT NULL,
    PRIMARY KEY (provider_id, day)
);
