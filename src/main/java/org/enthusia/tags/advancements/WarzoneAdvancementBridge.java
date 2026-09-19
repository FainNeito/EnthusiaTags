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
            node("welcome_to_thunderdome", null, "Welcome to the Thunderdome", "Send a valid Warzone Duel challenge.", Material.COMPASS, "TASK", 1, row),
            node("first_blood", "welcome_to_thunderdome", "Arena Initiate", "Win your first Warzone Duel (1 win).", Material.IRON_SWORD, "TASK", 2, row),
            node("victor_spoils", "first_blood", "To the Victor Go the Spoils", "Withdraw at least one captured item from your duel vault.", Material.CHEST, "TASK", 3, row),
            node("price_for_peace", "victor_spoils", "A Price for Peace", "Complete a duel by mutual draw agreement.", Material.PAPER, "TASK", 4, row),
            node("my_house_my_rules", "price_for_peace", "My House, My Rules", "As the 1v1 challenger, win a kill-result duel using non-default rules.", Material.REDSTONE_TORCH, "CHALLENGE", 5, row),
            node("adapt_and_overcome", "my_house_my_rules", "Adapt and Overcome", "Win a kill-result duel with Ender Pearls and Wind Charges disabled.", Material.SHIELD, "CHALLENGE", 6, row),
            node("not_even_close", "adapt_and_overcome", "Not Even Close", "Win a 1v1 kill-result duel with less than two hearts remaining.", Material.GOLDEN_APPLE, "CHALLENGE", 7, row),
            node("unstoppable", "not_even_close", "Arena Win Streak", "Achieve a best Warzone Duel win streak of 5.", Material.DIAMOND_SWORD, "CHALLENGE", 8, row),
            node("gladiator", "unstoppable", "The Gladiator", "Win 50 Warzone Duels in total.", Material.NETHERITE_SWORD, "CHALLENGE", 9, row));
    }
    private static ProjectionService.Node node(String id, String parent, String title, String requirement,
                                                Material icon, String frame, int x, int row) {
        return new ProjectionService.Node("warzone_duels/" + id, parent == null ? null : "warzone_duels/" + parent,
            title, List.of("§7Warzone Duels", "§7Requirements:", "§f" + requirement,
                "§7Progress is read from WarzoneDuels.", "§7Rewards: None (advancement only)."), icon, frame, x, row * 2);
    }
}
