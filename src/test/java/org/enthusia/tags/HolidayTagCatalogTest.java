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
        // Event reward ids plus the staff-awarded seasonal builder recognition.
        assertEquals(Set.of("pumpkin_hunter", "no_pumpkin_left_behind", "present_seeker",
            "home_for_the_holidays", "advent_keeper", "secret_santa", "seen_the_watcher", "pumpkin_king", "peppermint_royalty", "pumpkin_patcher", "remembered"), ids);
    }

    @Test
    void pumpkinKingWearsThePumpkinGlyphOnBothSides() {
        HolidayTagCatalog.HolidayTag king = HolidayTagCatalog.tags().stream()
            .filter(tag -> tag.id().equals("pumpkin_king")).findFirst().orElseThrow();
        // U+A0A0 is the pumpkin_king_icon glyph in resourcepack/holiday-tags (Nexo); kept outside <bold> so it isn't drawn twice.
        assertTrue(king.displayName().startsWith("\uA0A0<bold>"), king.displayName());
        assertTrue(king.displayName().endsWith("</bold>\uA0A0"), king.displayName());
        assertTrue(king.displayName().contains("Pumpkin King"), king.displayName());
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

    @Test
    void christmasTagsNamePeppermintsAndUpgradeUneditedOldDefaults() {
        EnthusiaTagsPlugin plugin = mock(EnthusiaTagsPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        // A server installed before the rename still has the untouched present wording.
        config.set("tags.present_seeker.display-name", "<gradient:#c0392b:#27ae60>Present Seeker</gradient>");
        config.set("tags.present_seeker.tag-text", "<gradient:#c0392b:#27ae60>Present Seeker</gradient>");
        config.set("tags.present_seeker.description", List.of("&7Found 12 presents in a Christmas hunt"));
        config.set("tags.present_seeker.icon", "CHEST_MINECART");
        // An administrator's own wording is kept.
        config.set("tags.home_for_the_holidays.description", List.of("&7Our own words"));
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("HolidayTagCatalogTest"));

        assertTrue(HolidayTagCatalog.ensureInstalled(plugin));

        assertEquals("<gradient:#c0392b:#27ae60>Home for the Holidays</gradient>", config.getString("tags.present_seeker.display-name"));
        assertEquals("<gradient:#c0392b:#27ae60>Home for the Holidays</gradient>", config.getString("tags.present_seeker.tag-text"));
        assertEquals(List.of("&7Found 12 peppermints in a Christmas hunt"), config.getStringList("tags.present_seeker.description"));
        assertEquals(List.of("&7Our own words"), config.getStringList("tags.home_for_the_holidays.description"));
        assertFalse(HolidayTagCatalog.ensureInstalled(plugin), "the upgrade runs once");
    }

    @Test
    void anEditedPresentSeekerNameIsNotRenamed() {
        EnthusiaTagsPlugin plugin = mock(EnthusiaTagsPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("tags.present_seeker.display-name", "<green>Gift Finder");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("HolidayTagCatalogTest"));

        HolidayTagCatalog.ensureInstalled(plugin);

        assertEquals("<green>Gift Finder", config.getString("tags.present_seeker.display-name"));
    }

    @Test
    void mintConditionIsNowTheFullClearTagAndHomeForTheHolidaysTheTwelveFindOne() {
        EnthusiaTagsPlugin plugin = mock(EnthusiaTagsPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        // A server installed before the swap: untouched names and icons on both tiers.
        config.set("tags.present_seeker.display-name", "<gradient:#c0392b:#27ae60>Mint Condition</gradient>");
        config.set("tags.present_seeker.tag-text", "<gradient:#c0392b:#27ae60>Mint Condition</gradient>");
        config.set("tags.present_seeker.icon", "CHEST_MINECART");
        config.set("tags.home_for_the_holidays.display-name", "<bold><gradient:#c0392b:#27ae60>Home for the Holidays</gradient>");
        config.set("tags.home_for_the_holidays.tag-text", "<bold><gradient:#c0392b:#27ae60>Home for the Holidays</gradient>");
        config.set("tags.home_for_the_holidays.icon", "CAMPFIRE");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("HolidayTagCatalogTest"));

        assertTrue(HolidayTagCatalog.ensureInstalled(plugin));

        // The ids stay on their tiers (12 finds, all 25), so tags earned in earlier years keep meaning the same.
        assertEquals("<gradient:#c0392b:#27ae60>Home for the Holidays</gradient>", config.getString("tags.present_seeker.tag-text"));
        assertEquals("CAMPFIRE", config.getString("tags.present_seeker.icon"));
        // U+A0A2 is the peppermint glyph (EnthusiaHolidays christmas/find), outside <bold> like the Pumpkin King's.
        assertEquals("\uA0A2<bold><gradient:#c0392b:#27ae60>Mint Condition</gradient></bold>\uA0A2",
            config.getString("tags.home_for_the_holidays.tag-text"));
        assertEquals("SUGAR", config.getString("tags.home_for_the_holidays.icon"));
        assertFalse(HolidayTagCatalog.ensureInstalled(plugin), "the upgrade runs once");
    }

    @Test
    void theFirstToFindEveryPeppermintBecomesPeppermintRoyalty() {
        HolidayTagCatalog.HolidayTag royalty = HolidayTagCatalog.tags().stream()
            .filter(tag -> tag.id().equals("peppermint_royalty")).findFirst().orElseThrow();
        // U+A0A3 is the crowned peppermint (resourcepack/holiday-tags), on both sides like the Pumpkin King's pumpkin.
        assertEquals("\uA0A3<bold><gradient:#e8283a:#fff6ee>Peppermint Royalty</gradient></bold>\uA0A3", royalty.displayName());
        assertEquals(List.of("&7The first to find every peppermint in a Christmas hunt"), List.of(royalty.description()));
    }

    @Test
    void thePeppermintGlyphsAreInTheHolidayTagsPackOnTheirOwnCharacters() throws Exception {
        java.nio.file.Path root = java.nio.file.Path.of("resourcepack/holiday-tags/Nexo");
        YamlConfiguration glyphs = YamlConfiguration.loadConfiguration(root.resolve("glyphs/enthusia_holiday_tags.yml").toFile());
        assertEquals("\uA0A2", glyphs.getString("peppermint_find_icon.char"));
        assertEquals("\uA0A3", glyphs.getString("peppermint_royalty_icon.char"));
        assertEquals("minecraft:default", glyphs.getString("peppermint_royalty_icon.font"));
        YamlConfiguration patcher = YamlConfiguration.loadConfiguration(root.resolve("glyphs/pumpkin_patcher.yml").toFile());
        assertEquals("\uA0A1", patcher.getString("pumpkin_patcher_seeds.char"), "the seeds keep U+A0A1");
        for (String png : List.of("peppermint.png", "peppermint_royalty.png")) {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(root.resolve("pack/assets/enthusia/textures/font/tags/" + png).toFile());
            assertEquals(16, image.getWidth(), png);
            assertEquals(16, image.getHeight(), png);
        }
    }

    @Test
    void peppermintRoyaltyIsInTheChristmasRewardsCategory() {
        YamlConfiguration rewards = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(
            getClass().getClassLoader().getResourceAsStream("rewards.yml"), java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(rewards.getStringList("categories.christmas.tags").contains("peppermint_royalty"));
    }
}
