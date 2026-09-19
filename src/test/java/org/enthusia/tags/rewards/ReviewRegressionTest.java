package org.enthusia.tags.rewards;
import java.lang.reflect.*;
import org.enthusia.tags.advancements.domain.CompletionBaseline;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReviewRegressionTest {
    private long parse(String raw) throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        var service=(RewardService)((sun.misc.Unsafe)field.get(null)).allocateInstance(RewardService.class);
        var method=RewardService.class.getDeclaredMethod("parseTokenizedPlaytimeMinutes",String.class); method.setAccessible(true);
        return (Long)method.invoke(service,raw);
    }
    @Test void duplicateHourAndMinuteTokensAreNotDropped() throws Exception {
        assertEquals(183,parse("1h 2h 1m 2m"));
    }
    @Test void repeatedSecondsAreCombinedBeforeRounding() throws Exception {
        assertEquals(1,parse("30s 30s"));
    }
    @Test void completionTransitionIsConsumedExactlyOnce() {
        var baseline=new CompletionBaseline();
        assertFalse(baseline.isLiveCompletion("new",999));
        assertTrue(baseline.isLiveCompletion("new",1000));
        assertFalse(baseline.isLiveCompletion("new",1000));
    }
}
