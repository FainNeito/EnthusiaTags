package org.enthusia.tags.rewards;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.enthusia.tags.PerformanceMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ImplicitCounterCriteriaTest {
    record Expected(String id, String key, long amount) {}
    private static final List<Expected> EXISTING = List.of(
        new Expected("sleeps_in_minecraft", "max_consecutive_active", 720),
        new Expected("marathon_session", "max_consecutive_active", 360),
        new Expected("yearn_for_mines", "underground_active", 600),
        new Expected("deep_dweller", "underground_active", 1800),
        new Expected("lag_was_crazy", "max_ping_ms", 150));

    public static class TestPlugin extends JavaPlugin {
        @Override public Logger getLogger() { return Logger.getLogger("counter-criteria-test"); }
    }

    private RewardService service() throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var plugin = (TestPlugin) ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(TestPlugin.class);
        Field enabled = JavaPlugin.class.getDeclaredField("isEnabled");
        enabled.setAccessible(true);
        enabled.set(plugin, true);
        return new RewardService(plugin, null, null, new PerformanceMonitor(null));
    }

    private YamlConfiguration bundled() throws Exception {
        try (var in = getClass().getClassLoader().getResourceAsStream("rewards.yml")) {
            assertNotNull(in);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    @SuppressWarnings("unchecked")
    private List<RewardCriterion> parse(RewardService service, ConfigurationSection section) throws Exception {
        var method = RewardService.class.getDeclaredMethod("loadCriteria", ConfigurationSection.class);
        method.setAccessible(true);
        return (List<RewardCriterion>) method.invoke(service, section);
    }

    @Test void allBundledCriteriaRemainValidIncludingTheFiveMissingMappings() throws Exception {
        var service = service();
        var config = bundled();
        for (var expected : EXISTING) {
            var criteria = parse(service, config.getConfigurationSection("rewards." + expected.id() + ".criteria"));
            assertEquals(1, criteria.size());
            var criterion = criteria.getFirst();
            assertTrue(criterion.isValid(), expected.id());
            assertEquals(RewardSourceType.CUSTOM_COUNTER, criterion.getSourceType());
            assertEquals(expected.key(), criterion.getKey());
            assertEquals(expected.amount(), criterion.getAmount());
        }
        for (var id : config.getConfigurationSection("rewards").getKeys(false)) {
            for (var criterion : parse(service, config.getConfigurationSection("rewards." + id + ".criteria"))) {
                assertTrue(criterion.isValid(), id);
            }
        }
    }

    @Test void administratorOverridesArePreservedAndUnknownCountersStillFailClosed() throws Exception {
        var service = service();
        var config = bundled();
        for (var expected : EXISTING) {
            var section = config.getConfigurationSection("rewards." + expected.id() + ".criteria");
            var entry = section.getConfigurationSection(section.getKeys(false).iterator().next());
            entry.set("key", "admin_counter");
            assertEquals("admin_counter", parse(service, section).getFirst().getKey());
            entry.set("counter", "admin_alias");
            assertEquals("admin_alias", parse(service, section).getFirst().getKey());
            entry.set("source", "CUSTOM_COUNTER");
            assertEquals("admin_alias", parse(service, section).getFirst().getKey());
            entry.set("key", null);
            entry.set("counter", null);
            assertFalse(parse(service, section).getFirst().isValid(), "Explicit custom source requires a key");
            entry.set("source", null);
            entry.set("type", "CUSTOM_COUNTER");
            assertFalse(parse(service, section).getFirst().isValid());
        }
    }

    @Test void existingSavedProgressAndClaimsAreReadWithoutMutation(@TempDir Path directory) throws Exception {
        UUID playerId = UUID.randomUUID();
        var counters = Map.of("max_consecutive_active", 721L, "underground_active", 1801L, "max_ping_ms", 151L);
        var saved = new RewardStorage.StoredRewardData(Set.of("sleeps_in_minecraft"), counters, Map.of(), 8L);
        var storage = new RewardStorage(directory.resolve("rewards.db").toFile(), new PerformanceMonitor(null));
        storage.init();
        assertEquals(RewardStorage.WriteResult.WRITTEN, storage.saveAsync(playerId, saved).get());
        storage.close();
        storage = new RewardStorage(directory.resolve("rewards.db").toFile(), new PerformanceMonitor(null));
        storage.init();
        var executor = Executors.newSingleThreadExecutor();
        try {
            var service = service();
            set(service, "storage", storage);
            set(service, "claimExecutor", executor);
            Field lifecycle = RewardService.class.getDeclaredField("lifecycle");
            lifecycle.setAccessible(true);
            @SuppressWarnings("unchecked")
            var lifecycleState = (java.util.concurrent.atomic.AtomicReference<Object>) lifecycle.get(service);
            Object running = java.util.Arrays.stream(lifecycleState.get().getClass().getEnumConstants())
                .filter(value -> value.toString().equals("RUNNING")).findFirst().orElseThrow();
            lifecycleState.set(running);
            assertTrue(service.isAvailable(), "Fixture must model an enabled service");
            var state = new RewardPlayerState();
            state.hydrate(storage.loadNow(playerId));
            Field states = RewardService.class.getDeclaredField("playerStates");
            states.setAccessible(true);
            @SuppressWarnings("unchecked")
            var players = (Map<UUID, RewardPlayerState>) states.get(service);
            players.put(playerId, state);
            Player player = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
                (p, method, args) -> method.getName().equals("getUniqueId") ? playerId : null);
            var compute = RewardService.class.getDeclaredMethod("computeProgress", Player.class, RewardCriterion.class);
            compute.setAccessible(true);
            var config = bundled();
            for (var expected : EXISTING) {
                var criterion = parse(service, config.getConfigurationSection("rewards." + expected.id() + ".criteria")).getFirst();
                assertTrue(criterion.isValid(), expected.id());
                assertEquals(counters.get(expected.key()), compute.invoke(service, player, criterion));
                assertTrue(storage.loadActionLedgerNow(playerId, expected.id()).isEmpty());
            }
            assertEquals(saved, storage.loadNow(playerId));
            assertEquals(counters, state.countersSnapshot());
            assertFalse(state.isDirty());
        } finally { executor.shutdownNow(); storage.close(); }
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
