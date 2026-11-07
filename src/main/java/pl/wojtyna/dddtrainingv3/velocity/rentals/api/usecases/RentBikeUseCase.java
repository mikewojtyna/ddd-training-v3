package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.*;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.Borrowing;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.BorrowingRepository;

import java.time.Clock;
import java.time.Instant;

/// Canvas command *Unlock bike* (rent). The borrower's rule R8 (a holder rents only the reserved bike) is checked on
/// `Borrowing`, the bike's rules (availability, R3, the reserved holder, 11 hours) on `BikeStation`.
@PrimaryPort
public class RentBikeUseCase {

    private final BikeStationRepo stations;
    private final BorrowingRepository borrowings;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;

    public RentBikeUseCase(BikeStationRepo stations, BorrowingRepository borrowings, Clock clock,
                           DomainEventPublisher eventPublisher) {
        this.stations = stations;
        this.borrowings = borrowings;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    public void rent(BikeStationId id, BikeId bike, BorrowerId borrowerId, RentalDuration duration) {
        var now = Instant.now(clock);
        var station = stations.load(id)
                              .orElseThrow(() -> new BikeStationNotFoundException("Unknown station " + id.value()));
        var borrowing = borrowings.load(borrowerId).orElseGet(() -> new Borrowing(borrowerId));

        borrowing.rent(bike).at(now);
        var events = station.rent(new Bike(bike)).by(new Borrower(borrowerId)).forDuration(duration).at(now);

        borrowings.save(borrowing);
        stations.save(station);
        eventPublisher.publish(events);
    }
}
