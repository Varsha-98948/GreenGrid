-- GreenGrid V6 Migration: Add normalized title index for duplicate problem prevention safely

DO $$
DECLARE
    dup_count INT;
BEGIN
    SELECT COUNT(*) INTO dup_count
    FROM (
        SELECT user_id, LOWER(REGEXP_REPLACE(TRIM(title), '\s+', ' ', 'g'))
        FROM problems
        GROUP BY user_id, LOWER(REGEXP_REPLACE(TRIM(title), '\s+', ' ', 'g'))
        HAVING COUNT(*) > 1
    ) dups;

    IF dup_count = 0 THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes WHERE indexname = 'uk_problems_user_normalized_title'
        ) THEN
            CREATE UNIQUE INDEX uk_problems_user_normalized_title
            ON problems (user_id, LOWER(REGEXP_REPLACE(TRIM(title), '\s+', ' ', 'g')));
        END IF;
    ELSE
        RAISE NOTICE 'Existing duplicate titles found in problems table (% group(s)). Skipping UNIQUE index creation to preserve existing data.', dup_count;
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes WHERE indexname = 'idx_problems_user_normalized_title'
        ) THEN
            CREATE INDEX idx_problems_user_normalized_title
            ON problems (user_id, LOWER(REGEXP_REPLACE(TRIM(title), '\s+', ' ', 'g')));
        END IF;
    END IF;
END $$;
