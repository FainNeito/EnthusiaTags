package org.enthusia.tags.advancements;

import io.github.badgersmc.advancements.pilot.ProjectionService;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import org.bukkit.Material;
import org.enthusia.tags.advancements.domain.DuelMilestoneProgress;

final class WarzoneAdvancementBridge {
    private final Path file;
    // Published atomically by the background reader; sessions are main-thread-only.
    private record Snapshot(long readStarted, Map<UUID, DuelMilestoneProgress.Stats> players) {}
    private volatile Snapshot latest;
    private final Map<UUID, DuelMilestoneProgress> sessions = new HashMap<>();
    private final Map<UUID, Long> sessionStarted = new HashMap<>();
    void beginSession(UUID player) {
        sessions.remove(player);
        sessionStarted.put(player, System.nanoTime());
    }
    WarzoneAdvancementBridge(Path file) { this.file = file; }
    void refresh() throws Exception {
        long started = System.nanoTime();
        try { latest = new Snapshot(started, WarzoneStatsReader.parse(Files.readString(file))); }
        catch (Exception failure) {
            latest = null;
            throw failure;
        }
    }
    DuelMilestoneProgress.Update observe(UUID player) {
        var snapshot = latest;
        // A valid complete file can prove absence; a failed file cannot.
        Long joined = sessionStarted.get(player);
        var stats = snapshot == null || (joined != null && snapshot.readStarted() - joined < 0)
            ? null : snapshot.players().getOrDefault(player, new DuelMilestoneProgress.Stats(0, 0));
        return sessions.computeIfAbsent(player, ignored -> new DuelMilestoneProgress()).observe(stats);
    }
    void forget(UUID player) { sessions.remove(player); sessionStarted.remove(player); }
    static List<ProjectionService.Node> nodes(int row) {
        return List.of(
            node("first_blood", null, "Arena Initiate", "Win your first Warzone Duel (1 win).", Material.IRON_SWORD, "TASK", 1, row),
            node("unstoppable", "first_blood", "Arena Win Streak", "Achieve a best Warzone Duel win streak of 5.", Material.DIAMOND_SWORD, "CHALLENGE", 2, row),
            node("gladiator", "unstoppable", "The Gladiator", "Win 50 Warzone Duels in total.", Material.NETHERITE_SWORD, "CHALLENGE", 3, row));
    }
    private static ProjectionService.Node node(String id, String parent, String title, String requirement,
                                                Material icon, String frame, int x, int row) {
        return new ProjectionService.Node("warzone_duels/" + id, parent == null ? null : "warzone_duels/" + parent,
            title, List.of("§7Warzone Duels", "§7Requirements:", "§f" + requirement,
                "§7Includes individual and Duel Party wins.", "§7Rewards: None (advancement only)."), icon, frame, x, row * 2);
    }
}
