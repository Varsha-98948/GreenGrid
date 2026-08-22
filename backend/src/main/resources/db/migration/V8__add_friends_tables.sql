-- GreenGrid V8 Friends and Friend Requests Schema

-- 1. Friend Requests Table
create table friend_requests (
    id            uuid primary key default gen_random_uuid(),
    requester_id  uuid not null references users(id) on delete cascade,
    addressee_id  uuid not null references users(id) on delete cascade,
    status        varchar(20) not null default 'PENDING',
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),

    constraint chk_friend_request_not_self check (requester_id <> addressee_id),
    constraint chk_friend_request_status check (status in ('PENDING', 'ACCEPTED', 'REJECTED')),
    constraint uk_friend_request_pair unique (requester_id, addressee_id)
);

create index idx_friend_requests_requester on friend_requests(requester_id);
create index idx_friend_requests_addressee on friend_requests(addressee_id);

create trigger friend_requests_updated_at
before update on friend_requests
for each row
execute function update_updated_at_column();

-- 2. Friendships Table (canonical order user_id1 < user_id2 enforced)
create table friendships (
    id          uuid primary key default gen_random_uuid(),
    user_id1    uuid not null references users(id) on delete cascade,
    user_id2    uuid not null references users(id) on delete cascade,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),

    constraint chk_friendship_user_order check (user_id1 < user_id2),
    constraint uk_friendship_pair unique (user_id1, user_id2)
);

create index idx_friendships_user1 on friendships(user_id1);
create index idx_friendships_user2 on friendships(user_id2);

create trigger friendships_updated_at
before update on friendships
for each row
execute function update_updated_at_column();
