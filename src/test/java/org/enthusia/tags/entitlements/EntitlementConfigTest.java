package org.enthusia.tags.entitlements;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class EntitlementConfigTest {
    private final YamlConfiguration entitlements =
        YamlConfiguration.loadConfiguration(new File("src/main/resources/entitlements.yml"));

    @Test
    void rosterAndSpecialLegacyAwardsAreUuidBound() {
        ConfigurationSection grants = entitlements.getConfigurationSection("legacy-grants");
        assertNotNull(grants);
        assertEquals(93, grants.getKeys(false).size(), "92 beta testers plus Flomish");

        int betaCount = 0;
        Set<String> names = new HashSet<>();
        for (String uuid : grants.getKeys(false)) {
            names.add(grants.getString(uuid + ".name", ""));
            if (grants.getStringList(uuid + ".entitlements").contains("beta_tester")) betaCount++;
        }
        assertEquals(92, betaCount);
        assertFalse(names.contains("Minhoo"));
        assertFalse(names.contains("Marshy_"));
        assertFalse(names.contains("ImMoonbow"));
        assertFalse(names.contains("JBOB_X_"));

        assertEquals("kostpits13_", grants.getString(
            "f53b9522-15e1-405c-bb2e-8170b55b20ca.name"));
        assertTrue(grants.getStringList(
            "f53b9522-15e1-405c-bb2e-8170b55b20ca.entitlements").contains("beta_tester"));

        assertEquals("Flomish", grants.getString(
            "28ccd780-76f7-4f68-95f6-195b41eaae10.name"));
        assertEquals(java.util.List.of("prologue_champion"), grants.getStringList(
            "28ccd780-76f7-4f68-95f6-195b41eaae10.entitlements"));
    }

    @Test
    void bugHunterHallOfFamePreservesPlacementAndCounts() {
        assertBugHunter("8042f1c4-b41b-443b-bdcf-c0433f2b206b", "placement=1;bugs=49");
        assertBugHunter("aff43171-336c-49ff-947b-c48daa8da812", "placement=2;bugs=13");
        assertBugHunter("9ea84cce-9fea-4a22-bdcf-301509b8de10", "placement=3;bugs=6");
    }

    @Test
    void donorDefinitionsSeparatePermanentAndActiveCosmetics() {
        assertTrue(entitlements.getStringList("definitions.donor_avid.cosmetics")
            .contains("join_avid_supporter"));
        assertTrue(entitlements.getStringList("definitions.donor_avid.active-cosmetics")
            .contains("kill_sparks"));

        assertTrue(entitlements.getStringList("definitions.donor_devotee.cosmetics")
            .contains("join_devotee_supporter"));
        assertTrue(entitlements.getStringList("definitions.donor_devotee.active-cosmetics")
            .contains("join_flashy"));

        assertTrue(entitlements.getStringList("definitions.donor_glorious.cosmetics")
            .contains("join_glorious_legacy"));
        assertEquals(java.util.List.of("trail_glorious_current"),
            entitlements.getStringList("definitions.donor_glorious.active-cosmetics"));
    }

    @Test
    void rewardsBrowserHasSupporterLegacyAndEventEntriesWithoutLoreItems() {
        YamlConfiguration rewards =
            YamlConfiguration.loadConfiguration(new File("src/main/resources/rewards.yml"));
        assertNotNull(rewards.getConfigurationSection("categories.supporter"));
        assertNotNull(rewards.getConfigurationSection("categories.legacy"));
        assertNotNull(rewards.getConfigurationSection("categories.events"));

        for (String id : java.util.List.of(
            "legacy_beta_tester", "event_prologue_champion", "legacy_bug_hunter",
            "donor_avid", "donor_avid_founder", "donor_devotee",
            "donor_devotee_founder", "donor_glorious", "donor_glorious_founder")) {
            ConfigurationSection reward = rewards.getConfigurationSection("rewards." + id);
            assertNotNull(reward, id);
            ConfigurationSection actions = reward.getConfigurationSection("rewards");
            assertNotNull(actions, id);
            for (String action : actions.getKeys(false)) {
                assertNotEquals("LORE_ITEM", actions.getString(action + ".type"), id);
            }
        }
    }

    @Test
    void flomishAndBugHunterCosmeticsMatchRequestedDesign() {
        YamlConfiguration cosmetics =
            YamlConfiguration.loadConfiguration(new File("src/main/resources/cosmetics.yml"));
        String join = cosmetics.getString("cosmetics.join_prologue_champion.message");
        assertEquals("<dark_red><bold>Billionaire Redstone Genius philosopher leading Mart Clan; Flomish has arrived</bold></dark_red>", join);
        String legacyJoin = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
            .character('&').hexColors().build().serialize(org.enthusia.tags.TagTextFormat.deserializeCompat(join));
        assertTrue(legacyJoin.startsWith("&4&l"), legacyJoin);
        assertFalse(join.toLowerCase(java.util.Locale.ROOT).contains("millionaire"));
        assertFalse(join.toLowerCase(java.util.Locale.ROOT).contains("wise philosopher"));
        String leave = cosmetics.getString("cosmetics.quit_prologue_champion.message");
        assertNotNull(leave);
        assertTrue(leave.contains("[Flomish]"));
        assertTrue(leave.contains("please come back soon king"));
        assertTrue(leave.contains("\n"), "YAML should deserialize the requested two-line message");
        assertEquals("HAPPY_VILLAGER", cosmetics.getString("cosmetics.trail_bug_hunter.particle"));
    }

    @Test
    void donorQualificationPermissionsAreDeclared() {
        YamlConfiguration plugin =
            YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));
        for (String permission : java.util.List.of(
            "enthusiatags.reward.avid", "enthusiatags.reward.avid-founder",
            "enthusiatags.reward.devotee", "enthusiatags.reward.devotee-founder",
            "enthusiatags.reward.glorious", "enthusiatags.reward.glorious-founder")) {
            assertNotNull(plugin.getConfigurationSection("permissions." + permission), permission);
            assertFalse(plugin.getBoolean("permissions." + permission + ".default"), permission);
        }
    }

    private void assertBugHunter(String uuid, String metadata) {
        assertTrue(entitlements.getStringList("legacy-grants." + uuid + ".entitlements")
            .contains("bug_hunter_hall_of_fame"));
        assertEquals(metadata, entitlements.getString(
            "legacy-grants." + uuid + ".metadata.bug_hunter_hall_of_fame"));
    }
}
