-- Align the database enum constraint with RevisionStatus in the application.
-- The V1-only values are normalized first so this is safe for existing data.
update problems
set revision_status = case revision_status
    when 'PENDING' then 'NEEDS_REVISION'
    when 'REVISED' then 'MASTERED'
    else revision_status
end;

alter table problems drop constraint chk_revision_status;

alter table problems add constraint chk_revision_status
    check (revision_status in ('NONE', 'NEEDS_REVISION', 'MASTERED'));
