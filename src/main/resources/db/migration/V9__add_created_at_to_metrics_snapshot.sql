-- =============================================
-- ServiceMetricsSnapshot mapea CREATED_AT,
-- pero V8 no creó esa columna.
-- =============================================
ALTER TABLE service_metrics_snapshot
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
