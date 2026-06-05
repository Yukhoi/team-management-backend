create table statistics.match_player_appearance_projection
(
    id bigserial primary key,
    match_id bigint not null,
    player_id bigint not null,
    season varchar(30) not null,
    tournament_id bigint not null,
    appearance_count integer default 0 not null,
    starter_count integer default 0 not null,
    updated_at timestamptz not null default now(),

    constraint uk_match_player_appearance
        unique(match_id, player_id)
);

create index idx_match_player_appearance_match
on statistics.match_player_appearance_projection(match_id);

create index idx_match_player_appearance_player
on statistics.match_player_appearance_projection(player_id);
