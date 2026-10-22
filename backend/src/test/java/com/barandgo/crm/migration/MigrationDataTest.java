package com.barandgo.crm.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Migraciones sobre datos existentes: se lleva la BD a una versión anterior, se cargan datos
 * como los que había antes del cambio y se aplica el resto. Sin contexto de Spring.
 */
@Testcontainers(disabledWithoutDocker = true)
class MigrationDataTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @BeforeEach
    void resetSchema() throws SQLException {
        execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
    }

    @Test
    void productCategoriesAreCreatedFromFreeText() throws SQLException {
        flyway().target(MigrationVersion.fromVersion("3")).load().migrate();
        execute("""
                INSERT INTO products (name, price, category, available) VALUES
                    ('Croquetas', 8.50, 'ENTRANTES', true),
                    ('Pincho moruno', 4.00, '  TAPAS ', true),
                    ('Sin familia', 1.00, NULL, true);
                """);

        flyway().load().migrate();

        Map<String, String> categoryByProduct = queryMap("""
                SELECT p.name, c.name FROM products p JOIN product_category c ON c.id = p.category_id
                """);
        assertThat(categoryByProduct)
                .containsEntry("Croquetas", "ENTRANTES")
                .containsEntry("Pincho moruno", "TAPAS")
                .containsEntry("Sin familia", "SIN CATEGORIA");
    }

    @Test
    void orderItemsAreLinkedByExactUniqueNameAndTheRestRecorded() throws SQLException {
        flyway().target(MigrationVersion.fromVersion("7")).load().migrate();
        execute("""
                INSERT INTO users (name, email, password, phone, role, date_of_creation)
                    VALUES ('Ana', 'ana@example.com', 'x', '+34600000001', 'CUSTOMER', now());
                INSERT INTO products (name, price, category_id, available)
                    SELECT v.name, 5.00, (SELECT MIN(id) FROM product_category), true
                    FROM (VALUES ('Croquetas'), ('Tarta'), ('Tarta')) AS v (name);
                INSERT INTO orders (user_id, date_order, time_take_away, total, status, payment_method)
                    VALUES ((SELECT id FROM users), now(), now(), 15.00, 'DELIVERED', 'CASH');
                INSERT INTO order_items (order_id, product_name, quantity, unit_price)
                    SELECT (SELECT id FROM orders), v.name, 1, 5.00
                    FROM (VALUES ('Croquetas'), ('Tarta'), ('Plato retirado')) AS v (name);
                """);

        flyway().load().migrate();

        Map<String, String> linked = queryMap("""
                SELECT oi.product_name, p.name FROM order_items oi JOIN products p ON p.id = oi.product_id
                """);
        assertThat(linked).containsOnly(Map.entry("Croquetas", "Croquetas"));

        Map<String, String> issues = queryMap("SELECT product_name, reason FROM order_item_link_issue");
        assertThat(issues).containsOnly(
                Map.entry("Tarta", "AMBIGUOUS"),
                Map.entry("Plato retirado", "NOT_FOUND"));
    }

    @Test
    void schemaCreatedBeforeFlywayIsAdoptedAsBaseline() throws Exception {
        // Esquema existente sin historial de Flyway, como el que dejaba ddl-auto=update
        String legacySchema = new ClassPathResource("db/migration/V1__baseline.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        execute(legacySchema);
        execute("""
                INSERT INTO users (name, email, password, phone, role, date_of_creation)
                    VALUES ('Admin', 'admin@bar.com', 'x', '+34600000001', 'ADMIN', now());
                """);

        flyway().load().migrate();

        Map<String, String> history = queryMap("SELECT version, type FROM flyway_schema_history WHERE version IS NOT NULL");
        assertThat(history).containsEntry("1", "BASELINE").containsKey("8");
        // V2 ha sustituido el CHECK de rol: el rol nuevo se admite
        execute("UPDATE users SET role = 'MANAGER' WHERE email = 'admin@bar.com'");
    }

    private static org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1");
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static Map<String, String> queryMap(String sql) throws SQLException {
        Map<String, String> result = new HashMap<>();
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                result.put(rows.getString(1), rows.getString(2));
            }
        }
        return result;
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
