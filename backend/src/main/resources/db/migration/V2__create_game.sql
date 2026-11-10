CREATE TABLE upgrade (
    id          TEXT PRIMARY KEY,
    name        TEXT    NOT NULL,
    description TEXT    NOT NULL,
    base_cost   BIGINT  NOT NULL CHECK (base_cost > 0),
    per_click   INTEGER NOT NULL DEFAULT 0 CHECK (per_click >= 0),
    per_second  INTEGER NOT NULL DEFAULT 0 CHECK (per_second >= 0),
    sort_order  INTEGER NOT NULL
);

CREATE TABLE game_state (
    id           BIGINT PRIMARY KEY CHECK (id = 1),
    cookies      DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (cookies >= 0),
    total_clicks BIGINT           NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE TABLE owned_upgrade (
    upgrade_id TEXT PRIMARY KEY REFERENCES upgrade (id),
    quantity   INTEGER NOT NULL CHECK (quantity > 0)
);

INSERT INTO upgrade (id, name, description, base_cost, per_click, per_second, sort_order)
VALUES ('cursor', 'Cursor', '+1 cookie per click', 15, 1, 0, 1),
       ('grandma', 'Grandma', 'Bakes 1 cookie per second', 100, 0, 1, 2),
       ('rolling-pin', 'Rolling pin', '+5 cookies per click', 500, 5, 0, 3),
       ('farm', 'Farm', 'Grows 8 cookies per second', 1100, 0, 8, 4),
       ('factory', 'Factory', 'Produces 47 cookies per second', 12000, 0, 47, 5);

INSERT INTO game_state (id) VALUES (1);
