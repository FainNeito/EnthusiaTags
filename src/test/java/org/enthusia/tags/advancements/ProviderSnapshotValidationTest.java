package org.enthusia.tags.advancements;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProviderSnapshotValidationTest {
    private static final String ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    @TempDir Path directory;

    private static String diary(String body) {
        return "players:\n  " + ID + ":\n" + body;
    }

    @Test void diaryRejectsMalformedPresentSections() {
        for (String value : new String[]{"[]", "[item]", "false", "42", "broken"}) {
            assertThrows(IllegalArgumentException.class,
                () -> DiaryStatsReader.parse("players: " + value + "\n"), value);
            assertThrows(IllegalArgumentException.class,
                () -> DiaryStatsReader.parse(diary("    issuedAt: 123\n    advancements: " + value + "\n")), value);
        }
    }

    @Test void diaryRejectsInvalidTimestamps() {
        for (String value : new String[]{"[]", "{}", "false", "broken", "-1", "1.5", "'123'"}) {
            assertThrows(IllegalArgumentException.class,
                () -> DiaryStatsReader.parse(diary("    issuedAt: " + value + "\n")), value);
        }
    }

    @Test void legacyAbsentFieldsAndLongTimestampsRemainValid() throws Exception {
        assertTrue(DiaryStatsReader.parse("players: {}").isEmpty());
        assertFalse(DiaryStatsReader.parse(diary("    id: existing\n"))
            .get(UUID.fromString(ID)).received());
        assertTrue(DiaryStatsReader.parse(diary("    issuedAt: 1750000000000\n    advancements: {}\n"))
            .get(UUID.fromString(ID)).received());
    }

    @Test void diaryRejectsDuplicateNormalizedUuid() {
        String row = "    issuedAt: 123\n";
        assertThrows(IllegalArgumentException.class,
            () -> DiaryStatsReader.parse(diary(row) + "  " + ID.toUpperCase(java.util.Locale.ROOT) + ":\n" + row));
    }

    @Test void commendRejectsDuplicateNormalizedUuid() {
        String row = "    positiveReceived: true\n    maxOverall: 20\n    minOverall: 0\n    recoveredFromSevere: false\n";
        assertThrows(IllegalArgumentException.class, () -> CommendStatsReader.parse(
            "advancementEvidence:\n  " + ID + ":\n" + row + "  " + ID.toUpperCase(java.util.Locale.ROOT) + ":\n" + row));
    }

    @Test void warzoneRejectsDuplicateNormalizedUuid() {
        String row = "    wins: 50\n    best-win-streak: 5\n";
        assertThrows(IllegalArgumentException.class, () -> WarzoneStatsReader.parse(
            "players:\n  " + ID + ":\n" + row + "  " + ID.toUpperCase(java.util.Locale.ROOT) + ":\n" + row));
    }

    @Test void malformedFirstSnapshotCannotTurnRestoredHistoryIntoLiveCompletion() throws Exception {
        Path file = directory.resolve("diaries.yml");
        var id = UUID.fromString(ID);
        var bridge = new DiaryAdvancementBridge(file);
        bridge.beginSession(id);
        Files.writeString(file, "players: []\n");
        assertThrows(IllegalArgumentException.class, bridge::refresh);
        assertTrue(bridge.observe(id).celebrate().isEmpty());
        Files.writeString(file, diary("    issuedAt: 123\n    advancements:\n      edits: 25\n"));
        bridge.refresh();
        var restored = bridge.observe(id);
        assertEquals(1000, restored.progress().get("diary/prolific_writer"));
        assertTrue(restored.celebrate().isEmpty());
        Files.writeString(file, diary("    issuedAt: broken\n"));
        assertThrows(IllegalArgumentException.class, bridge::refresh);
        assertEquals(restored.progress(), bridge.observe(id).progress());
    }
}
