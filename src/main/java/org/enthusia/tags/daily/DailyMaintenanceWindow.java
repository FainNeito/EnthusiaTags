package org.enthusia.tags.daily;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;

record DailyMaintenanceWindow(ZonedDateTime start, ZonedDateTime end, boolean preserveStreaks) {
    DailyMaintenanceWindow {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Daily maintenance end must be after start");
        }
    }

    boolean activeAt(ZonedDateTime now) {
        Objects.requireNonNull(now, "now");
        ZonedDateTime comparable = now.withZoneSameInstant(start.getZone());
        return !comparable.isBefore(start) && comparable.isBefore(end);
    }

    boolean preserves(LocalDate date) {
        Objects.requireNonNull(date, "date");
        if (!preserveStreaks) {
            return false;
        }
        LocalDate firstAffectedDate = start.toLocalDate();
        LocalDate lastAffectedDate = end.minusNanos(1L).toLocalDate();
        return !date.isBefore(firstAffectedDate) && !date.isAfter(lastAffectedDate);
    }
}
