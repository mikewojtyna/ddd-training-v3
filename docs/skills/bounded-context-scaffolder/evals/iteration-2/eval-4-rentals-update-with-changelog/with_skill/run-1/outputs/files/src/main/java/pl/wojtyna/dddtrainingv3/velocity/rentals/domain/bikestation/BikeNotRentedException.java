package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

/// Thrown when a bike is returned to a station where it is not rented: it is available there,
/// or the station doesn't know the bike at all (canvas rule "Return bike that is not rented").
public class BikeNotRentedException extends RuntimeException {

    public BikeNotRentedException(BikeId bikeId) {
        super("Bike " + bikeId.value() + " is not rented from this station");
    }
}
