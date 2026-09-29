package org.enthusia.tags.entitlements;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class EntitlementListener implements Listener {
    private final EntitlementService service;

    public EntitlementListener(EntitlementService service) {
        this.service = service;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        service.preloadPlayerBlocking(event.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        service.syncPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        service.unloadPlayer(event.getPlayer());
    }
}
