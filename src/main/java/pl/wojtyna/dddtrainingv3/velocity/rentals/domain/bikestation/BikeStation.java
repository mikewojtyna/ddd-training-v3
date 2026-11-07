package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Entity;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.By;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.ForDuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@AggregateRoot
public class BikeStation {

    /// Reservation window: 15 minutes, including its end time (product owner, 2026-11-07).
    private static final Duration RESERVATION_WINDOW = Duration.ofMinutes(15);

    private final BikeStationId id;
    private final Set<BikeInStation> bikes = new HashSet<>();

    public BikeStation(BikeStationId id) {
        this.id = id;
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
                        .filter(bikeInStation -> bikeInStation.isAvailableAt(now)).findAny()
                        .map(bikeInStation -> {
                            bikeInStation.setAvailable(false);
                            return DomainEvents.of(new BikeRentalEvent.BikeRented(bikeInStation.id(), borrower, rentalDuration));
                        })
                        .orElseThrow(() -> new BikeNotAvailableException("Bike not available"));
        };
    }

    public By<Borrower, At<Instant, DomainEvents>> reserve(Bike bike) {
        return borrower -> now -> bikes.stream()
                                      .filter(bikeInStation -> bikeInStation.id().equals(bike.id()))
                                      .filter(bikeInStation -> bikeInStation.isAvailableAt(now)).findAny()
                                      .map(bikeInStation -> {
                                          bikeInStation.reserveUntil(now.plus(RESERVATION_WINDOW));
                                          return DomainEvents.of(new BikeRentalEvent.BikeReserved(
                                              id, bike.id(), borrower, now, now.plus(RESERVATION_WINDOW)));
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

    @Entity
    private static class BikeInStation {

        private final BikeId id;
        private boolean available;
        private Instant reservedUntil;

        private BikeInStation(BikeId id, boolean available) {
            this.id = id;
            this.available = available;
        }

        boolean isAvailableAt(Instant now) {
            return available && (reservedUntil == null || now.isAfter(reservedUntil));
        }

        private void reserveUntil(Instant until) {
            this.reservedUntil = until;
        }

        private void setAvailable(boolean available) {
            this.available = available;
        }

        BikeId id() {
            return id;
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
