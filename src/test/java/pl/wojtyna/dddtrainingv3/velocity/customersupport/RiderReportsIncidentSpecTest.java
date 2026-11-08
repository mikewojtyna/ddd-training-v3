package pl.wojtyna.dddtrainingv3.velocity.customersupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.events.FleetIssueReported;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.customersupport.CustomerSupportTestApi.unique;
import static pl.wojtyna.dddtrainingv3.velocity.customersupport.CustomerSupportTestApi.warsaw;

/// Acceptance tests of spec `rider-reports-incident`, over Customer Support's REST resources.
@VelocitySpringTest
@DisplayName("rider-reports-incident")
class RiderReportsIncidentSpecTest {

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
    @DisplayName("E1.1 a wrongful-charge report is an open billing dispute with no assigned agent")
    void e1_1_wrongful_charge_report_is_open_billing_dispute() {
        // given
        var anna = unique("anna");
        clock.setTo(warsaw("2026-11-08T18:20"));

        // when
        var response = api.post("", java.util.Map.of("riderId", anna, "category", "WRONGFUL_CHARGE",
                                                      "description", "charged twice for one ride"));

        // then
        assertThat(response.status()).isEqualTo(201);
        assertThat(response.body()).containsEntry("type", "BILLING_DISPUTE");
        var incident = api.incident((String) response.body().get("incidentId"));
        assertThat(incident.riderId()).isEqualTo(anna);
        assertThat(incident.status()).isEqualTo("OPEN");
        assertThat(incident.assignedAgentId()).isNull();
    }

    @Test
    @DisplayName("E1.2 a blocked rider can report an incident (DE-97)")
    void e1_2_blocked_rider_can_report_incident() {
        // given: Ben's account is blocked for outstanding debt; Customer Support doesn't read blocks (plan D6)
        var ben = unique("ben");
        clock.setTo(warsaw("2026-11-10T09:10"));

        // when
        var incidentId = api.report(ben, "OTHER", "the app shows a ride I didn't take", null);

        // then
        var incident = api.incident(incidentId);
        assertThat(incident.riderId()).isEqualTo(ben);
        assertThat(incident.type()).isEqualTo("OTHER");
    }

    @Test
    @DisplayName("E2.1 a can't-unlock report for a bike is a fleet issue handed to Fleet Maintenance")
    void e2_1_cant_unlock_with_bike_is_fleet_issue() {
        // given
        var ben = unique("ben");
        var b22 = unique("B-22");
        clock.setTo(warsaw("2026-11-09T08:05"));

        // when
        var incidentId = api.report(ben, "CANT_UNLOCK", "the lock doesn't open", b22);

        // then
        assertThat(api.incident(incidentId).type()).isEqualTo("FLEET_ISSUE");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            assertThat(api.fleetIssuesReportedFor(incidentId)).containsExactly(
                new FleetIssueReported(incidentId, b22, "the lock doesn't open", warsaw("2026-11-09T08:05"))));
    }

    @Test
    @DisplayName("E2.2 a safety report is a safety concern")
    void e2_2_safety_report_is_safety_concern() {
        // given
        var ben = unique("ben");
        clock.setTo(warsaw("2026-11-09T17:40"));

        // when
        var incidentId = api.report(ben, "SAFETY", "a car cut me off at the Central crossing", null);

        // then
        assertThat(api.incident(incidentId).type()).isEqualTo("SAFETY_CONCERN");
        assertThat(api.fleetIssuesReportedFor(incidentId)).isEmpty();
    }

    @Test
    @DisplayName("E2.3 a broken-bike report without a bike is other and not handed over")
    void e2_3_broken_bike_without_bike_is_other_and_not_handed_over() {
        // given
        var anna = unique("anna");
        clock.setTo(warsaw("2026-11-10T12:00"));

        // when
        var incidentId = api.report(anna, "BROKEN_BIKE", "the bike I rode yesterday creaked", null);

        // then: published synchronously within the request (plan D7), so absence is deterministic
        assertThat(api.incident(incidentId).type()).isEqualTo("OTHER");
        assertThat(api.fleetIssuesReportedFor(incidentId)).isEmpty();
    }

    @Test
    @DisplayName("E3.1 a broken-bike report for B-17 is a fleet issue handed to Fleet Maintenance")
    void e3_1_fleet_issue_makes_bike_broken_and_unreservable() {
        // given
        var anna = unique("anna");
        var b17 = unique("B-17");
        clock.setTo(warsaw("2026-11-08T10:00"));

        // when
        var incidentId = api.report(anna, "BROKEN_BIKE", "rear brake doesn't work", b17);

        // then: Customer Support's part, the hand-over
        assertThat(api.incident(incidentId).type()).isEqualTo("FLEET_ISSUE");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            assertThat(api.fleetIssuesReportedFor(incidentId)).singleElement()
                                                              .extracting(FleetIssueReported::bikeId).isEqualTo(b17));
        // TODO plan step S6 (needs fleet-operations-takes-broken-bike-out-of-circulation, fleet-manager-adds-bike-to-fleet,
        //  rider-reserves-bike): add B-17 to Rentals first, then await GET /rentals/bikes/{bikeId} WITHDRAWN and a 409
        //  on a rider's reservation.
    }

    @Test
    @DisplayName("E3.2 a wrongful-charge report naming B-17 is a billing dispute; nothing is handed over")
    void e3_2_wrongful_charge_naming_bike_leaves_bike_available() {
        // given
        var anna = unique("anna");
        var b17 = unique("B-17");
        clock.setTo(warsaw("2026-11-08T10:30"));

        // when
        var incidentId = api.report(anna, "WRONGFUL_CHARGE", "charged for a ride on B-17 I never took", b17);

        // then
        assertThat(api.incident(incidentId).type()).isEqualTo("BILLING_DISPUTE");
        assertThat(api.fleetIssuesReportedFor(incidentId)).isEmpty();
    }

    @Test
    @DisplayName("E3.3 a broken-bike report during a ride is a fleet issue handed to Fleet Maintenance")
    void e3_3_fleet_issue_during_ride_breaks_bike_but_ride_goes_on() {
        // given
        var anna = unique("anna");
        var b17 = unique("B-17");
        clock.setTo(warsaw("2026-11-11T09:55"));

        // when
        var incidentId = api.report(anna, "BROKEN_BIKE", "gears slip", b17);

        // then: Customer Support's part, the hand-over
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            assertThat(api.fleetIssuesReportedFor(incidentId)).hasSize(1));
        // TODO plan step S6 (needs Rentals reservation/unlock and Fleet Maintenance's FleetIssueReported listener):
        //  start Anna's ride on B-17 first, then await B-17 WITHDRAWN while GET /rentals/riders/{riderId}/active-rental
        //  still shows her SESSION.
    }
}
