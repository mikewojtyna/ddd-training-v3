package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.fee.Ending;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.fee.FeeCalculator;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.fee.Session;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffSchedule;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffScheduleRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone.Position;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone.Zone;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone.ZoneMap;

import java.util.Optional;

import static pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.TariffsQueryHandler.decimal;

/// *Price for Session*: loads the tariff schedule, asks the City's zone map (valid at the session's end) for the zone
/// of a free-floating ending only, and lets {@link FeeCalculator} price the session.
///
/// Malformed input (unknown ending, end not after start, free-floating without a position) is an
/// {@link IllegalArgumentException}.
@PrimaryPort
public class PriceForSessionQueryHandler {

    private final TariffScheduleRepository schedules;
    private final ZoneMap zoneMap;
    private final FeeCalculator calculator;

    public PriceForSessionQueryHandler(TariffScheduleRepository schedules, ZoneMap zoneMap, FeeCalculator calculator) {
        this.schedules = schedules;
        this.zoneMap = zoneMap;
        this.calculator = calculator;
    }

    public SessionPrice handle(PriceForSession query) {
        var session = new Session(query.startedAt(), query.endedAt(), ending(query.ending()));
        var schedule = schedules.load(TariffSchedule.VELOCITY)
                                .orElseThrow(() -> new IllegalStateException("The tariff schedule isn't established"));
        var fee = calculator.calculate(session, schedule, zoneAtEnd(session, query));
        return new SessionPrice(fee.tariffId().value(),
                                fee.billingHours(),
                                decimal(fee.hourlyFeesTotal()),
                                decimal(fee.returnFee()),
                                fee.returnFeeKind().name(),
                                decimal(fee.penalty()),
                                decimal(fee.total()));
    }

    private Optional<Zone> zoneAtEnd(Session session, PriceForSession query) {
        if (session.ending() != Ending.FREE_FLOATING) {
            return Optional.empty();
        }
        if (query.latitude() == null || query.longitude() == null) {
            throw new IllegalArgumentException("A free-floating session needs the position where it ended");
        }
        return Optional.of(zoneMap.zoneAt(new Position(query.latitude(), query.longitude()), session.endedAt()));
    }

    private static Ending ending(String ending) {
        if (ending == null) {
            throw new IllegalArgumentException("The ending is required");
        }
        try {
            return Ending.valueOf(ending);
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException("Unknown ending: " + ending, unknown);
        }
    }
}
