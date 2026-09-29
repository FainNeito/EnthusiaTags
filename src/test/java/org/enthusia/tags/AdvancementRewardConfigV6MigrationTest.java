package org.enthusia.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class AdvancementRewardConfigV6MigrationTest {
    @Test
    void bundledAdvancementRewardsAndTagsAreCompleteAndBalanced() {
        YamlConfiguration config = resource("config.yml");
        YamlConfiguration rewards = resource("rewards.yml");
        assertEquals(6, config.getInt("config-version"));
        assertEquals(7, rewards.getInt("config-version"));
        assertTrue(rewards.isConfigurationSection("categories.advancements"));

        ConfigurationSection rewardSection = rewards.getConfigurationSection("rewards");
        ConfigurationSection tagSection = config.getConfigurationSection("tags");
        Set<String> rewardIds = rewardSection.getKeys(false).stream()
            .filter(id -> id.startsWith("adv_")).collect(java.util.stream.Collectors.toSet());
        Set<String> tagIds = tagSection.getKeys(false).stream()
            .filter(id -> id.startsWith("adv_")).collect(java.util.stream.Collectors.toSet());
        assertEquals(38, rewardIds.size());
        assertEquals(12, tagIds.size());

        long totalGold = 0L;
        for (String id : rewardIds) {
            String base = "rewards." + id;
            assertEquals("advancements", rewards.getString(base + ".category"));
            assertEquals("CUSTOM_COUNTER", rewards.getString(base + ".criteria.complete.type"));
            assertTrue(rewards.getString(base + ".criteria.complete.key", "")
                .startsWith("advancement_reward:"));
            if (rewards.contains(base + ".rewards.payout.amount")) {
                totalGold += rewards.getLong(base + ".rewards.payout.amount");
            }
            String tagId = rewards.getString(base + ".rewards.tag.id", "");
            if (!tagId.isBlank()) assertTrue(tagIds.contains(tagId), tagId);
        }
        assertEquals(16_850L, totalGold);
        assertFalse(rewards.contains("rewards.adv_commend_bad_reputation.rewards.payout"));
        assertFalse(rewards.contains("rewards.adv_commend_public_enemy.rewards.payout"));
    }

    @Test
    void v6MigrationAddsMissingSystemEntriesWithoutOverwritingExistingOnes() {
        YamlConfiguration defaultsConfig = resource("config.yml");
        YamlConfiguration defaultsRewards = resource("rewards.yml");
        YamlConfiguration config = new YamlConfiguration();
        YamlConfiguration rewards = new YamlConfiguration();
        config.set("tags.adv_clutch.display-name", "Custom Clutch");
        rewards.set("rewards.adv_warzone_gladiator.name", "Custom Gladiator");
        ConfigMigrator.MigrationReport report = new ConfigMigrator.MigrationReport();

        assertTrue(AdvancementRewardConfigV6Migration.migrateTags(config, defaultsConfig, report));
        assertTrue(AdvancementRewardConfigV6Migration.migrateRewards(rewards, defaultsRewards, report));

        assertEquals("Custom Clutch", config.getString("tags.adv_clutch.display-name"));
        assertEquals("Custom Gladiator", rewards.getString("rewards.adv_warzone_gladiator.name"));
        assertTrue(config.isConfigurationSection("tags.adv_postmaster"));
        assertTrue(rewards.isConfigurationSection("rewards.adv_diary_void_walker"));
        assertTrue(rewards.isConfigurationSection("categories.advancements"));
        for (String id : Set.of("legacy_beta_tester", "legacy_bug_hunter", "event_prologue_champion",
            "donor_avid", "donor_avid_founder", "donor_devotee", "donor_devotee_founder",
            "donor_glorious", "donor_glorious_founder")) {
            assertTrue(rewards.isConfigurationSection("rewards." + id), id);
        }
        assertTrue(rewards.isConfigurationSection("categories.supporter"));
        assertTrue(rewards.isConfigurationSection("categories.legacy"));
        assertTrue(rewards.isConfigurationSection("categories.events"));
    }

    @Test
    void existingV6RewardsGainNewEntriesWithoutReplacingCustomSupporterReward() {
        YamlConfiguration rewards = new YamlConfiguration();
        rewards.set("config-version", 6);
        rewards.set("rewards.donor_avid.name", "Custom Avid");
        var report = new ConfigMigrator.MigrationReport();

        assertTrue(AdvancementRewardConfigV6Migration.migrateRewards(
            rewards, resource("rewards.yml"), report));
        assertEquals("Custom Avid", rewards.getString("rewards.donor_avid.name"));
        assertTrue(rewards.isConfigurationSection("rewards.donor_glorious_founder"));
    }

    private static YamlConfiguration resource(String name) {
        var stream = AdvancementRewardConfigV6MigrationTest.class.getClassLoader().getResourceAsStream(name);
        if (stream == null) throw new IllegalStateException("Missing test resource " + name);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
