package pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries;

import java.util.List;

/// Answer to *Rider Balance*: money in PLN as decimal strings. A rider Settlement never heard of owes `0.00`.
public record RiderBalanceView(String riderId, String outstandingDebt, List<UnpaidRideView> unpaidRides) {

    public record UnpaidRideView(String sessionId, String amount) {
    }

    public static RiderBalanceView nothingOwedBy(String riderId) {
        return new RiderBalanceView(riderId, "0.00", List.of());
    }
}
