package org.enthusia.tags.advancements;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WarzoneBridgeTest {
    @TempDir Path directory;
    private final UUID player = UUID.randomUUID();
    private void save(Path file, int wins, int streak) throws Exception {
        Files.writeString(file, "players:\n  " + player + ":\n    wins: " + wins + "\n    best-win-streak: " + streak + "\n");
    }
    @Test void historicalLiveFailureAndReconnectLifecycle() throws Exception {
        Path file = directory.resolve("stats.yml");
        save(file, 49, 4);
        String original = Files.readString(file);
        var bridge = new WarzoneAdvancementBridge(file);
        bridge.refresh();
        assertEquals(980, bridge.observe(player).progress().get("warzone_duels/gladiator"));
        assertTrue(bridge.observe(player).celebrate().isEmpty());
        assertEquals(original, Files.readString(file));
        Files.writeString(file, "broken: [");
        assertThrows(Exception.class, bridge::refresh);
        assertEquals(980, bridge.observe(player).progress().get("warzone_duels/gladiator"));
        save(file, 50, 5);
        bridge.refresh();
        assertEquals(2, bridge.observe(player).celebrate().size());
        assertTrue(bridge.observe(player).celebrate().isEmpty());
        bridge.forget(player);
        assertTrue(bridge.observe(player).celebrate().isEmpty());
    }
    @Test void confirmedAbsentPlayerCanEarnFirstWinButMissingFileIsUnknown() throws Exception {
        Path file = directory.resolve("stats.yml");
        var bridge = new WarzoneAdvancementBridge(file);
        assertThrows(Exception.class, bridge::refresh);
        assertTrue(bridge.observe(player).progress().isEmpty());
        Files.writeString(file, "players: {}\n");
        bridge.refresh();
        assertEquals(0, bridge.observe(player).progress().get("warzone_duels/first_blood"));
        save(file, 1, 1);
        bridge.refresh();
        assertTrue(bridge.observe(player).celebrate().contains("warzone_duels/first_blood"));
    }
    @Test void overlappingRefreshAttemptCannotReplaceCurrentSnapshot() throws Exception {
        Path file = directory.resolve("stats.yml");
        save(file, 1, 1);
        var bridge = new WarzoneAdvancementBridge(file);
        bridge.refresh();
        assertEquals(20, bridge.observe(player).progress().get("warzone_duels/gladiator"));

        var field = WarzoneAdvancementBridge.class.getDeclaredField("refreshing");
        field.setAccessible(true);
        var refreshing = (AtomicBoolean) field.get(bridge);
        refreshing.set(true);
        save(file, 50, 5);
        bridge.refresh();
        assertEquals(20, bridge.observe(player).progress().get("warzone_duels/gladiator"));

        refreshing.set(false);
        bridge.refresh();
        assertEquals(1000, bridge.observe(player).progress().get("warzone_duels/gladiator"));
    }

    @Test void nodesAreDistinctFromTagsAndHaveRequirementsAndNoInventedRewards() {
        var nodes = WarzoneAdvancementBridge.nodes(8);
        assertEquals(9, nodes.size());
        assertEquals("Welcome to the Thunderdome", nodes.get(0).title());
        assertEquals("warzone_duels/welcome_to_thunderdome", nodes.get(0).key());
        assertEquals("Arena Initiate", nodes.get(1).title());
        assertEquals("warzone_duels/first_blood", nodes.get(1).key());
        assertEquals("To the Victor Go the Spoils", nodes.get(2).title());
        assertEquals("A Price for Peace", nodes.get(3).title());
        assertEquals("My House, My Rules", nodes.get(4).title());
        assertEquals("Adapt and Overcome", nodes.get(5).title());
        assertEquals("Not Even Close", nodes.get(6).title());
        assertEquals("Arena Win Streak", nodes.get(7).title());
        assertEquals("The Gladiator", nodes.get(8).title());
        assertEquals(9, nodes.stream().map(n -> n.key()).distinct().count());
        for (var node : nodes) {
            assertTrue(node.key().startsWith("warzone_duels/"));
            assertEquals(16, node.y());
            assertTrue(node.description().stream().anyMatch(s -> s.contains("Requirements:")));
            assertTrue(node.description().stream().anyMatch(s -> s.contains("Rewards: None")));
        }
    }
    @Test void joiningMustNotTreatAnOldCachedSnapshotAsLiveBaseline() throws Exception {
        Path file = directory.resolve("stats.yml");
        save(file, 49, 4);
        var bridge = new WarzoneAdvancementBridge(file);
        bridge.refresh();
        bridge.beginSession(player);
        assertTrue(bridge.observe(player).progress().isEmpty(), "Wait for a post-join read");
        save(file, 50, 5);
        bridge.refresh();
        var result = bridge.observe(player);
        assertEquals(1000, result.progress().get("warzone_duels/gladiator"));
        assertTrue(result.celebrate().isEmpty(), "First fresh read is historical, never a replayed toast");
    }
    @Test void nativeWiringIsOptInAsyncAndClosesWithController() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/config.yml"));
        String source = Files.readString(Path.of("src/main/java/org/enthusia/tags/advancements/NativeAdvancementController.java"));
        assertTrue(config.contains("warzone-duels-enabled: false"));
        assertTrue(source.contains("runTaskTimerAsynchronously"));
        assertTrue(source.contains("duels.observe(player.getUniqueId())"));
        assertTrue(source.contains("duels.forget(id)"));
        assertTrue(source.contains("duelTask.cancel()"));
    }
}
