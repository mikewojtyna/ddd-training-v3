package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

/// Canvas inbound command *Reserve bike* (Rider app). A borrower reserves one specific bike at a station,
/// without being at the station. Rules R1–R3, R5–R7, see `package-info.java`.
public record ReserveBike(String stationId, String bikeId, String borrowerId) {
}
