package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening.FluentLanguage.By;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/// State-based screening. Guards: a seat is reserved only once, and a reservation never leaves empty seats between
/// reserved seats in a row.
@AggregateRoot
public class Screening {

    private final ScreeningId id;
    private final Map<SeatNumber, Seat> seats = new HashMap<>(); // needed by: reserved only once; no empty seats between

    private Screening(ScreeningId id) {
        this.id = id;
    }

    public static Screening schedule(ScreeningId id) {
        return new Screening(id);
    }

    public By<CustomerId, DomainEvents> reserve(SeatNumber seatNumber) {
        return customer -> {
            var seat = seats.computeIfAbsent(seatNumber, Seat::new);
            if (!seat.isReserved()) {
                guardNoEmptySeatLeftBy(seatNumber);
            }
            seat.reserveFor(customer);
            return DomainEvents.of(new ScreeningEvent.SeatReserved(id, seatNumber, customer));
        };
    }

    private void guardNoEmptySeatLeftBy(SeatNumber seatNumber) {
        var reservedInRow = seats.values().stream()
                                 .filter(Seat::isReserved)
                                 .map(seat -> seat.number)
                                 .filter(number -> number.row() == seatNumber.row())
                                 .toList();
        // reserved seats in a row always form one block, so a new seat must touch it
        if (!reservedInRow.isEmpty() && reservedInRow.stream().noneMatch(other -> Math.abs(other.seat() - seatNumber.seat()) == 1)) {
            throw new EmptySeatLeftBetweenReservationsException(
                "Reserving " + seatNumber.row() + seatNumber.seat() + " would leave an empty seat between reservations");
        }
    }

    @Identity
    public ScreeningId id() {
        return id;
    }

    /// A place in the hall for this screening, free until a customer reserves it.
    @Entity
    private static class Seat {

        private final SeatNumber number;
        private CustomerId reservedBy; // null while the seat is free

        private Seat(SeatNumber number) {
            this.number = number;
        }

        private void reserveFor(CustomerId customer) {
            if (reservedBy != null) {
                throw new SeatAlreadyReservedException("Seat " + number.row() + number.seat() + " is already reserved");
            }
            reservedBy = customer;
        }

        private boolean isReserved() {
            return reservedBy != null;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Seat other && number.equals(other.number);
        }

        @Override
        public int hashCode() {
            return Objects.hash(number);
        }
    }
}
