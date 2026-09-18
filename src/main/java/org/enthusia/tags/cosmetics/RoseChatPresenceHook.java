package org.enthusia.tags.cosmetics;

import java.lang.reflect.Method;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/** Optional binding to the explicit companion RoseChat API, never to private internals. */
public final class RoseChatPresenceHook {
    private static final String EVENT = "dev.rosewood.rosechat.api.event.PresenceMessageEvent";
    private RoseChatPresenceHook() { }

    public static void register(JavaPlugin owner, CosmeticsService cosmetics) {
        Plugin roseChat = owner.getServer().getPluginManager().getPlugin("RoseChat");
        if (roseChat == null || !roseChat.isEnabled())
            return;
        try {
            Class<? extends Event> eventType = Class.forName(EVENT, true, roseChat.getClass().getClassLoader()).asSubclass(Event.class);
            // Validate all methods at startup, not during a player's join.
            eventType.getMethod("getPlayer");
            eventType.getMethod("getKind");
            eventType.getMethod("setLines", List.class);
            owner.getServer().getPluginManager().registerEvent(eventType, new Listener() { }, EventPriority.HIGH,
                (listener, event) -> {
                    try {
                        applyReplacement(event, cosmetics);
                    } catch (ReflectiveOperationException ex) {
                        throw new EventException(ex);
                    }
                }, owner, true);
        } catch (ReflectiveOperationException | ClassCastException ex) {
            owner.getLogger().warning("RoseChat presence hook unavailable. Keeping RoseChat defaults; install the companion RoseChat build for custom join/leave messages.");
        }
    }

    static void applyReplacement(Event event, CosmeticsService cosmetics) throws ReflectiveOperationException {
        if (event instanceof Cancellable cancellable && cancellable.isCancelled())
            return;
        Class<?> type = event.getClass();
        Player player = (Player) type.getMethod("getPlayer").invoke(event);
        String kind = (String) type.getMethod("getKind").invoke(event);
        String message = switch (kind) {
            case "join" -> cosmetics.getJoinMessage(player);
            case "quit" -> cosmetics.getQuitMessage(player);
            default -> null;
        };
        // Original or inaccessible selections leave every original template intact.
        if (message != null && !message.isBlank()) {
            Method setter = type.getMethod("setLines", List.class);
            setter.invoke(event, List.of(message));
        }
    }
}
