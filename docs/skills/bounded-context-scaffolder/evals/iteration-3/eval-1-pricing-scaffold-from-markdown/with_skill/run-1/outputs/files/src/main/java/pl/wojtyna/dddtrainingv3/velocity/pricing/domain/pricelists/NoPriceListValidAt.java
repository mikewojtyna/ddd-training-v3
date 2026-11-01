package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists;

import java.time.Instant;

/// Rule "No price list valid at ride start": a ride that started before the first published price list cannot be
/// priced.
public class NoPriceListValidAt extends RuntimeException {

    public NoPriceListValidAt(Instant moment) {
        super("No price list is valid at " + moment);
    }
}
