package org.enthusia.tags.advancements.application;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Main-thread polling orchestration; no blocking wait, SQL, or renderer dependency. */
public final class CompletionHistory {
    private final CompletionLedger ledger;
    private final UUID player;
    private final Set<String> known = new HashSet<>();
    private final Set<String> authorized = new HashSet<>();
    private CompletableFuture<Set<String>> loading;
    private boolean loaded;
    private CompletableFuture<Set<String>> writing;
    private Set<String> submitted = Set.of();
    private Set<String> live = Set.of();

    public CompletionHistory(CompletionLedger ledger, UUID player) {
        this.ledger = ledger;
        this.player = player;
    }

    /** Failed operations throw after clearing their future, making the next poll retryable. */
    public boolean reconcile(Map<String, Integer> progress, Set<String> pending, Set<String> registered) {
        if (!load()) return false;
        if (writing != null) {
            if (!writing.isDone()) return false;
            var finished = writing;
            writing = null;
            Set<String> inserted = finished.join();
            known.addAll(submitted);
            inserted.stream().filter(live::contains).forEach(authorized::add);
        }
        known.stream().filter(registered::contains).forEach(key -> progress.put(key, 1000));
        pending.removeIf(key -> registered.contains(key) && known.contains(key) && !authorized.contains(key));
        Set<String> completed = new HashSet<>();
        progress.forEach((key, value) -> {
            if (registered.contains(key) && value == 1000 && !known.contains(key)) completed.add(key);
        });
        if (completed.isEmpty()) return true;
        submitted = Set.copyOf(completed);
        live = new HashSet<>(pending);
        live.retainAll(completed);
        writing = ledger.record(player, submitted);
        return false;
    }

    private boolean load() {
        if (loaded) return true;
        if (loading == null) loading = ledger.load(player);
        if (!loading.isDone()) return false;
        var finished = loading;
        loading = null;
        known.addAll(finished.join());
        loaded = true;
        return true;
    }

    public void acknowledge(String key) { authorized.remove(key); }
}
