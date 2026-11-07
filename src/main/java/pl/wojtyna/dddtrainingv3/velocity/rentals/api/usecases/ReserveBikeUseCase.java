package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.*;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.Borrowing;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.BorrowingRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.eligibility.BorrowerEligibility;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.eligibility.Eligibility;

import java.time.Clock;
import java.time.Instant;

/// Canvas command *Reserve bike*. The borrower's rules (R2, R6, R7) are checked on `Borrowing`, the bike's rules
/// (R1, R3, R5) on `BikeStation`; both aggregates change in this one use case (plan decision D3).
@PrimaryPort
public class ReserveBikeUseCase {

    private final BikeStationRepo stations;
    private final BorrowingRepository borrowings;
    private final BorrowerEligibility eligibility;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;

    public ReserveBikeUseCase(BikeStationRepo stations, BorrowingRepository borrowings, BorrowerEligibility eligibility,
                              Clock clock, DomainEventPublisher eventPublisher) {
        this.stations = stations;
        this.borrowings = borrowings;
        this.eligibility = eligibility;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ReserveBike command) {
        var stationId = new BikeStationId(command.stationId());
        var bike = new Bike(new BikeId(command.bikeId()));
        var borrowerId = new BorrowerId(command.borrowerId());
        var now = Instant.now(clock);

        var station = stations.load(stationId)
                              .orElseThrow(() -> new BikeStationNotFoundException("Unknown station " + stationId.value()));
        var borrowing = borrowings.load(borrowerId).orElseGet(() -> new Borrowing(borrowerId));
        var borrowerEligibility = eligibility.isEligible(borrowerId) ? Eligibility.ELIGIBLE : Eligibility.NOT_ELIGIBLE;

        borrowing.reserve(bike.id()).as(borrowerEligibility).at(now);
        var events = station.reserve(bike).by(new Borrower(borrowerId)).at(now);

        borrowings.save(borrowing);
        stations.save(station);
        eventPublisher.publish(events);
    }
}
