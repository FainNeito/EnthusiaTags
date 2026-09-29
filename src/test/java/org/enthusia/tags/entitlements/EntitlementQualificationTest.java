package org.enthusia.tags.entitlements;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class EntitlementQualificationTest {
    @Test
    void explicitPermissionQualifiesWithoutGroupLookup() {
        Player player = mock(Player.class);
        when(player.hasPermission("enthusiatags.reward.devotee")).thenReturn(true);
        when(player.isPermissionSet("enthusiatags.reward.devotee")).thenReturn(true);
        var definition = new EntitlementDefinition("donor_devotee", "Supporter", true,
            Set.of("devotee"), Set.of(), Set.of(), "enthusiatags.reward.devotee", Set.of("devotee"));

        assertTrue(EntitlementService.qualifies(definition, player, ""));
    }

    @Test
    void inheritedLuckPermsGroupFallbackQualifies() {
        Player player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(false);
        var definition = new EntitlementDefinition("donor_avid", "Supporter", true,
            Set.of("avid"), Set.of(), Set.of(), "enthusiatags.reward.avid",
            Set.of("avid", "devotee", "glorious"));

        assertTrue(EntitlementService.qualifies(definition, player, "devotee"));
        assertTrue(EntitlementService.qualifies(definition, player, "glorious"));
        assertFalse(EntitlementService.qualifies(definition, player, "default"));
    }

    @Test
    void groupPermissionFallbackWorksWhenPrimaryGroupIsUnavailable() {
        Player player = mock(Player.class);
        when(player.isPermissionSet("group.devotee-founder")).thenReturn(true);
        when(player.hasPermission("group.devotee-founder")).thenReturn(true);
        var definition = new EntitlementDefinition("donor_devotee_founder", "Founder", true,
            Set.of(), Set.of(), Set.of(), "enthusiatags.reward.devotee-founder",
            Set.of("devotee-founder", "glorious-founders"));

        assertTrue(EntitlementService.qualifies(definition, player, ""));
    }

    @Test void operatorDefaultsDoNotCountAsExplicitGroupMembership() {
        Player player = mock(Player.class);
        when(player.isOp()).thenReturn(true);
        when(player.hasPermission("enthusiatags.reward.devotee")).thenReturn(true);
        when(player.hasPermission("group.devotee")).thenReturn(true);
        when(player.isPermissionSet("group.devotee")).thenReturn(false);
        var definition = new EntitlementDefinition("donor_devotee", "Supporter", true,
            Set.of("devotee"), Set.of(), Set.of(), "enthusiatags.reward.devotee", Set.of("devotee"));
        assertFalse(EntitlementService.qualifies(definition, player, ""));
    }
}
