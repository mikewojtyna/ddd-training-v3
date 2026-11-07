package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.By;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.ForDuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.Version;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@AggregateRoot
public class BikeStation {

    private final BikeStationId id;
    private final Version version;
    private final Set<BikeInStation> bikes = new HashSet<>();

    public BikeStation(BikeStationId id) {
        this(id, Version.initial());
    }

    private BikeStation(BikeStationId id, Version version) {
        this.id = id;
        this.version = version;
    }

    @Identity
    public BikeStationId id() {
        return id;
    }

    /// The version this station was loaded at; the repository compares it on save.
    public Version version() {
        return version;
    }

    public boolean holds(BikeId bike) {
        return bikes.stream().anyMatch(bikeInStation -> bikeInStation.id().equals(bike));
    }

    public void placeNew(Bike bike) {
        if (bikes.stream().map(BikeInStation::id).anyMatch(bikeId -> bikeId.equals(bike.id()))) {
            throw new BikeAlreadyInStation("Bike already in station");
        }
        bikes.add(new BikeInStation(bike.id(), true));
    }

    public By<Borrower, ForDuration<RentalDuration, At<Instant, DomainEvents>>> rent(Bike bike) {
        return borrower -> rentalDuration -> now -> {
            if (rentalDuration.duration().toHours() > 11) {
                throw new MaximumRentalDurationExceededException("Bike rent too long");
            }
            return bikes.stream()
                        .filter(bikeInStation -> bikeInStation.id().equals(bike.id()))
                        .filter(bikeInStation -> bikeInStation.isAvailableTo(borrower, now)).findAny()
                        .map(bikeInStation -> {
                            var fromReservation = bikeInStation.isReservedBy(borrower, now);
                            bikeInStation.setAvailable(false);
                            bikeInStation.endReservation();
                            return DomainEvents.of(new BikeRentalEvent.BikeRented(
                                bikeInStation.id(), borrower, rentalDuration, fromReservation));
                        })
                        .orElseThrow(() -> new BikeNotAvailableException("Bike not available"));
        };
    }

    public By<Borrower, At<Instant, DomainEvents>> reserve(Bike bike) {
        return borrower -> now -> bikes.stream()
                                      .filter(bikeInStation -> bikeInStation.id().equals(bike.id()))
                                      .filter(bikeInStation -> bikeInStation.isAvailableAt(now)).findAny()
                                      .map(bikeInStation -> {
                                          var window = new ReservationWindow(now);
                                          bikeInStation.reserveFor(borrower, window);
                                          return DomainEvents.of(new BikeRentalEvent.BikeReserved(
                                              id, bike.id(), borrower, window.start(), window.end()));
                                      })
                                      .orElseThrow(() -> new BikeNotAvailableException("Bike not available"));
    }

    /// Fleet Maintenance took the bike out of service: from now on it isn't available (rule R5).
    public DomainEvents withdraw(Bike bike) {
        bikes.stream()
             .filter(bikeInStation -> bikeInStation.id().equals(bike.id()))
             .forEach(bikeInStation -> bikeInStation.setAvailable(false));
        return DomainEvents.empty();
    }

    public DomainEvents returnBike(Bike bike) {
        throw new UnsupportedOperationException("Implement this method");
    }

    // ---- persistence support (memento); not used by domain logic ----

    public record Snapshot(BikeStationId id, Version version, List<BikeSnapshot> bikes) {
    }

    public record BikeSnapshot(BikeId bike, boolean available, Instant reservedAt, Borrower holder) {
    }

    public Snapshot snapshot() {
        return new Snapshot(id, version, bikes.stream().map(BikeInStation::snapshot).toList());
    }

    public static BikeStation restore(Snapshot snapshot) {
        var station = new BikeStation(snapshot.id(), snapshot.version());
        snapshot.bikes().forEach(bike -> station.bikes.add(BikeInStation.restore(bike)));
        return station;
    }

    @Entity
    private static class BikeInStation {

        private final BikeId id;
        private boolean available;
        private ReservationWindow reservation;
        private Borrower holder;

        private BikeInStation(BikeId id, boolean available) {
            this.id = id;
            this.available = available;
        }

        boolean isAvailableAt(Instant now) {
            return available && (reservation == null || !reservation.isOpenAt(now));
        }

        boolean isAvailableTo(Borrower borrower, Instant now) {
            return isAvailableAt(now) || (available && isReservedBy(borrower, now));
        }

        boolean isReservedBy(Borrower borrower, Instant now) {
            return reservation != null && reservation.isOpenAt(now) && borrower.equals(holder);
        }

        private void reserveFor(Borrower borrower, ReservationWindow window) {
            this.holder = borrower;
            this.reservation = window;
        }

        private void endReservation() {
            this.holder = null;
            this.reservation = null;
        }

        private void setAvailable(boolean available) {
            this.available = available;
        }

        BikeId id() {
            return id;
        }

        BikeSnapshot snapshot() {
            return new BikeSnapshot(id, available, reservation == null ? null : reservation.start(), holder);
        }

        static BikeInStation restore(BikeSnapshot snapshot) {
            var bike = new BikeInStation(snapshot.bike(), snapshot.available());
            if (snapshot.reservedAt() != null) {
                bike.reserveFor(snapshot.holder(), new ReservationWindow(snapshot.reservedAt()));
            }
            return bike;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof BikeInStation other && Objects.equals(id, other.id);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }
    }
}
