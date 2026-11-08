package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Station.StationId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Shared givens of the Rentals domain tests: the reference stations, riders, technicians, times on 2026-11-08 and
/// bikes brought into a state through their own commands.
final class BikeFixture {

    static final Stations STATIONS = Stations.reference();
    static final Station CENTRAL = STATIONS.named(new StationId("Central"));
    static final Station OLD_TOWN = STATIONS.named(new StationId("Old Town"));
    static final Station RIVERSIDE = STATIONS.named(new StationId("Riverside"));

    static final Holder ANNA = Holder.rider("anna");
    static final Holder BEN = Holder.rider("ben");
    static final Holder TOM = Holder.technician("tom");
    static final Holder TARA = Holder.technician("tara");

    static final RiderStanding ELIGIBLE = RiderStanding.eligibleHoldingNothing();

    private BikeFixture() {
    }

    /// `"10:00"` or `"10:02:01"` on 2026-11-08, UTC.
    static Instant at(String time) {
        return Instant.parse("2026-11-08T" + (time.length() == 5 ? time + ":00" : time) + "Z");
    }

    static Position near(Station station, double metres) {
        return station.position().northBy(metres);
    }

    static Bike availableAt(String bikeId, Station station) {
        return new Fleet(bike -> false, STATIONS).add(new BikeId(bikeId), station.id(), at("07:00")).bike();
    }

    static Bike reservedFor(Holder rider, String bikeId, Instant reservedAt) {
        var bike = availableAt(bikeId, CENTRAL);
        bike.reserveForRide(rider).standingAt(near(CENTRAL, 10)).given(ELIGIBLE).at(reservedAt);
        return bike;
    }

    static Bike inSessionFor(Holder rider, String bikeId, Instant startedAt) {
        var bike = reservedFor(rider, bikeId, startedAt);
        bike.unlockForRide(rider).given(RentalEligibility.Eligibility.ELIGIBLE).at(startedAt).using(FakeBikeLocks.working());
        return bike;
    }

    static Bike withdrawnAt(String bikeId, Station station, Instant withdrawnAt) {
        var bike = availableAt(bikeId, station);
        bike.withdraw(withdrawnAt);
        return bike;
    }

    static Bike reservedForService(Holder technician, String bikeId, Instant reservedAt) {
        var bike = withdrawnAt(bikeId, OLD_TOWN, reservedAt.minusSeconds(3600));
        bike.reserveForService(technician).at(reservedAt);
        return bike;
    }

    static Bike unlockedForService(Holder technician, String bikeId, Instant unlockedAt) {
        var bike = reservedForService(technician, bikeId, unlockedAt.minusSeconds(60));
        bike.unlockForService(technician).at(unlockedAt).using(FakeBikeLocks.working());
        return bike;
    }

    static <E extends DomainEvent> List<E> eventsOf(DomainEvents events, Class<E> type) {
        var found = new ArrayList<E>();
        events.stream().filter(type::isInstance).map(type::cast).forEach(found::add);
        return found;
    }

    static List<DomainEvent> all(DomainEvents events) {
        return events.stream().toList();
    }
}
