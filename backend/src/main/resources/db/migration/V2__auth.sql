create table app_user (
    id         uuid         primary key default gen_random_uuid(),
    email      varchar(320) not null unique,
    created_at timestamptz  not null default now()
);

create table login_code (
    email      varchar(320) primary key,
    code_hash  varchar(64)  not null,
    expires_at timestamptz  not null,
    attempts   int          not null default 0
);
