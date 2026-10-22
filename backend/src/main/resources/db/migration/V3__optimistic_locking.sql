-- Columna de versión para bloqueo optimista (@Version) en las entidades existentes.

ALTER TABLE users        ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE products     ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE orders       ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE reservations ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
