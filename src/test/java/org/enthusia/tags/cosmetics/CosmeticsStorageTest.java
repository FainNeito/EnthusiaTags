package org.enthusia.tags.cosmetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.enthusia.tags.PerformanceMonitor;
import org.junit.jupiter.api.Test;

final class CosmeticsStorageTest {
    @Test
    void selectionsPersistPerPlayerAndCategoryAndCanBeReplacedOrCleared() throws Exception {
        var database = Files.createTempFile("enthusia-cosmetics-", ".db").toFile();
        var monitor = new PerformanceMonitor(null);
        var storage = new CosmeticsStorage(database, monitor);
        UUID alice = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID bob = UUID.fromString("00000000-0000-0000-0000-000000000002");

        try {
            storage.init();

            storage.setSelectionAsync(alice, "join", "spark").join();
            storage.setSelectionAsync(alice, "kill", "burst").join();
            storage.setSelectionAsync(bob, "join", "smoke").join();

            assertEquals(Map.of("join", "spark", "kill", "burst"), storage.loadSelectionsNow(alice));
            assertEquals(Map.of("join", "smoke"), storage.loadSelectionsNow(bob));

            storage.setSelectionAsync(alice, "join", "heart").join();
            assertEquals("heart", storage.loadSelectionsNow(alice).get("join"));
            assertEquals(2, storage.loadSelectionsNow(alice).size());

            storage.setSelectionAsync(alice, "kill", null).join();
            assertEquals(Map.of("join", "heart"), storage.loadSelectionsNow(alice));
            assertEquals(Map.of("join", "smoke"), storage.loadSelectionsNow(bob));

            assertTrue(monitor.snapshot().getOrDefault("storage.cosmetics.save.success", 0L) >= 5L);
            assertTrue(monitor.snapshot().getOrDefault("storage.cosmetics.load.success", 0L) >= 1L);
        } finally {
            storage.close();
            Files.deleteIfExists(database.toPath());
        }
    }

    @Test
    void asyncOperationsFailAfterCloseInsteadOfUsingADeadExecutorOrConnection() throws Exception {
        var database = Files.createTempFile("enthusia-cosmetics-close-", ".db").toFile();
        var storage = new CosmeticsStorage(database, new PerformanceMonitor(null));
        UUID player = UUID.randomUUID();
        storage.init();
        storage.close();

        try {
            CompletionException failure = org.junit.jupiter.api.Assertions.assertThrows(
                    CompletionException.class,
                    () -> storage.setSelectionAsync(player, "join", "spark").join()
            );
            assertTrue(failure.getCause() instanceof java.sql.SQLException);
            assertTrue(failure.getCause().getMessage().contains("closed"));
        } finally {
            Files.deleteIfExists(database.toPath());
        }
    }
}
