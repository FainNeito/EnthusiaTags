package org.enthusia.tags;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ActivePlaytimeConfigTest {
    @TempDir Path folder;

    @Test
    void newerSchemaMigratesTimeWithoutDowngradeAndIsIdempotent() throws Exception {
        YamlConfiguration config = defaults();
        config.set("config-version", 8);
        config.set("rewards.first_hour.criteria.time.type", "PLAYTIME_TOTAL_MINUTES");
        config.set("rewards.first_hour.description", List.of("&7Spend your first hour on the server."));
        config.set("rewards.payday.criteria.c1.type", "playtime_total_minutes");
        config.set("rewards.payday.criteria.c1.amount", 900);
        config.set("rewards.payday.description", List.of("Custom wording"));
        config.set("rewards.custom.criteria.time.type", "PLAYTIME_AFK_MINUTES");
        config.set("rewards.custom.criteria.time.amount", 42);
        config.set("rewards.custom.rewards.payout.amount", 123);
        config.set("rewards.custom.criteria.blocks.type", "CUSTOM_COUNTER");
        config.set("rewards.custom.criteria.blocks.amount", 17);
        Path file = folder.resolve("rewards.yml");
        config.save(file.toFile());
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        when(plugin.getResource("rewards.yml")).thenAnswer(call -> getClass().getClassLoader().getResourceAsStream("rewards.yml"));
        ConfigMigrator migrator = new ConfigMigrator(plugin);
        migrator.migrate("rewards.yml", new ConfigMigrator.MigrationReport());
        YamlConfiguration actual = YamlConfiguration.loadConfiguration(file.toFile());
        assertEquals("PLAYTIME_ACTIVE_MINUTES", actual.getString("rewards.first_hour.criteria.time.type"));
        assertEquals("PLAYTIME_ACTIVE_MINUTES", actual.getString("rewards.payday.criteria.c1.type"));
        assertEquals("PLAYTIME_ACTIVE_MINUTES", actual.getString("rewards.custom.criteria.time.type"));
        assertEquals(8, actual.getInt("config-version"));
        assertEquals(900, actual.getInt("rewards.payday.criteria.c1.amount"));
        assertEquals(42, actual.getInt("rewards.custom.criteria.time.amount"));
        assertEquals(123, actual.getInt("rewards.custom.rewards.payout.amount"));
        assertEquals("CUSTOM_COUNTER", actual.getString("rewards.custom.criteria.blocks.type"));
        assertEquals(17, actual.getInt("rewards.custom.criteria.blocks.amount"));
        assertEquals(List.of("Custom wording"), actual.getStringList("rewards.payday.description"));
        assertEquals(defaults().getStringList("rewards.first_hour.description"), actual.getStringList("rewards.first_hour.description"));
        String once = Files.readString(file);
        migrator.migrate("rewards.yml", new ConfigMigrator.MigrationReport());
        assertEquals(once, Files.readString(file));
    }

    @Test
    void bundledRequirementsUseActiveTime() throws Exception {
        for (var entry : defaults().getValues(true).entrySet()) {
            if (!entry.getKey().endsWith(".type")) continue;
            assertNotEquals("PLAYTIME_TOTAL_MINUTES", entry.getValue(), entry.getKey());
            assertNotEquals("PLAYTIME_AFK_MINUTES", entry.getValue(), entry.getKey());
        }
    }

    private static YamlConfiguration defaults() throws Exception {
        try (var reader = new InputStreamReader(ActivePlaytimeConfigTest.class.getClassLoader().getResourceAsStream("rewards.yml"), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }
}
