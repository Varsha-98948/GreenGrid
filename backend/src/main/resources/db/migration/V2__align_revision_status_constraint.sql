-- Align revision status values

alter table problems
drop constraint chk_revision_status;


update problems
set revision_status = case revision_status
    when 'PENDING' then 'NEEDS_REVISION'
    when 'REVISED' then 'MASTERED'
    else revision_status
end;


alter table problems
add constraint chk_revision_status
check(
    revision_status in (
        'NONE',
        'NEEDS_REVISION',
        'MASTERED'
    )
);