package org.enthusia.tags;

import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;

/** One-time update of the supporter presentation leaves; player records are never involved. */
final class SupporterPresentationMigration {
    private static final String VERSION = "supporter-presentation-version";
    private static final List<String> TAG_IDS = List.of(
        "avid", "founding_avid", "devotee", "founding_devotee", "glorious_fellow", "founding_glorious");
    private static final List<String> PRESENCE_IDS = List.of(
        "avid_supporter", "founding_avid", "devotee_supporter", "founding_devotee",
        "glorious_legacy", "founding_glorious");

    private SupporterPresentationMigration() {}

    static boolean needsUpdate(String resource, YamlConfiguration config) {
        return ("config.yml".equals(resource) || "cosmetics.yml".equals(resource))
            && config.getInt(VERSION, 0) < 1;
    }

    static boolean migrate(String resource, YamlConfiguration config, YamlConfiguration defaults,
                           ConfigMigrator.MigrationReport report) {
        if (!needsUpdate(resource, config)) return false;
        if ("config.yml".equals(resource)) {
            for (String id : TAG_IDS) {
                update(config, defaults, "tags." + id, "display-name");
                update(config, defaults, "tags." + id, "tag-text");
            }
        } else {
            for (String id : PRESENCE_IDS) {
                update(config, defaults, "cosmetics.join_" + id, "message");
                update(config, defaults, "cosmetics.quit_" + id, "message");
            }
        }
        config.set(VERSION, 1);
        report.migrated(resource + ": updated supporter tags/presence presentation");
        return true;
    }

    private static void update(YamlConfiguration config, YamlConfiguration defaults,
                               String section, String key) {
        // Retain intentionally removed definitions and all other administrator settings.
        if (!config.isConfigurationSection(section)) return;
        String path = section + "." + key;
        config.set(path, Objects.requireNonNull(defaults.getString(path), "Missing default " + path));
    }
}
