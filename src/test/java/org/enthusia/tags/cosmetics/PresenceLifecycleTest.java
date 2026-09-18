package org.enthusia.tags.cosmetics;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresenceLifecycleTest {
    @Test
    void quitSelectionSurvivesUntilRoseChatHighestPriorityDelivery() throws Exception {
        UUID id = UUID.randomUUID();
        Player player = player(id);
        CosmeticsService service = new CosmeticsService(null, null, null);
        selections(service).put(id, new java.util.concurrent.ConcurrentHashMap<>(Map.of("quit", "original_quit")));
        CosmeticsListener listener = allocate(CosmeticsListener.class);
        Field serviceField = CosmeticsListener.class.getDeclaredField("cosmeticsService");
        serviceField.setAccessible(true);
        serviceField.set(listener, service);
        listener.onQuit(new PlayerQuitEvent(player, (String) null));
        assertEquals("original_quit", service.getSelection(id, "quit"),
            "HIGH must not discard the selection before RoseChat sends at HIGHEST");
        listener.onQuitCleanup(new PlayerQuitEvent(player, (String) null));
        assertNull(service.getSelection(id, "quit"));
    }

    @Test
    void roseChatOwnershipAndSuppressedMessagesNeverProduceBukkitDuplicates() throws Exception {
        UUID id = UUID.randomUUID();
        Player player = player(id);
        CosmeticsService service = new CosmeticsService(null, null, null);
        service.getCosmetics().put("custom", new CosmeticDefinition("custom", "custom", "quit",
            CosmeticType.QUIT_MESSAGE, null, null, null, null, "CUSTOM", "test", 0, 0, 0, 0));
        selections(service).put(id, new java.util.concurrent.ConcurrentHashMap<>(Map.of("quit", "custom")));
        CosmeticsListener listener = allocate(CosmeticsListener.class);
        Field serviceField = CosmeticsListener.class.getDeclaredField("cosmeticsService");
        serviceField.setAccessible(true);
        serviceField.set(listener, service);
        Field owner = CosmeticsListener.class.getDeclaredField("roseChatOwnsPresence");
        owner.setAccessible(true);
        owner.set(listener, (java.util.function.BooleanSupplier) () -> true);
        PlayerQuitEvent owned = new PlayerQuitEvent(player, "original");
        listener.onQuit(owned);
        assertEquals("original", owned.getQuitMessage());
        owner.set(listener, (java.util.function.BooleanSupplier) () -> false);
        PlayerQuitEvent hidden = new PlayerQuitEvent(player, (String) null);
        listener.onQuit(hidden);
        assertNull(hidden.quitMessage());
        PlayerQuitEvent standalone = new PlayerQuitEvent(player, "original");
        listener.onQuit(standalone);
        assertEquals("CUSTOM", standalone.getQuitMessage());
    }

    @Test
    void cleanupRunsAtMonitorAndMessageMutationDoesNot() throws Exception {
        var cleanup = CosmeticsListener.class.getDeclaredMethod("onQuitCleanup", PlayerQuitEvent.class);
        assertEquals(EventPriority.MONITOR, cleanup.getAnnotation(EventHandler.class).priority());
        var death = CosmeticsListener.class.getDeclaredMethod("onDeath", org.bukkit.event.entity.PlayerDeathEvent.class);
        assertNotEquals(EventPriority.MONITOR, death.getAnnotation(EventHandler.class).priority());
    }

    @SuppressWarnings("unchecked")
    static Map<UUID, Map<String, String>> selections(CosmeticsService service) throws Exception {
        Field f = CosmeticsService.class.getDeclaredField("selections");
        f.setAccessible(true);
        return (Map<UUID, Map<String, String>>) f.get(service);
    }

    static Player player(UUID id) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> id;
                case "getName" -> "Tester";
                case "hasPermission", "isOnline" -> true;
                case "hashCode" -> id.hashCode();
                case "equals" -> proxy == args[0];
                default -> null;
            });
    }

    static <T> T allocate(Class<T> type) throws Exception {
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return type.cast(((sun.misc.Unsafe) f.get(null)).allocateInstance(type));
    }
}
