package org.enthusia.tags.entitlements;

import org.junit.jupiter.api.Test;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.enthusia.tags.TagService;
import org.enthusia.tags.cosmetics.CosmeticsService;
import org.enthusia.tags.rewards.RewardService;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;
import java.util.Set;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntitlementLifecycleTest {
    private EntitlementService service(EntitlementStorage storage) throws Exception {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        var service = new EntitlementService(plugin, mock(TagService.class),
            mock(CosmeticsService.class), mock(RewardService.class), storage);
        var enabled = EntitlementService.class.getDeclaredField("enabled");
        enabled.setAccessible(true);
        enabled.set(service, true);
        return service;
    }

    @Test void immediatelyFailedLoadCanRetry() throws Exception {
        UUID id = UUID.randomUUID();
        var storage = mock(EntitlementStorage.class);
        when(storage.loadAsync(id)).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("busy")))
            .thenReturn(CompletableFuture.completedFuture(Set.of("beta_tester")));
        var service = service(storage);
        service.preloadPlayer(id);
        service.preloadPlayer(id);
        verify(storage, times(2)).loadAsync(id);
        assertTrue(service.owns(id, "beta_tester"));
    }

    @Test void lateLoadAfterQuitCannotReplaceNextSessionsEvidence() throws Exception {
        UUID id = UUID.randomUUID();
        var storage = mock(EntitlementStorage.class);
        var old = new CompletableFuture<Set<String>>();
        when(storage.loadAsync(id)).thenReturn(old)
            .thenReturn(CompletableFuture.completedFuture(Set.of("donor_avid")));
        var service = service(storage);
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        service.preloadPlayer(id);
        service.unloadPlayer(player);
        service.preloadPlayer(id);
        old.complete(Set.of("stale"));
        assertEquals(Set.of("donor_avid"), service.getOwnedEntitlements(id));
    }

    @Test void lateLoadAfterDisableCannotRepopulateOwnershipCache() throws Exception {
        UUID id = UUID.randomUUID();
        var storage = mock(EntitlementStorage.class);
        var pending = new CompletableFuture<Set<String>>();
        when(storage.loadAsync(id)).thenReturn(pending);
        var service = service(storage);
        service.preloadPlayer(id);
        service.disable();
        pending.complete(Set.of("beta_tester"));
        assertTrue(service.getOwnedEntitlements(id).isEmpty());
    }
}
