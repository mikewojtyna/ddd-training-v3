package pl.wojtyna.dddtrainingv3.velocity.customersupport;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.DisplayName;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.MessageProviderVerification;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityContractTest;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.events.FleetIssueReported;

import static pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.CUSTOMER_SUPPORT_EVENTS;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;

/// Provider side of Customer Support's integration events: verifies Fleet Maintenance's pact for `FleetIssueReported`
/// with the event Customer Support really publishes when a rider reports a broken bike.
@VelocityContractTest
@Provider(CUSTOMER_SUPPORT_EVENTS)
@PactFolder(PACT_DIRECTORY)
@DisplayName("Contract: Customer Support publishes the events its consumers expect")
class CustomerSupportEventsPactProviderTest extends MessageProviderVerification {

    @PactVerifyProvider("a rider reported a fleet issue about a bike")
    String fleetIssueReported() {
        var bikeId = states.bikeAt(CENTRAL);
        var incidentId = states.incident("BROKEN_BIKE", bikeId);
        return asMessage(events.awaitFirst(FleetIssueReported.class, event -> event.incidentId().equals(incidentId)));
    }
}
