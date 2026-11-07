package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.ReservationWindow;

import java.time.Instant;

/// What one borrower currently holds, across all stations: "you can rent only one bike at a time"
/// (product owner, 2026-11-07). Guards the per-borrower rules R2, R7 and R8 of
/// `docs/specs/rider-reserves-bike-at-station.md`; the per-bike rules live in `BikeStation`.
@AggregateRoot
public class Borrowing {

    private final BorrowerId borrower;
    private BikeId reservedBike;
    private ReservationWindow reservation;
    private boolean riding;

    public Borrowing(BorrowerId borrower) {
        this.borrower = borrower;
    }

    @Identity
    public BorrowerId borrower() {
        return borrower;
    }

    public At<Instant, DomainEvents> reserve(BikeId bike) {
        return now -> {
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
}
