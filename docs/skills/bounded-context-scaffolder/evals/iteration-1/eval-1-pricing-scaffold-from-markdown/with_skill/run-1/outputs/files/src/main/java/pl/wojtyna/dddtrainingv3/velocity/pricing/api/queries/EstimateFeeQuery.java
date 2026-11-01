package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

/// Canvas inbound query: "Estimate Fee" from the rider app. How much would a ride of `rideMinutes` minutes cost?
public record EstimateFeeQuery(long rideMinutes) {

    public EstimateFeeQuery {
        if (rideMinutes < 0) {
            throw new IllegalArgumentException("Ride minutes cannot be negative");
        }
    }
}
