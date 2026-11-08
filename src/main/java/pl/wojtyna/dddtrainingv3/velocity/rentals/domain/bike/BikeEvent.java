package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Station.StationId;

import java.time.Instant;

/// What happens to a bike in Rentals. Events carry identities and value objects only.
public sealed interface BikeEvent extends DomainEvent {

    BikeId bikeId();

    record BikeAddedToFleet(BikeId bikeId, StationId stationId, Position position, Instant addedAt) implements BikeEvent {
    }

    record BikeReserved(BikeId bikeId, Holder holder, Instant reservedAt, Instant reservedUntil) implements BikeEvent {
    }

    /// The reservation ran out before its holder unlocked the bike. `expiredAt` is the moment the expiry was noticed.
    record ReservationExpired(BikeId bikeId, Holder holder, Instant reservedAt, Instant expiredAt) implements BikeEvent {
    }

    /// A rider's reservation ended because the bike was withdrawn (a fault signal or a client's request).
    record ReservationCancelled(BikeId bikeId, Holder holder, Instant cancelledAt) implements BikeEvent {
    }

    record RentalSessionStarted(BikeId bikeId, SessionId sessionId, Holder rider, Instant startedAt) implements BikeEvent {
    }

    /// A technician's unlock opened the lock; it never starts a session.
    record BikeUnlocked(BikeId bikeId, Holder holder, Instant unlockedAt) implements BikeEvent {
    }

    record UnlockFailed(BikeId bikeId, Holder holder, Instant failedAt) implements BikeEvent {
    }

    /// `stationId` and `position` are where the session ended; both are null when nobody knows (maximum rental time).
    record RentalSessionEnded(BikeId bikeId, SessionId sessionId, Holder rider, Instant startedAt, Instant endedAt,
                              EndReason endReason, StationId stationId, Position position) implements BikeEvent {
    }

    /// `faultDescription` is the rider's description or the bike's fault code, when there is one.
    record BikeWithdrawn(BikeId bikeId, WithdrawalReason reason, String faultDescription, Instant withdrawnAt) implements BikeEvent {
    }

    record BikeMadeAvailable(BikeId bikeId, StationId stationId, Position position, Instant madeAvailableAt) implements BikeEvent {
    }
}
