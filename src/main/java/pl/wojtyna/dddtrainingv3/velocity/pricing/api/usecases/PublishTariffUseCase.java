package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.PricingManagerId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.Tariff;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffSchedule;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffScheduleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;

/// A pricing manager publishes a tariff: load the schedule, publish now, save, publish the events.
@PrimaryPort
public class PublishTariffUseCase {

    private final TariffScheduleRepository schedules;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public PublishTariffUseCase(TariffScheduleRepository schedules, DomainEventPublisher eventPublisher, Clock clock) {
        this.schedules = schedules;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public void handle(PublishTariff command) {
        var tariff = new Tariff(new TariffId(command.tariffId()),
                                command.validFrom(),
                                command.freeMinutes() == null ? null : Duration.ofMinutes(command.freeMinutes()),
                                pln(command.firstHourFee()),
                                pln(command.secondHourFee()),
                                pln(command.thirdHourFee()),
                                pln(command.furtherHourFee()),
                                pln(command.dropZoneFee()),
                                pln(command.unauthorizedZoneFee()),
                                pln(command.penalty()));
        var schedule = schedules.load(TariffSchedule.VELOCITY)
                                .orElseThrow(() -> new IllegalStateException("The tariff schedule isn't established"));
        var events = schedule.publish(tariff)
                             .by(PricingManagerId.ofNullable(command.publishedBy()))
                             .at(clock.instant());
        schedules.save(schedule);
        eventPublisher.publish(events);
    }

    /// A missing price stays missing (the domain rejects the tariff); more than two decimals is malformed input.
    private static Money pln(BigDecimal amount) {
        return amount == null ? null : Money.of(Tariff.PLN, amount, RoundingMode.UNNECESSARY);
    }
}
