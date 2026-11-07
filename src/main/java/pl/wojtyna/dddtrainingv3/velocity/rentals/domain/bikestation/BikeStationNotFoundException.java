package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

/// No station with this id (or holding this bike) is known to Rentals.
public class BikeStationNotFoundException extends RuntimeException {

    public BikeStationNotFoundException(String message) {
        super(message);
    }
}
