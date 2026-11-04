CREATE TABLE IF NOT EXISTS actions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating FLOAT NOT NULL,
    ts TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (event_id, user_id)
);

CREATE TABLE IF NOT EXISTS similarities (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    similarity FLOAT NOT NULL,
    ts TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (event_a, event_b)
);

CREATE INDEX IF NOT EXISTS idx_actions_user_ts ON actions (user_id, ts DESC);