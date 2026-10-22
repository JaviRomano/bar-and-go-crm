-- Registro de publicaciones de eventos de Spring Modulith (patrón outbox): cada evento entre módulos se
-- guarda en la misma transacción que lo publica y se marca al procesarse. Lo pendiente se reintenta al
-- arrancar. DDL tomado de spring-modulith-events-jdbc 2.1 (schemas/v2/schema-postgresql.sql).

CREATE TABLE IF NOT EXISTS event_publication
(
    id                     UUID NOT NULL,
    listener_id            TEXT NOT NULL,
    event_type             TEXT NOT NULL,
    serialized_event       TEXT NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 TEXT,
    completion_attempts    INT,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);
