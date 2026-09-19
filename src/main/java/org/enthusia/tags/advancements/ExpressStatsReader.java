package org.enthusia.tags.advancements;

import org.enthusia.tags.advancements.domain.ExpressMilestoneProgress;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Read-only aggregate adapter for EnthusiaExpress mail.db. */
final class ExpressStatsReader {
    private ExpressStatsReader() {
    }

    static Map<UUID, ExpressMilestoneProgress.Stats> read(Path database) throws Exception {
        if (database == null || !Files.isRegularFile(database)) {
            throw new IllegalArgumentException("EnthusiaExpress mail.db is unavailable");
        }
        Class.forName(org.sqlite.JDBC.class.getName());
        String url = "jdbc:sqlite:" + database.toUri() + "?mode=ro";
        try (Connection connection = DriverManager.getConnection(url)) {
            Set<String> columns = columns(connection);
            requireColumns(columns);
            Map<UUID, MutableStats> players = new HashMap<>();
            readSenderStats(connection, players);
            readRecipientStats(connection, players, columns.contains("delivery_pending"));
            Map<UUID, ExpressMilestoneProgress.Stats> result = new HashMap<>();
            players.forEach((id, stats) -> result.put(id, stats.freeze()));
            return Map.copyOf(result);
        }
    }

    private static Set<String> columns(Connection connection) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (var statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(mail)")) {
            while (result.next()) {
                columns.add(result.getString("name"));
            }
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Missing EnthusiaExpress mail table");
        }
        return Set.copyOf(columns);
    }

    private static void requireColumns(Set<String> columns) {
        for (String required : Set.of(
            "sender_uuid", "recipient_uuid", "type", "status",
            "packed_item_count", "unread"
        )) {
            if (!columns.contains(required)) {
                throw new IllegalArgumentException("Missing EnthusiaExpress column: " + required);
            }
        }
    }
    private static void readSenderStats(
        Connection connection,
        Map<UUID, MutableStats> players
    ) throws SQLException {
        String sql = """
            SELECT sender_uuid,
                   SUM(CASE WHEN type='PACKAGE' THEN 1 ELSE 0 END) AS sent_packages,
                   SUM(CASE WHEN type='LETTER' THEN 1 ELSE 0 END) AS sent_letters,
                   MAX(CASE WHEN type='PACKAGE' THEN packed_item_count ELSE 0 END) AS max_packed
              FROM mail
             WHERE sender_uuid IS NOT NULL
             GROUP BY sender_uuid
            """;
        try (var statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                UUID id = uuid(result.getString("sender_uuid"));
                MutableStats stats = players.computeIfAbsent(id, ignored -> new MutableStats());
                stats.sentPackages = safeInt(result.getLong("sent_packages"));
                stats.sentLetters = safeInt(result.getLong("sent_letters"));
                stats.maxPackedItems = safeInt(result.getLong("max_packed"));
            }
        }
    }
    private static void readRecipientStats(
        Connection connection,
        Map<UUID, MutableStats> players,
        boolean hasDeliveryPending
    ) throws SQLException {
        String delivered = hasDeliveryPending ? " AND delivery_pending=0" : "";
        String sql = """
            SELECT recipient_uuid,
                   SUM(CASE WHEN type='PACKAGE' AND status='CLAIMED'%s THEN 1 ELSE 0 END) AS claimed_packages,
                   SUM(CASE WHEN type='LETTER' AND unread=0 THEN 1 ELSE 0 END) AS read_letters,
                   SUM(CASE WHEN type='PACKAGE' AND status='RETURN_CLAIMED'%s THEN 1 ELSE 0 END) AS returned_claims
              FROM mail
             GROUP BY recipient_uuid
            """.formatted(delivered, delivered);
        try (var statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                UUID id = uuid(result.getString("recipient_uuid"));
                MutableStats stats = players.computeIfAbsent(id, ignored -> new MutableStats());
                stats.claimedPackages = safeInt(result.getLong("claimed_packages"));
                stats.readLetters = safeInt(result.getLong("read_letters"));
                stats.returnedClaims = safeInt(result.getLong("returned_claims"));
            }
        }
    }
    private static UUID uuid(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing player UUID in EnthusiaExpress history");
        }
        UUID id = UUID.fromString(value);
        if (!id.toString().equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("Invalid player UUID in EnthusiaExpress history");
        }
        return id;
    }

    private static int safeInt(long value) {
        if (value < 0 || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Express counter outside supported range");
        }
        return (int) value;
    }

    private static final class MutableStats {
        int sentPackages;
        int sentLetters;
        int claimedPackages;
        int readLetters;
        int maxPackedItems;
        int returnedClaims;

        ExpressMilestoneProgress.Stats freeze() {
            return new ExpressMilestoneProgress.Stats(
                sentPackages, sentLetters, claimedPackages,
                readLetters, maxPackedItems, returnedClaims);
        }
    }
}
