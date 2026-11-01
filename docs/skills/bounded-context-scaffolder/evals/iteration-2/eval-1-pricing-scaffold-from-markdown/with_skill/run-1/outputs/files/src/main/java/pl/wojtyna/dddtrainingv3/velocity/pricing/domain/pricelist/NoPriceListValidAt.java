package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import java.time.Instant;

/// Thrown when no price list is valid at the given moment, for example when a ride starts before the first price
/// list's valid-from moment.
public class NoPriceListValidAt extends RuntimeException {

    public NoPriceListValidAt(Instant moment) {
        super("No price list is valid at " + moment);
    }
}
