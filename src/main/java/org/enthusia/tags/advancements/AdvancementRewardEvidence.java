package org.enthusia.tags.advancements;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class AdvancementRewardEvidence {
    static final String COUNTER_PREFIX = "advancement_reward:";
    private static final List<String> PROVIDER_PREFIXES = List.of(
        "warzone_duels/",
        "reputation/",
        "express/",
        "diary/"
    );

    private AdvancementRewardEvidence() {}

    static Set<String> completed(Map<String, Integer> progress) {
        Set<String> result = new LinkedHashSet<>();
        if (progress == null || progress.isEmpty()) return result;
        for (Map.Entry<String, Integer> entry : progress.entrySet()) {
            String key = normalize(entry.getKey());
            Integer value = entry.getValue();
            if (value != null && value >= 1000 && isProviderKey(key)) {
                result.add(key);
            }
        }
        return Set.copyOf(result);
    }

    static String counterKey(String advancementKey) {
        String normalized = normalize(advancementKey);
        if (!isProviderKey(normalized)) {
            throw new IllegalArgumentException("Unsupported advancement reward key: " + advancementKey);
        }
        return COUNTER_PREFIX + normalized;
    }

    static boolean isProviderKey(String key) {
        if (key == null || key.isBlank()) return false;
        return PROVIDER_PREFIXES.stream().anyMatch(key::startsWith);
    }

    private static String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }
}
