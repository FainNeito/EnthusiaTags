package org.enthusia.tags.cosmetics;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Cancellable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RoseChatPresenceIntegrationTest {
    @Test
    void actualRoseChatContractReplacesOnceAndOriginalLeavesDefaultsIntact(@TempDir Path output) throws Exception {
        Path source = Path.of(System.getProperty("rosechat.contract.source",
            "../Enthusia-RoseChat/src/main/java/dev/rosewood/rosechat/api/event/PresenceMessageEvent.java"));
        assertTrue(java.nio.file.Files.exists(source), "RoseChat needs an explicit per-viewer replacement contract");
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null,
            "-classpath", System.getProperty("java.class.path"), "-d", output.toString(), source.toString());
        assertEquals(0, result, "Compile actual companion source against the same Paper API");
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{output.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> type = loader.loadClass("dev.rosewood.rosechat.api.event.PresenceMessageEvent");
            var constructor = type.getConstructor(Player.class, Player.class, String.class, List.class);
            UUID id = UUID.randomUUID();
            Player subject = PresenceLifecycleTest.player(id);
            Player viewer = PresenceLifecycleTest.player(UUID.randomUUID());
            CosmeticsService service = new CosmeticsService(null, null, null);
            service.getCosmetics().put("custom", new CosmeticDefinition("custom", "custom", "quit",
                CosmeticType.QUIT_MESSAGE, null, null, null, null, "&6{player} left", "test", 0, 0, 0, 0));
            PresenceLifecycleTest.selections(service).put(id, new java.util.concurrent.ConcurrentHashMap<>(Map.of("quit", "custom")));
            var apply = Class.forName("org.enthusia.tags.cosmetics.RoseChatPresenceHook")
                .getDeclaredMethod("applyReplacement", Event.class, CosmeticsService.class);
            apply.setAccessible(true);
            Event event = (Event) constructor.newInstance(subject, viewer, "quit", List.of("default1", "default2"));
            apply.invoke(null, event, service);
            assertEquals(List.of("&6Tester left"), type.getMethod("getLines").invoke(event));
            assertSame(viewer, type.getMethod("getViewer").invoke(event));
            assertSame(subject, type.getMethod("getPlayer").invoke(event));

            Event cancelled = (Event) constructor.newInstance(subject, viewer, "quit", List.of("default"));
            ((Cancellable) cancelled).setCancelled(true);
            apply.invoke(null, cancelled, service);
            assertEquals(List.of("default"), type.getMethod("getLines").invoke(cancelled));

            service.getCosmetics().put("original_quit", new CosmeticDefinition("original_quit", "Original", "quit",
                CosmeticType.ORIGINAL, null, null, null, null, "MUST NOT REPLACE", "test", 0, 0, 0, 0));
            PresenceLifecycleTest.selections(service).get(id).put("quit", "original_quit");
            Event original = (Event) constructor.newInstance(subject, viewer, "quit", List.of("default1", "default2"));
            apply.invoke(null, original, service);
            assertEquals(List.of("default1", "default2"), type.getMethod("getLines").invoke(original));
        }
    }
}
