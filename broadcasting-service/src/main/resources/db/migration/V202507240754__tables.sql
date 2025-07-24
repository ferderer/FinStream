create table settings (
  user_id       bigint        primary key,
  timezone      varchar(50)   default 'UTC',
  currency      varchar(3)    default 'USD',
  notifications boolean       default true,
  created       timestamp     default NOW(),
  modified      timestamp     default NOW()
);

create table watchlist (
    id          bigint        primary key,
    user_id     bigint        not null,
    symbol      varchar(15)   not null,
    added       timestamp     default NOW(),
    notes       text,
    unique(user_id, symbol)
);

create table stocks (
    symbol      varchar(15)   primary key,
    company     varchar(100)  not null,
    sector      varchar(100),
    market_cap  bigint,
    updated     timestamp     default NOW()
);

create table alerts (
    id          bigint        primary key,
    user_id     bigint        not null,
    symbol      varchar(15)   not null,
    alert_type  varchar(8)   not null,
    target_price decimal(10,2),
    target_percent decimal(5,2),
    active      boolean       default true,
    created     timestamp     default now(),
    triggered   timestamp
);

create index idx_watchlist_user_id on watchlist(user_id);
create index idx_price_alerts_user_symbol on alerts(user_id, symbol);
create index idx_price_alerts_active on alerts(active) where active = true;
