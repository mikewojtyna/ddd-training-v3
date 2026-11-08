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

/// REST-level test of Customer Support's OHS command *Resolve Fleet Issue*
/// (`POST /customer-support/incidents/{incidentId}/fleet-resolution`), plan technician-returns-fixed-bike-to-circulation
/// S5. The end-to-end examples (Fleet Maintenance calling it) are that plan's S9.
@VelocitySpringTest
@DisplayName("Resolve Fleet Issue (OHS)")
class ResolveFleetIssueRestTest {

    static final String ZOE = "zoe";

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

    @Test
    @DisplayName("E3.1 (CS half) an accepted and a reported fleet issue of B-17 are both resolved with \"bike fixed\"")
    void fleet_issues_in_any_open_state_are_resolved_with_bike_fixed() {
        // given
        var b17 = unique("B-17");
        clock.setTo(warsaw("2026-11-08T09:00"));
        var i1 = api.report(unique("anna"), "BROKEN_BIKE", "rear brake doesn't work", b17);
        api.pickUp(i1, ZOE);
        api.accept(i1, ZOE);
        var i2 = api.report(unique("ben"), "CANT_UNLOCK", "the lock doesn't open", b17);
        clock.setTo(warsaw("2026-11-08T11:00"));

        // when
        var first = api.resolveFleetIssue(i1, b17);
        var second = api.resolveFleetIssue(i2, b17);

        // then
        assertThat(first.status()).isEqualTo(200);
        assertThat(second.status()).isEqualTo(200);
        assertThat(api.incident(i1).resolution()).isEqualTo("bike fixed");
        assertThat(api.incident(i2).status()).isEqualTo("RESOLVED");
        assertThat(api.incident(i2).resolution()).isEqualTo("bike fixed");
    }

    @Test
    @DisplayName("E3.2 (CS half) an incident Zoe already resolved keeps her resolution")
    void already_resolved_incident_keeps_its_resolution() {
        // given
        var b17 = unique("B-17");
        clock.setTo(warsaw("2026-11-08T09:00"));
        var i1 = api.report(unique("anna"), "BROKEN_BIKE", "rear brake doesn't work", b17);
        api.pickUp(i1, ZOE);
        api.accept(i1, ZOE);
        clock.setTo(warsaw("2026-11-08T10:00"));
        api.resolve(i1, ZOE, "brake adjusted on site");
        clock.setTo(warsaw("2026-11-08T11:00"));

        // when
        var response = api.resolveFleetIssue(i1, b17);

        // then
        assertThat(response.status()).isEqualTo(200);
        assertThat(api.incident(i1).resolution()).isEqualTo("brake adjusted on site");
    }

    @Test
    @DisplayName("an unknown incident answers 404; an incident that isn't a fleet issue of the bike answers 409")
    void unknown_incident_and_wrong_bike_are_rejected() {
        // given
        clock.setTo(warsaw("2026-11-08T09:00"));
        var billingDispute = api.report(unique("anna"), "WRONGFUL_CHARGE", "charged twice for one ride", null);

        // when
        var unknown = api.resolveFleetIssue(unique("I-unknown"), "B-17");
        var notAFleetIssue = api.resolveFleetIssue(billingDispute, "B-17");

        // then
        assertThat(unknown.status()).isEqualTo(404);
        assertThat(unknown.error()).isEqualTo("IncidentNotFound");
        assertThat(notAFleetIssue.status()).isEqualTo(409);
        assertThat(notAFleetIssue.error()).isEqualTo("NotAFleetIssueOfBike");
    }
}
