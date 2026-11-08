package pl.wojtyna.dddtrainingv3.velocity.customersupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases.ResolveUncontestedRejections;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases.ResolveUncontestedRejectionsUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.customersupport.CustomerSupportTestApi.unique;
import static pl.wojtyna.dddtrainingv3.velocity.customersupport.CustomerSupportTestApi.warsaw;

/// Acceptance tests of spec `support-agent-resolves-incident`. Agents act over REST; the support clock (E6.x) is
/// driven by calling the time-driven use case directly, because scheduling is off in tests.
@VelocitySpringTest
@DisplayName("support-agent-resolves-incident")
class SupportAgentResolvesIncidentSpecTest {

    static final String ZOE = "zoe";
    static final String MAX = "max";

    @LocalServerPort
    int port;

    @Autowired
    ConfigurableApplicationContext context;

    @Autowired
    MutableClock clock;

    @Autowired
    ResolveUncontestedRejectionsUseCase resolveUncontestedRejections;

    CustomerSupportTestApi api;

    @BeforeEach
    void setUp() {
        api = new CustomerSupportTestApi(port, context);
    }

    private String bensI4AcceptedByZoe() {
        clock.setTo(warsaw("2026-11-09T13:00"));
        var i4 = api.report(unique("ben"), "SAFETY", "a car cut me off at the Central crossing", null);
        api.pickUp(i4, ZOE);
        api.accept(i4, ZOE);
        return i4;
    }

    private String annasI2EscalatedToBilling() {
        clock.setTo(warsaw("2026-11-09T09:00"));
        var i2 = api.report(unique("anna"), "WRONGFUL_CHARGE", "charged twice for one ride", null);
        api.pickUp(i2, ZOE);
        api.accept(i2, ZOE);
        api.escalate(i2, ZOE, "BILLING");
        return i2;
    }

    private String bensI6RejectedAt10OnNov9(String ben) {
        clock.setTo(warsaw("2026-11-09T09:00"));
        var i6 = api.report(ben, "WRONGFUL_CHARGE", "charged PLN 1.00 when I added my card", null);
        api.pickUp(i6, ZOE);
        clock.setTo(warsaw("2026-11-09T10:00"));
        api.reject(i6, ZOE, "the PLN 1.00 was a card check, released at once");
        return i6;
    }

    @Test
    @DisplayName("E1.1 Zoe resolves the accepted I-4 directly")
    void e1_1_assigned_agent_resolves_accepted_incident() {
        // given
        var i4 = bensI4AcceptedByZoe();
        clock.setTo(warsaw("2026-11-09T15:00"));

        // when
        var response = api.resolve(i4, ZOE, "talked to Ben, crossing reported to the city");

        // then
        assertThat(response.status()).isEqualTo(200);
        var incident = api.incident(i4);
        assertThat(incident.status()).isEqualTo("RESOLVED");
        assertThat(incident.resolution()).isEqualTo("talked to Ben, crossing reported to the city");
    }

    @Test
    @DisplayName("E2.1 Zoe records the billing team's outcome for the escalated I-2")
    void e2_1_agent_records_team_outcome_for_escalated_incident() {
        // given
        var i2 = annasI2EscalatedToBilling();
        clock.setTo(warsaw("2026-11-10T16:00"));

        // when
        var response = api.resolve(i2, ZOE, "billing team refunded the double charge");

        // then
        assertThat(response.status()).isEqualTo(200);
        var incident = api.incident(i2);
        assertThat(incident.status()).isEqualTo("RESOLVED");
        assertThat(incident.resolution()).isEqualTo("billing team refunded the double charge");
        assertThat(incident.team()).isEqualTo("BILLING");
    }

    @Test
    @DisplayName("E3.1 Zoe resolves the reopened I-6")
    void e3_1_rejecting_agent_resolves_reopened_incident() {
        // given
        var ben = unique("ben");
        var i6 = bensI6RejectedAt10OnNov9(ben);
        clock.setTo(warsaw("2026-11-12T09:00"));
        api.reopen(i6, ben);
        clock.setTo(warsaw("2026-11-13T11:00"));

        // when
        var response = api.resolve(i6, ZOE, "explained the PLN 1.00 card check again, Ben agreed");

        // then
        assertThat(response.status()).isEqualTo(200);
        assertThat(api.incident(i6).status()).isEqualTo("RESOLVED");
    }

    @Test
    @DisplayName("E4.1 Max can't resolve I-2, escalated and assigned to Zoe")
    void e4_1_other_agent_cannot_resolve() {
        // given
        var i2 = annasI2EscalatedToBilling();

        // when
        var response = api.resolve(i2, MAX, "refunded");

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("NotAssignedAgent");
        assertThat(api.incident(i2).status()).isEqualTo("ESCALATED");
    }

    @Test
    @DisplayName("E5.1 the undecided I-4 can't be resolved")
    void e5_1_undecided_incident_cannot_be_resolved() {
        // given
        clock.setTo(warsaw("2026-11-09T17:40"));
        var i4 = api.report(unique("ben"), "SAFETY", "a car cut me off at the Central crossing", null);
        api.pickUp(i4, ZOE);

        // when
        var response = api.resolve(i4, ZOE, "talked to Ben");

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("IncidentNotResolvable");
        assertThat(api.incident(i4).status()).isEqualTo("PICKED_UP");
    }

    @Test
    @DisplayName("E5.2 the rejected, not reopened I-6 can't be resolved by Zoe")
    void e5_2_rejected_incident_cannot_be_resolved_by_agent() {
        // given
        var i6 = bensI6RejectedAt10OnNov9(unique("ben"));

        // when
        var response = api.resolve(i6, ZOE, "closing");

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("IncidentNotResolvable");
        assertThat(api.incident(i6).status()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("E5.3 the resolved I-4 keeps its first resolution")
    void e5_3_resolved_incident_keeps_its_first_resolution() {
        // given
        var i4 = bensI4AcceptedByZoe();
        clock.setTo(warsaw("2026-11-09T15:00"));
        api.resolve(i4, ZOE, "talked to Ben, crossing reported to the city");

        // when
        var response = api.resolve(i4, ZOE, "follow-up call");

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("IncidentNotResolvable");
        assertThat(api.incident(i4).resolution()).isEqualTo("talked to Ben, crossing reported to the city");
    }

    @Test
    @DisplayName("E6.1 the uncontested rejection of I-6 is resolved when its window ends (DE-87)")
    void e6_1_uncontested_rejection_is_resolved_when_window_ends() {
        // given
        var i6 = bensI6RejectedAt10OnNov9(unique("ben"));
        resolveUncontestedRejections.handle(new ResolveUncontestedRejections(warsaw("2026-11-23T23:59")));
        assertThat(api.incident(i6).status()).isEqualTo("REJECTED");
        clock.setTo(warsaw("2026-11-24T00:00"));

        // when
        resolveUncontestedRejections.handle(new ResolveUncontestedRejections(clock.instant()));

        // then
        var incident = api.incident(i6);
        assertThat(incident.status()).isEqualTo("RESOLVED");
        assertThat(incident.resolution()).isEqualTo("rejection not contested");
    }

    @Test
    @DisplayName("E6.2 the reopened I-6 isn't resolved by the clock")
    void e6_2_reopened_incident_is_not_resolved_by_the_clock() {
        // given
        var ben = unique("ben");
        var i6 = bensI6RejectedAt10OnNov9(ben);
        clock.setTo(warsaw("2026-11-12T09:00"));
        api.reopen(i6, ben);
        clock.setTo(warsaw("2026-11-24T00:00"));

        // when
        resolveUncontestedRejections.handle(new ResolveUncontestedRejections(clock.instant()));

        // then
        var incident = api.incident(i6);
        assertThat(incident.status()).isEqualTo("REOPENED");
        assertThat(incident.assignedAgentId()).isEqualTo(ZOE);
    }
}
