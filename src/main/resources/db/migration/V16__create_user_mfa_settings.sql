alter table users
    alter column mfa_secret_encrypted type text;

create table if not exists user_mfa_settings
(
    deleted               boolean                           default false not null,
    created_at            timestamp with time zone not null default now(),
    updated_at            timestamp with time zone not null default now(),
    totp_enabled_at       timestamp with time zone not null,
    version               bigint,
    id                    uuid                     not null,
    user_id               uuid                     not null,
    created_by            varchar(255)             not null default 'migration',
    updated_by            varchar(255)             not null default 'migration',
    totp_secret_encrypted text                     not null,
    constraint user_mfa_settings_pkey primary key (id),
    constraint user_mfa_settings_user_id_key unique (user_id),
    constraint fk_user_mfa_settings_user foreign key (user_id) references users (id)
);

create index if not exists idx_user_mfa_settings_user_id on user_mfa_settings (user_id);

create table if not exists user_mfa_recovery_codes
(
    deleted            boolean                           default false not null,
    used               boolean                           default false not null,
    created_at         timestamp with time zone not null default now(),
    updated_at         timestamp with time zone not null default now(),
    used_at            timestamp with time zone null,
    version            bigint,
    id                 uuid                     not null,
    mfa_settings_id    uuid                     not null,
    created_by         varchar(255)             not null default 'migration',
    updated_by         varchar(255)             not null default 'migration',
    recovery_code_hash varchar(255)             not null,
    constraint user_mfa_recovery_codes_pkey primary key (id),
    constraint fk_user_mfa_recovery_codes_settings foreign key (mfa_settings_id) references user_mfa_settings (id)
);

create index if not exists idx_user_mfa_recovery_codes_settings_id
    on user_mfa_recovery_codes (mfa_settings_id);

create index if not exists idx_user_mfa_recovery_codes_active
    on user_mfa_recovery_codes (mfa_settings_id, used, deleted);

insert into user_mfa_settings (deleted,
                               created_at,
                               updated_at,
                               totp_enabled_at,
                               version,
                               id,
                               user_id,
                               created_by,
                               updated_by,
                               totp_secret_encrypted)
select false,
       now(),
       now(),
       coalesce(u.mfa_enabled_at, now()),
       0,
       gen_random_uuid(),
       u.id,
       'migration',
       'migration',
       u.mfa_secret_encrypted
from users u
where u.mfa_secret_encrypted is not null
  and not exists (select 1
                  from user_mfa_settings existing
                  where existing.user_id = u.id);

alter table users
    drop column if exists mfa_secret_encrypted,
    drop column if exists mfa_recovery_codes_encrypted;
