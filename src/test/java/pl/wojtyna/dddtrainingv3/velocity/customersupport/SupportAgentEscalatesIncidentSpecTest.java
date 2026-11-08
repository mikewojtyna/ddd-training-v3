package pl.wojtyna.dddtrainingv3.velocity.customersupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.customersupport.CustomerSupportTestApi.unique;
import static pl.wojtyna.dddtrainingv3.velocity.customersupport.CustomerSupportTestApi.warsaw;

/// Acceptance tests of spec `support-agent-escalates-incident`, over Customer Support's REST resources.
@VelocitySpringTest
@DisplayName("support-agent-escalates-incident")
class SupportAgentEscalatesIncidentSpecTest {

    static final String ZOE = "zoe";
    static final String MAX = "max";

    @LocalServerPort
    int port;

    @Autowired
    ConfigurableApplicationContext context;

    @Autowired
    MutableClock clock;

    CustomerSupportTestApi api;

    @BeforeEach
    void setUp() {
        api = new CustomerSupportTestApi(port, context);
    }

    private String acceptedByZoe(String category, String description) {
        clock.setTo(warsaw("2026-11-09T08:00"));
        var incident = api.report(unique("rider"), category, description, null);
        api.pickUp(incident, ZOE);
        api.accept(incident, ZOE);
        return incident;
    }

    @Test
    @DisplayName("E1.1 Zoe escalates the accepted I-2 to billing; it stays assigned to her")
    void e1_1_assigned_agent_escalates_accepted_incident_to_billing() {
        // given
        var i2 = acceptedByZoe("WRONGFUL_CHARGE", "charged twice for one ride");
        clock.setTo(warsaw("2026-11-09T10:00"));

        // when
        var response = api.escalate(i2, ZOE, "BILLING");

        // then
        assertThat(response.status()).isEqualTo(200);
        var incident = api.incident(i2);
        assertThat(incident.status()).isEqualTo("ESCALATED");
        assertThat(incident.team()).isEqualTo("BILLING");
        assertThat(incident.assignedAgentId()).isEqualTo(ZOE);
    }

    @Test
    @DisplayName("E2.1 the rejected I-6 can't be escalated")
    void e2_1_rejected_incident_cannot_be_escalated() {
        // given
        clock.setTo(warsaw("2026-11-09T09:00"));
        var i6 = api.report(unique("ben"), "WRONGFUL_CHARGE", "charged PLN 1.00 when I added my card", null);
        api.pickUp(i6, ZOE);
        api.reject(i6, ZOE, "the PLN 1.00 was a card check, released at once");

        // when
        var response = api.escalate(i6, ZOE, "BILLING");

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("IncidentNotAccepted");
        assertThat(api.incident(i6).status()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("E2.2 the undecided I-4 can't be escalated")
    void e2_2_undecided_incident_cannot_be_escalated() {
        // given
        clock.setTo(warsaw("2026-11-09T17:40"));
        var i4 = api.report(unique("ben"), "SAFETY", "a car cut me off at the Central crossing", null);
        api.pickUp(i4, ZOE);

        // when
        var response = api.escalate(i4, ZOE, "SAFETY");

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("IncidentNotAccepted");
        assertThat(api.incident(i4).status()).isEqualTo("PICKED_UP");
    }

    @Test
    @DisplayName("E2.3 the escalated I-2 can't be escalated again; it stays with billing (DE-97)")
    void e2_3_escalated_incident_cannot_be_escalated_again() {
        // given
        var i2 = acceptedByZoe("WRONGFUL_CHARGE", "charged twice for one ride");
        api.escalate(i2, ZOE, "BILLING");

        // when
        var response = api.escalate(i2, ZOE, "FLEET");

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("IncidentNotAccepted");
        assertThat(api.incident(i2).team()).isEqualTo("BILLING");
    }

    @Test
    @DisplayName("E3.1 Max can't escalate I-2, accepted by Zoe")
    void e3_1_other_agent_cannot_escalate() {
        // given
        var i2 = acceptedByZoe("WRONGFUL_CHARGE", "charged twice for one ride");

        // when
        var response = api.escalate(i2, MAX, "BILLING");

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("NotAssignedAgent");
        assertThat(api.incident(i2).status()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("E4.1 escalating I-7 to a legal team is rejected")
    void e4_1_escalation_to_unknown_team_is_rejected() {
        // given
        var i7 = acceptedByZoe("OTHER", "the app charged me in the wrong currency");

        // when
        var response = api.escalate(i7, ZOE, "LEGAL");

        // then
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.error()).isEqualTo("UnknownTeam");
        assertThat(api.incident(i7).status()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("E4.2 I-7 (other) is escalated to billing; the team need not match the type (DE-97)")
    void e4_2_team_need_not_match_incident_type() {
        // given
        var i7 = acceptedByZoe("OTHER", "the app charged me in the wrong currency");
        clock.setTo(warsaw("2026-11-09T11:15"));

        // when
        var response = api.escalate(i7, ZOE, "BILLING");

        // then
        assertThat(response.status()).isEqualTo(200);
        var incident = api.incident(i7);
        assertThat(incident.type()).isEqualTo("OTHER");
        assertThat(incident.team()).isEqualTo("BILLING");
    }
}
