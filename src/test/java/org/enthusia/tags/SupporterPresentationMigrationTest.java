package org.enthusia.tags;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupporterPresentationMigrationTest {
    private YamlConfiguration defaults(String name) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
            getClass().getResourceAsStream("/" + name), StandardCharsets.UTF_8));
    }

    @Test void updatesOnlyPresentationAndPreservesLaterAdministratorEdits() {
        var config = defaults("config.yml");
        config.set("supporter-presentation-version", null);
        config.set("tags.avid.tag-text", "old avid");
        config.set("tags.avid.icon", "DIAMOND");
        config.set("tags.custom.tag-text", "Keep custom");
        config.set("daily.payouts", java.util.List.of(11, 22));
        var report = new ConfigMigrator.MigrationReport();
        assertTrue(SupporterPresentationMigration.migrate("config.yml", config, defaults("config.yml"), report));
        assertEquals("Avidian", TagTextFormat.plainText(config.getString("tags.avid.tag-text")));
        assertEquals("DIAMOND", config.getString("tags.avid.icon"));
        assertEquals("Keep custom", config.getString("tags.custom.tag-text"));
        assertEquals(java.util.List.of(11, 22), config.getIntegerList("daily.payouts"));
        config.set("tags.avid.tag-text", "My later edit");
        assertFalse(SupporterPresentationMigration.migrate("config.yml", config, defaults("config.yml"), report));
        assertEquals("My later edit", config.getString("tags.avid.tag-text"));
        assertFalse(SupporterPresentationMigration.needsUpdate("rewards.yml", new YamlConfiguration()));
        assertFalse(SupporterPresentationMigration.needsUpdate("entitlements.yml", new YamlConfiguration()));
    }

    @Test void backsUpExistingMessagesAndPreservesPermissions(@TempDir Path directory) throws Exception {
        var config = defaults("cosmetics.yml");
        config.set("supporter-presentation-version", null);
        config.set("cosmetics.join_avid_supporter.message", "OLD");
        config.set("cosmetics.join_avid_supporter.permission", "custom.permission");
        Path file = directory.resolve("cosmetics.yml");
        config.save(file.toFile());
        String before = Files.readString(file);
        JavaPlugin plugin = plugin(directory);
        new ConfigMigrator(plugin).migrate("cosmetics.yml", new ConfigMigrator.MigrationReport());
        var after = YamlConfiguration.loadConfiguration(file.toFile());
        assertEquals(1, after.getInt("supporter-presentation-version"));
        assertEquals("custom.permission", after.getString("cosmetics.join_avid_supporter.permission"));
        assertEquals(defaults("cosmetics.yml").getString("cosmetics.join_avid_supporter.message"),
            after.getString("cosmetics.join_avid_supporter.message"));
        try (var backups = Files.list(directory.resolve("backups"))) {
            var paths = backups.toList();
            assertEquals(1, paths.size());
            assertEquals(before, Files.readString(paths.getFirst()));
        }
    }

    @Test void backupFailureLeavesExistingConfigurationUntouched(@TempDir Path directory) throws Exception {
        var config = defaults("cosmetics.yml");
        config.set("supporter-presentation-version", null);
        Path file = directory.resolve("cosmetics.yml");
        config.save(file.toFile());
        String before = Files.readString(file);
        Files.writeString(directory.resolve("backups"), "blocked directory");
        var report = new ConfigMigrator.MigrationReport();
        new ConfigMigrator(plugin(directory)).migrate("cosmetics.yml", report);
        assertEquals(before, Files.readString(file));
        assertTrue(report.summaryLines().stream().anyMatch(s -> s.contains("migration failed")));
    }

    private JavaPlugin plugin(Path directory) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getResource("cosmetics.yml"))
            .thenAnswer(call -> getClass().getResourceAsStream("/cosmetics.yml"));
        return plugin;
    }
}
