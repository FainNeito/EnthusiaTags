package org.enthusia.tags.advancements;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DiaryStatsReaderTest {
    private static final UUID PLAYER =
        UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test void readsProviderOwnedEvidence() throws Exception {
        String yaml = """
            players:
              00000000-0000-0000-0000-000000000001:
                id: diary-id
                issuedAt: 123
                advancements:
                  received: true
                  edits: 25
                  destructionAttempts: 10
                  voidReturns: 1
                  containerAttempts: 1
                  groundPickups: 2
            """;
        var stats = DiaryStatsReader.parse(yaml).get(PLAYER);
        assertTrue(stats.received());
        assertEquals(25, stats.edits());
        assertEquals(10, stats.destructionAttempts());
        assertEquals(1, stats.voidReturns());
        assertEquals(1, stats.containerAttempts());
        assertEquals(2, stats.groundPickups());
    }

    @Test void legacyIssuedPlayerReceivesOnlyProvableHistoricalCredit() throws Exception {
        String yaml = """
            players:
              00000000-0000-0000-0000-000000000001:
                id: diary-id
                issuedAt: 123
            """;
        var stats = DiaryStatsReader.parse(yaml).get(PLAYER);
        assertTrue(stats.received());
        assertEquals(0, stats.edits());
        assertEquals(0, stats.destructionAttempts());
        assertEquals(0, stats.voidReturns());
        assertEquals(0, stats.containerAttempts());
        assertEquals(0, stats.groundPickups());
    }

    @Test void emptyStoreIsValid() throws Exception {
        assertTrue(DiaryStatsReader.parse("lastWorldUid: abc\n").isEmpty());
    }
    @Test void malformedEvidenceFailsClosed() {
        String negative = """
            players:
              00000000-0000-0000-0000-000000000001:
                id: diary-id
                issuedAt: 123
                advancements:
                  edits: -1
            """;
        assertThrows(Exception.class, () -> DiaryStatsReader.parse(negative));

        String badUuid = """
            players:
              not-a-uuid:
                id: diary-id
                issuedAt: 123
            """;
        assertThrows(Exception.class, () -> DiaryStatsReader.parse(badUuid));
    }
}
