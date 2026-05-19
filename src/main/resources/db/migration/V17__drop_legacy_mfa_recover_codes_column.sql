alter table users
    drop column if exists mfa_recover_codes_encrypted;

