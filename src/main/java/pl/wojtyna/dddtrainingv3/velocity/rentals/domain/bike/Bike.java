package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeReserved;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.RentalSessionStarted;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.ReservationExpired;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.UnlockFailed;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeLocks.LockResult;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.FluentLanguage.By;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.FluentLanguage.Given;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.FluentLanguage.Reporting;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.FluentLanguage.StandingAt;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.FluentLanguage.Using;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibility.Eligibility;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Station.StationId;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/// A bike Rentals can lend. Guards "one bike, one active holder": it is *available* or *withdrawn*, and at the same
/// time free, reserved (by a rider or a technician) or unlocked (a rider's rental session or a technician's service).
@AggregateRoot
public class Bike {

    static final Duration RIDER_RESERVATION = Duration.ofMinutes(2);       // DE-10
    static final Duration TECHNICIAN_RESERVATION = Duration.ofMinutes(30); // DE-11
    static final BigDecimal NEAR_METRES = new BigDecimal("50.00");        // DE-12
    static final Duration MAXIMUM_RENTAL_TIME = Duration.ofHours(12);     // DE-20

    public enum Availability {
        AVAILABLE,
        WITHDRAWN
    }

    /// Who holds the bike right now, if anyone.
    public sealed interface Hold {

        @ValueObject
        record Free() implements Hold {
        }

        /// Holds up to and including `until` (DE-81).
        @ValueObject
        record Reserved(Holder holder, Instant reservedAt, Instant until) implements Hold {
        }

        @ValueObject
        record RentalSession(SessionId sessionId, Holder rider, Instant startedAt) implements Hold {
        }

        @ValueObject
        record ServiceUnlock(Holder technician, Instant unlockedAt) implements Hold {
        }
    }

    private final BikeId id;
    private final Version version;  // the version this bike was added or loaded at
    private Availability availability; // needed by: riders reserve only available bikes, technicians only withdrawn ones
    private Hold hold;                 // needed by: one bike, one active holder; only the holder unlocks or ends the ride
    private StationId stationId;       // where the bike stands (null between stations or when unknown)
    private Position position;         // needed by: a rider reserves only within 50 m (null when unknown, DE-21)
    private String pendingFault;       // needed by: a fault signalled during a ride withdraws the bike at the session end (DE-25)

    private Bike(BikeId id, Version version, Availability availability, Hold hold, StationId stationId, Position position,
                 String pendingFault) {
        this.id = id;
        this.version = version;
        this.availability = availability;
        this.hold = hold;
        this.stationId = stationId;
        this.position = position;
        this.pendingFault = pendingFault;
    }

    static Bike availableAt(BikeId id, Station station) {
        return new Bike(id, Version.initial(), Availability.AVAILABLE, new Hold.Free(), station.id(), station.position(), null);
    }

    /// A rider reserves the bike for 2 minutes. Rejected when the rider isn't eligible (Accounts' answer), already
    /// holds another bike, the bike isn't available and free, or the rider stands more than 50 m away.
    public StandingAt<Position, Given<RiderStanding, At<Instant, DomainEvents>>> reserveForRide(Holder rider) {
        return riderPosition -> standing -> now -> {
            if (standing.eligibility() != Eligibility.ELIGIBLE) {
                throw new RiderNotEligible("Rider " + rider.id() + " is not eligible to rent");
            }
            if (standing.holdsAnotherBike()) {
                throw new OneRiderOneBike("Rider " + rider.id() + " already holds a bike");
            }
            if (availability != Availability.AVAILABLE || !(hold instanceof Hold.Free)) {
                throw new BikeNotAvailable(id.value() + " is not available");
            }
            if (position.metresTo(riderPosition).compareTo(NEAR_METRES) > 0) {
                throw new TooFarFromBike("Rider " + rider.id() + " is " + position.metresTo(riderPosition) + " m from " + id.value());
            }
            return reserve(rider, now, RIDER_RESERVATION);
        };
    }

    public At<Instant, DomainEvents> reserveForService(Holder technician) {
        return now -> {
            if (availability != Availability.WITHDRAWN) {
                throw new BikeNotWithdrawn(id.value() + " is not withdrawn; technicians reserve only withdrawn bikes");
            }
            if (!(hold instanceof Hold.Free)) {
                throw new BikeAlreadyHeld(id.value() + " is already held");
            }
            return reserve(technician, now, TECHNICIAN_RESERVATION);
        };
    }

    private DomainEvents reserve(Holder holder, Instant now, Duration period) {
        var until = now.plus(period);
        hold = new Hold.Reserved(holder, now, until);
        return DomainEvents.of(new BikeReserved(id, holder, now, until));
    }

    /// The reservation's rider unlocks the bike; when the lock opens a rental session starts, when it doesn't the
    /// reservation is released (DE-15). Rejected, with the lock untouched, for anyone but the holder of a reservation
    /// that still holds, and for a rider who is no longer eligible (DE-80).
    public Given<Eligibility, At<Instant, Using<BikeLocks, DomainEvents>>> unlockForRide(Holder rider) {
        return eligibility -> now -> locks -> {
            var reservation = reservationOf(rider, now);
            if (eligibility != Eligibility.ELIGIBLE) {
                throw new RiderNotEligible("Rider " + rider.id() + " is not eligible to rent");
            }
            if (locks.open(id) == LockResult.FAILED) {
                return unlockFailed(reservation, now);
            }
            var session = new Hold.RentalSession(SessionId.newSession(), rider, now);
            hold = session;
            return DomainEvents.of(new RentalSessionStarted(id, session.sessionId(), rider, now));
        };
    }

    /// The reservation's technician unlocks the withdrawn bike for service. It never starts a session; a failed lock
    /// releases the reservation and the bike stays withdrawn.
    public At<Instant, Using<BikeLocks, DomainEvents>> unlockForService(Holder technician) {
        return now -> locks -> {
            var reservation = reservationOf(technician, now);
            if (locks.open(id) == LockResult.FAILED) {
                return unlockFailed(reservation, now);
            }
            hold = new Hold.ServiceUnlock(technician, now);
            return DomainEvents.of(new BikeEvent.BikeUnlocked(id, technician, now));
        };
    }

    private Hold.Reserved reservationOf(Holder holder, Instant now) {
        if (hold instanceof Hold.Reserved reserved && reserved.holder().equals(holder) && !now.isAfter(reserved.until())) {
            return reserved;
        }
        throw new NotReservationHolder(holder.id() + " holds no reservation of " + id.value() + " at " + now);
    }

    private DomainEvents unlockFailed(Hold.Reserved reservation, Instant now) {
        hold = new Hold.Free();
        return DomainEvents.of(new UnlockFailed(id, reservation.holder(), now));
    }

    /// Releases a reservation whose period is over (from the second after its end, DE-81). The bike's availability
    /// doesn't change: a technician's bike stays withdrawn.
    public DomainEvents releaseIfExpired(Instant now) {
        if (hold instanceof Hold.Reserved reserved && now.isAfter(reserved.until())) {
            hold = new Hold.Free();
            return DomainEvents.of(new ReservationExpired(id, reserved.holder(), reserved.reservedAt(), now));
        }
        return DomainEvents.empty();
    }

    /// The riding rider returns the bike at the station they name (trusted, DE-22). A lock that closes ends the session
    /// as a *station return*; a fault the rider reports withdraws the bike, the fees stay normal (DE-24). A lock that
    /// doesn't close ends it as a *lock failure* and withdraws the bike.
    public By<Holder, Reporting<String, At<Instant, Using<BikeLocks, DomainEvents>>>> returnAtStation(Station station) {
        return rider -> faultDescription -> now -> locks -> {
            var session = sessionOf(rider);
            var locked = locks.close(id) == LockResult.SUCCEEDED;
            stationId = station.id();
            position = station.position();
            if (!locked) {
                return endSession(session, now, EndReason.LOCK_FAILURE)
                    .append(withdrawAtSessionEnd(WithdrawalReason.LOCK_FAILED, null, now));
            }
            var ended = endSession(session, now, EndReason.STATION_RETURN);
            if (faultDescription != null && !faultDescription.isBlank()) {
                return ended.append(withdrawAtSessionEnd(WithdrawalReason.FAULT_REPORTED_AT_RETURN, faultDescription, now));
            }
            return ended.append(withdrawForPendingFault(now));
        };
    }

    /// The riding rider ends the ride where they are, outside a station. Rentals doesn't judge the zone: Pricing does
    /// (plan `rider-ends-ride-in-drop-zone` D6). A lock that doesn't close is a *lock failure* wherever it is (DE-23).
    public By<Holder, At<Instant, Using<BikeLocks, DomainEvents>>> endRideHere(Position here) {
        return rider -> now -> locks -> {
            var session = sessionOf(rider);
            var locked = locks.close(id) == LockResult.SUCCEEDED;
            stationId = null;
            position = here;
            if (!locked) {
                return endSession(session, now, EndReason.LOCK_FAILURE)
                    .append(withdrawAtSessionEnd(WithdrawalReason.LOCK_FAILED, null, now));
            }
            return endSession(session, now, EndReason.FREE_FLOATING_RETURN).append(withdrawForPendingFault(now));
        };
    }

    /// Ends a session that runs longer than the maximum rental time, at the 12-hour mark whenever it is noticed
    /// (DE-20, DE-82). Nobody knows where the bike is, so it is withdrawn as *not returned* (DE-21).
    public DomainEvents endIfOverdue(Instant now) {
        if (!(hold instanceof Hold.RentalSession session)) {
            return DomainEvents.empty();
        }
        var limit = session.startedAt().plus(MAXIMUM_RENTAL_TIME);
        if (!now.isAfter(limit)) {
            return DomainEvents.empty();
        }
        stationId = null;
        position = null;
        return endSession(session, limit, EndReason.MAX_RENTAL_TIME_EXCEEDED)
            .append(withdrawAtSessionEnd(WithdrawalReason.NOT_RETURNED, null, now));
    }

    private Hold.RentalSession sessionOf(Holder rider) {
        if (hold instanceof Hold.RentalSession session && session.rider().equals(rider)) {
            return session;
        }
        throw new NoRunningSession(rider.id() + " has no running session on " + id.value());
    }

    private DomainEvents endSession(Hold.RentalSession session, Instant endedAt, EndReason reason) {
        hold = new Hold.Free();
        return DomainEvents.of(new BikeEvent.RentalSessionEnded(id, session.sessionId(), session.rider(), session.startedAt(),
                                                                endedAt, reason, stationId, position));
    }

    private DomainEvents withdrawAtSessionEnd(WithdrawalReason reason, String faultDescription, Instant now) {
        pendingFault = null;
        if (availability == Availability.WITHDRAWN) {
            return DomainEvents.empty();
        }
        availability = Availability.WITHDRAWN;
        return DomainEvents.of(new BikeWithdrawn(id, reason, faultDescription, now));
    }

    /// A fault the bike signalled during the ride withdraws it when the session ends, unless the end withdraws it
    /// anyway (plan `fleet-operations-withdraws-bike-signalling-fault` D6: one withdrawal, the reason found at the end).
    private DomainEvents withdrawForPendingFault(Instant now) {
        if (pendingFault == null) {
            return DomainEvents.empty();
        }
        var fault = pendingFault;
        pendingFault = null;
        return withdrawAtSessionEnd(WithdrawalReason.FAULT_SIGNALLED, fault, now);
    }

    /// The bike detected a fault itself (DE-25). Outside a ride it is withdrawn at once, cancelling a reservation on
    /// it; during a ride nothing changes until the session ends (DE-86); a withdrawn bike stays as it is.
    public At<Instant, DomainEvents> signalFault(String faultCode) {
        return now -> {
            if (availability == Availability.WITHDRAWN) {
                return DomainEvents.empty();
            }
            if (hold instanceof Hold.RentalSession) {
                pendingFault = faultCode;
                return DomainEvents.empty();
            }
            return cancelReservation(now).append(withdrawNow(WithdrawalReason.FAULT_SIGNALLED, faultCode, now));
        };
    }

    /// A client asks to withdraw the bike (reason *requested*). A reservation on it is cancelled; a ride on it goes on
    /// (DE-86) and the bike simply isn't available after the return. Withdrawing a withdrawn bike changes nothing.
    public DomainEvents withdraw(Instant now) {
        if (availability == Availability.WITHDRAWN) {
            return DomainEvents.empty();
        }
        return cancelReservation(now).append(withdrawNow(WithdrawalReason.REQUESTED, null, now));
    }

    /// The rider cancels their reservation; the bike is free for anyone at once (spec rider-cancels-reservation).
    public At<Instant, DomainEvents> cancelReservationOf(Holder rider) {
        return now -> {
            if (!(hold instanceof Hold.Reserved reserved)) {
                throw new NoReservationToCancel(id.value() + " has no reservation to cancel");
            }
            hold = new Hold.Free();
            return DomainEvents.of(new BikeEvent.ReservationCancelled(id, reserved.holder(), now));
        };
    }

    private DomainEvents cancelReservation(Instant now) {
        if (hold instanceof Hold.Reserved reserved) {
            hold = new Hold.Free();
            return DomainEvents.of(new BikeEvent.ReservationCancelled(id, reserved.holder(), now));
        }
        return DomainEvents.empty();
    }

    private DomainEvents withdrawNow(WithdrawalReason reason, String faultDescription, Instant now) {
        availability = Availability.WITHDRAWN;
        pendingFault = null;
        return DomainEvents.of(new BikeWithdrawn(id, reason, faultDescription, now));
    }

    /// Back in circulation after service, at a station.
    public At<Instant, DomainEvents> makeAvailableAt(Station station) {
        return now -> makeAvailable(station.id(), station.position(), now);
    }

    /// Back in circulation after service, at the position where the technician left it (trusted, DE-91).
    public At<Instant, DomainEvents> makeAvailableAt(Position here) {
        return now -> makeAvailable(null, here, now);
    }

    private DomainEvents makeAvailable(StationId newStation, Position newPosition, Instant now) {
        if (availability != Availability.WITHDRAWN || !(hold instanceof Hold.ServiceUnlock)) {
            throw new BikeNotUnlockedForService(id.value() + " is not a withdrawn bike unlocked for a technician");
        }
        availability = Availability.AVAILABLE;
        hold = new Hold.Free();
        stationId = newStation;
        position = newPosition;
        pendingFault = null;
        return DomainEvents.of(new BikeEvent.BikeMadeAvailable(id, newStation, newPosition, now));
    }

    /// Whether `holder` holds this bike at `now`: a reservation that still holds, a running session or a service unlock.
    public boolean isHeldBy(Holder holder, Instant now) {
        return switch (hold) {
            case Hold.Free free -> false;
            case Hold.Reserved reserved -> reserved.holder().equals(holder) && !now.isAfter(reserved.until());
            case Hold.RentalSession session -> session.rider().equals(holder);
            case Hold.ServiceUnlock unlock -> unlock.technician().equals(holder);
        };
    }

    /// Whether a reservation on this bike ended before `instant`; lets the repository find reservations to expire.
    public boolean hasReservationEndedBefore(Instant instant) {
        return hold instanceof Hold.Reserved reserved && reserved.until().isBefore(instant);
    }

    /// Whether a rental session runs on this bike that started before `instant`.
    public boolean hasSessionStartedBefore(Instant instant) {
        return hold instanceof Hold.RentalSession session && session.startedAt().isBefore(instant);
    }

    @Identity
    public BikeId id() {
        return id;
    }

    public Version version() {
        return version;
    }

    /// Memento for persistence adapters and read models.
    public record Snapshot(BikeId id, Version version, Availability availability, Hold hold, StationId stationId,
                           Position position, String pendingFault) {

        public Snapshot withVersion(Version newVersion) {
            return new Snapshot(id, newVersion, availability, hold, stationId, position, pendingFault);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(id, version, availability, hold, stationId, position, pendingFault);
    }

    public static Bike restore(Snapshot snapshot) {
        return new Bike(snapshot.id(), snapshot.version(), snapshot.availability(), snapshot.hold(), snapshot.stationId(),
                        snapshot.position(), snapshot.pendingFault());
    }
}
