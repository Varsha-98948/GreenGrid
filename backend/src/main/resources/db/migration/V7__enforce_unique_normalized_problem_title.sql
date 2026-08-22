-- Replace the legacy normalized-title lookup index with the per-user unique index.
-- PostgreSQL DDL is transactional here: if duplicate normalized titles exist, the
-- CREATE UNIQUE INDEX fails and the preceding DROP operations are rolled back.

DROP INDEX IF EXISTS idx_problems_user_normalized_title;
DROP INDEX IF EXISTS uk_problems_user_normalized_title;

CREATE UNIQUE INDEX uk_problems_user_normalized_title
    ON problems (user_id, LOWER(REGEXP_REPLACE(TRIM(title), '\s+', ' ', 'g')));
