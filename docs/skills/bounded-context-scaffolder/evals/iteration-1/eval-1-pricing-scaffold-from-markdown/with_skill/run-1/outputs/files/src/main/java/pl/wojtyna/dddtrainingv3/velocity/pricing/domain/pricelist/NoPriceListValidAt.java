package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import java.time.Instant;

/// Thrown when no published price list was valid at the given moment, so no fee can be calculated.
public class NoPriceListValidAt extends RuntimeException {

    public NoPriceListValidAt(Instant moment) {
        super("No price list was valid at " + moment);
    }
}
