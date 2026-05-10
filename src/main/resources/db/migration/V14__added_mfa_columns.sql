alter table users
    add column if not exists mfa_enabled                  boolean                  not null default false,
    add column if not exists mfa_enabled_at               timestamp with time zone null,
    add column if not exists mfa_recovery_codes_encrypted varchar(255) null;
