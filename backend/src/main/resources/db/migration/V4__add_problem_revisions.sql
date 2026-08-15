-- GreenGrid V4 Migration: Add problem_revisions table and backfill Revision 1

CREATE TABLE IF NOT EXISTS problem_revisions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id          UUID NOT NULL REFERENCES problems(id),
    revision_number     INT NOT NULL,
    title               VARCHAR(255),
    language            VARCHAR(50) NOT NULL,
    code                TEXT NOT NULL,
    notes               TEXT,
    time_complexity     VARCHAR(100),
    space_complexity    VARCHAR(100),
    repo_folder_path    TEXT,
    last_commit_sha     VARCHAR(64),
    commit_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_problem_revision_number UNIQUE(problem_id, revision_number),
    CONSTRAINT chk_rev_commit_status CHECK(commit_status IN ('PENDING','READY','FAILED','COMMITTED'))
);

CREATE INDEX IF NOT EXISTS idx_problem_revisions_problem ON problem_revisions(problem_id, revision_number);

-- Backfill: Every existing problem receives exactly ONE Revision 1 record
INSERT INTO problem_revisions (
    id, problem_id, revision_number, title, language, code, notes,
    time_complexity, space_complexity, repo_folder_path, last_commit_sha,
    commit_status, created_at, updated_at
)
SELECT
    gen_random_uuid(), p.id, 1, 'Initial Solution', p.language, p.code, p.notes,
    p.time_complexity, p.space_complexity, p.repo_folder_path, p.last_commit_sha,
    p.commit_status, p.created_at, p.updated_at
FROM problems p
WHERE NOT EXISTS (
    SELECT 1 FROM problem_revisions pr WHERE pr.problem_id = p.id AND pr.revision_number = 1
);

-- Updated_at trigger for problem_revisions
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'problem_revisions_updated_at'
    ) THEN
        CREATE TRIGGER problem_revisions_updated_at
        BEFORE UPDATE ON problem_revisions
        FOR EACH ROW
        EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;
