package org.enthusia.tags;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Seasonal event and staff-recognition tags granted with {@code tag give <player> <id>} (REQ-910).
 * Ids are year-agnostic so next year's events reuse them. Missing values are installed
 * without overwriting administrator edits, like {@link FrontierPortableTagCatalog}.
 */
final class HolidayTagCatalog {
    private static final List<HolidayTag> DEFINITIONS = List.of(
        tag("pumpkin_patcher", "\uA0A1<bold><gradient:#82c6a2:#eee7c8>Pumpkin Patcher</gradient></bold>\uA0A1", "NAME_TAG",
            "&7Staff recognition for placing all 31 pumpkins around spawn"),
        tag("pumpkin_hunter", "<bold><gradient:#ff7b00:#ffb347>Pumpkin Hunter</gradient></bold>", "PUMPKIN",
            "&7Found 15 pumpkins in a Halloween hunt"),
        tag("no_pumpkin_left_behind", "<bold><gradient:#ff7b00:#8a2be2>No Pumpkin Left Behind</gradient>",
            "CARVED_PUMPKIN", "&7Found every pumpkin in a Halloween hunt"),
        // Ids stay on their tiers so tags earned in earlier years keep their meaning: present_seeker is 12 finds,
        // home_for_the_holidays is every find. The 2026 names swapped (owner, 2026-11-18): the full clear is Mint
        // Condition, with the peppermint glyph \uA0A2 (EnthusiaHolidays christmas/find) outside <bold>.
        tag("present_seeker", "<gradient:#c0392b:#27ae60>Home for the Holidays</gradient>", "CAMPFIRE",
            "&7Found 12 peppermints in a Christmas hunt"),
        tag("home_for_the_holidays", "\uA0A2<bold><gradient:#c0392b:#27ae60>Mint Condition</gradient></bold>\uA0A2",
            "SUGAR", "&7Found every peppermint in a Christmas hunt"),
        tag("advent_keeper", "<#f4d03f>Advent Keeper", "CLOCK",
            "&7Opened the last door of the Advent calendar"),
        tag("secret_santa", "<#e74c3c>Secret <#27ae60>Santa", "CHEST",
            "&7Sent a Secret Santa gift"),
        tag("seen_the_watcher", "<#9aa0a6>Seen the <#f5f5f5>Watcher", "ENDER_EYE",
            "&7Caught a glimpse of the Watcher in the dark"),
        tag("remembered", "<#a7adb4>Remembered", "NAME_TAG",
            "&7The Last Watch"),
        // \uA0A0 is the pumpkin_king_icon glyph (resourcepack/holiday-tags), outside <bold> so it isn't drawn twice.
        tag("pumpkin_king", "\uA0A0<bold><gradient:#FF3A00:#FF5A00>Pumpkin King</gradient></bold>\uA0A0", "JACK_O_LANTERN",
            "&7The first to find every pumpkin in a Halloween hunt"),
        // \uA0A3 is the crowned peppermint (resourcepack/holiday-tags): the Christmas hunt's first finishers.
        tag("peppermint_royalty", "\uA0A3<bold><gradient:#e8283a:#fff6ee>Peppermint Royalty</gradient></bold>\uA0A3", "GOLDEN_HELMET",
            "&7The first to find every peppermint in a Christmas hunt")
    );

    /**
     * Earlier built-in values, by config path (oldest first). A value still equal to one of these was never edited,
     * so it is upgraded to the current default; anything an administrator changed is left alone.
     */
    private static final Map<String, List<Object>> RETIRED_DEFAULTS = Map.of(
        "tags.present_seeker.display-name", List.of("<gradient:#c0392b:#27ae60>Present Seeker</gradient>",
            "<gradient:#c0392b:#27ae60>Mint Condition</gradient>"),
        "tags.present_seeker.tag-text", List.of("<gradient:#c0392b:#27ae60>Present Seeker</gradient>",
            "<gradient:#c0392b:#27ae60>Mint Condition</gradient>"),
        "tags.present_seeker.description", List.of(List.of("&7Found 12 presents in a Christmas hunt")),
        "tags.present_seeker.icon", List.of("CHEST_MINECART"),
        "tags.home_for_the_holidays.display-name", List.of("<bold><gradient:#c0392b:#27ae60>Home for the Holidays</gradient>"),
        "tags.home_for_the_holidays.tag-text", List.of("<bold><gradient:#c0392b:#27ae60>Home for the Holidays</gradient>"),
        "tags.home_for_the_holidays.description", List.of(List.of("&7Found every present in a Christmas hunt")),
        "tags.home_for_the_holidays.icon", List.of("CAMPFIRE")
    );

    private HolidayTagCatalog() {
    }

    static boolean ensureInstalled(EnthusiaTagsPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        boolean changed = false;
        for (HolidayTag tag : DEFINITIONS) {
            String root = "tags." + tag.id();
            changed |= install(config, root + ".display-name", tag.displayName());
            changed |= install(config, root + ".tag-text", tag.displayName());
            changed |= install(config, root + ".icon", tag.icon());
            changed |= install(config, root + ".description", List.of(tag.description()));
        }
        if (changed) {
            plugin.saveConfig();
            plugin.getLogger().info("Installed missing holiday reward tag definitions.");
        }
        return changed;
    }

    static List<HolidayTag> tags() {
        return DEFINITIONS;
    }

    private static boolean install(FileConfiguration config, String path, Object value) {
        if (config.contains(path) && !isRetiredDefault(config, path)) {
            return false;
        }
        config.set(path, value);
        return true;
    }

    /** True when the value is still one of this path's earlier built-in defaults (a description is a list). */
    private static boolean isRetiredDefault(FileConfiguration config, String path) {
        for (Object retired : RETIRED_DEFAULTS.getOrDefault(path, List.of())) {
            Object current = retired instanceof List<?> ? config.getStringList(path) : config.getString(path);
            if (retired.equals(current)) {
                return true;
            }
        }
        return false;
    }

    private static HolidayTag tag(String id, String displayName, String icon, String description) {
        return new HolidayTag(id, displayName, icon, description);
    }

    record HolidayTag(String id, String displayName, String icon, String description) {
    }
}
