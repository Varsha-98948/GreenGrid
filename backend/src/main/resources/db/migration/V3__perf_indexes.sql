-- Add performance indexes for high-frequency queries:
-- 1. Language breakdown & language search
CREATE INDEX IF NOT EXISTS idx_problem_user_language ON problems(user_id, language);

-- 2. Platform filtering
CREATE INDEX IF NOT EXISTS idx_problem_user_platform ON problems(user_id, platform);

-- 3. Tag name lookup & topic aggregation
CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name);
