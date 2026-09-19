package org.enthusia.tags.advancements;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.enthusia.tags.advancements.domain.DiaryMilestoneProgress;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Strict read-only adapter for DiaryKeeper's persisted advancement evidence. */
final class DiaryStatsReader {
    private DiaryStatsReader() {
    }

    static Map<UUID, DiaryMilestoneProgress.Stats> parse(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            return Map.of();
        }

        Map<UUID, DiaryMilestoneProgress.Stats> result = new HashMap<>();
        for (String key : players.getKeys(false)) {
            UUID player = UUID.fromString(key);
            if (!player.toString().equalsIgnoreCase(key)) {
                throw new IllegalArgumentException("Invalid DiaryKeeper player UUID");
            }
            result.put(player, parsePlayer(players.getConfigurationSection(key)));
        }
        return Map.copyOf(result);
    }
    private static DiaryMilestoneProgress.Stats parsePlayer(ConfigurationSection player) {
        if (player == null) {
            throw new IllegalArgumentException("Invalid DiaryKeeper player record");
        }
        long issuedAt = player.getLong("issuedAt", 0L);
        ConfigurationSection evidence = player.getConfigurationSection("advancements");
        if (evidence == null) {
            return new DiaryMilestoneProgress.Stats(
                issuedAt > 0, 0, 0, 0, 0, 0);
        }

        boolean received = evidence.contains("received")
            ? booleanValue(evidence.get("received"))
            : issuedAt > 0;
        return new DiaryMilestoneProgress.Stats(
            received,
            counter(evidence, "edits"),
            counter(evidence, "destructionAttempts"),
            counter(evidence, "voidReturns"),
            counter(evidence, "containerAttempts"),
            counter(evidence, "groundPickups")
        );
    }

    private static int counter(ConfigurationSection section, String key) {
        Object value = section.get(key);
        if (value == null) return 0;
        if (!(value instanceof Integer result) || result < 0) {
            throw new IllegalArgumentException("Invalid DiaryKeeper counter: " + key);
        }
        return result;
    }
    private static boolean booleanValue(Object value) {
        if (!(value instanceof Boolean result)) {
            throw new IllegalArgumentException("Invalid DiaryKeeper boolean");
        }
        return result;
    }
}
