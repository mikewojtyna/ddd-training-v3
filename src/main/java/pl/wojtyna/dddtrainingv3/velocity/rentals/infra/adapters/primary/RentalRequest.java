package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.primary;

/// Rider app request body: the rider who rents, and for how long (ISO-8601, e.g. "PT1H").
public record RentalRequest(String riderId, String duration) {
}
