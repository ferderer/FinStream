create table login (
    id              bigint          primary key,
    created         timestamp       not null default CURRENT_TIMESTAMP,
    modified        timestamp       not null default CURRENT_TIMESTAMP,
    enabled         boolean         not null default true,
    failures        smallint        not null default 0,
    roles           bigint          not null default 8,
    username        varchar(50)     not null unique,
    email           varchar(320)        null unique,
    password        varchar(255)        null
);

create table persistent_logins (
    series          varchar(64)     primary key,
    username        varchar(64)     not null,
    token           varchar(64)     not null,
    last_used       timestamp       not null
);
