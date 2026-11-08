package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.*;

import java.util.Optional;

/// A fault report from Rentals (it withdrew the bike) or from a fleet-issue incident. The bike is recorded broken
/// first; only then, if the domain asks for it, Rentals is asked to withdraw it. A failed withdrawal call is logged
/// and doesn't undo the record: the register must never miss a broken bike (plan
/// fleet-operations-takes-broken-bike-out-of-circulation D6).
@PrimaryPort
public class ReportBikeBrokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReportBikeBrokenUseCase.class);

    private final Bikes bikes;
    private final BikeWithdrawals withdrawals;
    private final DomainEventPublisher eventPublisher;

    public ReportBikeBrokenUseCase(Bikes bikes, BikeWithdrawals withdrawals, DomainEventPublisher eventPublisher) {
        this.bikes = bikes;
        this.withdrawals = withdrawals;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ReportBikeBroken command) {
        var bikeId = new BikeId(command.bikeId());
        var report = toReport(command);
        var events = Retrying.onConflict(() -> {
            var bike = bikes.loadOrUnknown(bikeId);
            var happened = bike.reportFault(report).at(command.reportedAt());
            if (happened.stream().findAny().isPresent()) {
                bikes.save(bike);
            }
            return happened;
        });
        eventPublisher.publish(events);
        if (events.hasOccurredEventOfType(BikeEvent.WithdrawalRequested.class)) {
            withdraw(bikeId);
        }
    }

    private void withdraw(BikeId bikeId) {
        try {
            withdrawals.withdraw(bikeId);
        } catch (RuntimeException failure) {
            log.error("Bike {} is broken but Rentals didn't withdraw it", bikeId.value(), failure);
        }
    }

    private static FaultReport toReport(ReportBikeBroken command) {
        var source = FaultSource.valueOf(command.faultSource());
        var description = Optional.ofNullable(command.description()).filter(text -> !text.isBlank()).map(FaultDescription::new);
        if (source == FaultSource.FLEET_ISSUE) {
            return FaultReport.fromFleetIssue(new IncidentId(command.incidentId()),
                                              description.orElse(new FaultDescription("fleet issue")));
        }
        return FaultReport.fromRentals(source, description);
    }
}
