package org.enthusia.tags.advancements;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdvancementLayoutTest {
    @Test void everyBundledRewardHasAnIntentionalPlacement() throws Exception {
        YamlConfiguration config;
        try (var in = getClass().getClassLoader().getResourceAsStream("rewards.yml")) {
            assertNotNull(in);
            config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        var rewards = config.getConfigurationSection("rewards");
        assertNotNull(rewards);
        for (String id : rewards.getKeys(false)) {
            assertTrue(AdvancementLayout.hasFixed(id), id);
        }
    }

    @Test void fixedPlacementsDoNotOverlap() throws Exception {
        var coordinates = new HashSet<String>();
        String[] ids = bundledIds();
        for (String id : ids) {
            var placement = AdvancementLayout.placement(id, null, 99, 99);
            assertTrue(
                coordinates.add(placement.x() + ":" + placement.y()),
                id + " overlaps another bundled advancement");
        }
    }

    @Test void branchesUseExpectedParentsAndCompactBounds() {
        var combat = AdvancementLayout.placement("deadeye", null, 99, 99);
        assertEquals("arrow_storm", combat.parentId());
        assertEquals(11, combat.x());
        assertEquals(10, combat.y());

        var economy = AdvancementLayout.placement("baltop_top3", null, 99, 99);
        assertEquals("wealthy", economy.parentId());
        assertEquals(4, economy.x());
        assertEquals(16, economy.y());

        assertEquals(29, AdvancementLayout.warzoneBaseY(7));
        assertEquals(33, AdvancementLayout.warzoneBaseY(8));
    }

    @Test void unknownFutureRewardKeepsLinearFallback() {
        var fallback = AdvancementLayout.placement("future_reward", "previous", 7, 31);
        assertEquals("previous", fallback.parentId());
        assertEquals(7, fallback.x());
        assertEquals(31, fallback.y());
    }
    private String[] bundledIds() throws Exception {
        YamlConfiguration config;
        try (var in = getClass().getClassLoader().getResourceAsStream("rewards.yml")) {
            assertNotNull(in);
            config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        var rewards = config.getConfigurationSection("rewards");
        assertNotNull(rewards);
        return rewards.getKeys(false).toArray(String[]::new);
    }
}
