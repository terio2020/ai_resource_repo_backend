-- Destructive rollback: use only after disabling Playground and taking a verified DB backup.
-- Dropping these tables removes participation, activities, event history and public links.
DROP TABLE IF EXISTS playground_match_queue;
DROP TABLE IF EXISTS playground_match_mutex;
DROP TABLE IF EXISTS playground_shares;
DROP TABLE IF EXISTS playground_events;
DROP TABLE IF EXISTS playground_actions;
DROP TABLE IF EXISTS playground_daily_budgets;
DROP TABLE IF EXISTS playground_attempts;
DROP TABLE IF EXISTS playground_tasks;
DROP TABLE IF EXISTS playground_seats;
DROP TABLE IF EXISTS playground_activities;
DROP TABLE IF EXISTS playground_participations;
