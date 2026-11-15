-- Permite que un profesional tenga más de un servicio en la misma categoría.
-- La unicidad de (PROVIDER_ID, TITLE) se conserva.
ALTER TABLE services
DROP CONSTRAINT IF EXISTS uk_services_provider_category;

-- Cubre el caso de un índice único creado por separado en algún entorno.
DROP INDEX IF EXISTS uk_services_provider_category;