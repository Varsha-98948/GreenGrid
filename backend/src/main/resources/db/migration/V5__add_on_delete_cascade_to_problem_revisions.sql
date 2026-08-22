-- GreenGrid V5 Migration: Add ON DELETE CASCADE to problem_revisions foreign key safely

DO $$
DECLARE
    fk_name text;
BEGIN
    SELECT tc.constraint_name INTO fk_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
      ON tc.constraint_name = kcu.constraint_name
     AND tc.table_schema = kcu.table_schema
    WHERE tc.table_name = 'problem_revisions'
      AND kcu.column_name = 'problem_id'
      AND tc.constraint_type = 'FOREIGN KEY'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        EXECUTE 'ALTER TABLE problem_revisions DROP CONSTRAINT ' || quote_ident(fk_name);
    END IF;
END $$;

ALTER TABLE problem_revisions
    DROP CONSTRAINT IF EXISTS fk_problem_revisions_problem;

ALTER TABLE problem_revisions
    ADD CONSTRAINT fk_problem_revisions_problem
    FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE;
