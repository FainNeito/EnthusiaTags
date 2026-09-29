package org.enthusia.tags.entitlements;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EntitlementStorage {
    private final File databaseFile;
    private final ExecutorService executor;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private Connection connection;

    public EntitlementStorage(File databaseFile) {
        this.databaseFile = databaseFile;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "enthusia-tags-entitlement-storage");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void init() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
            statement.execute("PRAGMA busy_timeout=5000");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_entitlements (
                    player_uuid TEXT NOT NULL,
                    entitlement_id TEXT NOT NULL,
                    source TEXT NOT NULL,
                    unlocked_at INTEGER NOT NULL,
                    metadata TEXT NOT NULL DEFAULT '',
                    PRIMARY KEY (player_uuid, entitlement_id)
                )
                """);
        }
    }

    public CompletableFuture<Boolean> grantAsync(UUID playerId, String entitlementId, String source, String metadata) {
        return submit(() -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO player_entitlements
                    (player_uuid, entitlement_id, source, unlocked_at, metadata)
                VALUES (?, ?, ?, ?, ?)
                """)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, entitlementId);
                statement.setString(3, source == null ? "" : source);
                statement.setLong(4, System.currentTimeMillis());
                statement.setString(5, metadata == null ? "" : metadata);
                return statement.executeUpdate() == 1;
            }
        });
    }

    public CompletableFuture<Set<String>> loadAsync(UUID playerId) {
        return submit(() -> loadDirect(playerId));
    }

    public Set<String> loadNow(UUID playerId) throws SQLException {
        return executeBlocking(() -> loadDirect(playerId));
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) { }
        }
    }

    private Set<String> loadDirect(UUID playerId) throws SQLException {
        Set<String> ids = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT entitlement_id FROM player_entitlements WHERE player_uuid = ?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) ids.add(rs.getString(1));
            }
        }
        return Set.copyOf(ids);
    }

    private <T> CompletableFuture<T> submit(SqlSupplier<T> supplier) {
        if (closed.get()) return CompletableFuture.failedFuture(new SQLException("Entitlement storage is closed"));
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (SQLException ex) {
                throw new StorageRuntimeException(ex);
            }
        }, executor);
    }

    private <T> T executeBlocking(SqlSupplier<T> supplier) throws SQLException {
        try {
            return submit(supplier).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted waiting for entitlement storage", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof StorageRuntimeException runtime && runtime.getCause() instanceof SQLException sql) throw sql;
            throw new SQLException("Entitlement storage failed", cause);
        }
    }

    @FunctionalInterface
    private interface SqlSupplier<T> { T get() throws SQLException; }

    private static final class StorageRuntimeException extends RuntimeException {
        private StorageRuntimeException(SQLException cause) { super(cause); }
    }
}
