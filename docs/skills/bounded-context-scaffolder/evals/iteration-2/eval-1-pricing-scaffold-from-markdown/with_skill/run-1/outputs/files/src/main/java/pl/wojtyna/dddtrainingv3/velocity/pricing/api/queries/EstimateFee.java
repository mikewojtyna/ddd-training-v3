package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

/// Canvas inbound query "Estimate Fee" (from Rider app): how much would a ride of N minutes cost?
public record EstimateFee(int rideMinutes) {

    public EstimateFee {
        if (rideMinutes < 0) {
            throw new IllegalArgumentException("Ride minutes cannot be negative");
        }
    }
}
