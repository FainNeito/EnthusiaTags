package org.enthusia.tags;

import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** Move existing playtime requirements to active minutes without changing thresholds or rewards. */
final class ActivePlaytimeRewardMigration {
    private static final Map<String, List<String>> OLD_DESCRIPTIONS = Map.of(
        "first_hour", List.of("&7Spend your first hour on the server."),
        "payday", List.of("&7Play 5 hours total."),
        "market_access", List.of("&7Unlock stall access."));

    private ActivePlaytimeRewardMigration() {}

    static boolean needsUpdate(YamlConfiguration config, YamlConfiguration defaults) {
        for (var entry : config.getValues(true).entrySet()) {
            if (entry.getKey().startsWith("rewards.") && entry.getKey().contains(".criteria.")
                && entry.getKey().endsWith(".type") && isLegacyTime(String.valueOf(entry.getValue()))) return true;
        }
        return OLD_DESCRIPTIONS.entrySet().stream().anyMatch(entry -> needsDescription(config, defaults, entry));
    }

    private static boolean isLegacyTime(String type) {
        return "PLAYTIME_TOTAL_MINUTES".equalsIgnoreCase(type) || "PLAYTIME_AFK_MINUTES".equalsIgnoreCase(type);
    }

    private static boolean needsDescription(YamlConfiguration config, YamlConfiguration defaults,
                                            Map.Entry<String, List<String>> entry) {
        String path = "rewards." + entry.getKey() + ".description";
        return config.getStringList(path).equals(entry.getValue()) && defaults.contains(path)
            && !config.getStringList(path).equals(defaults.getStringList(path));
    }

    static boolean migrate(YamlConfiguration config, YamlConfiguration defaults,
                           ConfigMigrator.MigrationReport report) {
        ConfigurationSection rewards = config.getConfigurationSection("rewards");
        if (rewards == null) return false;
        boolean changed = false;
        for (String id : rewards.getKeys(false)) {
            ConfigurationSection criteria = rewards.getConfigurationSection(id + ".criteria");
            if (criteria == null) continue;
            for (String key : criteria.getKeys(false)) {
                String type = criteria.getString(key + ".type", "");
                if (!isLegacyTime(type)) continue;
                criteria.set(key + ".type", "PLAYTIME_ACTIVE_MINUTES");
                report.migrated("rewards.yml: active playtime requirement for " + id + "." + key);
                changed = true;
            }
        }
        for (var entry : OLD_DESCRIPTIONS.entrySet()) {
            String path = "rewards." + entry.getKey() + ".description";
            if (needsDescription(config, defaults, entry)) {
                config.set(path, defaults.getStringList(path));
                changed = true;
            }
        }
        return changed;
    }
}
