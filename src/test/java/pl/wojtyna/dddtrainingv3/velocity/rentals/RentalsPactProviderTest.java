package pl.wojtyna.dddtrainingv3.velocity.rentals;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.DisplayName;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.HttpProviderVerification;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityContractTest;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates;

import java.util.Map;

import static pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates.unique;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.RENTALS;

/// Provider side of Rentals' Open Host Service: verifies Fleet Maintenance's pact (*Bike Availability*, *Withdraw
/// Bike*, *Reserve Bike for service*, *Unlock Bike*, *Make Bike Available*) against the running application.
@VelocityContractTest
@Provider(RENTALS)
@PactFolder(PACT_DIRECTORY)
@DisplayName("Contract: Rentals honours its consumers' pacts")
class RentalsPactProviderTest extends HttpProviderVerification {

    /// The technician Fleet Maintenance's pact acts for.
    private static final String TECHNICIAN = "tech-pact-1";

    @State("a bike parked at station Central")
    Map<String, Object> bikeAtCentral() {
        return Map.of("bikeId", states.bikeAt(CENTRAL));
    }

    @State("a withdrawn bike")
    Map<String, Object> withdrawnBike() {
        var bikeId = states.bikeAt(CENTRAL);
        states.withdraw(bikeId);
        return Map.of("bikeId", bikeId);
    }

    @State("a withdrawn bike reserved by another technician")
    Map<String, Object> withdrawnBikeReservedByAnotherTechnician() {
        return Map.of("bikeId", states.withdrawnBikeReservedBy(unique("tech-other")));
    }

    @State("a withdrawn bike reserved by technician " + TECHNICIAN)
    Map<String, Object> withdrawnBikeReservedByTheTechnician() {
        return Map.of("bikeId", states.withdrawnBikeReservedBy(TECHNICIAN));
    }

    @State("a withdrawn bike unlocked for technician " + TECHNICIAN)
    Map<String, Object> withdrawnBikeUnlockedForTheTechnician() {
        var bikeId = states.withdrawnBikeReservedBy(TECHNICIAN);
        ProviderStates.expect(states.unlockForTechnician(bikeId, TECHNICIAN), 200);
        return Map.of("bikeId", bikeId);
    }
}
