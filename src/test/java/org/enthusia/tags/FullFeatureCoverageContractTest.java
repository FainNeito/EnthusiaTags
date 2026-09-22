package org.enthusia.tags;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Guards the major production feature families against silent loss of regression coverage.
 *
 * <p>This is an inventory contract, not a replacement for behavioral assertions. A new feature
 * must add a real test first, then add that evidence to this map.</p>
 */
final class FullFeatureCoverageContractTest {
    @Test
    void everyMajorFeatureFamilyRetainsConcreteRegressionEvidence() {
        Path root = repositoryRoot();
        coverage().forEach((feature, evidence) -> evidence.forEach(path -> assertTrue(
                Files.isRegularFile(root.resolve(path)),
                () -> feature + " lost required regression evidence: " + path
        )));
    }

    private static Map<String, List<String>> coverage() {
        Map<String, List<String>> coverage = new LinkedHashMap<>();
        coverage.put("tag text compatibility and dynamic-value escaping", List.of(
                "src/test/java/org/enthusia/tags/TagTextFormatTest.java"
        ));
        coverage.put("placeholder resolution and safe rendering", List.of(
                "src/test/java/org/enthusia/tags/CachedTagPlaceholderResolverTest.java",
                "src/test/java/org/enthusia/tags/PlaceholderApiHookTest.java"
        ));
        coverage.put("nametag refresh/runtime renderer lifecycle", List.of(
                "src/test/java/org/enthusia/tags/NametagRefreshIntegrationTest.java",
                "src/test/java/org/enthusia/tags/RendererRetirementTest.java"
        ));
        coverage.put("tag configuration migration", List.of(
                "src/test/java/org/enthusia/tags/TagConfigV5MigrationTest.java"
        ));
        coverage.put("cosmetic selection persistence", List.of(
                "src/test/java/org/enthusia/tags/cosmetics/CosmeticsStorageTest.java"
        ));
        coverage.put("daily reward ledger, reset and payout rules", List.of(
                "src/test/java/org/enthusia/tags/daily/DailyStorageTest.java",
                "src/test/java/org/enthusia/tags/daily/DailyRulesTest.java",
                "src/test/java/org/enthusia/tags/daily/DailyPayoutsTest.java"
        ));
        coverage.put("reward anti-farm and natural-block accounting", List.of(
                "src/test/java/org/enthusia/tags/rewards/KillFarmLimiterTest.java",
                "src/test/java/org/enthusia/tags/rewards/NaturalBlockPolicyTest.java",
                "src/test/java/org/enthusia/tags/rewards/NaturalBlockStorageTest.java"
        ));
        coverage.put("reward persistence/reentrancy/recovery", List.of(
                "src/test/java/org/enthusia/tags/rewards/RewardStorageReentrancyTest.java",
                "src/test/java/org/enthusia/tags/rewards/RewardStorageLoreItemRecoveryTest.java"
        ));
        coverage.put("reward configuration and money policy", List.of(
                "src/test/java/org/enthusia/tags/rewards/RewardConfigPolicyTest.java",
                "src/test/java/org/enthusia/tags/rewards/RewardMoneyPolicyTest.java"
        ));
        coverage.put("LoreItems handoff idempotency and recovery", List.of(
                "src/test/java/org/enthusia/tags/rewards/loreitems/LoreItemHandoffCoordinatorTest.java",
                "src/test/java/org/enthusia/tags/rewards/loreitems/LoreItemHandoffStoreTest.java",
                "src/test/java/org/enthusia/tags/rewards/loreitems/LoreItemsEndToEndIdempotencyTest.java"
        ));
        coverage.put("released LoreItems API architecture contract", List.of(
                "src/test/java/org/enthusia/tags/rewards/loreitems/ReleasedLoreItemsApiContractTest.java",
                "src/test/java/org/enthusia/tags/rewards/loreitems/LoreItemsArchitectureTest.java"
        ));
        return Map.copyOf(coverage);
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if (Files.isRegularFile(current.resolve("pom.xml"))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.isRegularFile(parent.resolve("pom.xml"))) {
            return parent;
        }
        throw new IllegalStateException("Could not locate EnthusiaTags repository root from " + current);
    }
}
