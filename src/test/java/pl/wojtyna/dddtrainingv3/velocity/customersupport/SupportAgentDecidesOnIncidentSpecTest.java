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

/// Acceptance tests of spec `support-agent-decides-on-incident`, over Customer Support's REST resources.
@VelocitySpringTest
@DisplayName("support-agent-decides-on-incident")
class SupportAgentDecidesOnIncidentSpecTest {

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

    private String annasI2() {
        clock.setTo(warsaw("2026-11-08T18:20"));
        return api.report(unique("anna"), "WRONGFUL_CHARGE", "charged twice for one ride", null);
    }

    private String bensI6RejectedAndReopened() {
        var ben = unique("ben");
        clock.setTo(warsaw("2026-11-09T09:00"));
        var i6 = api.report(ben, "WRONGFUL_CHARGE", "charged PLN 1.00 when I added my card", null);
        api.pickUp(i6, ZOE);
        clock.setTo(warsaw("2026-11-09T10:00"));
        api.reject(i6, ZOE, "the PLN 1.00 was a card check, released at once");
        clock.setTo(warsaw("2026-11-12T09:00"));
        assertThat(api.reopen(i6, ben).status()).isEqualTo(200);
        return i6;
    }

    @Test
    @DisplayName("E1.1 support agent Zoe picks up Anna's open incident I-2")
    void e1_1_agent_picks_up_open_incident() {
        // given
        var i2 = annasI2();
        assertThat(api.openIncidents()).anySatisfy(open -> assertThat(open).containsEntry("incidentId", i2)
                                                                           .containsEntry("type", "BILLING_DISPUTE")
                                                                           .containsEntry("category", "WRONGFUL_CHARGE"));
        clock.setTo(warsaw("2026-11-09T09:00"));

        // when
        var response = api.pickUp(i2, ZOE);

        // then
        assertThat(response.status()).isEqualTo(200);
        assertThat(api.incident(i2).assignedAgentId()).isEqualTo(ZOE);
        assertThat(api.incident(i2).status()).isEqualTo("PICKED_UP");
        assertThat(api.openIncidents()).noneSatisfy(open -> assertThat(open).containsEntry("incidentId", i2));
    }

    @Test
    @DisplayName("E1.2 Max's pick-up of I-2, assigned to Zoe, is rejected")
    void e1_2_second_pick_up_is_rejected() {
        // given
        var i2 = annasI2();
        api.pickUp(i2, ZOE);

        // when
        var response = api.pickUp(i2, MAX);

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("IncidentAlreadyAssigned");
        assertThat(api.incident(i2).assignedAgentId()).isEqualTo(ZOE);
    }

    @Test
    @DisplayName("E2.1 Zoe accepts I-2")
    void e2_1_assigned_agent_accepts_incident() {
        // given
        var i2 = annasI2();
        api.pickUp(i2, ZOE);
        clock.setTo(warsaw("2026-11-09T09:30"));

        // when
        var response = api.accept(i2, ZOE);

        // then
        assertThat(response.status()).isEqualTo(200);
        assertThat(api.incident(i2).status()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("E2.2 Zoe rejects Ben's I-6 with a reason")
    void e2_2_assigned_agent_rejects_incident_with_reason() {
        // given
        clock.setTo(warsaw("2026-11-09T09:00"));
        var i6 = api.report(unique("ben"), "WRONGFUL_CHARGE", "charged PLN 1.00 when I added my card", null);
        api.pickUp(i6, ZOE);
        clock.setTo(warsaw("2026-11-09T10:00"));

        // when
        var response = api.reject(i6, ZOE, "the PLN 1.00 was a card check, released at once");

        // then
        assertThat(response.status()).isEqualTo(200);
        var incident = api.incident(i6);
        assertThat(incident.status()).isEqualTo("REJECTED");
        assertThat(incident.rejectionReason()).isEqualTo("the PLN 1.00 was a card check, released at once");
        assertThat(incident.rejectedAt()).isEqualTo(warsaw("2026-11-09T10:00"));
    }

    @Test
    @DisplayName("E2.3 Zoe accepts the reopened I-6 (DE-97)")
    void e2_3_assigned_agent_accepts_reopened_incident() {
        // given
        var i6 = bensI6RejectedAndReopened();
        clock.setTo(warsaw("2026-11-13T09:00"));

        // when
        var response = api.accept(i6, ZOE);

        // then
        assertThat(response.status()).isEqualTo(200);
        assertThat(api.incident(i6).status()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("E3.1 Max can't accept I-2, assigned to Zoe")
    void e3_1_other_agent_cannot_accept() {
        // given
        var i2 = annasI2();
        api.pickUp(i2, ZOE);

        // when
        var response = api.accept(i2, MAX);

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("NotAssignedAgent");
        assertThat(api.incident(i2).status()).isEqualTo("PICKED_UP");
        assertThat(api.incident(i2).assignedAgentId()).isEqualTo(ZOE);
    }

    @Test
    @DisplayName("E3.2 Zoe can't accept Ben's open I-4 without picking it up")
    void e3_2_open_incident_cannot_be_accepted_without_pick_up() {
        // given
        clock.setTo(warsaw("2026-11-09T17:40"));
        var i4 = api.report(unique("ben"), "SAFETY", "a car cut me off at the Central crossing", null);

        // when
        var response = api.accept(i4, ZOE);

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("NotAssignedAgent");
        assertThat(api.incident(i4).status()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("E4.1 Zoe can't reject I-2 after accepting it")
    void e4_1_accepted_incident_cannot_be_rejected() {
        // given
        var i2 = annasI2();
        api.pickUp(i2, ZOE);
        api.accept(i2, ZOE);

        // when
        var response = api.reject(i2, ZOE, "changed my mind");

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("IncidentAlreadyDecided");
        assertThat(api.incident(i2).status()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("E4.2 Zoe can't reject the reopened I-6 again (DE-54)")
    void e4_2_reopened_incident_cannot_be_rejected_again() {
        // given
        var i6 = bensI6RejectedAndReopened();

        // when
        var response = api.reject(i6, ZOE, "still a card check");

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("ReopenedIncidentCannotBeRejected");
        assertThat(api.incident(i6).status()).isEqualTo("REOPENED");
    }

    @Test
    @DisplayName("A rejection without a reason is malformed input (DE-97)")
    void rejection_without_reason_is_bad_request() {
        // given
        var i2 = annasI2();
        api.pickUp(i2, ZOE);

        // when
        var response = api.reject(i2, ZOE, " ");

        // then
        assertThat(response.status()).isEqualTo(400);
        assertThat(api.incident(i2).status()).isEqualTo("PICKED_UP");
    }
}
