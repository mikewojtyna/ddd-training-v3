package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.*;

/// A technician reports a bike broken during a routine check (DE-62). A technician waits for the answer, so Rentals
/// is asked to withdraw the bike before anything is recorded: a bike unknown to the fleet is rejected (`UnknownBike`)
/// and nothing is recorded (plan technician-reports-fault D5). A report on a broken bike changes nothing.
@PrimaryPort
public class ReportFaultUseCase {

    private final Bikes bikes;
    private final BikeWithdrawals withdrawals;
    private final DomainEventPublisher eventPublisher;

    public ReportFaultUseCase(Bikes bikes, BikeWithdrawals withdrawals, DomainEventPublisher eventPublisher) {
        this.bikes = bikes;
        this.withdrawals = withdrawals;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ReportFault command) {
        var bike = bikes.loadOrUnknown(new BikeId(command.bikeId()));
        var report = FaultReport.fromTechnician(new TechnicianId(command.technicianId()), new FaultDescription(command.description()));
        var events = bike.reportFault(report).at(command.reportedAt());
        if (events.stream().findAny().isEmpty()) {
            return;
        }
        if (events.hasOccurredEventOfType(BikeEvent.WithdrawalRequested.class)) {
            withdrawals.withdraw(bike.id());
        }
        bikes.save(bike);
        eventPublisher.publish(events);
    }
}
