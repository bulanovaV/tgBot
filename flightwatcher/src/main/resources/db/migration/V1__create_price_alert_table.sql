CREATE TABLE price_alert (
    id BIGSERIAL PRIMARY KEY,
    origin VARCHAR(3) NOT NULL,
    destination VARCHAR(3) NOT NULL,
    max_price INTEGER NOT NULL
);