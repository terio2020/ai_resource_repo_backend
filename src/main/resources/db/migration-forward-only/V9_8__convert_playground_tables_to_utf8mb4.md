# V9.8 is forward-only

Converting these tables back from utf8mb4 may lose Unicode data. Before release, verify a complete database backup; rollback to an older schema requires restoring a matching pre-release database backup with the matching application artifact. Do not fabricate a lossy SQL undo.
