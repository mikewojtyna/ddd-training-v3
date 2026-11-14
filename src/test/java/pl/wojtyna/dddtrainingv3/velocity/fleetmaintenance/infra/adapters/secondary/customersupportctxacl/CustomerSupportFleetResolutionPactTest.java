package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.customersupportctxacl;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.FleetIssueNotResolved;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.IncidentId;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.CUSTOMER_SUPPORT;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.FLEET_MAINTENANCE;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.urlsOf;

/// Consumer side of `CustomerSupport [U,OHS,PL]->[D,ACL] FleetMaintenance`: when a bike is fixed, Fleet Maintenance asks
/// Customer Support to *Resolve Fleet Issue* of the incident that reported it. Any `2xx` is success; `404` (unknown
/// incident) and `409` (not a fleet issue of that bike) become `FleetIssueNotResolved`.
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = CUSTOMER_SUPPORT, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Fleet Maintenance → Customer Support (resolve fleet issue)")
class CustomerSupportFleetResolutionPactTest {

    private static final String INCIDENT = "incident-1";
    private static final String UNKNOWN_INCIDENT = "incident-unknown-to-customer-support";
    private static final String BIKE = "bike-17";

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact fleetIssueIsResolved(PactDslWithProvider builder) {
        return builder.given("a fleet issue incident about a bike")
                      .uponReceiving("resolving the fleet issue of the bike it reported")
                      .pathFromProviderState("/customer-support/incidents/${incidentId}/fleet-resolution",
                                             "/customer-support/incidents/" + INCIDENT + "/fleet-resolution")
                      .method("POST")
                      .headers(Map.of("Content-Type", "application/json"))
                      .body(new PactDslJsonBody().valueFromProviderState("bikeId", "${bikeId}", BIKE))
                      .willRespondWith()
                      .status(200)
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact unknownIncidentIsNotResolved(PactDslWithProvider builder) {
        return builder.uponReceiving("resolving the fleet issue of an incident Customer Support doesn't know")
                      .path("/customer-support/incidents/" + UNKNOWN_INCIDENT + "/fleet-resolution")
                      .method("POST")
                      .headers(Map.of("Content-Type", "application/json"))
                      .body(new PactDslJsonBody().stringValue("bikeId", BIKE))
                      .willRespondWith()
                      .status(404)
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact incidentThatIsNotAFleetIssueIsNotResolved(PactDslWithProvider builder) {
        return builder.given("an incident that is not a fleet issue")
                      .uponReceiving("resolving the fleet issue of an incident that is not a fleet issue")
                      .pathFromProviderState("/customer-support/incidents/${incidentId}/fleet-resolution",
                                             "/customer-support/incidents/" + INCIDENT + "/fleet-resolution")
                      .method("POST")
                      .headers(Map.of("Content-Type", "application/json"))
                      .body(new PactDslJsonBody().stringValue("bikeId", BIKE))
                      .willRespondWith()
                      .status(409)
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "fleetIssueIsResolved")
    void fleetIssueIsResolved(MockServer customerSupport) {
        assertThatCode(() -> resolutions(customerSupport).resolve(new IncidentId(INCIDENT), new BikeId(BIKE)))
            .doesNotThrowAnyException();
    }

    @Test
    @PactTestFor(pactMethod = "unknownIncidentIsNotResolved")
    void unknownIncidentIsNotResolved(MockServer customerSupport) {
        assertThatThrownBy(() -> resolutions(customerSupport).resolve(new IncidentId(UNKNOWN_INCIDENT), new BikeId(BIKE)))
            .isInstanceOf(FleetIssueNotResolved.class);
    }

    @Test
    @PactTestFor(pactMethod = "incidentThatIsNotAFleetIssueIsNotResolved")
    void incidentThatIsNotAFleetIssueIsNotResolved(MockServer customerSupport) {
        assertThatThrownBy(() -> resolutions(customerSupport).resolve(new IncidentId(INCIDENT), new BikeId(BIKE)))
            .isInstanceOf(FleetIssueNotResolved.class);
    }

    private static CustomerSupportFleetIssueResolutionsAdapter resolutions(MockServer customerSupport) {
        return new CustomerSupportFleetIssueResolutionsAdapter(RestClient.builder().build(), urlsOf(customerSupport.getUrl()));
    }
}
