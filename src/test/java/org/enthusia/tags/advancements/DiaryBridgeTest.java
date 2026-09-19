package org.enthusia.tags.advancements;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DiaryBridgeTest {
    @TempDir Path directory;
    private final UUID player = UUID.randomUUID();

    private void save(Path file, int edits, int destruction, boolean signed) throws Exception {
        Files.writeString(file,
            "players:\n  " + player + ":\n" +
            "    id: diary-id\n" +
            "    issuedAt: 123\n" +
            "    advancements:\n" +
            "      received: true\n" +
            "      edits: " + edits + "\n" +
            "      signed: " + signed + "\n" +
            "      destructionAttempts: " + destruction + "\n" +
            "      voidReturns: 0\n" +
            "      containerAttempts: 0\n" +
            "      groundPickups: 0\n");
    }
    @Test void historicalProgressIsSilentAndLiveCrossingCelebrates() throws Exception {
        Path file = directory.resolve("diaries.yml");
        save(file, 24, 9, false);

        var bridge = new DiaryAdvancementBridge(file);
        bridge.beginSession(player);
        bridge.refresh();
        var historical = bridge.observe(player);
        assertEquals(1000, historical.progress().get("diary/dear_diary"));
        assertTrue(historical.celebrate().isEmpty());

        save(file, 25, 10, true);
        bridge.refresh();
        var live = bridge.observe(player);
        assertTrue(live.celebrate().contains("diary/prolific_writer"));
        assertTrue(live.celebrate().contains("diary/stubborn"));
        assertTrue(live.celebrate().contains("diary/signed_sealed_delivered"));

        Files.writeString(file, "players: [");
        assertThrows(Exception.class, bridge::refresh);
        assertEquals(live.progress(), bridge.observe(player).progress());
    }
    @Test void nodesUseTheDiaryBranchesAndHaveNoRewards() {
        var nodes = DiaryAdvancementBridge.nodes(49);
        assertEquals(9, nodes.size());
        var byKey = nodes.stream().collect(
            java.util.stream.Collectors.toMap(node -> node.key(), node -> node));

        assertEquals("Dear Diary...", byKey.get("diary/dear_diary").title());
        assertEquals("diary/dear_diary", byKey.get("diary/first_entry").parentKey());
        assertEquals("diary/first_entry", byKey.get("diary/prolific_writer").parentKey());
        assertEquals("diary/first_entry", byKey.get("diary/signed_sealed_delivered").parentKey());
        assertEquals("diary/indestructible", byKey.get("diary/stubborn").parentKey());
        assertEquals("diary/dear_diary", byKey.get("diary/void_walker").parentKey());
        assertEquals("diary/dear_diary", byKey.get("diary/nice_try").parentKey());
        assertEquals("diary/dear_diary", byKey.get("diary/finders_keepers").parentKey());

        for (var node : nodes) {
            assertTrue(node.description().stream().anyMatch(s -> s.contains("Requirements:")));
            assertTrue(node.description().stream().anyMatch(s -> s.contains("Rewards: None")));
        }
    }
    @Test void nativeWiringIsOptionalAsyncAndSoftDependent() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/config.yml"));
        String plugin = Files.readString(Path.of("src/main/resources/plugin.yml"));
        String source = Files.readString(Path.of(
            "src/main/java/org/enthusia/tags/advancements/NativeAdvancementController.java"));

        assertTrue(config.contains("diary-enabled: true"));
        assertTrue(plugin.contains("- DiaryKeeper"));
        assertTrue(source.contains("getPlugin(\"DiaryKeeper\")"));
        assertTrue(source.contains("DiaryAdvancementBridge"));
        assertTrue(source.contains("diary.refresh()"));
        assertTrue(source.contains("diary.observe(player.getUniqueId())"));
        assertTrue(source.contains("diary.forget(id)"));
        assertTrue(source.contains("diaryTask.cancel()"));
    }
}
