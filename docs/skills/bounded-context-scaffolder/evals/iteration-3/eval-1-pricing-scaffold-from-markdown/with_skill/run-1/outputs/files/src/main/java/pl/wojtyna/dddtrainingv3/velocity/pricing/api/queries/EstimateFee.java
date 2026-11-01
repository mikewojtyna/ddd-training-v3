package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

/// Canvas inbound query "Estimate Fee" (from the rider app): how much would a ride of N minutes cost.
public record EstimateFee(long minutes) {

    public EstimateFee {
        if (minutes < 0) {
            throw new IllegalArgumentException("Minutes cannot be negative");
        }
    }
}
