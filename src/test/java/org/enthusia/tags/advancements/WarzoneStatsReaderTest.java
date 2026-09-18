package org.enthusia.tags.advancements;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WarzoneStatsReaderTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private String record(String values) { return "players:\n  " + PLAYER + ":\n" + values; }
    @Test void readsVerifiedPersistedFieldsWithoutChangingSource() throws Exception {
        var result = WarzoneStatsReader.parse(record("    wins: 50\n    best-win-streak: 5\n    losses: 12\n"));
        assertEquals(50, result.get(PLAYER).wins());
        assertEquals(5, result.get(PLAYER).bestStreak());
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }
    @Test void missingMalformedAndIncompleteAreNotZero() {
        for (String input : new String[]{"", "players: [", "other: 1", "players: 7",
                record("    wins: 1\n"), record("    wins: -1\n    best-win-streak: 0\n"),
                record("    wins: 1.5\n    best-win-streak: 0\n"),
                record("    wins: 2147483648\n    best-win-streak: 0\n"),
                record("    wins: 2\n    best-win-streak: 3\n"),
                "players:\n  bad-uuid:\n    wins: 1\n    best-win-streak: 1\n"}) {
            assertThrows(Exception.class, () -> WarzoneStatsReader.parse(input), input);
        }
    }
    @Test void absentPlayerIsNotManufactured() throws Exception {
        assertTrue(WarzoneStatsReader.parse("players: {}\n").isEmpty());
    }
}
