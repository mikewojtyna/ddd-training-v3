package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary.customersupportctxacl;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.events.FleetIssueReported;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportBikeBroken;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportBikeBrokenUseCase;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.CUSTOMER_SUPPORT_EVENTS;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.EVENT_JSON;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.FLEET_MAINTENANCE;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.INSTANT;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;

/// Consumer side of Customer Support's integration event `FleetIssueReported` (CML
/// `CustomerSupport [U,OHS,PL]->[D,ACL] FleetMaintenance`): a rider's incident about a faulty bike makes the bike broken,
/// remembering the incident so it can be resolved when the bike is fixed.
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = CUSTOMER_SUPPORT_EVENTS, providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Customer Support → Fleet Maintenance (FleetIssueReported)")
class FleetIssueReportedPactTest {

    private static final String REPORTED_AT = "2026-11-08T10:00:00Z";

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact fleetIssueReported(MessagePactBuilder builder) {
        return builder.expectsToReceive("a rider reported a fleet issue about a bike")
                      .withContent(new PactDslJsonBody().stringType("incidentId", "incident-1")
                                                        .stringType("bikeId", "bike-17")
                                                        .stringType("description", "The rear brake doesn't work")
                                                        .stringMatcher("reportedAt", INSTANT, REPORTED_AT))
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "fleetIssueReported")
    void reportsTheBikeBroken(V4Interaction.AsynchronousMessage message) throws Exception {
        var reportBikeBroken = mock(ReportBikeBrokenUseCase.class);

        new FleetIssueReportedListener(reportBikeBroken).on(EVENT_JSON.readValue(message.contentsAsBytes(), FleetIssueReported.class));

        verify(reportBikeBroken).handle(new ReportBikeBroken("bike-17", "FLEET_ISSUE", "The rear brake doesn't work", "incident-1",
                                                             Instant.parse(REPORTED_AT)));
    }
}
