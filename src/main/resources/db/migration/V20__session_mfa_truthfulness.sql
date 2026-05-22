-- Backfill MFA verification timestamps for non-MFA-required sessions created before MFA migration.
update user_sessions
set mfa_verified_at = created_at
where mfa_required = false
  and mfa_verified_at is null;

-- Remove implicit defaults, so session creation must set truthful MFA/AMR state explicitly.
alter table user_sessions
    alter column mfa_required drop default;

alter table user_sessions
    alter column primary_amr drop default;

