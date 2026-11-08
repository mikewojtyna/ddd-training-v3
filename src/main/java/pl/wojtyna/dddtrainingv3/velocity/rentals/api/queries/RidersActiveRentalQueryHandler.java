package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.time.Clock;
import java.util.Optional;

@PrimaryPort
public class RidersActiveRentalQueryHandler {

    private final RentalsReadModel readModel;
    private final Clock clock;

    public RidersActiveRentalQueryHandler(RentalsReadModel readModel, Clock clock) {
        this.readModel = readModel;
        this.clock = clock;
    }

    public Optional<RidersActiveRental> handle(RidersActiveRentalQuery query) {
        return readModel.activeRentalOf(query.riderId(), clock.instant());
    }
}
