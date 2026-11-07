package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Entity;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.By;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.ForDuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@AggregateRoot
public class BikeStation {

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

    public By<Borrower, ForDuration<RentalDuration, DomainEvents>> rent(
        Bike bike) {
        return borrower -> rentalDuration -> {
            if (rentalDuration.duration().toHours() > 11) {
                throw new MaximumRentalDurationExceededException("Bike rent too long");
            }
            return bikes.stream()
                        .filter(bikeInStation -> bikeInStation.id().equals(bike.id()))
                        .filter(BikeInStation::isAvailable).findAny()
                        .map(bikeInStation -> {
                            bikeInStation.setAvailable(false);
                            return DomainEvents.of(new BikeRentalEvent.BikeRented(bikeInStation.id(), borrower, rentalDuration));
                        })
                        .orElseThrow(() -> new BikeNotAvailableException("Bike not available"));
        };
    }

    public DomainEvents returnBike(Bike bike) {
        throw new UnsupportedOperationException("Implement this method");
    }

    @Entity
    private static class BikeInStation {

        private final BikeId id;
        private boolean available;

        private BikeInStation(BikeId id, boolean available) {
            this.id = id;
            this.available = available;
        }

        boolean isAvailable() {
            return available;
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
