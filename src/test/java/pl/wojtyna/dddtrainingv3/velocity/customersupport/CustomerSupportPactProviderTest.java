package pl.wojtyna.dddtrainingv3.velocity.customersupport;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.DisplayName;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.HttpProviderVerification;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityContractTest;

import java.util.Map;

import static pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.CUSTOMER_SUPPORT;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;

/// Provider side of Customer Support's Open Host Service: verifies Fleet Maintenance's pact (*Resolve Fleet Issue*)
/// against the running application.
@VelocityContractTest
@Provider(CUSTOMER_SUPPORT)
@PactFolder(PACT_DIRECTORY)
@DisplayName("Contract: Customer Support honours its consumers' pacts")
class CustomerSupportPactProviderTest extends HttpProviderVerification {

    /// A rider reports a broken bike that is in VeloCity's fleet: the incident is a fleet issue (DE-50).
    @State("a fleet issue incident about a bike")
    Map<String, Object> fleetIssueAboutABike() {
        var bikeId = states.bikeAt(CENTRAL);
        return Map.of("incidentId", states.incident("BROKEN_BIKE", bikeId), "bikeId", bikeId);
    }

    /// A wrongful charge names no bike, so it is no fleet issue of any bike.
    @State("an incident that is not a fleet issue")
    Map<String, Object> incidentThatIsNotAFleetIssue() {
        return Map.of("incidentId", states.incident("WRONGFUL_CHARGE", null));
    }
}
