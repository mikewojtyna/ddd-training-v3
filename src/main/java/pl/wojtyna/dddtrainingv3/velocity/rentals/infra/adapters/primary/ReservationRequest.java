package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.primary;

/// Rider app request body: the rider who reserves (spec term *rider*, Rentals term *borrower*).
public record ReservationRequest(String riderId) {
}
