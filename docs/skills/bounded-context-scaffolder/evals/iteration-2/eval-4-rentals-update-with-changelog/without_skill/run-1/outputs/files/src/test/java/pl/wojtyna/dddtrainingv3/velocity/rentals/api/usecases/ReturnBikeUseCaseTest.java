package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikesAtStationQuery;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.*;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.repository.InMemoryBikeStationRepo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Return bike use case examples")
class ReturnBikeUseCaseTest {

    // @formatter:off
    @DisplayName(
        """
         given bike station where bike "bike01" was rented by George,
         when the rider app returns bike "bike01" to the station,
         then Bike Returned is published and the bike is listed as available again
        """
    )
    // @formatter:on
    @Test
    void test0() {
        // given
        var repo = new InMemoryBikeStationRepo();
        var publisher = new RecordingPublisher();
        var stationId = new BikeStationId("Warsaw01");
        var station = new BikeStation(stationId);
        var bike = new Bike(new BikeId("bike01"));
        station.placeNew(bike);
        station.rent(bike)
               .by(new Borrower(new BorrowerId("george")))
               .forDuration(new RentalDuration(Duration.ofHours(6)));
        repo.save(station);
        var useCase = new ReturnBikeUseCase(repo, publisher);
        var query = new AvailableBikesAtStationQuery(repo);

        // when
        useCase.returnBike(stationId, bike.id());

        // then
        assertThat(publisher.published).hasOnlyElementsOfType(BikeRentalEvent.BikeReturned.class).hasSize(1);
        assertThat(query.availableBikesAt(stationId).bikeIds()).containsExactly("bike01");
    }

    private static class RecordingPublisher implements DomainEventPublisher {

        private final List<DomainEvent> published = new ArrayList<>();

        @Override
        public void publish(DomainEvents events) {
            events.stream().forEach(published::add);
        }

        @Override
        public void publish(DomainEvent event) {
            published.add(event);
        }
    }
}
