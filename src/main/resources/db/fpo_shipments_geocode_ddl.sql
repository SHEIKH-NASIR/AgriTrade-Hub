-- Adds geocoding verification metadata to the existing fpo_shipments table.
-- Run once against an existing database if Hibernate schema update is not being used.

ALTER TABLE fpo_shipments
    ADD COLUMN geocode_confidence_score DOUBLE NULL,
    ADD COLUMN geocode_source VARCHAR(50) NULL,
    ADD COLUMN geocoded_at DATETIME NULL;
