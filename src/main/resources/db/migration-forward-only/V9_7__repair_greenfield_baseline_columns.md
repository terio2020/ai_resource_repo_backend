# V9.7 is forward-only

This migration conditionally adds columns absent from a fresh Flyway baseline. A generic undo would drop columns that may have existed before V9.7 and destroy data. Before release, verify a complete database backup; rollback to an older schema requires restoring a matching pre-release database backup with the matching application artifact.
