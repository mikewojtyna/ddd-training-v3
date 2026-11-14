package pl.wojtyna.dddtrainingv3.velocity.rentals;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.DisplayName;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.MessageProviderVerification;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityContractTest;
import org.springframework.beans.factory.annotation.Autowired;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeUnlocked;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.RentalSessionEnded;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.ReservationExpired;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.UnlockFailed;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.primary.RentalsClockJobs;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.bikelocksacl.SimulatedBikeLocks;

import java.time.Duration;

import static pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates.OLD_TOWN_SQUARE_LATITUDE;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates.OLD_TOWN_SQUARE_LONGITUDE;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates.unique;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.RENTALS_EVENTS;

/// Provider side of Rentals' integration events: verifies the pacts of Settlement (`RentalSessionEnded`) and Fleet
/// Maintenance (`BikeWithdrawn`, `BikeUnlocked`, `ReservationExpired`, `UnlockFailed`) with the events Rentals really
/// publishes. The bike locks are Rentals' simulated ones; reservation expiry is driven by Rentals' clock job.
@VelocityContractTest
@Provider(RENTALS_EVENTS)
@PactFolder(PACT_DIRECTORY)
@DisplayName("Contract: Rentals publishes the events its consumers expect")
class RentalsEventsPactProviderTest extends MessageProviderVerification {

    @Autowired
    private RentalsClockJobs clockJobs;

    @Autowired
    private SimulatedBikeLocks bikeLocks;

    @PactVerifyProvider("a rental session ended with a return at a station")
    String sessionEndedAtStation() {
        var riderId = states.onboardedRider();
        var bikeId = states.bikeAt(CENTRAL);
        states.rideFromCentral(riderId, bikeId, 30);
        states.returnAtStation(bikeId, riderId, CENTRAL);
        return asMessage(events.awaitFirst(RentalSessionEnded.class, event -> event.bikeId().equals(bikeId)));
    }

    @PactVerifyProvider("a rental session ended with a free-floating return")
    String sessionEndedFreeFloating() {
        var riderId = states.onboardedRider();
        var bikeId = states.bikeAt(CENTRAL);
        states.rideFromCentral(riderId, bikeId, 30);
        states.endRideHere(bikeId, riderId, OLD_TOWN_SQUARE_LATITUDE, OLD_TOWN_SQUARE_LONGITUDE);
        return asMessage(events.awaitFirst(RentalSessionEnded.class, event -> event.bikeId().equals(bikeId)));
    }

    /// The bike's telemetry signals a fault while nobody rides it: Rentals withdraws it at once (`FAULT_SIGNALLED`).
    @PactVerifyProvider("a bike was withdrawn because of a fault")
    String bikeWithdrawnForAFault() {
        var bikeId = states.bikeAt(CENTRAL);
        states.signalFault(bikeId, "E42");
        return asMessage(events.awaitFirst(BikeWithdrawn.class, event -> event.bikeId().equals(bikeId)));
    }

    @PactVerifyProvider("a withdrawn bike was unlocked for a technician")
    String bikeUnlockedForATechnician() {
        var technician = unique("tech");
        var bikeId = states.withdrawnBikeReservedBy(technician);
        states.unlockForTechnician(bikeId, technician);
        return asMessage(events.awaitFirst(BikeUnlocked.class, event -> event.bikeId().equals(bikeId)));
    }

    /// A technician's reservation holds for 30 minutes (DE-11) and is released from the second after its end (DE-81).
    @PactVerifyProvider("a technician's reservation expired")
    String technicianReservationExpired() {
        var bikeId = states.withdrawnBikeReservedBy(unique("tech"));
        states.clock().advanceBy(Duration.ofMinutes(31));
        clockJobs.releaseExpiredReservations();
        return asMessage(events.awaitFirst(ReservationExpired.class, event -> event.bikeId().equals(bikeId)));
    }

    @PactVerifyProvider("a technician's unlock failed")
    String technicianUnlockFailed() {
        var technician = unique("tech");
        var bikeId = states.withdrawnBikeReservedBy(technician);
        bikeLocks.registerFaulty(bikeId);
        states.unlockForTechnician(bikeId, technician);
        return asMessage(events.awaitFirst(UnlockFailed.class, event -> event.bikeId().equals(bikeId)));
    }
}
