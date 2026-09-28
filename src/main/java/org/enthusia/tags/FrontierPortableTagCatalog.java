package org.enthusia.tags;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * System-owned presentation definitions for the portable Frontier firsts.
 * Missing values are installed without overwriting administrator edits.
 */
final class FrontierPortableTagCatalog {
    private static final List<SystemTag> TAGS = List.of(
        tag("frontier_first_diamonds", "<bold><#5FD3FF>Diamond Pioneer", "DIAMOND",
            "&7First to obtain Diamonds on Frontier Test.", "enthusia.frontier.first.diamonds"),
        tag("frontier_first_nether", "<bold><#FF6B6B>Nether Pioneer", "NETHERRACK",
            "&7First to enter the Nether on Frontier Test.", "enthusia.frontier.first.nether"),
        tag("frontier_first_fortress", "<bold><#B54832>Fortress Founder", "NETHER_BRICKS",
            "&7First Nether Fortress discovery on Frontier Test.", "enthusia.frontier.first.fortress"),
        tag("frontier_first_ancient_debris", "<bold><#8A5A44>Ancient Miner", "ANCIENT_DEBRIS",
            "&7First Ancient Debris on Frontier Test.", "enthusia.frontier.first.ancient_debris"),
        tag("frontier_first_netherite_ingot", "<bold><#8B8B95>Netherite Pioneer", "NETHERITE_INGOT",
            "&7First Netherite Ingot on Frontier Test.", "enthusia.frontier.first.netherite_ingot"),
        tag("frontier_first_netherite_armor", "<bold><#6F6F7A>Netherite Vanguard", "NETHERITE_CHESTPLATE",
            "&7First full Netherite armor set on Frontier Test.", "enthusia.frontier.first.netherite_armor"),
        tag("frontier_first_stronghold", "<bold><#C690FF>Stronghold Scout", "ENDER_EYE",
            "&7First Stronghold discovery on Frontier Test.", "enthusia.frontier.first.stronghold"),
        tag("frontier_first_mace", "<bold><#D0D0D0>Heavy Hitter", "MACE",
            "&7First legitimate Mace on Frontier Test.", "enthusia.frontier.first.mace"),
        tag("frontier_first_end", "<bold><#8B7CFF>End Pioneer", "END_STONE",
            "&7First to enter The End on Frontier Test.", "enthusia.frontier.first.end"),
        tag("frontier_first_dragon", "<bold><#E04BFF>Dragonbreaker", "DRAGON_HEAD",
            "&7First credited Ender Dragon final blow on Frontier Test.", "enthusia.frontier.first.dragon"),
        tag("frontier_first_elytra", "<bold><#7EE7FF>First Flight", "ELYTRA",
            "&cLOCKED: Elytras are unobtainable on Frontier Test.", "enthusia.frontier.first.elytra"),
        tag("frontier_first_wither", "<bold><#8B8B8B>Witherbreaker", "WITHER_SKELETON_SKULL",
            "&7First credited Wither final blow on Frontier Test.", "enthusia.frontier.first.wither"),
        tag("frontier_first_beacon", "<bold><#F7F29A>Beacon Pioneer", "BEACON",
            "&7First Beacon activation on Frontier Test.", "enthusia.frontier.first.beacon"),
        tag("frontier_first_iron", "<bold><#D9D9D9>Iron Pioneer", "IRON_INGOT",
            "&7First Iron Ingot on Frontier Test.", "enthusia.frontier.first.iron"),
        tag("frontier_first_obsidian", "<bold><#664A8A>Obsidian Pioneer", "OBSIDIAN",
            "&7First Obsidian on Frontier Test.", "enthusia.frontier.first.obsidian"),
        tag("frontier_first_blaze_rod", "<bold><#FFB347>Blaze Pioneer", "BLAZE_ROD",
            "&7First Blaze Rod on Frontier Test.", "enthusia.frontier.first.blaze_rod"),
        tag("frontier_first_heavy_core", "<bold><#4FC3F7>Heavy Core Pioneer", "HEAVY_CORE",
            "&7First Heavy Core on Frontier Test.", "enthusia.frontier.first.heavy_core"),
        tag("frontier_first_dragon_egg", "<bold><#B36CFF>Egg Holder", "DRAGON_EGG",
            "&7First Dragon Egg on Frontier Test.", "enthusia.frontier.first.dragon_egg"),
        tag("frontier_first_totem", "<bold><#FFD166>Undying Pioneer", "TOTEM_OF_UNDYING",
            "&7First Totem of Undying on Frontier Test.", "enthusia.frontier.first.totem"),
        tag("frontier_first_god_apple", "<bold><#FFD700>Golden Legend", "ENCHANTED_GOLDEN_APPLE",
            "&7First Enchanted Golden Apple on Frontier Test.", "enthusia.frontier.first.god_apple"),
        tag("frontier_first_full_beacon", "<bold><#FFF3A3>Beaconator", "BEACON",
            "&7First fully powered Beacon on Frontier Test.", "enthusia.frontier.first.full_beacon")
    );

    private FrontierPortableTagCatalog() {
    }

    static boolean ensureInstalled(EnthusiaTagsPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        boolean changed = false;
        for (SystemTag tag : TAGS) {
            String root = "tags." + tag.id();
            changed |= setIfMissing(config, root + ".display-name", tag.displayName());
            changed |= setIfMissing(config, root + ".tag-text", tag.displayName());
            changed |= setIfMissing(config, root + ".icon", tag.icon());
            changed |= setIfMissing(config, root + ".description", List.of(tag.description()));
            changed |= setIfMissing(config, root + ".entitlement-permission", tag.permission());
        }
        if (changed) {
            plugin.saveConfig();
            plugin.getLogger().info("Installed missing portable Frontier reward tag definitions.");
        }
        return changed;
    }

    static List<SystemTag> tags() {
        return TAGS;
    }

    private static boolean setIfMissing(FileConfiguration config, String path, Object value) {
        if (config.contains(path)) {
            return false;
        }
        config.set(path, value);
        return true;
    }

    private static SystemTag tag(String id, String displayName, String icon,
                                 String description, String permission) {
        return new SystemTag(id, displayName, icon, description, permission);
    }

    record SystemTag(String id, String displayName, String icon, String description, String permission) {
    }
}
