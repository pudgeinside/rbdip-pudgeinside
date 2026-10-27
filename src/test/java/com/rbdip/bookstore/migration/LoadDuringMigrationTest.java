package com.rbdip.bookstore.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class LoadDuringMigrationTest {

    private static final int THREADS_PER_ROLE = 4;
    private static final int MIN_LEGACY_WRITES = 5;
    private static final int TIMEOUT_SECONDS = 10;
    private static final long WRITES_DEADLINE_SECONDS = 5;

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookstore_load")
            .withUsername("bookstore")
            .withPassword("bookstore");

    static {
        POSTGRES.start();
    }

    @Test
    void expandStepsKeepLegacyReadersAndWritersWorking() throws Exception {
        PGSimpleDataSource dataSource = dataSource();

        MigrationInfo[] pending = flyway(dataSource).info().pending();
        assertThat(pending.length)
                .as("в цепочке должны быть expand-шаги и финальный contract-шаг")
                .isGreaterThan(1);
        String lastBeforeContract = pending[pending.length - 2].getVersion().getVersion();

        Flyway.configure().dataSource(dataSource).target("1").load().migrate();
        seedLegacyData(dataSource);

        List<Exception> errors = new CopyOnWriteArrayList<>();
        AtomicBoolean keepRunning = new AtomicBoolean(true);
        AtomicInteger legacyWrites = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(THREADS_PER_ROLE * 2);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS_PER_ROLE * 2);

        for (int i = 0; i < THREADS_PER_ROLE; i++) {
            pool.submit(legacyReader(dataSource, errors, keepRunning, started));
            pool.submit(legacyWriter(dataSource, errors, legacyWrites, keepRunning, started));
        }

        assertThat(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        waitForLegacyWrites(legacyWrites);

        Flyway.configure().dataSource(dataSource).target(lastBeforeContract).load().migrate();

        keepRunning.set(false);
        pool.shutdown();
        assertThat(pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        assertThat(errors)
                .as("expand/backfill-миграции не должны ломать старых читателей и писателей")
                .isEmpty();
        assertLegacyReadsStillWork(dataSource);
        assertLegacyWritesStillWork(dataSource);
        assertDualWriteAndBackfill(dataSource);
    }

    private PGSimpleDataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }

    private Flyway flyway(PGSimpleDataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    private void seedLegacyData(PGSimpleDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO products (name, price) VALUES ('Dune', 10.00)");
            statement.executeUpdate(
                    "INSERT INTO orders (customer_full_name, customer_address) "
                            + "VALUES ('Ivan Petrov', 'Moscow')");
            statement.executeUpdate(
                    "INSERT INTO order_items (order_id, product_name, product_price, quantity) "
                            + "VALUES (1, 'Dune', 10.00, 1)");
        }
    }

    private Runnable legacyReader(
            PGSimpleDataSource dataSource,
            List<Exception> errors,
            AtomicBoolean keepRunning,
            CountDownLatch started) {
        return () -> {
            started.countDown();
            while (keepRunning.get()) {
                try (Connection connection = dataSource.getConnection();
                        Statement statement = connection.createStatement()) {
                    readFirstRow(statement, "SELECT customer_full_name FROM orders LIMIT 1");
                    readFirstRow(statement, "SELECT product_name, product_price FROM order_items LIMIT 1");
                } catch (SQLException e) {
                    errors.add(e);
                }
            }
        };
    }

    private void readFirstRow(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
        }
    }

    private Runnable legacyWriter(
            PGSimpleDataSource dataSource,
            List<Exception> errors,
            AtomicInteger legacyWrites,
            AtomicBoolean keepRunning,
            CountDownLatch started) {
        return () -> {
            started.countDown();
            while (keepRunning.get()) {
                try (Connection connection = dataSource.getConnection();
                        Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                            "INSERT INTO orders (customer_full_name, customer_address) "
                                    + "VALUES ('Legacy Writer', 'Moscow')");
                    legacyWrites.incrementAndGet();
                } catch (SQLException e) {
                    errors.add(e);
                }
            }
        };
    }

    private void waitForLegacyWrites(AtomicInteger legacyWrites) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WRITES_DEADLINE_SECONDS);
        while (legacyWrites.get() < MIN_LEGACY_WRITES && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertThat(legacyWrites.get())
                .as("фоновые писатели должны успеть поработать до начала миграции")
                .isGreaterThanOrEqualTo(MIN_LEGACY_WRITES);
    }

    private void assertLegacyReadsStillWork(PGSimpleDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT count(*) FROM orders WHERE customer_full_name = 'Ivan Petrov'")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1))
                    .as("старая колонка customer_full_name должна оставаться читаемой после expand-шагов")
                    .isPositive();
        }
    }

    private void assertLegacyWritesStillWork(PGSimpleDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO orders (customer_full_name, customer_address) "
                            + "VALUES ('Legacy Writer After Expand', 'Moscow')");
            statement.executeUpdate(
                    "INSERT INTO order_items (order_id, product_name, product_price, quantity) "
                            + "VALUES (1, 'Dune', 10.00, 1)");
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT count(*) FROM orders "
                            + "WHERE customer_full_name = 'Legacy Writer After Expand'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1))
                        .as("старые колонки должны оставаться записываемыми после expand-шагов")
                        .isPositive();
            }
        }
    }

    private void assertDualWriteAndBackfill(PGSimpleDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT first_name, last_name FROM customers WHERE full_name = 'Ivan Petrov'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("first_name")).isEqualTo("Ivan");
                assertThat(resultSet.getString("last_name")).isEqualTo("Petrov");
            }

            statement.executeUpdate(
                    "INSERT INTO customers (full_name, address) VALUES ('Petr Smirnov', 'Kazan')");
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT first_name, last_name FROM customers WHERE full_name = 'Petr Smirnov'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("first_name")).isEqualTo("Petr");
                assertThat(resultSet.getString("last_name")).isEqualTo("Smirnov");
            }

            statement.executeUpdate(
                    "INSERT INTO customers (first_name, last_name, address) "
                            + "VALUES ('Anna', 'Sidorova', 'SPb')");
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT full_name FROM customers WHERE first_name = 'Anna'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("full_name"))
                        .as("триггер dual-write должен поддержать legacy-колонку для новых записей")
                        .isEqualTo("Anna Sidorova");
            }

            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT count(*) FROM customers WHERE first_name IS NULL OR last_name IS NULL")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1))
                        .as("backfill/триггер должны заполнить новые колонки для всех клиентов")
                        .isZero();
            }
        }
    }
}
