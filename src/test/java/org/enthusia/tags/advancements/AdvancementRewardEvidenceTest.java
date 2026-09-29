package org.enthusia.tags.advancements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdvancementRewardEvidenceTest {
    @Test
    void onlyCompletedProviderMilestonesBecomeRewardEvidence() {
        Map<String, Integer> progress = new LinkedHashMap<>();
        progress.put("warzone_duels/gladiator", 1000);
        progress.put("reputation/redemption_arc", 1000);
        progress.put("express/postal_legend", 999);
        progress.put("diary/void_walker", 1001);
        progress.put("tags/1234", 1000);

        assertEquals(Set.of(
            "warzone_duels/gladiator",
            "reputation/redemption_arc",
            "diary/void_walker"
        ), AdvancementRewardEvidence.completed(progress));
    }

    @Test
    void counterKeysAreStableAndNamespaced() {
        assertEquals(
            "advancement_reward:express/first_class",
            AdvancementRewardEvidence.counterKey(" Express/First_Class ")
        );
        assertTrue(AdvancementRewardEvidence.isProviderKey("diary/dear_diary"));
        assertThrows(IllegalArgumentException.class,
            () -> AdvancementRewardEvidence.counterKey("tags/not-a-provider-node"));
    }
}
