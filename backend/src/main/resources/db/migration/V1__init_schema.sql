-- GreenGrid V1 Initial Schema
-- PostgreSQL + UUID + Flyway

create extension if not exists "pgcrypto";


-- =====================================================
-- USERS
-- =====================================================

create table users (
    id                     uuid primary key default gen_random_uuid(),
    email                  varchar(255) not null,
    password_hash          varchar(255),
    display_name           varchar(120) not null,
    avatar_url             text,
    onboarding_completed   boolean not null default false,
    theme_preference       varchar(20) not null default 'dark',

    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now(),

    constraint uk_users_email unique(email)
);


-- =====================================================
-- GITHUB ACCOUNTS
-- =====================================================

create table github_accounts (
    id                     uuid primary key default gen_random_uuid(),

    user_id                uuid not null unique
                           references users(id)
                           on delete cascade,

    github_user_id         bigint not null unique,
    github_username        varchar(255) not null,
    github_avatar_url      text,

    encrypted_access_token text not null,
    token_scope            varchar(255),

    connected_at           timestamptz not null default now(),

    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now()
);


create index idx_github_account_user
on github_accounts(user_id);



-- =====================================================
-- GITHUB REPOSITORIES
-- =====================================================

create table git_repositories (

    id                       uuid primary key default gen_random_uuid(),

    user_id                  uuid not null unique
                             references users(id)
                             on delete cascade,

    owner                    varchar(255) not null,
    name                     varchar(255) not null,
    full_name                varchar(511) not null,

    default_branch           varchar(100)
                             not null
                             default 'main',

    is_private               boolean
                             not null
                             default false,

    html_url                 text not null,

    was_created_by_greengrid boolean
                             not null
                             default false,

    last_sync_status         varchar(50),

    last_synced_at           timestamptz,

    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now()
);


create index idx_repository_user
on git_repositories(user_id);



-- =====================================================
-- TAGS
-- =====================================================

create table tags (

    id          uuid primary key default gen_random_uuid(),

    user_id     uuid not null
                references users(id)
                on delete cascade,

    name        varchar(100) not null,

    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),

    constraint uk_tag_user_name
    unique(user_id,name)
);


create index idx_tags_user
on tags(user_id);



-- =====================================================
-- PROBLEMS
-- =====================================================

create table problems (

    id                          uuid primary key default gen_random_uuid(),

    user_id                     uuid not null
                                references users(id)
                                on delete cascade,

    platform                    varchar(100) not null,

    title                       varchar(255) not null,

    problem_url                 text,

    difficulty                  varchar(20)
                                not null,

    language                    varchar(50)
                                not null,

    code                        text not null,

    notes                       text,

    time_complexity             varchar(100),

    space_complexity            varchar(100),


    solved_date                 date not null,


    revision_status             varchar(20)
                                not null
                                default 'NONE',

    is_favorite                 boolean
                                not null
                                default false,


    repo_folder_path            text,

    last_commit_sha             varchar(64),

    commit_status               varchar(20)
                                not null
                                default 'PENDING',


    external_slug               varchar(255),

    external_metadata_fetched   boolean
                                not null
                                default false,


    created_at                  timestamptz not null default now(),
    updated_at                  timestamptz not null default now(),



    constraint chk_problem_difficulty
    check(
        difficulty in ('EASY','MEDIUM','HARD')
    ),


    constraint chk_revision_status
    check(
        revision_status in ('NONE','PENDING','REVISED','MASTERED')
    ),


    constraint chk_commit_status
    check(
        commit_status in ('PENDING','READY','FAILED','COMMITTED')
    )

);


create index idx_problem_user
on problems(user_id);


create index idx_problem_user_date
on problems(user_id, solved_date);


create index idx_problem_user_difficulty
on problems(user_id,difficulty);



-- =====================================================
-- MANY TO MANY PROBLEM TAGS
-- =====================================================

create table problem_tags (

    problem_id uuid not null
                 references problems(id)
                 on delete cascade,


    tag_id uuid not null
           references tags(id)
           on delete cascade,


    primary key(problem_id,tag_id)

);



create index idx_problem_tags_problem
on problem_tags(problem_id);


create index idx_problem_tags_tag
on problem_tags(tag_id);



-- =====================================================
-- UPDATED_AT TRIGGER
-- =====================================================

create or replace function update_updated_at_column()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;



create trigger users_updated_at
before update on users
for each row
execute function update_updated_at_column();


create trigger github_accounts_updated_at
before update on github_accounts
for each row
execute function update_updated_at_column();


create trigger repositories_updated_at
before update on git_repositories
for each row
execute function update_updated_at_column();


create trigger tags_updated_at
before update on tags
for each row
execute function update_updated_at_column();


create trigger problems_updated_at
before update on problems
for each row
execute function update_updated_at_column();