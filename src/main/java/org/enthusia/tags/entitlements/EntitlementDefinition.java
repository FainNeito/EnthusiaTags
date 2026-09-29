package org.enthusia.tags.entitlements;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record EntitlementDefinition(
    String id,
    String source,
    boolean permanent,
    Set<String> tags,
    Set<String> cosmetics,
    Set<String> activeCosmetics,
    String permission,
    Set<String> groups
) {
    public EntitlementDefinition {
        id = normalize(id);
        source = source == null ? "" : source;
        tags = normalizeSet(tags);
        cosmetics = normalizeSet(cosmetics);
        activeCosmetics = normalizeSet(activeCosmetics);
        permission = permission == null ? "" : permission.trim().toLowerCase(Locale.ROOT);
        groups = normalizeSet(groups);
    }

    public boolean hasQualification() {
        return !permission.isBlank() || !groups.isEmpty();
    }

    private static Set<String> normalizeSet(Set<String> values) {
        if (values == null || values.isEmpty()) return Set.of();
        return values.stream().filter(java.util.Objects::nonNull)
            .map(EntitlementDefinition::normalize).filter(value -> !value.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
