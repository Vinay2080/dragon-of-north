alter table user_mfa_recovery_codes
    drop constraint fk_user_mfa_recovery_codes_settings;

alter table user_mfa_recovery_codes
    add constraint fk_user_mfa_recovery_codes_settings
        foreign key (mfa_settings_id)
            references user_mfa_settings (id)
            on delete cascade;