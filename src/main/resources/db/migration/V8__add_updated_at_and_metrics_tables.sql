-- =============================================
-- 1. Add updated_at column to orders table
-- =============================================
ALTER TABLE orders ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Backfill existing orders with created_at value
UPDATE orders SET updated_at = created_at;

-- =============================================
-- 2. Create metrics tables
-- =============================================
CREATE TABLE service_metrics (
                                 service_id VARCHAR(255) PRIMARY KEY REFERENCES services(id),
                                 total_views BIGINT NOT NULL DEFAULT 0,
                                 favorite_count INT NOT NULL DEFAULT 0,
                                 total_dwell_seconds BIGINT NOT NULL DEFAULT 0,
                                 dwell_event_count BIGINT NOT NULL DEFAULT 0,
                                 period_views BIGINT NOT NULL DEFAULT 0,
                                 period_dwell_seconds BIGINT NOT NULL DEFAULT 0,
                                 period_dwell_events BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE service_metrics_snapshot (
                                          id VARCHAR(255) PRIMARY KEY,
                                          service_id VARCHAR(255) NOT NULL REFERENCES services(id),
                                          year INT NOT NULL,
                                          week INT NOT NULL,
                                          snapshot_date DATE NOT NULL,
                                          views_in_period BIGINT NOT NULL DEFAULT 0,
                                          dwell_seconds_in_period BIGINT NOT NULL DEFAULT 0,
                                          dwell_events_in_period BIGINT NOT NULL DEFAULT 0,
                                          new_orders_in_period INT NOT NULL DEFAULT 0,
                                          favorite_count INT NOT NULL DEFAULT 0,
                                          total_orders INT NOT NULL DEFAULT 0,
                                          pending_orders INT NOT NULL DEFAULT 0,
                                          accepted_orders INT NOT NULL DEFAULT 0,
                                          rejected_orders INT NOT NULL DEFAULT 0,
                                          delivering_orders INT NOT NULL DEFAULT 0,
                                          completed_orders INT NOT NULL DEFAULT 0,
                                          cancelled_orders INT NOT NULL DEFAULT 0,
                                          not_delivered_orders INT NOT NULL DEFAULT 0,
                                          dispute_related_orders INT NOT NULL DEFAULT 0,
                                          UNIQUE (service_id, year, week)
);

CREATE INDEX idx_snapshot_service_date ON service_metrics_snapshot(service_id, snapshot_date);

CREATE TABLE service_view_event (
                                    id VARCHAR(255) PRIMARY KEY,
                                    service_id VARCHAR(255) NOT NULL REFERENCES services(id),
                                    user_id VARCHAR(255) NOT NULL,
                                    dwell_time_seconds INT NOT NULL DEFAULT 0,
                                    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_view_event_service_date ON service_view_event(service_id, created_at);

-- Backfill metrics for existing services
INSERT INTO service_metrics (service_id, total_views, favorite_count, total_dwell_seconds, dwell_event_count, period_views, period_dwell_seconds, period_dwell_events)
SELECT id, 0, 0, 0, 0, 0, 0, 0 FROM services;

-- Populate favorite_count from existing favorites
UPDATE service_metrics sm
SET favorite_count = (
    SELECT COUNT(*) FROM client_favorites cf WHERE cf.service_id = sm.service_id
);