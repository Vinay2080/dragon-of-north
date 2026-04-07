alter table user_profiles
    add column if not exists avatar_public_id varchar(255) null;