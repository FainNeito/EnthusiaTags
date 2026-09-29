package org.enthusia.tags.daily;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DailyMaintenancePolicyTest {
    private static final ZoneId ZONE = ZoneId.of("America/Indiana/Indianapolis");
    private static final DailyMaintenanceWindow CHAPTER_TWO = new DailyMaintenanceWindow(
        ZonedDateTime.of(2026, 9, 28, 7, 0, 0, 0, ZONE),
        ZonedDateTime.of(2026, 10, 1, 17, 0, 0, 0, ZONE),
        true);

    @Test
    void maintenanceWindowUsesInclusiveStartAndExclusiveEnd() {
        assertAll(
            () -> assertFalse(CHAPTER_TWO.activeAt(
                ZonedDateTime.of(2026, 9, 28, 6, 59, 59, 0, ZONE))),
            () -> assertTrue(CHAPTER_TWO.activeAt(
                ZonedDateTime.of(2026, 9, 28, 7, 0, 0, 0, ZONE))),
            () -> assertTrue(CHAPTER_TWO.activeAt(
                ZonedDateTime.of(2026, 10, 1, 16, 59, 59, 0, ZONE))),
            () -> assertFalse(CHAPTER_TWO.activeAt(
                ZonedDateTime.of(2026, 10, 1, 17, 0, 0, 0, ZONE)))
        );
    }

    @Test
    void everyLocalDateTouchedByMaintenanceCanProtectAStreak() {
        assertAll(
            () -> assertFalse(CHAPTER_TWO.preserves(LocalDate.of(2026, 9, 27))),
            () -> assertTrue(CHAPTER_TWO.preserves(LocalDate.of(2026, 9, 28))),
            () -> assertTrue(CHAPTER_TWO.preserves(LocalDate.of(2026, 9, 30))),
            () -> assertTrue(CHAPTER_TWO.preserves(LocalDate.of(2026, 10, 1))),
            () -> assertFalse(CHAPTER_TWO.preserves(LocalDate.of(2026, 10, 2)))
        );
    }

    @Test
    void chapterTwoGapContinuesExistingStreak() {
        int next = DailyRules.nextStreak(LocalDate.of(2026, 9, 27),
            LocalDate.of(2026, 10, 2), 37, List.of(CHAPTER_TWO));

        assertEquals(38, next);
    }

    @Test
    void missingTheFirstNormalDayAfterMaintenanceStillResets() {
        int next = DailyRules.nextStreak(LocalDate.of(2026, 9, 27),
            LocalDate.of(2026, 10, 3), 37, List.of(CHAPTER_TWO));

        assertEquals(1, next);
    }

    @Test
    void aMissBeforeMaintenanceIsNotForgiven() {
        int next = DailyRules.nextStreak(LocalDate.of(2026, 9, 26),
            LocalDate.of(2026, 10, 2), 37, List.of(CHAPTER_TWO));

        assertEquals(1, next);
    }

    @Test
    void maintenanceWindowDatesUseTheSameZoneAsDailyClaims() {
        ZoneId claimZone = ZoneId.of("America/New_York");
        assertEquals(claimZone, DailyMaintenancePolicy.claimZone(
            claimZone.getId(), "UTC", java.util.logging.Logger.getAnonymousLogger()));
    }
}
