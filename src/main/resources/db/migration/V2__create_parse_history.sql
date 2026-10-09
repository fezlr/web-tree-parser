CREATE TABLE public.parse_history (
    id BIGSERIAL primary key,
    user_id BIGINT not null references public.users (id),
    source TEXT not null,
    total_elements INTEGER not null,
    set_count INTEGER not null,
    subsets INTEGER not null,
    max_depth INTEGER not null,
    number_count INTEGER not null,
    identifier_count INTEGER not null,
    string_count INTEGER not null,
    created_at timestamptz not null default now()
);

CREATE INDEX idx_parse_history_user_created ON public.parse_history (user_id, created_at DESC);

CREATE INDEX idx_parse_history_created ON public.parse_history (created_at DESC);