alter table user_sessions
    add column if not exists mfa_verified_at timestamp   null,
    add column if not exists mfa_required    boolean     not null default false,
    add column if not exists primary_amr     varchar(32) not null default 'pwd';
