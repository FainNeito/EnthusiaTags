package org.enthusia.tags.cosmetics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.enthusia.tags.Messages;
import org.enthusia.tags.PerformanceMonitor;
import org.junit.jupiter.api.Test;

class CosmeticsEntitlementAccessTest {
    @Test
    void persistentEntitlementCanUnlockCosmeticWithoutLuckPermsPermission() {
        CosmeticsService service = new CosmeticsService(
            mock(JavaPlugin.class), mock(Messages.class), mock(PerformanceMonitor.class));
        Player player = mock(Player.class);
        when(player.hasPermission("locked.permission")).thenReturn(false);
        CosmeticDefinition cosmetic = new CosmeticDefinition(
            "legacy_cosmetic", "Legacy", "trail", CosmeticType.TRAIL_PARTICLE,
            Material.SLIME_BALL, org.bukkit.Particle.HAPPY_VILLAGER,
            null, null, null, "locked.permission", 1, 0, 0, 0);

        assertFalse(service.canUseCosmetic(player, cosmetic));
        service.setEntitlementAccess((subject, cosmeticId) -> "legacy_cosmetic".equals(cosmeticId));
        assertTrue(service.canUseCosmetic(player, cosmetic));
    }

    @Test
    void normalPermissionStillWorksAlongsideEntitlements() {
        CosmeticsService service = new CosmeticsService(
            mock(JavaPlugin.class), mock(Messages.class), mock(PerformanceMonitor.class));
        Player player = mock(Player.class);
        when(player.hasPermission("normal.permission")).thenReturn(true);
        CosmeticDefinition cosmetic = new CosmeticDefinition(
            "normal", "Normal", "join", CosmeticType.JOIN_MESSAGE,
            Material.PAPER, null, null, null, "&aHi", "normal.permission", 0, 0, 0, 0);

        assertTrue(service.canUseCosmetic(player, cosmetic));
    }

    @Test
    void operatorPermissionDoesNotBypassEntitlementCosmeticOwnership() {
        CosmeticsService service = new CosmeticsService(
            mock(JavaPlugin.class), mock(Messages.class), mock(PerformanceMonitor.class));
        Player player = mock(Player.class);
        when(player.hasPermission("enthusiatags.entitlement.donor_avid")).thenReturn(true);
        CosmeticDefinition cosmetic = new CosmeticDefinition(
            "join_avid_supporter", "Avid", "join", CosmeticType.JOIN_MESSAGE,
            Material.PAPER, null, null, null, "&aHi", "enthusiatags.entitlement.donor_avid", 0, 0, 0, 0);

        assertFalse(service.canUseCosmetic(player, cosmetic));
        service.setEntitlementAccess((subject, cosmeticId) -> "join_avid_supporter".equals(cosmeticId));
        assertTrue(service.canUseCosmetic(player, cosmetic));
    }
}
