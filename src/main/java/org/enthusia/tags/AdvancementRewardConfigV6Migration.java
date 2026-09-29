package org.enthusia.tags;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

final class AdvancementRewardConfigV6Migration {
    private static final List<String> TAG_IDS = List.of(
        "adv_clutch", "adv_unstoppable", "adv_gladiator",
        "adv_well_regarded", "adv_infamous", "adv_public_enemy",
        "adv_redeemed", "adv_courier", "adv_postmaster",
        "adv_writer", "adv_indestructible", "adv_void_walker"
    );

    private static final List<String> REWARD_IDS = List.of(
        "adv_warzone_welcome_to_thunderdome", "adv_warzone_first_blood",
        "adv_warzone_victor_spoils", "adv_warzone_price_for_peace",
        "adv_warzone_my_house_my_rules", "adv_warzone_adapt_and_overcome",
        "adv_warzone_not_even_close", "adv_warzone_unstoppable", "adv_warzone_gladiator",
        "adv_commend_a_good_word", "adv_commend_well_regarded",
        "adv_commend_pillar_of_the_community", "adv_commend_kind_soul",
        "adv_commend_generous_spirit", "adv_commend_trusted_name",
        "adv_commend_merchant_of_merit", "adv_commend_bad_reputation",
        "adv_commend_public_enemy", "adv_commend_redemption_arc",
        "adv_express_first_class", "adv_express_care_package", "adv_express_frequent_shipper",
        "adv_express_postal_legend", "adv_express_youve_got_mail",
        "adv_express_parcel_collector", "adv_express_return_to_sender",
        "adv_express_pen_pal", "adv_express_correspondent",
        "adv_express_read_all_about_it", "adv_express_avid_reader",
        "adv_diary_dear_diary", "adv_diary_first_entry", "adv_diary_prolific_writer",
        "adv_diary_finders_keepers", "adv_diary_indestructible", "adv_diary_stubborn",
        "adv_diary_void_walker", "adv_diary_nice_try"
    );

    private AdvancementRewardConfigV6Migration() {}

    static boolean migrateTags(YamlConfiguration target, YamlConfiguration defaults,
                               ConfigMigrator.MigrationReport report) {
        boolean changed = false;
        for (String id : TAG_IDS) {
            changed |= copyMissingSection(defaults, target, "tags." + id, "config.yml", report);
        }
        return changed;
    }

    static boolean migrateRewards(YamlConfiguration target, YamlConfiguration defaults,
                                  ConfigMigrator.MigrationReport report) {
        boolean changed = copyMissingSection(
            defaults, target, "categories.advancements", "rewards.yml", report);
        for (String id : REWARD_IDS) {
            changed |= copyMissingSection(defaults, target, "rewards." + id, "rewards.yml", report);
        }
        return changed;
    }

    private static boolean copyMissingSection(
        YamlConfiguration defaults,
        YamlConfiguration target,
        String path,
        String resource,
        ConfigMigrator.MigrationReport report
    ) {
        if (target.contains(path)) return false;
        ConfigurationSection source = defaults.getConfigurationSection(path);
        if (source == null) return false;
        copySection(source, target, path);
        report.added(resource + ": added advancement reward defaults " + path);
        return true;
    }

    private static void copySection(ConfigurationSection source, YamlConfiguration target, String path) {
        for (String key : source.getKeys(false)) {
            Object value = source.get(key);
            String childPath = path + "." + key;
            if (value instanceof ConfigurationSection child) {
                copySection(child, target, childPath);
            } else {
                target.set(childPath, value);
            }
        }
    }
}
