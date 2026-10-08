package org.enthusia.tags;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HolidayTagCatalogTest {
    @Test
    void catalogHasTheEnthusiaHolidaysRewardTags() {
        Set<String> ids = new HashSet<>();
        for (HolidayTagCatalog.HolidayTag tag : HolidayTagCatalog.tags()) {
            assertTrue(ids.add(tag.id()), "duplicate tag id: " + tag.id());
            assertEquals(tag.id().toLowerCase(), tag.id(), "tag give lowercases ids");
        }
        // Ids EnthusiaHolidays' bundled events grant with "tag: <id>".
        assertEquals(Set.of("pumpkin_hunter", "no_pumpkin_left_behind", "present_seeker",
            "home_for_the_holidays", "advent_keeper", "secret_santa", "seen_the_watcher"), ids);
    }

    @Test
    void installAddsMissingHolidayTagsWithoutOverwritingCustomization() {
        EnthusiaTagsPlugin plugin = mock(EnthusiaTagsPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("tags.secret_santa.display-name", "<red>Custom Santa");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("HolidayTagCatalogTest"));

        assertTrue(HolidayTagCatalog.ensureInstalled(plugin));

        assertEquals("<red>Custom Santa", config.getString("tags.secret_santa.display-name"));
        assertEquals("CHEST", config.getString("tags.secret_santa.icon"));
        assertEquals(List.of("&7Sent a Secret Santa gift"), config.getStringList("tags.secret_santa.description"));
        assertEquals("CARVED_PUMPKIN", config.getString("tags.no_pumpkin_left_behind.icon"));
        // Holiday tags are granted directly; they are not permission entitlements.
        assertNull(config.getString("tags.no_pumpkin_left_behind.entitlement-permission"));
        verify(plugin).saveConfig();

        assertFalse(HolidayTagCatalog.ensureInstalled(plugin));
        verify(plugin).saveConfig();
        verify(plugin, never()).reloadConfig();
    }
}
