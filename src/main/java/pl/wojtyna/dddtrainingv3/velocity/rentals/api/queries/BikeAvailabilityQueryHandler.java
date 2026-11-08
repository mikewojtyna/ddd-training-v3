package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Optional;

@PrimaryPort
public class BikeAvailabilityQueryHandler {

    private final RentalsReadModel readModel;

    public BikeAvailabilityQueryHandler(RentalsReadModel readModel) {
        this.readModel = readModel;
    }

    public Optional<BikeAvailability> handle(BikeAvailabilityQuery query) {
        return readModel.bikeAvailability(query.bikeId());
    }
}
