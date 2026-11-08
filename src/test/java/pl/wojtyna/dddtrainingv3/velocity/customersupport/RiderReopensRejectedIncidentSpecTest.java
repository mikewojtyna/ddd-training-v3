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

/// Acceptance tests of spec `rider-reopens-rejected-incident`, over Customer Support's REST resources. The
/// examples' times are Warsaw local time (plan D5).
@VelocitySpringTest
@DisplayName("rider-reopens-rejected-incident")
class RiderReopensRejectedIncidentSpecTest {

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

    private String i6RejectedByZoeAt10OnNov9(String ben) {
        clock.setTo(warsaw("2026-11-09T09:00"));
        var i6 = api.report(ben, "WRONGFUL_CHARGE", "charged PLN 1.00 when I added my card", null);
        api.pickUp(i6, ZOE);
        clock.setTo(warsaw("2026-11-09T10:00"));
        api.reject(i6, ZOE, "the PLN 1.00 was a card check, released at once");
        return i6;
    }

    @Test
    @DisplayName("E1.1 Ben reopens I-6 at 23:59 on 2026-11-23; it is reopened and assigned to Zoe")
    void e1_1_rider_reopens_on_last_minute_of_window() {
        // given
        var ben = unique("ben");
        var i6 = i6RejectedByZoeAt10OnNov9(ben);
        var ridersView = api.incidentAsRider(i6, ben);
        assertThat(ridersView.rejectionReason()).isEqualTo("the PLN 1.00 was a card check, released at once");
        assertThat(ridersView.rejectedAt()).isEqualTo(warsaw("2026-11-09T10:00"));
        assertThat(ridersView.reopenableUntil()).isEqualTo(warsaw("2026-11-24T00:00"));
        clock.setTo(warsaw("2026-11-23T23:59"));

        // when
        var response = api.reopen(i6, ben);

        // then
        assertThat(response.status()).isEqualTo(200);
        var incident = api.incident(i6);
        assertThat(incident.status()).isEqualTo("REOPENED");
        assertThat(incident.assignedAgentId()).isEqualTo(ZOE);
    }

    @Test
    @DisplayName("E1.2 Ben's reopening at 00:00 on 2026-11-24 is rejected; I-6 stays rejected")
    void e1_2_reopening_after_window_is_rejected() {
        // given
        var ben = unique("ben");
        var i6 = i6RejectedByZoeAt10OnNov9(ben);
        assertThat(api.incidentAsRider(i6, ben).reopenableUntil()).isEqualTo(warsaw("2026-11-24T00:00"));
        clock.setTo(warsaw("2026-11-24T00:00"));

        // when
        var response = api.reopen(i6, ben);

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("ReopeningWindowClosed");
        assertThat(api.incident(i6).status()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("E2.1 Anna can't reopen Ben's I-6")
    void e2_1_other_rider_cannot_reopen() {
        // given
        var ben = unique("ben");
        var anna = unique("anna");
        var i6 = i6RejectedByZoeAt10OnNov9(ben);
        clock.setTo(warsaw("2026-11-10T12:00"));

        // when
        var response = api.reopen(i6, anna);

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("NotReportingRider");
        assertThat(api.incident(i6).status()).isEqualTo("REJECTED");
        assertThat(api.incidentStatusCode(i6, anna)).isEqualTo(404);
    }

    @Test
    @DisplayName("E3.1 Anna can't reopen her accepted I-2")
    void e3_1_accepted_incident_cannot_be_reopened() {
        // given
        var anna = unique("anna");
        clock.setTo(warsaw("2026-11-08T18:20"));
        var i2 = api.report(anna, "WRONGFUL_CHARGE", "charged twice for one ride", null);
        api.pickUp(i2, ZOE);
        api.accept(i2, ZOE);

        // when
        var response = api.reopen(i2, anna);

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("IncidentNotRejected");
        assertThat(api.incident(i2).status()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("E3.2 Ben can't reopen I-6 again at 09:30 on 2026-11-12")
    void e3_2_reopened_incident_cannot_be_reopened_again() {
        // given
        var ben = unique("ben");
        var i6 = i6RejectedByZoeAt10OnNov9(ben);
        clock.setTo(warsaw("2026-11-12T09:00"));
        api.reopen(i6, ben);
        clock.setTo(warsaw("2026-11-12T09:30"));

        // when
        var response = api.reopen(i6, ben);

        // then
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("IncidentNotRejected");
        assertThat(api.incident(i6).status()).isEqualTo("REOPENED");
    }
}
