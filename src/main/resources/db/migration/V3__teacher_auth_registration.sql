create table if not exists teacher_codes (
    id bigserial primary key,
    code varchar(255) not null unique,
    code_hash varchar(255) unique,
    email varchar(255),
    is_active boolean not null default true,
    used boolean not null default false,
    used_at timestamp,
    used_by_user_id varchar(255),
    created_at timestamp not null default now(),
    created_by_admin_id varchar(255),
    expires_at timestamp,
    usage_limit integer,
    times_used integer not null default 0
);

create table if not exists teacher_registration_tokens (
    id bigserial primary key,
    token_hash varchar(255) not null unique,
    email varchar(255),
    teacher_code_id bigint not null references teacher_codes(id),
    created_at timestamp not null default now(),
    expires_at timestamp not null,
    used_at timestamp
);

create table if not exists local_auth_users (
    id bigserial primary key,
    auth_user_id varchar(255) not null unique,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    email_verified boolean not null default false,
    email_verification_token_hash varchar(255),
    email_verification_expires_at timestamp,
    created_at timestamp not null default now()
);

create index if not exists idx_teacher_registration_tokens_hash on teacher_registration_tokens(token_hash);
create index if not exists idx_local_auth_users_email on local_auth_users(email);
