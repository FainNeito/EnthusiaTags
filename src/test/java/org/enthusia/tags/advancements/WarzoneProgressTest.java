package org.enthusia.tags.advancements;

import java.util.Set;
import org.enthusia.tags.advancements.domain.DuelMilestoneProgress;
import org.enthusia.tags.advancements.domain.DuelMilestoneProgress.Stats;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WarzoneProgressTest {
    @Test void historicalCompletionsAreSilent() {
        var result = new DuelMilestoneProgress().observe(new Stats(50, 5));
        assertEquals(3, result.progress().size());
        assertTrue(result.progress().values().stream().allMatch(v -> v == 1000));
        assertTrue(result.celebrate().isEmpty());
    }
    @Test void onlyNewThresholdsCelebrateAndNeverReplay() {
        var policy = new DuelMilestoneProgress();
        policy.observe(new Stats(0, 0));
        assertEquals(Set.of("warzone_duels/first_blood"), policy.observe(new Stats(1, 1)).celebrate());
        assertEquals(Set.of("warzone_duels/unstoppable"), policy.observe(new Stats(5, 5)).celebrate());
        assertEquals(Set.of("warzone_duels/gladiator"), policy.observe(new Stats(50, 5)).celebrate());
        assertTrue(policy.observe(new Stats(50, 5)).celebrate().isEmpty());
    }
    @Test void failedMissingOrRegressedObservationsRetainProgress() {
        var policy = new DuelMilestoneProgress();
        assertTrue(policy.observe(null).progress().isEmpty());
        var historical = policy.observe(new Stats(49, 4));
        assertEquals(980, historical.progress().get("warzone_duels/gladiator"));
        assertEquals(800, historical.progress().get("warzone_duels/unstoppable"));
        assertEquals(historical.progress(), policy.observe(null).progress());
        assertEquals(historical.progress(), policy.observe(new Stats(1, 1)).progress());
        assertTrue(policy.observe(new Stats(49, 4)).celebrate().isEmpty());
    }
    @Test void reconnectIsSilentAndLargeCountersDoNotOverflow() {
        var update = new DuelMilestoneProgress().observe(new Stats(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(3, update.progress().size());
        assertTrue(update.progress().values().stream().allMatch(v -> v == 1000));
        assertTrue(update.celebrate().isEmpty());
    }
    @Test void invalidStatisticsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Stats(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Stats(1, 2));
    }
}
