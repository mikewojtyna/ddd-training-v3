package pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries;

/// *Ride Settlement*: where the settlement of a ride (identified by its session) stands.
public record RideSettlementQuery(String sessionId) {
}
