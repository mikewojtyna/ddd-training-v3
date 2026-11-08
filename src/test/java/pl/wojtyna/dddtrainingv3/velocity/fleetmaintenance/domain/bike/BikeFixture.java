package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// Bikes, technicians and fake collaborators for the Fleet Maintenance examples. Times are UTC instants on 2026-11-09.
final class BikeFixture {

    static final BikeId B17 = new BikeId("B-17");
    static final BikeId B22 = new BikeId("B-22");
    static final BikeId B40 = new BikeId("B-40");
    static final TechnicianId TOM = new TechnicianId("tom");
    static final TechnicianId TARA = new TechnicianId("tara");
    static final IncidentId I1 = new IncidentId("I-1");
    static final IncidentId I2 = new IncidentId("I-2");

    private BikeFixture() {
    }

    static Instant at(String hhmm) {
        return Instant.parse("2026-11-09T" + (hhmm.length() == 5 ? hhmm + ":00" : hhmm) + "Z");
    }

    static <T extends DomainEvent> List<T> eventsOf(DomainEvents events, Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    static FaultReport rentalsWithdrew(FaultSource source) {
        return FaultReport.fromRentals(source, Optional.empty());
    }

    static FaultReport fleetIssue(IncidentId incident, String description) {
        return FaultReport.fromFleetIssue(incident, new FaultDescription(description));
    }

    static FaultReport technicianReport(TechnicianId technician, String description) {
        return FaultReport.fromTechnician(technician, new FaultDescription(description));
    }

    /// A bike broken since `since`, reported by Rentals with `source`.
    static Bike brokenBike(BikeId id, FaultSource source, String since) {
        var bike = Bike.unknown(id);
        bike.reportFault(rentalsWithdrew(source)).at(at(since));
        return bike;
    }

    /// A broken bike Rentals holds for `technician` until `until`.
    static Bike reservedBrokenBike(BikeId id, TechnicianId technician, String until) {
        var bike = brokenBike(id, FaultSource.LOCK_FAILED, "08:00");
        bike.reserveFor(technician).through(new FakeServiceReservations(at(until)));
        return bike;
    }

    /// A bike under maintenance by `technician` since `since`.
    static Bike bikeUnderMaintenance(BikeId id, TechnicianId technician, String since) {
        var bike = reservedBrokenBike(id, technician, "09:30");
        bike.startMaintenanceBy(technician).at(at(since));
        return bike;
    }

    /// Rentals' reservations: answers a fixed end time, or refuses because someone else holds the bike.
    static final class FakeServiceReservations implements ServiceReservations {

        final List<TechnicianId> asked = new ArrayList<>();
        private final Instant until;
        private final boolean alreadyHeld;

        FakeServiceReservations(Instant until) {
            this(until, false);
        }

        private FakeServiceReservations(Instant until, boolean alreadyHeld) {
            this.until = until;
            this.alreadyHeld = alreadyHeld;
        }

        static FakeServiceReservations heldBySomeoneElse() {
            return new FakeServiceReservations(null, true);
        }

        @Override
        public Instant reserve(BikeId bike, TechnicianId technician) {
            asked.add(technician);
            if (alreadyHeld) {
                throw new AlreadyReserved(bike.value() + " is held by someone else");
            }
            return until;
        }
    }

    static final class FakeServiceUnlocks implements ServiceUnlocks {

        final List<TechnicianId> asked = new ArrayList<>();

        @Override
        public Outcome unlock(BikeId bike, TechnicianId technician) {
            asked.add(technician);
            return Outcome.UNLOCKED;
        }
    }

    /// Rentals' *Make Bike Available*: records the places, or refuses.
    static final class FakeCirculationReturns implements CirculationReturns {

        final List<Place> places = new ArrayList<>();
        private final boolean refusing;

        FakeCirculationReturns() {
            this(false);
        }

        private FakeCirculationReturns(boolean refusing) {
            this.refusing = refusing;
        }

        static FakeCirculationReturns refusing() {
            return new FakeCirculationReturns(true);
        }

        @Override
        public void makeAvailable(BikeId bike, Place place) {
            if (refusing) {
                throw new CirculationReturnRefused("Rentals refused to make " + bike.value() + " available");
            }
            places.add(place);
        }
    }
}
