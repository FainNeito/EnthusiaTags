package org.enthusia.tags.rewards;

import java.util.UUID;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaytimeHookTest {
    @Test
    void unavailableProviderIsNotAuthoritativeZero() {
        PlaytimeHook hook = new PlaytimeHook();
        assertThrows(IllegalStateException.class,
            () -> hook.getMinutes(UUID.randomUUID(), RewardCriterionType.PLAYTIME_ACTIVE_MINUTES));
    }

    @Test
    void emptyInvalidAndThrowingReadsNeverBecomeZero() {
        Provider provider = new Provider();
        PlaytimeHook hook = new PlaytimeHook(() -> provider);
        assertTrue(hook.readMinutes(UUID.randomUUID(), RewardCriterionType.PLAYTIME_ACTIVE_MINUTES).isEmpty());
        provider.value = "unexpected provider response";
        assertTrue(hook.readMinutes(UUID.randomUUID(), RewardCriterionType.PLAYTIME_ACTIVE_MINUTES).isEmpty());
        provider.fail = true;
        assertThrows(PlaytimeHook.ProgressUnavailableException.class,
            () -> hook.getMinutes(UUID.randomUUID(), RewardCriterionType.PLAYTIME_ACTIVE_MINUTES));
        provider.fail = false;
        provider.value = Optional.of(new Snapshot());
        assertEquals(42, hook.getMinutes(UUID.randomUUID(), RewardCriterionType.PLAYTIME_ACTIVE_MINUTES));
        assertEquals(0, hook.getMinutes(UUID.randomUUID(), RewardCriterionType.PLAYTIME_AFK_MINUTES));
        assertEquals(60, hook.getMinutes(UUID.randomUUID(), RewardCriterionType.PLAYTIME_TOTAL_MINUTES));
    }

    @Test
    void replacementProviderAndNegativeValuesAreHandled() {
        Provider initial = new Provider();
        Provider replacement = new Provider();
        var reference = new java.util.concurrent.atomic.AtomicReference<Object>(initial);
        PlaytimeHook hook = new PlaytimeHook(reference::get);
        assertTrue(hook.readMinutes(UUID.randomUUID(), RewardCriterionType.PLAYTIME_ACTIVE_MINUTES).isEmpty());
        Snapshot snapshot = new Snapshot();
        snapshot.activeMinutes = -1;
        replacement.value = Optional.of(snapshot);
        reference.set(replacement);
        assertTrue(hook.readMinutes(UUID.randomUUID(), RewardCriterionType.PLAYTIME_ACTIVE_MINUTES).isEmpty());
        snapshot.activeMinutes = 100;
        assertEquals(100, hook.getMinutes(UUID.randomUUID(), RewardCriterionType.PLAYTIME_ACTIVE_MINUTES));
        reference.set(null);
        assertTrue(hook.readMinutes(UUID.randomUUID(), RewardCriterionType.PLAYTIME_ACTIVE_MINUTES).isEmpty());
    }

    public static final class Provider {
        Object value = Optional.empty();
        boolean fail;
        public Object getLifetime(UUID player) {
            if (fail) throw new IllegalStateException("database unavailable");
            return value;
        }
        public String getLiveState(UUID player) { return "ACTIVE"; }
    }

    @Test
    void placeholderErrorsAreNotConvertedToProgress() throws Exception {
        RewardService service = new RewardService(null, null, null, new org.enthusia.tags.PerformanceMonitor(null));
        var parse = RewardService.class.getDeclaredMethod("parsePlaytimeMinutes", String.class, String.class);
        parse.setAccessible(true);
        for (String invalid : new String[] { "", "%playtime_123_minutes%", "database error 503", "-20", "NaN", "999999999999999999999h" }) {
            assertEquals(-1L, parse.invoke(service, invalid, "%playtime_minutes%"), invalid);
        }
        assertEquals(0L, parse.invoke(service, "0", "%playtime_minutes%"));
        assertEquals(90L, parse.invoke(service, "1h 30m", "%playtime_minutes%"));
        assertEquals(1000L, parse.invoke(service, "1,000", "%enthusiaplaytime_active_minutes%"));
    }

    public static final class Snapshot {
        public long activeMinutes = 42;
        public long afkMinutes = 0;
        public long totalMinutes = 60;
    }
}
