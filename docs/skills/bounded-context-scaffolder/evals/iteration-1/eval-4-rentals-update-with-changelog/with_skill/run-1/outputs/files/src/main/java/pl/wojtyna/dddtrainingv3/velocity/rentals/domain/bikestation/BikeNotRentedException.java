package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

/// Canvas rule "Return bike that is not rented": a bike that is not currently rented from the station
/// cannot be returned to it.
public class BikeNotRentedException extends RuntimeException {

    public BikeNotRentedException(String message) {
        super(message);
    }

    public BikeNotRentedException(String message, Throwable cause) {
        super(message, cause);
    }
}
