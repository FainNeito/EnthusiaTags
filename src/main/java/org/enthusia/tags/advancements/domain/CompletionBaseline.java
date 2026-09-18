package org.enthusia.tags.advancements.domain;

import java.util.HashSet;
import java.util.Set;

/** Session-local evidence. An unavailable read is never an incomplete baseline. */
public final class CompletionBaseline {
    private final Set<String> observedIncomplete = new HashSet<>();

    public boolean isLiveCompletion(String id, int verifiedProgress) {
        if (verifiedProgress < 0) return false;
        if (verifiedProgress < 1000) {
            observedIncomplete.add(id);
            return false;
        }
        return observedIncomplete.contains(id);
    }
}
