package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Entity;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.By;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.FluentLanguage.ForDuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@AggregateRoot
public class BikeStation {

    private final BikeStationId id;
    private final Set<BikeInStation> bikes = new HashSet<>();

    public BikeStation(BikeStationId id) {
        this.id = id;
    }

    public BikeStationId id() {
        return id;
    }

    public void placeNew(Bike bike) {
        if (find(bike.id()).isPresent()) {
            throw new BikeAlreadyInStation("Bike already in station");
        }
        bikes.add(new BikeInStation(bike.id()));
    }

    public By<Borrower, ForDuration<RentalDuration, DomainEvents>> rent(
        Bike bike) {
        return borrower -> rentalDuration -> {
            if (rentalDuration.exceedsMaximum()) {
                throw new MaximumRentalDurationExceededException("Bike rent too long");
            }
            return find(bike.id())
                .filter(BikeInStation::isAvailable)
                .map(bikeInStation -> {
                    bikeInStation.rentBy(borrower);
                    return DomainEvents.of(new BikeRentalEvent.BikeRented(id, bike, borrower, rentalDuration));
                })
                .orElseThrow(() -> new BikeNotAvailableException("Bike not available"));
        };
    }

    public DomainEvents returnBike(Bike bike) {
        return find(bike.id())
            .filter(BikeInStation::isRented)
            .map(bikeInStation -> {
                var borrower = bikeInStation.giveBack();
                return DomainEvents.of(new BikeRentalEvent.BikeReturned(id, bike, borrower));
            })
            .orElseThrow(() -> new BikeNotRentedException("Bike is not rented"));
    }

    public List<Bike> availableBikes() {
        return bikes.stream()
                    .filter(BikeInStation::isAvailable)
                    .map(bikeInStation -> new Bike(bikeInStation.id()))
                    .toList();
    }

    private Optional<BikeInStation> find(BikeId bikeId) {
        return bikes.stream().filter(bikeInStation -> bikeInStation.id().equals(bikeId)).findAny();
    }

    @Entity
    private static class BikeInStation {

        private final BikeId id;
        private Borrower rentedBy;

        private BikeInStation(BikeId id) {
            this.id = id;
        }

        boolean isAvailable() {
            return rentedBy == null;
        }

        boolean isRented() {
            return rentedBy != null;
        }

        private void rentBy(Borrower borrower) {
            this.rentedBy = borrower;
        }

        private Borrower giveBack() {
            var borrower = rentedBy;
            rentedBy = null;
            return borrower;
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
