-- Roles de back-office (encargado y personal) y etiquetas informativas de puesto.

-- Nombre que PostgreSQL asigna al CHECK en línea, tanto en V1 como en esquemas generados por Hibernate
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('CUSTOMER', 'STAFF', 'MANAGER', 'ADMIN'));

CREATE TABLE user_tag (
    user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    tag     VARCHAR(20) NOT NULL CHECK (tag IN ('KITCHEN', 'FLOOR', 'BAR', 'DISHWASHING')),
    CONSTRAINT pk_user_tag PRIMARY KEY (user_id, tag)
);
