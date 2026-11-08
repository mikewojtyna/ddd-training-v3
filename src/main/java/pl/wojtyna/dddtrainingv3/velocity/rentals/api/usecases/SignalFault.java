package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

/// The bike's telemetry signals a fault it detected.
public record SignalFault(String bikeId, String faultCode) {
}
