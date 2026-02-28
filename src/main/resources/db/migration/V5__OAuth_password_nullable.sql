alter table users
    add provider varchar(20) default 'LOCAL';

update users
set provider = 'LOCAL'
where users.provider is null;

alter table users
    alter column provider set not null;

alter table users
    add constraint users_provider_check
        check ( users.provider in ('LOCAL', 'GOOGLE'));

alter table users
    add provider_id varchar(255);

alter table users
    alter column password drop not null;

create index idx_users_provider_id on users (provider_id);
create index idx_users_provider on users (provider);