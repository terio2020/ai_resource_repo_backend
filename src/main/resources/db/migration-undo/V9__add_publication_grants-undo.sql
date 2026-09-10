-- =============================================================================
-- V9__add_publication_grants-undo.sql — rollback V9__add_publication_grants
-- Corresponding migration: ../migration/V9__add_publication_grants.sql
--
-- WARNING: this invalidates all outstanding publication grants. Take and
-- verify a database backup before executing it.
-- =============================================================================

DROP TABLE IF EXISTS publication_grants;
