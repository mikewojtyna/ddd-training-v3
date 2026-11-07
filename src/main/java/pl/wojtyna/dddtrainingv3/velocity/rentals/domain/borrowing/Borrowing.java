package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.As;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.ReservationWindow;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.eligibility.Eligibility;

import java.time.Instant;

/// What one borrower currently holds, across all stations: "you can rent only one bike at a time"
/// (product owner, 2026-11-07). Guards the per-borrower rules R2, R7 and R8 of
/// `docs/specs/rider-reserves-bike-at-station.md`; the per-bike rules live in `BikeStation`.
@AggregateRoot
public class Borrowing {

    private final BorrowerId borrower;
    private final Version version;
    private BikeId reservedBike;             // needed by: R8 (only the reserved bike can be rented)
    private ReservationWindow reservation;   // needed by: R2 (one active reservation), R8
    private boolean riding;                  // needed by: R7 (no reserving while riding)

    public Borrowing(BorrowerId borrower) {
        this(borrower, Version.initial());
    }

    private Borrowing(BorrowerId borrower, Version version) {
        this.borrower = borrower;
        this.version = version;
    }

    /// The version this borrowing was loaded at; the repository compares it on save.
    public Version version() {
        return version;
    }

    @Identity
    public BorrowerId borrower() {
        return borrower;
    }

    public As<Eligibility, At<Instant, DomainEvents>> reserve(BikeId bike) {
        return eligibility -> now -> {
            if (eligibility != Eligibility.ELIGIBLE) {
                throw new BorrowerNotEligibleException("Borrower's account isn't activated");
            }
            if (riding) {
                throw new AlreadyRidingException("Borrower is riding a bike");
            }
            if (reservation != null && reservation.isOpenAt(now)) {
                throw new AlreadyHoldsReservationException("Borrower already holds a reservation");
            }
            reservedBike = bike;
            reservation = new ReservationWindow(now);
            return DomainEvents.empty();
        };
    }

    public At<Instant, DomainEvents> rent(BikeId bike) {
        return now -> {
            if (reservation != null && reservation.isOpenAt(now) && !reservedBike.equals(bike)) {
                throw new HoldsReservationForAnotherBikeException("Borrower holds a reservation for another bike");
            }
            riding = true;
            return DomainEvents.empty();
        };
    }

    // ---- persistence support (memento); not used by domain logic ----

    public record Snapshot(BorrowerId borrower, Version version, BikeId reservedBike, Instant reservedAt, boolean riding) {
    }

    public Snapshot snapshot() {
        return new Snapshot(borrower, version, reservedBike, reservation == null ? null : reservation.start(), riding);
    }

    public static Borrowing restore(Snapshot snapshot) {
        var borrowing = new Borrowing(snapshot.borrower(), snapshot.version());
        borrowing.reservedBike = snapshot.reservedBike();
        borrowing.reservation = snapshot.reservedAt() == null ? null : new ReservationWindow(snapshot.reservedAt());
        borrowing.riding = snapshot.riding();
        return borrowing;
    }
}
