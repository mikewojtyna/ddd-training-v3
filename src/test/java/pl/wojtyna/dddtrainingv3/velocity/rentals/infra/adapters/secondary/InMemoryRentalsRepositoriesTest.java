package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.*;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.Borrowing;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.eligibility.Eligibility.ELIGIBLE;

@DisplayName("In-memory Rentals repositories: optimistic locking")
class InMemoryRentalsRepositoriesTest {

    private static final Instant TEN = Instant.parse("2026-11-07T10:00:00Z");

    @Test
    void bikeStationRepoRejectsAStaleSave() {
        // given a saved station with two bikes, loaded twice
        var repo = new InMemoryBikeStationRepo();
        var station = new BikeStation(new BikeStationId("Central"));
        station.placeNew(new Bike(new BikeId("B-17")));
        station.placeNew(new Bike(new BikeId("B-22")));
        repo.save(station);
        var first = repo.load(station.id()).orElseThrow();
        var second = repo.load(station.id()).orElseThrow();
        first.reserve(new Bike(new BikeId("B-17"))).by(new Borrower(new BorrowerId("anna"))).at(TEN);
        second.reserve(new Bike(new BikeId("B-22"))).by(new Borrower(new BorrowerId("ben"))).at(TEN);

        // when / then
        repo.save(first);
        assertThat(catchThrowable(() -> repo.save(second))).isInstanceOf(OptimisticLockingException.class);
    }

    @Test
    void borrowingRepositoryRejectsAStaleSave() {
        // given a saved borrowing, loaded twice
        var repo = new InMemoryBorrowingRepository();
        repo.save(new Borrowing(new BorrowerId("anna")));
        var first = repo.load(new BorrowerId("anna")).orElseThrow();
        var second = repo.load(new BorrowerId("anna")).orElseThrow();
        first.reserve(new BikeId("B-17")).as(ELIGIBLE).at(TEN);
        second.reserve(new BikeId("B-22")).as(ELIGIBLE).at(TEN);

        // when / then
        repo.save(first);
        assertThat(catchThrowable(() -> repo.save(second))).isInstanceOf(OptimisticLockingException.class);
    }
}
