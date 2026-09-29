package org.enthusia.tags.entitlements;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EntitlementStorageTest {
    @Test
    void grantsAreIdempotentAndPersistAcrossRestart(@TempDir Path directory) throws Exception {
        UUID player = UUID.randomUUID();
        var file = directory.resolve("entitlements.db").toFile();

        var first = new EntitlementStorage(file);
        first.init();
        assertTrue(first.grantAsync(player, "beta_tester", "Legacy: Beta Tester", "source=test").get());
        assertFalse(first.grantAsync(player, "beta_tester", "Legacy: Beta Tester", "source=test").get());
        assertEquals(java.util.Set.of("beta_tester"), first.loadAsync(player).get());
        first.close();

        var reopened = new EntitlementStorage(file);
        reopened.init();
        try {
            assertEquals(java.util.Set.of("beta_tester"), reopened.loadNow(player));
        } finally {
            reopened.close();
        }
    }
}
