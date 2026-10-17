package org.enthusia.tags;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FrontierPortableTagCatalogTest {
    @Test
    void catalogUsesUniqueTagIdsAndPermissions() {
        Set<String> ids = new HashSet<>();
        Set<String> permissions = new HashSet<>();

        for (FrontierPortableTagCatalog.SystemTag tag : FrontierPortableTagCatalog.tags()) {
            assertTrue(ids.add(tag.id()), "duplicate tag id: " + tag.id());
            assertTrue(permissions.add(tag.permission()), "duplicate permission: " + tag.permission());
            assertTrue(tag.id().startsWith("frontier_first_"));
            assertTrue(tag.permission().startsWith("enthusia.frontier.first."));
        }

        assertEquals(21, ids.size());
        assertTrue(ids.contains("frontier_first_dragon"));
        assertTrue(ids.contains("frontier_first_mace"));
        assertTrue(ids.contains("frontier_first_netherite_armor"));
        assertTrue(permissions.contains("enthusia.frontier.first.dragon"));
        assertTrue(permissions.contains("enthusia.frontier.first.mace"));
    }

    @Test
    void installAddsMissingSystemTagsWithoutOverwritingCustomization() {
        EnthusiaTagsPlugin plugin = mock(EnthusiaTagsPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("tags.frontier_first_dragon.display-name", "<gold>Custom Dragon Winner");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("FrontierPortableTagCatalogTest"));

        assertTrue(FrontierPortableTagCatalog.ensureInstalled(plugin));

        assertEquals("<gold>Custom Dragon Winner",
            config.getString("tags.frontier_first_dragon.display-name"));
        assertEquals("enthusia.frontier.first.dragon",
            config.getString("tags.frontier_first_dragon.entitlement-permission"));
        assertEquals("<bold><#D0D0D0>Heavy Hitter",
            config.getString("tags.frontier_first_mace.display-name"));
        assertEquals("enthusia.frontier.first.mace",
            config.getString("tags.frontier_first_mace.entitlement-permission"));
        verify(plugin).saveConfig();

        assertFalse(FrontierPortableTagCatalog.ensureInstalled(plugin));
        verify(plugin).saveConfig();
        verify(plugin, never()).reloadConfig();
    }
}
