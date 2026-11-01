package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.RentalDuration;

public class RentBikeUseCase {

    public void rent(BikeStationId id, BikeId bike, BorrowerId borrowerId, RentalDuration duration) {
        // load aggregate
        // execute command
        // save aggregate
        // publish events
    }
}
