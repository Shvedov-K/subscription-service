-- users
create table users
(
    id uuid not null
        primary key
);

-- subscriptions
create table subscriptions
(
    id      uuid default gen_random_uuid() not null
        primary key,
    user_id uuid                           not null
        references users,
    type    varchar(10)                    not null
        constraint subscriptions_type_check
            check ((type)::text = ANY ((ARRAY ['BASIC':: character varying, 'PRO':: character varying])::text[])
) ,
    activation_date   date                                not null,
    active            boolean   default true              not null,
    deactivation_date date,
    created_at        timestamp default now()
);

create unique index idx_one_active_per_user
    on subscriptions (user_id) where (active = true);

create index idx_subscriptions_user_id
    on subscriptions (user_id);

-- invoices
create table invoices
(
    id                           uuid      default gen_random_uuid() not null
        primary key,
    user_id                      uuid                                not null
        references users,
    subscription_id              uuid                                not null
        references subscriptions,
    issue_date                   date                                not null,
    amount                       numeric(10, 2)                      not null,
    subscription_type            varchar(10)                         not null,
    subscription_activation_date date                                not null,
    created_at                   timestamp default now()
);

CREATE INDEX IF NOT EXISTS idx_invoices_user_id_issue_date
    ON invoices(user_id, issue_date DESC);
CREATE INDEX IF NOT EXISTS idx_invoices_subscription_id_issue_date
    ON invoices(subscription_id, issue_date);

-- outbox
create table outbox
(
    id           uuid                     default gen_random_uuid() not null
        primary key,
    aggregate_id varchar(255)                                       not null,
    event_type   varchar(100)                                       not null,
    routing_key  varchar(255)                                       not null,
    payload      jsonb                                              not null,
    created_at   timestamp with time zone default now()             not null
);