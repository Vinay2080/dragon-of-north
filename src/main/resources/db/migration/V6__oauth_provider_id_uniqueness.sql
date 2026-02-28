create unique index if not exists uk_users_provider_provider_id
    on users (provider, provider_id)
    where provider_id is not null;
