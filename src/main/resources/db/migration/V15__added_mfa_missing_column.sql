alter table users
    add column if not exists mfa_secret_encrypted varchar(255) null;