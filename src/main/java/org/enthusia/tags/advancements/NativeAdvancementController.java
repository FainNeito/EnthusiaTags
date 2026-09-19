package org.enthusia.tags.advancements;

import io.github.badgersmc.advancements.pilot.ProjectionService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.enthusia.tags.rewards.RewardAction;
import org.enthusia.tags.rewards.RewardDefinition;
import org.enthusia.tags.rewards.RewardService;

/** Main-thread, bounded display projection. No reward delivery or SQL in this adapter. */
public final class NativeAdvancementController implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final RewardService rewards;
    private final ProjectionService projection;
    private final Map<UUID, Set<String>> pendingCelebrations = new HashMap<>();
    private final ArrayDeque<UUID> queue = new ArrayDeque<>();
    private Map<String, RewardDefinition> definitions;
    private BukkitTask task;
    private BukkitTask duelTask;
    private BukkitTask commendTask;
    private BukkitTask expressTask;
    private BukkitTask diaryTask;
    private WarzoneAdvancementBridge duels;
    private CommendAdvancementBridge commend;
    private ExpressAdvancementBridge express;
    private DiaryAdvancementBridge diary;
    private long nextWarning;
    private boolean registered;

    public NativeAdvancementController(JavaPlugin plugin, RewardService rewards) {
        this.plugin = plugin;
        this.rewards = rewards;
        projection = Bukkit.getServicesManager().load(ProjectionService.class);
        if (projection == null) throw new IllegalStateException("EnthusiaAdvancements projection service unavailable; install the pilot companion build");
        if (plugin.getConfig().getBoolean("advancements.warzone-duels-enabled", false)) {
            var provider = Bukkit.getPluginManager().getPlugin("WarzoneDuels");
            if (provider != null && provider.isEnabled()) {
                duels = new WarzoneAdvancementBridge(provider.getDataFolder().toPath().resolve("stats.yml"));
                Bukkit.getOnlinePlayers().forEach(player -> duels.beginSession(player.getUniqueId()));
            } else plugin.getLogger().warning("Warzone Duels advancements requested but WarzoneDuels is unavailable; bridge disabled.");
        }
        if (plugin.getConfig().getBoolean("advancements.commendation-enabled", true)) {
            var provider = Bukkit.getPluginManager().getPlugin("EnthusiaCommend");
            if (provider != null && provider.isEnabled()) {
                commend = new CommendAdvancementBridge(provider.getDataFolder().toPath().resolve("data.yml"));
                Bukkit.getOnlinePlayers().forEach(player -> commend.beginSession(player.getUniqueId()));
            } else plugin.getLogger().warning("Reputation advancements requested but EnthusiaCommend is unavailable; bridge disabled.");
        }
        if (plugin.getConfig().getBoolean("advancements.express-enabled", true)) {
            var provider = Bukkit.getPluginManager().getPlugin("EnthusiaExpress");
            if (provider != null && provider.isEnabled()) {
                express = new ExpressAdvancementBridge(provider.getDataFolder().toPath().resolve("mail.db"));
                Bukkit.getOnlinePlayers().forEach(player -> express.beginSession(player.getUniqueId()));
            } else plugin.getLogger().warning("EnthusiaExpress advancements requested but EnthusiaExpress is unavailable; bridge disabled.");
        }
        if (plugin.getConfig().getBoolean("advancements.diary-enabled", true)) {
            var provider = Bukkit.getPluginManager().getPlugin("DiaryKeeper");
            if (provider != null && provider.isEnabled()) {
                diary = new DiaryAdvancementBridge(provider.getDataFolder().toPath().resolve("diaries.yml"));
                Bukkit.getOnlinePlayers().forEach(player -> diary.beginSession(player.getUniqueId()));
            } else plugin.getLogger().warning("DiaryKeeper advancements requested but DiaryKeeper is unavailable; bridge disabled.");
        }
        rebuild();
        if (duels != null) duelTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            private long warningAfter;
            @Override public void run() {
                try { duels.refresh(); }
                catch (Exception ex) {
                    if (System.currentTimeMillis() >= warningAfter) {
                        warningAfter = System.currentTimeMillis() + 60000;
                        plugin.getLogger().warning("Warzone duel statistics unavailable; retaining known advancement progress: " + ex.getMessage());
                    }
                }
            }
        }, 20L, 100L);
        if (commend != null) commendTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            private long warningAfter;
            @Override public void run() {
                try { commend.refresh(); }
                catch (Exception ex) {
                    if (System.currentTimeMillis() >= warningAfter) {
                        warningAfter = System.currentTimeMillis() + 60000;
                        plugin.getLogger().warning("Reputation advancement evidence unavailable; retaining known progress: "
                            + ex.getMessage());
                    }
                }
            }
        }, 20L, 100L);
        if (express != null) expressTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            private long warningAfter;
            @Override public void run() {
                try { express.refresh(); }
                catch (Exception ex) {
                    if (System.currentTimeMillis() >= warningAfter) {
                        warningAfter = System.currentTimeMillis() + 60000;
                        plugin.getLogger().warning("EnthusiaExpress mail history unavailable; retaining known advancement progress: "
                            + ex.getMessage());
                    }
                }
            }
        }, 20L, 100L);
        if (diary != null) diaryTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            private long warningAfter;
            @Override public void run() {
                try { diary.refresh(); }
                catch (Exception ex) {
                    if (System.currentTimeMillis() >= warningAfter) {
                        warningAfter = System.currentTimeMillis() + 60000;
                        plugin.getLogger().warning("DiaryKeeper advancement evidence unavailable; retaining known progress: "
                            + ex.getMessage());
                    }
                }
            }
        }, 20L, 100L);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        rewards.setAdvancementNotifications((player, completed) -> {
            Set<String> pending = pendingCelebrations.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());
            completed.forEach(reward -> pending.add(key(reward.getId())));
        });
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public static String key(String rewardId) {
        // Injective encoding, stable across config reordering and punctuation in legacy IDs.
        return "tags/" + HexFormat.of().formatHex(rewardId.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    private void rebuild() {
        List<ProjectionService.Node> nodes = new ArrayList<>();
        Map<String, List<RewardDefinition>> categories = new LinkedHashMap<>();
        Map<String, RewardDefinition> next = rewards.getRewards();
        for (RewardDefinition reward : next.values()) categories.computeIfAbsent(reward.getCategory(), ignored -> new ArrayList<>()).add(reward);
        int row = 0;
        for (List<RewardDefinition> group : categories.values()) {
            String fallbackParentId = null;
            for (int index = 0; index < group.size(); index++) {
                RewardDefinition reward = group.get(index);
                String frame = plugin.getConfig().getString("advancements.frames." + reward.getId(), "TASK").toUpperCase(Locale.ROOT);
                if (!List.of("TASK", "GOAL", "CHALLENGE").contains(frame)) frame = "TASK";
                var placement = AdvancementLayout.placement(
                    reward.getId(), fallbackParentId, index + 1, row * 4 + 1);
                String parent = placement.parentId() == null ? null : key(placement.parentId());
                nodes.add(new ProjectionService.Node(key(reward.getId()), parent, color(reward.getName()),
                    description(reward), reward.getIcon(), frame, placement.x(), placement.y()));
                fallbackParentId = reward.getId();
            }
            row++;
        }
        if (duels != null) {
            nodes.addAll(WarzoneAdvancementBridge.nodes(AdvancementLayout.warzoneBaseY(row)));
        }
        if (commend != null) {
            nodes.addAll(CommendAdvancementBridge.nodes(
                AdvancementLayout.reputationBaseY(row, duels != null)));
        }
        if (express != null) {
            nodes.addAll(ExpressAdvancementBridge.nodes(
                AdvancementLayout.expressBaseY(row, duels != null, commend != null)));
        }
        if (diary != null) {
            String diaryIconItemModel = plugin.getConfig().getString(
                "advancements.diary-icon-item-model", "enthusia:journal_quill");
            nodes.addAll(DiaryAdvancementBridge.nodes(
                AdvancementLayout.diaryBaseY(
                    row, duels != null, commend != null, express != null),
                diaryIconItemModel));
        }
        ItemStack icon = new ItemStack(Material.PAPER);
        var meta = icon.getItemMeta();
        meta.setCustomModelData(plugin.getConfig().getInt("advancements.logo-custom-model-data", 815001));
        icon.setItemMeta(meta);
        projection.registerTree(plugin, "enthusia", icon, AdvancementNodeOrder.parentFirst(nodes));
        registered = true;
        definitions = next;
        plugin.getLogger().info("Enthusia native track: " + nodes.size() + " challenges; no reward execution in renderer.");
    }

    static List<String> description(RewardDefinition reward) {
        List<String> lines = new ArrayList<>();
        reward.getDescription().forEach(line -> lines.add(color(line)));
        lines.add("§7Requirements:");
        reward.getCriteria().forEach(criterion -> lines.add("§f" + color(criterion.getLabel()) + ": " + criterion.getAmount()));
        lines.add("§7Rewards:");
        if (reward.getActions().isEmpty()) lines.add("§fNone");
        reward.getActions().forEach(action -> lines.add("§f" + actionDescription(action)));
        if (reward.getActions().stream().anyMatch(RewardAction::isGoldNetworkLimited))
            lines.add("§7Gold: one account per challenge/IP.");
        lines.add("§eClaim with /rewards. Completion is not payment.");
        return List.copyOf(lines);
    }

    private static String actionDescription(RewardAction action) {
        String label = action.getLabel() == null ? "" : color(action.getLabel());
        return switch (action.getType()) {
            case MONEY -> action.getAmount() + " currency (Raw Gold-backed)" + (label.isBlank() ? "" : " - " + label);
            case ITEM -> action.getItemAmount() + "x " + (label.isBlank() ? action.getMaterial() : label);
            case TAG -> "Tag: " + (label.isBlank() ? action.getValue() : label);
            case COMMAND -> label.isBlank() ? "Configured unlock (see /rewards)" : label;
            default -> label.isBlank() ? "Configured item reward (see /rewards)" : label;
        };
    }
    private static String color(String value) {
        return value == null ? "" : ChatColor.translateAlternateColorCodes('&', value);
    }

    private void tick() {
        if (!rewards.isAvailable()) return;
        try {
            if (!plugin.getConfig().getBoolean("advancements.enabled", true)) {
                if (registered) projection.removeTree(plugin, "enthusia");
                registered = false;
                pendingCelebrations.clear();
                return;
            }
            if (!registered || definitions != rewards.getRewards()) rebuild();
            if (queue.isEmpty()) Bukkit.getOnlinePlayers().forEach(player -> queue.add(player.getUniqueId()));
            int limit = Math.max(1, Math.min(100, plugin.getConfig().getInt("advancements.players-per-tick", 25)));
            for (int count = 0; count < limit && !queue.isEmpty(); count++) {
                Player player = Bukkit.getPlayer(queue.removeFirst());
                if (player == null) continue;
                rewards.queueProgressRefresh(player);
                if (!projection.ready(player)) continue;
                Map<String, Integer> progress = new LinkedHashMap<>();
                for (RewardDefinition reward : definitions.values()) {
                    int value = rewards.getAdvancementProgress(player, reward);
                    if (value >= 0) progress.put(key(reward.getId()), value);
                }
                if (duels != null) {
                    var update = duels.observe(player.getUniqueId());
                    progress.putAll(update.progress());
                    if (!update.celebrate().isEmpty()) pendingCelebrations
                        .computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).addAll(update.celebrate());
                }
                if (commend != null) {
                    var update = commend.observe(player.getUniqueId());
                    progress.putAll(update.progress());
                    if (!update.celebrate().isEmpty()) pendingCelebrations
                        .computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).addAll(update.celebrate());
                }
                if (express != null) {
                    var update = express.observe(player.getUniqueId());
                    progress.putAll(update.progress());
                    if (!update.celebrate().isEmpty()) pendingCelebrations
                        .computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).addAll(update.celebrate());
                }
                if (diary != null) {
                    var update = diary.observe(player.getUniqueId());
                    progress.putAll(update.progress());
                    if (!update.celebrate().isEmpty()) pendingCelebrations
                        .computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).addAll(update.celebrate());
                }
                projection.project("enthusia", player, progress);
                Set<String> pending = pendingCelebrations.get(player.getUniqueId());
                if (pending == null) continue;
                var iterator = pending.iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    if (progress.getOrDefault(key, -1) != 1000) continue;
                    // Consume before sending: retries must not replay a partially sent announcement.
                    iterator.remove();
                    projection.celebrate("enthusia", player, key);
                }
                if (pending.isEmpty()) pendingCelebrations.remove(player.getUniqueId());
            }
        } catch (RuntimeException ex) {
            if (System.currentTimeMillis() >= nextWarning) {
                nextWarning = System.currentTimeMillis() + 60000;
                plugin.getLogger().warning("Native advancement projection will retry: " + ex.getMessage());
            }
        }
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (duels != null) duels.beginSession(id);
        if (commend != null) commend.beginSession(id);
        if (express != null) express.beginSession(id);
        if (diary != null) diary.beginSession(id);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        pendingCelebrations.remove(id);
        if (duels != null) duels.forget(id);
        if (commend != null) commend.forget(id);
        if (express != null) express.forget(id);
        if (diary != null) diary.forget(id);
        queue.removeIf(id::equals);
    }
    @Override public void close() {
        if (task != null) task.cancel();
        if (duelTask != null) duelTask.cancel();
        if (commendTask != null) commendTask.cancel();
        if (expressTask != null) expressTask.cancel();
        if (diaryTask != null) diaryTask.cancel();
        rewards.setAdvancementNotifications(null);
        HandlerList.unregisterAll(this);
        try {
            if (registered && Bukkit.getPluginManager().isPluginEnabled("EnthusiaAdvancements"))
                projection.removeTree(plugin, "enthusia");
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Native advancement tree removal failed during shutdown: " + ex.getMessage());
        } finally {
            registered = false;
            pendingCelebrations.clear();
            queue.clear();
        }
    }
}
