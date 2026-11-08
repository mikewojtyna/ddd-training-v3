package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.fee;

import org.jmolecules.ddd.annotation.Service;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.Tariff;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffSchedule;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone.Zone;

import java.util.Optional;
import java.util.stream.IntStream;

/// Calculates the total fee of a finished session from the tariff valid when it started:
///
/// - **hourly fees**: one per started billing hour after the free period, measured to the second (DE-31, DE-83);
///   hours 1, 2 and 3 have their own fee, every further hour costs the same;
/// - **return fee**: only for a free-floating ending, by the zone where the session ended (DE-34, DE-84);
/// - **penalty**: only when the session was ended for exceeding the maximum rental time (DE-33).
///
/// The zone is a fact the caller looked up on the zone map valid at the session's end; it is needed only for a
/// free-floating ending.
@Service
public class FeeCalculator {

    private static final long SECONDS_PER_HOUR = 3600;

    public SessionFee calculate(Session session, TariffSchedule schedule, Optional<Zone> zoneAtEnd) {
        var tariff = schedule.tariffValidAt(session.startedAt());
        var hourlyFees = IntStream.rangeClosed(1, billingHours(session, tariff))
                                  .mapToObj(tariff::hourlyFeeOf)
                                  .toList();
        var returnFeeKind = returnFeeKind(session, zoneAtEnd);
        return new SessionFee(tariff.id(), hourlyFees, returnFeeKind, returnFee(returnFeeKind, tariff),
                              penalty(session, tariff));
    }

    /// Every started hour after the free period, measured to the second (DE-31, DE-83).
    private static int billingHours(Session session, Tariff tariff) {
        var secondsAfterFreePeriod = session.duration().minus(tariff.freePeriod()).getSeconds();
        if (secondsAfterFreePeriod <= 0) {
            return 0;
        }
        return (int) ((secondsAfterFreePeriod + SECONDS_PER_HOUR - 1) / SECONDS_PER_HOUR);
    }

    private static ReturnFeeKind returnFeeKind(Session session, Optional<Zone> zoneAtEnd) {
        if (session.ending() != Ending.FREE_FLOATING) {
            return ReturnFeeKind.NONE;
        }
        var zone = zoneAtEnd.orElseThrow(
            () -> new IllegalArgumentException("A free-floating session needs the zone where it ended"));
        return switch (zone) {
            case DROP_ZONE -> ReturnFeeKind.DROP_ZONE;
            case UNAUTHORIZED_ZONE -> ReturnFeeKind.UNAUTHORIZED_ZONE;
        };
    }

    private static Money returnFee(ReturnFeeKind kind, Tariff tariff) {
        return switch (kind) {
            case NONE -> Money.zero(Tariff.PLN);
            case DROP_ZONE -> tariff.dropZoneFee();
            case UNAUTHORIZED_ZONE -> tariff.unauthorizedZoneFee();
        };
    }

    private static Money penalty(Session session, Tariff tariff) {
        return session.ending() == Ending.MAX_RENTAL_TIME ? tariff.penalty() : Money.zero(Tariff.PLN);
    }
}
