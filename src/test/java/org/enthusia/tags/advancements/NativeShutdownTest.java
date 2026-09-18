package org.enthusia.tags.advancements;

import io.github.badgersmc.advancements.pilot.ProjectionService;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.enthusia.tags.PerformanceMonitor;
import org.enthusia.tags.rewards.RewardService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeShutdownTest {
    public static class TestPlugin extends JavaPlugin {
        @Override public Logger getLogger() { return Logger.getLogger("shutdown-test"); }
    }

    @Test void providerFailureDoesNotEscapeOrLeavePendingState() throws Exception {
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        Object previous = serverField.get(null);
        AtomicInteger removals = new AtomicInteger();
        PluginManager manager = (PluginManager) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{PluginManager.class}, (p, m, a) -> m.getName().equals("isPluginEnabled") ? true : null);
        Server server = (Server) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Server.class},
            (p, m, a) -> m.getName().equals("getPluginManager") ? manager : null);
        NativeAdvancementController controller = allocate(NativeAdvancementController.class);
        Map<UUID, Set<String>> pending = new HashMap<>();
        UUID id = UUID.randomUUID();
        pending.put(id, Set.of("test"));
        ArrayDeque<UUID> queue = new ArrayDeque<>();
        queue.add(id);
        ProjectionService provider = (ProjectionService) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{ProjectionService.class}, (p, m, a) -> {
                if (m.getName().equals("removeTree")) {
                    removals.incrementAndGet();
                    throw new IllegalStateException("provider unavailable");
                }
                return null;
            });
        set(controller, "plugin", allocate(TestPlugin.class));
        set(controller, "rewards", new RewardService(null, null, null, new PerformanceMonitor(null)));
        set(controller, "projection", provider);
        set(controller, "registered", true);
        set(controller, "pendingCelebrations", pending);
        set(controller, "queue", queue);
        try {
            serverField.set(null, server);
            assertDoesNotThrow(controller::close);
            assertTrue(pending.isEmpty());
            assertTrue(queue.isEmpty());
            assertDoesNotThrow(controller::close);
            assertEquals(1, removals.get(), "Closed controller must not remove its tree again");
        } finally { serverField.set(null, previous); }
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe) field.get(null)).allocateInstance(type));
    }
}
