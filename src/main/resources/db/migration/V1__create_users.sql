CREATE TABLE users (
    id BIGSERIAL primary key,
    username VARCHAR(50) not null,
    password_hash VARCHAR(100) not null,
    role VARCHAR(20) not null,
    enabled BOOLEAN not null default true,
    created_at timestamptz not null default now(),
    constraint chk_users_role check (role in ('USER', 'ADMIN'))
);

CREATE unique index ux_users_username_lower on users (lower(username));