package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.Tariff;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffSchedule;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffScheduleRepository;

import java.util.List;

/// Every published tariff, ordered by valid from.
@PrimaryPort
public class TariffsQueryHandler {

    private final TariffScheduleRepository schedules;

    public TariffsQueryHandler(TariffScheduleRepository schedules) {
        this.schedules = schedules;
    }

    public List<TariffView> handle(TariffsQuery query) {
        return schedules.load(TariffSchedule.VELOCITY)
                        .map(schedule -> schedule.tariffs().stream().map(TariffsQueryHandler::toView).toList())
                        .orElse(List.of());
    }

    private static TariffView toView(Tariff tariff) {
        return new TariffView(tariff.id().value(),
                              tariff.validFrom().toString(),
                              (int) tariff.freePeriod().toMinutes(),
                              decimal(tariff.firstHourFee()),
                              decimal(tariff.secondHourFee()),
                              decimal(tariff.thirdHourFee()),
                              decimal(tariff.furtherHourFee()),
                              decimal(tariff.dropZoneFee()),
                              decimal(tariff.unauthorizedZoneFee()),
                              decimal(tariff.penalty()));
    }

    static String decimal(Money money) {
        return money.getAmount().toPlainString();
    }
}
