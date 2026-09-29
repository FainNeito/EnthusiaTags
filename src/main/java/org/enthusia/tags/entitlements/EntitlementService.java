package org.enthusia.tags.entitlements;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.enthusia.tags.TagService;
import org.enthusia.tags.cosmetics.CosmeticsService;
import org.enthusia.tags.rewards.RewardService;

public final class EntitlementService {
    public static final String COUNTER_PREFIX = "entitlement:";

    private final JavaPlugin plugin;
    private final TagService tagService;
    private final CosmeticsService cosmeticsService;
    private final RewardService rewardService;
    private final Map<UUID, Set<String>> owned = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> active = new ConcurrentHashMap<>();
    private final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, CompletableFuture<Void>> pendingLoads = new ConcurrentHashMap<>();
    private final AtomicBoolean groupLookupWarning = new AtomicBoolean(false);

    private volatile Map<String, EntitlementDefinition> definitions = Map.of();
    private volatile Map<UUID, LegacyGrant> legacyGrants = Map.of();
    private EntitlementStorage storage;
    private BukkitTask activeSyncTask;
    private volatile boolean enabled;

    public EntitlementService(JavaPlugin plugin, TagService tagService,
                              CosmeticsService cosmeticsService, RewardService rewardService) {
        this.plugin = plugin;
        this.tagService = tagService;
        this.cosmeticsService = cosmeticsService;
        this.rewardService = rewardService;
    }

    EntitlementService(JavaPlugin plugin, TagService tagService, CosmeticsService cosmeticsService,
                       RewardService rewardService, EntitlementStorage storage) {
        this(plugin, tagService, cosmeticsService, rewardService);
        this.storage = storage;
    }

    public void enable() {
        ensureDefaults();
        loadConfig();
        if (storage == null) storage = new EntitlementStorage(new File(plugin.getDataFolder(), "entitlements.db"));
        try {
            storage.init();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Entitlement storage failed to initialize: " + ex.getMessage());
            return;
        }
        enabled = true;
        cosmeticsService.setEntitlementAccess(this::canUseCosmetic);
        seedLegacyGrants();
        startActiveSync();
        for (Player player : Bukkit.getOnlinePlayers()) {
            preloadPlayer(player.getUniqueId());
            syncPlayer(player);
        }
    }

    public void disable() {
        enabled = false;
        stopActiveSync();
        cosmeticsService.setEntitlementAccess((player, cosmeticId) -> false);
        synchronized (pendingLoads) {
            pendingLoads.clear();
            active.clear();
            loadedPlayers.clear();
            owned.clear();
        }
        if (storage != null) storage.close();
    }

    public void reload() {
        if (!enabled) return;
        ensureDefaults();
        loadConfig();
        seedLegacyGrants();
        for (Player player : Bukkit.getOnlinePlayers()) syncPlayer(player);
    }

    public Map<String, EntitlementDefinition> getDefinitions() {
        return definitions;
    }

    public Set<String> getOwnedEntitlements(UUID playerId) {
        Set<String> value = owned.get(playerId);
        return value == null ? Set.of() : Set.copyOf(value);
    }

    public boolean owns(UUID playerId, String entitlementId) {
        Set<String> value = owned.get(playerId);
        return value != null && value.contains(normalize(entitlementId));
    }

    public java.util.List<EntitlementDefinition> definitionsForTag(String tagId) {
        String id = normalize(tagId);
        return definitions.values().stream()
            .filter(definition -> definition.tags().contains(id))
            .sorted(java.util.Comparator.comparing(EntitlementDefinition::id))
            .toList();
    }

    public java.util.List<EntitlementDefinition> definitionsForCosmetic(String cosmeticId) {
        String id = normalize(cosmeticId);
        return definitions.values().stream()
            .filter(definition -> definition.cosmetics().contains(id) || definition.activeCosmetics().contains(id))
            .sorted(java.util.Comparator.comparing(EntitlementDefinition::id))
            .toList();
    }

    public boolean isActiveOnlyCosmetic(String cosmeticId) {
        String id = normalize(cosmeticId);
        return definitions.values().stream().anyMatch(definition -> definition.activeCosmetics().contains(id));
    }

    public boolean canUseCosmetic(Player player, String cosmeticId) {
        if (player == null || cosmeticId == null) return false;
        String id = normalize(cosmeticId);
        Set<String> permanent = owned.getOrDefault(player.getUniqueId(), Set.of());
        for (String entitlementId : permanent) {
            EntitlementDefinition definition = definitions.get(entitlementId);
            if (definition != null && definition.cosmetics().contains(id)) return true;
        }
        Set<String> activeIds = active.getOrDefault(player.getUniqueId(), Set.of());
        for (String entitlementId : activeIds) {
            EntitlementDefinition definition = definitions.get(entitlementId);
            if (definition != null && definition.activeCosmetics().contains(id)) return true;
        }
        return false;
    }

    public void preloadPlayer(UUID playerId) {
        beginLoad(playerId);
    }

    public void preloadPlayerBlocking(UUID playerId) {
        beginLoad(playerId).join();
    }

    private CompletableFuture<Void> beginLoad(UUID playerId) {
        synchronized (pendingLoads) {
            if (!enabled || loadedPlayers.contains(playerId)) return CompletableFuture.completedFuture(null);
            CompletableFuture<Void> existing = pendingLoads.get(playerId);
            if (existing != null) return existing;
            // Install the session token before attaching a possibly synchronous completion.
            CompletableFuture<Void> token = new CompletableFuture<>();
            pendingLoads.put(playerId, token);
            CompletableFuture<Set<String>> read;
            try {
                read = storage.loadAsync(playerId);
            } catch (RuntimeException failure) {
                read = CompletableFuture.failedFuture(failure);
            }
            read.whenComplete((ids, error) -> {
                synchronized (pendingLoads) {
                    if (enabled && token.equals(pendingLoads.get(playerId))) {
                        if (error == null) installLoaded(playerId, ids);
                        else plugin.getLogger().warning("Failed to load entitlements for " + playerId + ": " + error.getMessage());
                    }
                    pendingLoads.remove(playerId, token);
                }
                token.complete(null);
            });
            return token;
        }
    }

    public void syncPlayer(Player player) {
        if (!enabled || player == null || !player.isOnline()) return;
        preloadPlayer(player.getUniqueId());
        CompletableFuture<Void> pending = pendingLoads.get(player.getUniqueId());
        if (pending == null) {
            syncNow(player);
            return;
        }
        pending.thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (enabled && player.isOnline()) syncNow(player);
        }));
    }

    public void unloadPlayer(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        synchronized (pendingLoads) {
            active.remove(playerId);
            loadedPlayers.remove(playerId);
            owned.remove(playerId);
            pendingLoads.remove(playerId);
        }
    }

    private void syncNow(Player player) {
        if (!loadedPlayers.contains(player.getUniqueId())) return;
        boolean firstSessionSync = !active.containsKey(player.getUniqueId());
        Set<String> luckPermsGroups = luckPermsGroups(player);
        Set<String> current = ConcurrentHashMap.newKeySet();
        for (EntitlementDefinition definition : definitions.values()) {
            if (!definition.hasQualification()) continue;
            if (qualifies(definition, player, luckPermsGroups)) {
                current.add(definition.id());
                if (!owns(player.getUniqueId(), definition.id())) {
                    grantEntitlement(player.getUniqueId(), definition.id(), "groups=" + String.join(",", luckPermsGroups));
                }
            }
        }
        active.put(player.getUniqueId(), current);

        if (firstSessionSync) {
            Set<String> playerOwned = owned.getOrDefault(player.getUniqueId(), Set.of());
            for (String entitlementId : Set.copyOf(playerOwned)) {
                applyOwned(player.getUniqueId(), entitlementId);
            }
        }
        cosmeticsService.refreshEntitlementAccess(player);
    }

    static boolean qualifies(EntitlementDefinition definition, Player player, String primaryGroup) {
        String group = normalize(primaryGroup);
        return qualifies(definition, player, group.isBlank() ? Set.of() : Set.of(group));
    }

    static boolean qualifies(EntitlementDefinition definition, Player player, Set<String> inheritedGroups) {
        if (definition == null || player == null) return false;
        if (!definition.permission().isBlank() && player.isPermissionSet(definition.permission())
            && player.hasPermission(definition.permission())) return true;
        if (inheritedGroups != null) {
            for (String group : inheritedGroups) {
                if (definition.groups().contains(normalize(group))) return true;
            }
        }
        for (String candidate : definition.groups()) {
            String permission = "group." + candidate;
            if (player.isPermissionSet(permission) && player.hasPermission(permission)) return true;
        }
        return false;
    }

    private void installLoaded(UUID playerId, Set<String> stored) {
        Set<String> ids = owned.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet());
        if (stored != null) ids.addAll(stored.stream().map(EntitlementService::normalize).toList());
        LegacyGrant legacy = legacyGrants.get(playerId);
        if (legacy != null) ids.addAll(legacy.entitlements());
        loadedPlayers.add(playerId);
    }

    private void startActiveSync() {
        stopActiveSync();
        activeSyncTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled) return;
            for (Player player : Bukkit.getOnlinePlayers()) syncPlayer(player);
        }, 1200L, 1200L);
    }

    private void stopActiveSync() {
        if (activeSyncTask != null) {
            activeSyncTask.cancel();
            activeSyncTask = null;
        }
    }

    private void seedLegacyGrants() {
        if (!enabled) return;
        java.util.List<CompletableFuture<Void>> writes = new java.util.ArrayList<>();
        for (Map.Entry<UUID, LegacyGrant> entry : legacyGrants.entrySet()) {
            for (String entitlementId : entry.getValue().entitlements()) {
                String metadata = entry.getValue().metadata().getOrDefault(entitlementId, "");
                writes.add(grantEntitlement(entry.getKey(), entitlementId, metadata));
            }
        }
        CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).whenComplete((ignored, error) -> {
            if (error != null) plugin.getLogger().warning("Legacy entitlement seeding finished with an error: " + error.getMessage());
            else plugin.getLogger().info("Legacy entitlement roster synchronized: " + legacyGrants.size() + " UUIDs.");
        });
    }

    private CompletableFuture<Void> grantEntitlement(UUID playerId, String entitlementId, String metadata) {
        if (!enabled) return CompletableFuture.completedFuture(null);
        String id = normalize(entitlementId);
        EntitlementDefinition definition = definitions.get(id);
        if (definition == null) {
            plugin.getLogger().warning("Unknown entitlement '" + id + "' for " + playerId);
            return CompletableFuture.completedFuture(null);
        }
        return storage.grantAsync(playerId, id, definition.source(), metadata).thenAccept(inserted -> {
            Set<String> values = owned.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet());
            values.add(id);
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!enabled) return;
            Player online = Bukkit.getPlayer(playerId);
            if (online != null && online.isOnline()) applyOwned(playerId, id);
        })).exceptionally(error -> {
            plugin.getLogger().warning("Failed to grant entitlement " + id + " to " + playerId + ": " + error.getMessage());
            return null;
        });
    }

    private void applyOwned(UUID playerId, String entitlementId) {
        EntitlementDefinition definition = definitions.get(normalize(entitlementId));
        if (definition == null) return;
        for (String tag : definition.tags()) tagService.grantTagPersisted(playerId, tag);
        rewardService.latchCounterWhenLoaded(playerId, COUNTER_PREFIX + definition.id());
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) cosmeticsService.refreshEntitlementAccess(player);
    }

    private Set<String> luckPermsGroups(Player player) {
        Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (luckPerms == null || !luckPerms.isEnabled()) return Set.of();
        try {
            var api = Bukkit.getServicesManager().load(net.luckperms.api.LuckPerms.class);
            if (api == null) return Set.of();
            var user = api.getUserManager().getUser(player.getUniqueId());
            if (user == null) return Set.of();
            user.auditTemporaryNodes();
            Set<String> groups = new HashSet<>();
            for (var group : user.getInheritedGroups(user.getQueryOptions())) {
                groups.add(normalize(group.getName()));
            }
            return Set.copyOf(groups);
        } catch (LinkageError | RuntimeException ex) {
            if (groupLookupWarning.compareAndSet(false, true)) {
                plugin.getLogger().warning("LuckPerms group lookup unavailable; donor entitlements will use permissions: "
                    + ex.getClass().getSimpleName());
            }
            return Set.of();
        }
    }

    private void ensureDefaults() {
        File file = new File(plugin.getDataFolder(), "entitlements.yml");
        if (!file.exists()) plugin.saveResource("entitlements.yml", false);
        YamlConfiguration configFile = YamlConfiguration.loadConfiguration(file);
        try (var stream = plugin.getResource("entitlements.yml")) {
            if (stream == null) return;
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
            configFile.setDefaults(defaults);
            configFile.options().copyDefaults(true);
            configFile.save(file);
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not merge entitlement defaults: " + ex.getMessage());
        }
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "entitlements.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, EntitlementDefinition> loadedDefinitions = new LinkedHashMap<>();
        ConfigurationSection definitionsSection = config.getConfigurationSection("definitions");
        if (definitionsSection != null) {
            for (String rawId : definitionsSection.getKeys(false)) {
                ConfigurationSection section = definitionsSection.getConfigurationSection(rawId);
                if (section == null) continue;
                String id = normalize(rawId);
                ConfigurationSection qualification = section.getConfigurationSection("qualification");
                String permission = qualification == null ? "" : qualification.getString("permission", "");
                Set<String> groups = qualification == null ? Set.of()
                    : normalizeSet(qualification.getStringList("groups"));
                loadedDefinitions.put(id, new EntitlementDefinition(
                    id,
                    section.getString("source", id),
                    section.getBoolean("permanent", true),
                    normalizeSet(section.getStringList("tags")),
                    normalizeSet(section.getStringList("cosmetics")),
                    normalizeSet(section.getStringList("active-cosmetics")),
                    permission,
                    groups
                ));
            }
        }

        Map<UUID, LegacyGrant> loadedGrants = new LinkedHashMap<>();
        ConfigurationSection grantsSection = config.getConfigurationSection("legacy-grants");
        if (grantsSection != null) {
            for (String rawUuid : grantsSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(rawUuid);
                    ConfigurationSection section = grantsSection.getConfigurationSection(rawUuid);
                    if (section == null) continue;
                    Map<String, String> metadata = new HashMap<>();
                    ConfigurationSection metadataSection = section.getConfigurationSection("metadata");
                    if (metadataSection != null) {
                        for (String key : metadataSection.getKeys(false)) {
                            metadata.put(normalize(key), metadataSection.getString(key, ""));
                        }
                    }
                    loadedGrants.put(playerId, new LegacyGrant(
                        section.getString("name", rawUuid),
                        normalizeSet(section.getStringList("entitlements")),
                        Map.copyOf(metadata)));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Invalid legacy entitlement UUID: " + rawUuid);
                }
            }
        }
        definitions = Map.copyOf(loadedDefinitions);
        legacyGrants = Map.copyOf(loadedGrants);
    }

    private static Set<String> normalizeSet(java.util.Collection<String> values) {
        if (values == null || values.isEmpty()) return Set.of();
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            String clean = normalize(value);
            if (!clean.isBlank()) normalized.add(clean);
        }
        return Set.copyOf(normalized);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    record LegacyGrant(String name, Set<String> entitlements, Map<String, String> metadata) { }
}
