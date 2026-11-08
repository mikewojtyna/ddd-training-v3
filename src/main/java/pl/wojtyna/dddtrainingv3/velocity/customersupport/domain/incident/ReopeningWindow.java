package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;
import java.time.ZoneId;

/// The time a rider has to reopen a rejected incident: from the rejection until the end of the 14th calendar day
/// after the rejection day, in the city's local time (DE-54, DE-56). `closesAt` is the first instant at which
/// reopening is no longer possible (00:00 local time of the 15th day).
@ValueObject
public record ReopeningWindow(Instant rejectedAt, Instant closesAt) {

    static final int CALENDAR_DAYS = 14;

    public static ReopeningWindow afterRejectionAt(Instant rejectedAt, ZoneId cityZone) {
        var firstDayAfterWindow = rejectedAt.atZone(cityZone).toLocalDate().plusDays(CALENDAR_DAYS + 1);
        return new ReopeningWindow(rejectedAt, firstDayAfterWindow.atStartOfDay(cityZone).toInstant());
    }

    public boolean isOpenAt(Instant moment) {
        return moment.isBefore(closesAt);
    }
}
