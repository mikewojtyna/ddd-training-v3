package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentEvent.FleetIssueReported;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentEvent.IncidentReported;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/// Incident examples from the specs `rider-reports-incident`, `support-agent-decides-on-incident`,
/// `support-agent-escalates-incident`, `rider-reopens-rejected-incident`, `support-agent-resolves-incident` and
/// `technician-returns-fixed-bike-to-circulation` (Customer Support's half). Times are given as UTC instants;
/// 10:00 in Warsaw in November is 09:00Z.
@DisplayName("Incident examples")
class IncidentTest {

    static final RiderId ANNA = new RiderId("anna");
    static final RiderId BEN = new RiderId("ben");
    static final AgentId ZOE = new AgentId("zoe");
    static final AgentId MAX = new AgentId("max");

    static Incident.ReportedIncident report(String id, RiderId rider, Category category, String description,
                                            String bike, String reportedAt) {
        return Incident.report(new IncidentId(id),
                               new IncidentReport(rider, category, new Description(description),
                                                  Optional.ofNullable(bike).map(BikeId::new)),
                               Instant.parse(reportedAt));
    }

    static <T extends DomainEvent> List<T> eventsOf(DomainEvents events, Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    @Nested
    @DisplayName("rider-reports-incident")
    class Reporting {

        // @formatter:off
        @DisplayName(
            """
             E1.1 given Anna has no incidents,
             when she reports wrongful charge with "charged twice for one ride" at 18:20 on 2026-11-08,
             then incident I-2 is reported for Anna as open, with no assigned agent and type billing dispute (Incident Reported)
            """
        )
        // @formatter:on
        @Test
        void wrongfulChargeReportIsOpenBillingDispute() {
            // when
            var reported = report("I-2", ANNA, Category.WRONGFUL_CHARGE, "charged twice for one ride", null, "2026-11-08T17:20:00Z");

            // then
            assertThat(eventsOf(reported.events(), IncidentReported.class)).containsExactly(
                new IncidentReported(new IncidentId("I-2"), ANNA, Category.WRONGFUL_CHARGE, IncidentType.BILLING_DISPUTE,
                                     new Description("charged twice for one ride"), Optional.empty(),
                                     Instant.parse("2026-11-08T17:20:00Z")));
            assertThat(reported.incident().status()).isEqualTo(IncidentStatus.OPEN);
            assertThat(reported.incident().assignedAgent()).isEmpty();
        }

        // @formatter:off
        @DisplayName(
            """
             E1.2 given Ben's account is blocked for outstanding debt,
             when Ben reports other with "the app shows a ride I didn't take" at 09:10 on 2026-11-10,
             then incident I-8 is reported for Ben with type other (Incident Reported) (DE-97)
            """
        )
        // @formatter:on
        @Test
        void blockedRiderCanReportIncident() {
            // given: Customer Support doesn't know about account blocks (plan D6)

            // when
            var reported = report("I-8", BEN, Category.OTHER, "the app shows a ride I didn't take", null, "2026-11-10T08:10:00Z");

            // then
            assertThat(eventsOf(reported.events(), IncidentReported.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.riderId()).isEqualTo(BEN);
                    assertThat(event.type()).isEqualTo(IncidentType.OTHER);
                });
        }

        // @formatter:off
        @DisplayName(
            """
             E2.1 given bike B-22 stands at station Old Town,
             when Ben reports can't unlock for B-22 with "the lock doesn't open" at 08:05 on 2026-11-09,
             then incident I-3 is reported with type fleet issue (Incident Reported, Fleet Issue Reported)
            """
        )
        // @formatter:on
        @Test
        void cantUnlockWithBikeIsFleetIssue() {
            // when
            var reported = report("I-3", BEN, Category.CANT_UNLOCK, "the lock doesn't open", "B-22", "2026-11-09T07:05:00Z");

            // then
            assertThat(eventsOf(reported.events(), IncidentReported.class))
                .singleElement().extracting(IncidentReported::type).isEqualTo(IncidentType.FLEET_ISSUE);
            assertThat(eventsOf(reported.events(), FleetIssueReported.class)).containsExactly(
                new FleetIssueReported(new IncidentId("I-3"), new BikeId("B-22"), new Description("the lock doesn't open"),
                                       Instant.parse("2026-11-09T07:05:00Z")));
        }

        // @formatter:off
        @DisplayName(
            """
             E2.2 given Ben has no incidents,
             when he reports safety with "a car cut me off at the Central crossing" at 17:40 on 2026-11-09,
             then incident I-4 is reported with type safety concern (Incident Reported)
            """
        )
        // @formatter:on
        @Test
        void safetyReportIsSafetyConcern() {
            // when
            var reported = report("I-4", BEN, Category.SAFETY, "a car cut me off at the Central crossing", null, "2026-11-09T16:40:00Z");

            // then
            assertThat(eventsOf(reported.events(), IncidentReported.class))
                .singleElement().extracting(IncidentReported::type).isEqualTo(IncidentType.SAFETY_CONCERN);
            assertThat(eventsOf(reported.events(), FleetIssueReported.class)).isEmpty();
        }

        // @formatter:off
        @DisplayName(
            """
             E2.3 given Anna has no open incidents,
             when she reports broken bike without naming a bike, with "the bike I rode yesterday creaked" at 12:00 on 2026-11-10,
             then incident I-5 is reported with type other and nothing is handed over to technicians (Incident Reported)
            """
        )
        // @formatter:on
        @Test
        void brokenBikeWithoutBikeIsOtherAndNotHandedOver() {
            // when
            var reported = report("I-5", ANNA, Category.BROKEN_BIKE, "the bike I rode yesterday creaked", null, "2026-11-10T11:00:00Z");

            // then
            assertThat(eventsOf(reported.events(), IncidentReported.class))
                .singleElement().extracting(IncidentReported::type).isEqualTo(IncidentType.OTHER);
            assertThat(eventsOf(reported.events(), FleetIssueReported.class)).isEmpty();
        }

        // @formatter:off
        @DisplayName(
            """
             E3.1 given bike B-17 is available at station Central,
             when Anna reports broken bike for B-17 with "rear brake doesn't work" at 10:00 on 2026-11-08,
             then incident I-1 is reported with type fleet issue and B-17 is handed over to technicians
             (Incident Reported, Fleet Issue Reported); making it broken is Fleet Maintenance's part
            """
        )
        // @formatter:on
        @Test
        void brokenBikeReportIsFleetIssueHandedToFleetMaintenance() {
            // when
            var reported = report("I-1", ANNA, Category.BROKEN_BIKE, "rear brake doesn't work", "B-17", "2026-11-08T09:00:00Z");

            // then
            assertThat(eventsOf(reported.events(), IncidentReported.class))
                .singleElement().extracting(IncidentReported::type).isEqualTo(IncidentType.FLEET_ISSUE);
            assertThat(eventsOf(reported.events(), FleetIssueReported.class)).containsExactly(
                new FleetIssueReported(new IncidentId("I-1"), new BikeId("B-17"), new Description("rear brake doesn't work"),
                                       Instant.parse("2026-11-08T09:00:00Z")));
        }

        // @formatter:off
        @DisplayName(
            """
             E3.2 given bike B-17 is available at Central,
             when Anna reports wrongful charge naming B-17 with "charged for a ride on B-17 I never took",
             then the incident's type is billing dispute and nothing is handed over to technicians (Incident Reported)
            """
        )
        // @formatter:on
        @Test
        void wrongfulChargeNamingBikeIsNotHandedOver() {
            // when
            var reported = report("I-10", ANNA, Category.WRONGFUL_CHARGE, "charged for a ride on B-17 I never took", "B-17", "2026-11-08T09:30:00Z");

            // then
            assertThat(eventsOf(reported.events(), IncidentReported.class))
                .singleElement().extracting(IncidentReported::type).isEqualTo(IncidentType.BILLING_DISPUTE);
            assertThat(eventsOf(reported.events(), FleetIssueReported.class)).isEmpty();
        }

        // @formatter:off
        @DisplayName(
            """
             E3.3 given Anna is riding B-17 in a rental session that started at 09:40 on 2026-11-11,
             when she reports broken bike for B-17 with "gears slip" at 09:55,
             then incident I-9 is a fleet issue handed over to technicians (Incident Reported, Fleet Issue Reported);
             the ride going on is Rentals' part (DE-86)
            """
        )
        // @formatter:on
        @Test
        void brokenBikeReportDuringRideIsFleetIssue() {
            // when
            var reported = report("I-9", ANNA, Category.BROKEN_BIKE, "gears slip", "B-17", "2026-11-11T08:55:00Z");

            // then
            assertThat(eventsOf(reported.events(), FleetIssueReported.class))
                .singleElement().extracting(FleetIssueReported::bikeId).isEqualTo(new BikeId("B-17"));
        }
    }

    static Incident annasBillingDisputeI2() {
        return report("I-2", ANNA, Category.WRONGFUL_CHARGE, "charged twice for one ride", null, "2026-11-08T17:20:00Z").incident();
    }

    static Incident bensSafetyConcernI4() {
        return report("I-4", BEN, Category.SAFETY, "a car cut me off at the Central crossing", null, "2026-11-09T16:40:00Z").incident();
    }

    static Incident bensBillingDisputeI6() {
        return report("I-6", BEN, Category.WRONGFUL_CHARGE, "charged PLN 1.00 when I added my card", null, "2026-11-09T08:00:00Z").incident();
    }

    static Instant at(String instant) {
        return Instant.parse(instant);
    }

    @Nested
    @DisplayName("support-agent-decides-on-incident: picking up")
    class PickingUp {

        // @formatter:off
        @DisplayName(
            """
             E1.1 given Anna's incident I-2 (billing dispute, "charged twice for one ride") is open,
             when support agent Zoe picks it up at 09:00 on 2026-11-09,
             then I-2 is assigned to Zoe (Incident Picked Up)
            """
        )
        // @formatter:on
        @Test
        void agentPicksUpOpenIncident() {
            // given
            var incident = annasBillingDisputeI2();

            // when
            var events = incident.pickUpBy(ZOE).at(at("2026-11-09T08:00:00Z"));

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentPickedUp.class)).containsExactly(
                new IncidentEvent.IncidentPickedUp(new IncidentId("I-2"), ZOE, at("2026-11-09T08:00:00Z")));
            assertThat(incident.assignedAgent()).contains(ZOE);
            assertThat(incident.status()).isEqualTo(IncidentStatus.PICKED_UP);
        }

        // @formatter:off
        @DisplayName(
            """
             E1.2 given I-2 is assigned to Zoe,
             when support agent Max picks up I-2,
             then the pick-up is rejected and I-2 stays assigned to Zoe
            """
        )
        // @formatter:on
        @Test
        void secondPickUpIsRejected() {
            // given
            var incident = annasBillingDisputeI2();
            incident.pickUpBy(ZOE).at(at("2026-11-09T08:00:00Z"));

            // when
            var exception = catchThrowable(() -> incident.pickUpBy(MAX).at(at("2026-11-09T08:05:00Z")));

            // then
            assertThat(exception).isInstanceOf(IncidentAlreadyAssigned.class);
            assertThat(incident.assignedAgent()).contains(ZOE);
        }

        // @formatter:off
        @DisplayName(
            """
             E3.1 given I-2 is assigned to Zoe and not yet decided,
             when Max accepts I-2,
             then the acceptance is rejected and I-2 stays undecided with Zoe
            """
        )
        // @formatter:on
        @Test
        void otherAgentCannotAccept() {
            // given
            var incident = annasBillingDisputeI2();
            incident.pickUpBy(ZOE).at(at("2026-11-09T08:00:00Z"));

            // when
            var exception = catchThrowable(() -> incident.acceptBy(MAX).at(at("2026-11-09T08:30:00Z")));

            // then
            assertThat(exception).isInstanceOf(NotAssignedAgent.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.PICKED_UP);
            assertThat(incident.assignedAgent()).contains(ZOE);
        }

        // @formatter:off
        @DisplayName(
            """
             E3.2 given Ben's incident I-4 (safety concern) is open,
             when Zoe accepts I-4 without picking it up,
             then the acceptance is rejected and I-4 stays open
            """
        )
        // @formatter:on
        @Test
        void openIncidentCannotBeAcceptedWithoutPickUp() {
            // given
            var incident = bensSafetyConcernI4();

            // when
            var exception = catchThrowable(() -> incident.acceptBy(ZOE).at(at("2026-11-09T17:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(NotAssignedAgent.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.OPEN);
        }
    }

    static final java.time.ZoneId WARSAW = java.time.ZoneId.of("Europe/Warsaw");

    static ReopeningWindow windowAfterRejectionAt(String rejectedAt) {
        return ReopeningWindow.afterRejectionAt(at(rejectedAt), WARSAW);
    }

    @Nested
    @DisplayName("support-agent-decides-on-incident: accepting and rejecting")
    class Deciding {

        // @formatter:off
        @DisplayName(
            """
             E2.1 given I-2 is assigned to Zoe and not yet decided,
             when Zoe accepts I-2 at 09:30 on 2026-11-09,
             then I-2 is accepted (Incident Accepted)
            """
        )
        // @formatter:on
        @Test
        void assignedAgentAcceptsIncident() {
            // given
            var incident = annasBillingDisputeI2();
            incident.pickUpBy(ZOE).at(at("2026-11-09T08:00:00Z"));

            // when
            var events = incident.acceptBy(ZOE).at(at("2026-11-09T08:30:00Z"));

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentAccepted.class)).containsExactly(
                new IncidentEvent.IncidentAccepted(new IncidentId("I-2"), ZOE, at("2026-11-09T08:30:00Z")));
            assertThat(incident.status()).isEqualTo(IncidentStatus.ACCEPTED);
        }

        // @formatter:off
        @DisplayName(
            """
             E2.2 given Ben's incident I-6 (billing dispute, "charged PLN 1.00 when I added my card") is assigned to Zoe,
             when Zoe rejects I-6 at 10:00 on 2026-11-09 with the reason "the PLN 1.00 was a card check, released at once",
             then I-6 is rejected with that reason (Incident Rejected)
            """
        )
        // @formatter:on
        @Test
        void assignedAgentRejectsIncidentWithReason() {
            // given
            var incident = bensBillingDisputeI6();
            incident.pickUpBy(ZOE).at(at("2026-11-09T08:30:00Z"));
            var window = windowAfterRejectionAt("2026-11-09T09:00:00Z");

            // when
            var events = incident.rejectBy(ZOE)
                                 .because(new RejectionReason("the PLN 1.00 was a card check, released at once"))
                                 .within(window);

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentRejected.class)).containsExactly(
                new IncidentEvent.IncidentRejected(new IncidentId("I-6"), BEN, ZOE,
                                                   new RejectionReason("the PLN 1.00 was a card check, released at once"),
                                                   at("2026-11-09T09:00:00Z"), window.closesAt()));
            assertThat(incident.status()).isEqualTo(IncidentStatus.REJECTED);
        }

        // @formatter:off
        @DisplayName(
            """
             E4.1 given I-2 was accepted by Zoe,
             when Zoe rejects I-2,
             then the rejection is rejected and I-2 stays accepted
            """
        )
        // @formatter:on
        @Test
        void acceptedIncidentCannotBeRejected() {
            // given
            var incident = annasBillingDisputeI2();
            incident.pickUpBy(ZOE).at(at("2026-11-09T08:00:00Z"));
            incident.acceptBy(ZOE).at(at("2026-11-09T08:30:00Z"));

            // when
            var exception = catchThrowable(() -> incident.rejectBy(ZOE)
                                                         .because(new RejectionReason("changed my mind"))
                                                         .within(windowAfterRejectionAt("2026-11-09T09:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(IncidentAlreadyDecided.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.ACCEPTED);
        }
    }

    static Incident bensI6RejectedByZoeAt10OnNov9() {
        var incident = bensBillingDisputeI6();
        incident.pickUpBy(ZOE).at(at("2026-11-09T08:30:00Z"));
        incident.rejectBy(ZOE)
                .because(new RejectionReason("the PLN 1.00 was a card check, released at once"))
                .within(windowAfterRejectionAt("2026-11-09T09:00:00Z"));
        return incident;
    }

    @Nested
    @DisplayName("rider-reopens-rejected-incident")
    class Reopening {

        // @formatter:off
        @DisplayName(
            """
             E1.1 given Ben's incident I-6 was rejected by support agent Zoe at 10:00 on 2026-11-09,
             when Ben reopens I-6 at 23:59 on 2026-11-23,
             then I-6 is reopened and assigned to Zoe (Incident Reopened)
            """
        )
        // @formatter:on
        @Test
        void riderReopensOnLastMinuteOfWindow() {
            // given
            var incident = bensI6RejectedByZoeAt10OnNov9();

            // when
            var events = incident.reopenBy(BEN).at(at("2026-11-23T22:59:00Z"));

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentReopened.class)).containsExactly(
                new IncidentEvent.IncidentReopened(new IncidentId("I-6"), BEN, ZOE, at("2026-11-23T22:59:00Z")));
            assertThat(incident.status()).isEqualTo(IncidentStatus.REOPENED);
            assertThat(incident.assignedAgent()).contains(ZOE);
        }

        // @formatter:off
        @DisplayName(
            """
             E1.2 given I-6 was rejected by Zoe at 10:00 on 2026-11-09,
             when Ben reopens I-6 at 00:00 on 2026-11-24,
             then the reopening is rejected and I-6 stays rejected
            """
        )
        // @formatter:on
        @Test
        void reopeningAfterWindowIsRejected() {
            // given
            var incident = bensI6RejectedByZoeAt10OnNov9();

            // when
            var exception = catchThrowable(() -> incident.reopenBy(BEN).at(at("2026-11-23T23:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(ReopeningWindowClosed.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.REJECTED);
        }

        // @formatter:off
        @DisplayName(
            """
             E2.1 given Ben's incident I-6 was rejected at 10:00 on 2026-11-09,
             when Anna reopens I-6 at 12:00 on 2026-11-10,
             then the reopening is rejected and I-6 stays rejected
            """
        )
        // @formatter:on
        @Test
        void otherRiderCannotReopen() {
            // given
            var incident = bensI6RejectedByZoeAt10OnNov9();

            // when
            var exception = catchThrowable(() -> incident.reopenBy(ANNA).at(at("2026-11-10T11:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(NotReportingRider.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.REJECTED);
        }

        // @formatter:off
        @DisplayName(
            """
             E3.1 given Anna's incident I-2 was accepted by Zoe,
             when Anna reopens I-2,
             then the reopening is rejected and I-2 stays accepted
            """
        )
        // @formatter:on
        @Test
        void acceptedIncidentCannotBeReopened() {
            // given
            var incident = annasBillingDisputeI2();
            incident.pickUpBy(ZOE).at(at("2026-11-09T08:00:00Z"));
            incident.acceptBy(ZOE).at(at("2026-11-09T08:30:00Z"));

            // when
            var exception = catchThrowable(() -> incident.reopenBy(ANNA).at(at("2026-11-10T11:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(IncidentNotRejected.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.ACCEPTED);
        }

        // @formatter:off
        @DisplayName(
            """
             E3.2 given Ben reopened I-6 at 09:00 on 2026-11-12,
             when Ben reopens I-6 again at 09:30 on 2026-11-12,
             then the reopening is rejected and I-6 stays reopened
            """
        )
        // @formatter:on
        @Test
        void reopenedIncidentCannotBeReopenedAgain() {
            // given
            var incident = bensI6RejectedByZoeAt10OnNov9();
            incident.reopenBy(BEN).at(at("2026-11-12T08:00:00Z"));

            // when
            var exception = catchThrowable(() -> incident.reopenBy(BEN).at(at("2026-11-12T08:30:00Z")));

            // then
            assertThat(exception).isInstanceOf(IncidentNotRejected.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.REOPENED);
        }
    }

    static Incident bensI6ReopenedAndAssignedToZoe() {
        var incident = bensI6RejectedByZoeAt10OnNov9();
        incident.reopenBy(BEN).at(at("2026-11-12T08:00:00Z"));
        return incident;
    }

    @Nested
    @DisplayName("support-agent-decides-on-incident: reopened incidents")
    class DecidingReopened {

        // @formatter:off
        @DisplayName(
            """
             E2.3 given I-6 was rejected by Zoe, reopened by Ben and is assigned to Zoe again,
             when Zoe accepts I-6 at 09:00 on 2026-11-13,
             then I-6 is accepted (Incident Accepted) (DE-97)
            """
        )
        // @formatter:on
        @Test
        void assignedAgentAcceptsReopenedIncident() {
            // given
            var incident = bensI6ReopenedAndAssignedToZoe();

            // when
            var events = incident.acceptBy(ZOE).at(at("2026-11-13T08:00:00Z"));

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentAccepted.class)).containsExactly(
                new IncidentEvent.IncidentAccepted(new IncidentId("I-6"), ZOE, at("2026-11-13T08:00:00Z")));
            assertThat(incident.status()).isEqualTo(IncidentStatus.ACCEPTED);
        }

        // @formatter:off
        @DisplayName(
            """
             E4.2 given I-6 was rejected by Zoe and Ben reopened it, so it is reopened and assigned to Zoe,
             when Zoe rejects I-6,
             then the rejection is rejected and I-6 stays reopened; Zoe has to resolve it (DE-54)
            """
        )
        // @formatter:on
        @Test
        void reopenedIncidentCannotBeRejectedAgain() {
            // given
            var incident = bensI6ReopenedAndAssignedToZoe();

            // when
            var exception = catchThrowable(() -> incident.rejectBy(ZOE)
                                                         .because(new RejectionReason("still a card check"))
                                                         .within(windowAfterRejectionAt("2026-11-13T08:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(ReopenedIncidentCannotBeRejected.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.REOPENED);
        }

        // @formatter:off
        @DisplayName(
            """
             R4 (no spec example; derived) given I-2 was accepted by Zoe,
             when Zoe accepts I-2 again,
             then the acceptance is rejected: an accepted incident can't be decided again
            """
        )
        // @formatter:on
        @Test
        void acceptedIncidentCannotBeAcceptedAgain() {
            // given
            var incident = annasBillingDisputeI2();
            incident.pickUpBy(ZOE).at(at("2026-11-09T08:00:00Z"));
            incident.acceptBy(ZOE).at(at("2026-11-09T08:30:00Z"));

            // when
            var exception = catchThrowable(() -> incident.acceptBy(ZOE).at(at("2026-11-09T09:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(IncidentAlreadyDecided.class);
        }
    }

    static Incident annasI2AcceptedByZoe() {
        var incident = annasBillingDisputeI2();
        incident.pickUpBy(ZOE).at(at("2026-11-09T08:00:00Z"));
        incident.acceptBy(ZOE).at(at("2026-11-09T08:30:00Z"));
        return incident;
    }

    static Incident bensOtherI7AcceptedByZoe() {
        var incident = report("I-7", BEN, Category.OTHER, "the app charged me in the wrong currency", null, "2026-11-09T09:00:00Z").incident();
        incident.pickUpBy(ZOE).at(at("2026-11-09T09:30:00Z"));
        incident.acceptBy(ZOE).at(at("2026-11-09T09:45:00Z"));
        return incident;
    }

    @Nested
    @DisplayName("support-agent-escalates-incident")
    class Escalating {

        // @formatter:off
        @DisplayName(
            """
             E1.1 given Anna's incident I-2 (billing dispute, "charged twice for one ride") was accepted by support agent Zoe,
             when Zoe escalates I-2 to the billing team at 10:00 on 2026-11-09,
             then I-2 is escalated to billing and stays assigned to Zoe (Incident Escalated)
            """
        )
        // @formatter:on
        @Test
        void assignedAgentEscalatesAcceptedIncidentToBilling() {
            // given
            var incident = annasI2AcceptedByZoe();

            // when
            var events = incident.escalateBy(ZOE).to(Team.BILLING).at(at("2026-11-09T09:00:00Z"));

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentEscalated.class)).containsExactly(
                new IncidentEvent.IncidentEscalated(new IncidentId("I-2"), ZOE, Team.BILLING, at("2026-11-09T09:00:00Z")));
            assertThat(incident.status()).isEqualTo(IncidentStatus.ESCALATED);
            assertThat(incident.assignedAgent()).contains(ZOE);
        }

        // @formatter:off
        @DisplayName(
            """
             E2.1 given Ben's incident I-6 was rejected by Zoe,
             when Zoe escalates I-6 to the billing team,
             then the escalation is rejected and I-6 stays rejected
            """
        )
        // @formatter:on
        @Test
        void rejectedIncidentCannotBeEscalated() {
            // given
            var incident = bensI6RejectedByZoeAt10OnNov9();

            // when
            var exception = catchThrowable(() -> incident.escalateBy(ZOE).to(Team.BILLING).at(at("2026-11-09T10:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(IncidentNotAccepted.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.REJECTED);
        }

        // @formatter:off
        @DisplayName(
            """
             E2.2 given Ben's incident I-4 (safety concern) is assigned to Zoe and not yet decided,
             when Zoe escalates I-4 to the safety team,
             then the escalation is rejected and I-4 stays undecided
            """
        )
        // @formatter:on
        @Test
        void undecidedIncidentCannotBeEscalated() {
            // given
            var incident = bensSafetyConcernI4();
            incident.pickUpBy(ZOE).at(at("2026-11-09T17:00:00Z"));

            // when
            var exception = catchThrowable(() -> incident.escalateBy(ZOE).to(Team.SAFETY).at(at("2026-11-09T17:10:00Z")));

            // then
            assertThat(exception).isInstanceOf(IncidentNotAccepted.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.PICKED_UP);
        }

        // @formatter:off
        @DisplayName(
            """
             E2.3 given I-2 is escalated to the billing team,
             when Zoe escalates I-2 to the fleet team,
             then the escalation is rejected and I-2 stays with the billing team (DE-97)
            """
        )
        // @formatter:on
        @Test
        void escalatedIncidentCannotBeEscalatedAgain() {
            // given
            var incident = annasI2AcceptedByZoe();
            incident.escalateBy(ZOE).to(Team.BILLING).at(at("2026-11-09T09:00:00Z"));

            // when
            var exception = catchThrowable(() -> incident.escalateBy(ZOE).to(Team.FLEET).at(at("2026-11-09T09:30:00Z")));

            // then
            assertThat(exception).isInstanceOf(IncidentNotAccepted.class);
            assertThat(incident.team()).contains(Team.BILLING);
        }

        // @formatter:off
        @DisplayName(
            """
             E3.1 given I-2 was accepted by Zoe,
             when support agent Max escalates I-2 to the billing team,
             then the escalation is rejected and I-2 stays accepted
            """
        )
        // @formatter:on
        @Test
        void otherAgentCannotEscalate() {
            // given
            var incident = annasI2AcceptedByZoe();

            // when
            var exception = catchThrowable(() -> incident.escalateBy(MAX).to(Team.BILLING).at(at("2026-11-09T09:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(NotAssignedAgent.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.ACCEPTED);
        }

        // @formatter:off
        @DisplayName(
            """
             E4.2 given I-7 (other, "the app charged me in the wrong currency") was accepted by Zoe,
             when Zoe escalates I-7 to the billing team at 11:15 on 2026-11-09,
             then I-7 is escalated to billing (Incident Escalated), although its type is other; the team need not match the type (DE-97)
            """
        )
        // @formatter:on
        @Test
        void teamNeedNotMatchIncidentType() {
            // given
            var incident = bensOtherI7AcceptedByZoe();

            // when
            var events = incident.escalateBy(ZOE).to(Team.BILLING).at(at("2026-11-09T10:15:00Z"));

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentEscalated.class))
                .singleElement().extracting(IncidentEvent.IncidentEscalated::team).isEqualTo(Team.BILLING);
            assertThat(incident.team()).contains(Team.BILLING);
        }
    }

    static Incident bensI4AcceptedByZoe() {
        var incident = bensSafetyConcernI4();
        incident.pickUpBy(ZOE).at(at("2026-11-09T17:00:00Z"));
        incident.acceptBy(ZOE).at(at("2026-11-09T17:10:00Z"));
        return incident;
    }

    static Incident annasI2EscalatedToBillingByZoe() {
        var incident = annasI2AcceptedByZoe();
        incident.escalateBy(ZOE).to(Team.BILLING).at(at("2026-11-09T09:00:00Z"));
        return incident;
    }

    @Nested
    @DisplayName("support-agent-resolves-incident")
    class Resolving {

        // @formatter:off
        @DisplayName(
            """
             E1.1 given Ben's incident I-4 (safety concern) was accepted by support agent Zoe,
             when Zoe resolves I-4 at 15:00 on 2026-11-09 with "talked to Ben, crossing reported to the city",
             then I-4 is resolved with that resolution (Incident Resolved)
            """
        )
        // @formatter:on
        @Test
        void assignedAgentResolvesAcceptedIncident() {
            // given
            var incident = bensI4AcceptedByZoe();

            // when
            var events = incident.resolveBy(ZOE).with(new Resolution("talked to Ben, crossing reported to the city"))
                                 .at(at("2026-11-09T14:00:00Z"));

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentResolved.class)).containsExactly(
                new IncidentEvent.IncidentResolved(new IncidentId("I-4"), BEN, Optional.of(ZOE),
                                                   new Resolution("talked to Ben, crossing reported to the city"),
                                                   Optional.empty(), at("2026-11-09T14:00:00Z")));
            assertThat(incident.status()).isEqualTo(IncidentStatus.RESOLVED);
        }

        // @formatter:off
        @DisplayName(
            """
             E2.1 given Anna's incident I-2 was escalated by Zoe to the billing team,
             when Zoe resolves I-2 at 16:00 on 2026-11-10 with "billing team refunded the double charge",
             then I-2 is resolved with that resolution, escalated to billing (Incident Resolved)
            """
        )
        // @formatter:on
        @Test
        void agentRecordsTeamOutcomeForEscalatedIncident() {
            // given
            var incident = annasI2EscalatedToBillingByZoe();

            // when
            var events = incident.resolveBy(ZOE).with(new Resolution("billing team refunded the double charge"))
                                 .at(at("2026-11-10T15:00:00Z"));

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentResolved.class)).containsExactly(
                new IncidentEvent.IncidentResolved(new IncidentId("I-2"), ANNA, Optional.of(ZOE),
                                                   new Resolution("billing team refunded the double charge"),
                                                   Optional.of(Team.BILLING), at("2026-11-10T15:00:00Z")));
        }

        // @formatter:off
        @DisplayName(
            """
             E3.1 given Ben's incident I-6 was rejected by Zoe and reopened by Ben, and is assigned to Zoe,
             when Zoe resolves I-6 at 11:00 on 2026-11-13 with "explained the PLN 1.00 card check again, Ben agreed",
             then I-6 is resolved (Incident Resolved)
            """
        )
        // @formatter:on
        @Test
        void rejectingAgentResolvesReopenedIncident() {
            // given
            var incident = bensI6ReopenedAndAssignedToZoe();

            // when
            var events = incident.resolveBy(ZOE).with(new Resolution("explained the PLN 1.00 card check again, Ben agreed"))
                                 .at(at("2026-11-13T10:00:00Z"));

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentResolved.class)).hasSize(1);
            assertThat(incident.status()).isEqualTo(IncidentStatus.RESOLVED);
        }

        // @formatter:off
        @DisplayName(
            """
             E4.1 given I-2 is escalated and assigned to Zoe,
             when support agent Max resolves I-2 with "refunded",
             then the resolution is rejected and I-2 stays escalated
            """
        )
        // @formatter:on
        @Test
        void otherAgentCannotResolve() {
            // given
            var incident = annasI2EscalatedToBillingByZoe();

            // when
            var exception = catchThrowable(() -> incident.resolveBy(MAX).with(new Resolution("refunded")).at(at("2026-11-10T15:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(NotAssignedAgent.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.ESCALATED);
        }

        // @formatter:off
        @DisplayName(
            """
             E5.1 given Ben's incident I-4 is assigned to Zoe and not yet decided,
             when Zoe resolves I-4,
             then the resolution is rejected and I-4 stays undecided
            """
        )
        // @formatter:on
        @Test
        void undecidedIncidentCannotBeResolved() {
            // given
            var incident = bensSafetyConcernI4();
            incident.pickUpBy(ZOE).at(at("2026-11-09T17:00:00Z"));

            // when
            var exception = catchThrowable(() -> incident.resolveBy(ZOE).with(new Resolution("talked to Ben")).at(at("2026-11-09T18:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(IncidentNotResolvable.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.PICKED_UP);
        }

        // @formatter:off
        @DisplayName(
            """
             E5.2 given I-6 was rejected by Zoe and not reopened,
             when Zoe resolves I-6,
             then the resolution is rejected and I-6 stays rejected
            """
        )
        // @formatter:on
        @Test
        void rejectedIncidentCannotBeResolvedByAgent() {
            // given
            var incident = bensI6RejectedByZoeAt10OnNov9();

            // when
            var exception = catchThrowable(() -> incident.resolveBy(ZOE).with(new Resolution("closing")).at(at("2026-11-10T09:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(IncidentNotResolvable.class);
            assertThat(incident.status()).isEqualTo(IncidentStatus.REJECTED);
        }

        // @formatter:off
        @DisplayName(
            """
             E5.3 given I-4 was resolved at 15:00 on 2026-11-09,
             when Zoe resolves I-4 again with "follow-up call",
             then the resolution is rejected and I-4 keeps its first resolution
            """
        )
        // @formatter:on
        @Test
        void resolvedIncidentKeepsItsFirstResolution() {
            // given
            var incident = bensI4AcceptedByZoe();
            incident.resolveBy(ZOE).with(new Resolution("talked to Ben, crossing reported to the city")).at(at("2026-11-09T14:00:00Z"));

            // when
            var exception = catchThrowable(() -> incident.resolveBy(ZOE).with(new Resolution("follow-up call")).at(at("2026-11-10T09:00:00Z")));

            // then
            assertThat(exception).isInstanceOf(IncidentNotResolvable.class);
            assertThat(incident.resolution()).contains(new Resolution("talked to Ben, crossing reported to the city"));
        }

        // @formatter:off
        @DisplayName(
            """
             E6.1 given Ben's incident I-6 was rejected by Zoe at 10:00 on 2026-11-09 and Ben did not reopen it,
             when its reopening window ends at the end of 2026-11-23,
             then I-6 is resolved with "rejection not contested" (Incident Resolved)
            """
        )
        // @formatter:on
        @Test
        void uncontestedRejectionIsResolvedWhenWindowEnds() {
            // given
            var incident = bensI6RejectedByZoeAt10OnNov9();

            // when
            var events = incident.resolveIfUncontestedAt(at("2026-11-23T23:00:00Z"));

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentResolved.class)).containsExactly(
                new IncidentEvent.IncidentResolved(new IncidentId("I-6"), BEN, Optional.empty(),
                                                   Resolution.REJECTION_NOT_CONTESTED, Optional.empty(),
                                                   at("2026-11-23T23:00:00Z")));
            assertThat(incident.status()).isEqualTo(IncidentStatus.RESOLVED);
        }

        // @formatter:off
        @DisplayName(
            """
             E6.1 boundary (plan D5) given I-6 was rejected at 10:00 on 2026-11-09 and not reopened,
             when open incidents are checked at 23:59 on 2026-11-23 (Warsaw),
             then nothing happens and I-6 is still rejected
            """
        )
        // @formatter:on
        @Test
        void rejectionIsStillContestableOnTheLastMinuteOfTheWindow() {
            // given
            var incident = bensI6RejectedByZoeAt10OnNov9();

            // when
            var events = incident.resolveIfUncontestedAt(at("2026-11-23T22:59:00Z"));

            // then
            assertThat(events.stream()).isEmpty();
            assertThat(incident.status()).isEqualTo(IncidentStatus.REJECTED);
        }

        // @formatter:off
        @DisplayName(
            """
             E6.2 given I-6 was rejected at 10:00 on 2026-11-09 and Ben reopened it at 09:00 on 2026-11-12,
             when the end of 2026-11-23 passes,
             then nothing happens and I-6 stays reopened with Zoe
            """
        )
        // @formatter:on
        @Test
        void reopenedIncidentIsNotResolvedByTheClock() {
            // given
            var incident = bensI6ReopenedAndAssignedToZoe();

            // when
            var events = incident.resolveIfUncontestedAt(at("2026-11-23T23:00:00Z"));

            // then
            assertThat(events.stream()).isEmpty();
            assertThat(incident.status()).isEqualTo(IncidentStatus.REOPENED);
            assertThat(incident.assignedAgent()).contains(ZOE);
        }
    }

    static Incident annasFleetIssueI1AboutB17() {
        return report("I-1", ANNA, Category.BROKEN_BIKE, "rear brake doesn't work", "B-17", "2026-11-08T08:30:00Z").incident();
    }

    @Nested
    @DisplayName("technician-returns-fixed-bike-to-circulation (Customer Support's half)")
    class ResolvingFleetIssues {

        // @formatter:off
        @DisplayName(
            """
             E3.1 given B-17 is linked to fleet-issue incident I-1 (accepted by agent Zoe),
             when Fleet Maintenance reports B-17 fixed at 11:00,
             then I-1 is resolved with "bike fixed" (Incident Resolved)
            """
        )
        // @formatter:on
        @Test
        void acceptedFleetIssueIsResolvedWhenBikeIsFixed() {
            // given
            var incident = annasFleetIssueI1AboutB17();
            incident.pickUpBy(ZOE).at(at("2026-11-08T09:10:00Z"));
            incident.acceptBy(ZOE).at(at("2026-11-08T09:20:00Z"));

            // when
            var events = incident.resolveFleetIssueOf(new BikeId("B-17")).at(at("2026-11-08T10:00:00Z"));

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentResolved.class)).containsExactly(
                new IncidentEvent.IncidentResolved(new IncidentId("I-1"), ANNA, Optional.empty(), Resolution.BIKE_FIXED,
                                                   Optional.empty(), at("2026-11-08T10:00:00Z")));
            assertThat(incident.status()).isEqualTo(IncidentStatus.RESOLVED);
        }

        // @formatter:off
        @DisplayName(
            """
             E3.1 given B-17 is linked to fleet-issue incident I-2 (reported, not picked up yet),
             when Fleet Maintenance reports B-17 fixed at 11:00,
             then I-2 is resolved with "bike fixed" (Incident Resolved)
            """
        )
        // @formatter:on
        @Test
        void openFleetIssueIsResolvedWhenBikeIsFixed() {
            // given
            var incident = report("I-2", BEN, Category.CANT_UNLOCK, "the lock doesn't open", "B-17", "2026-11-08T09:30:00Z").incident();

            // when
            var events = incident.resolveFleetIssueOf(new BikeId("B-17")).at(at("2026-11-08T10:00:00Z"));

            // then
            assertThat(eventsOf(events, IncidentEvent.IncidentResolved.class))
                .singleElement().extracting(IncidentEvent.IncidentResolved::resolution).isEqualTo(Resolution.BIKE_FIXED);
            assertThat(incident.status()).isEqualTo(IncidentStatus.RESOLVED);
        }

        // @formatter:off
        @DisplayName(
            """
             E3.2 given B-17 is linked to fleet-issue incident I-1, which Zoe resolved at 10:00 with "brake adjusted on site",
             when Fleet Maintenance reports B-17 fixed at 11:00,
             then I-1 stays resolved with Zoe's resolution and no second Incident Resolved is recorded for it
            """
        )
        // @formatter:on
        @Test
        void alreadyResolvedFleetIssueKeepsItsResolution() {
            // given
            var incident = annasFleetIssueI1AboutB17();
            incident.pickUpBy(ZOE).at(at("2026-11-08T08:40:00Z"));
            incident.acceptBy(ZOE).at(at("2026-11-08T08:50:00Z"));
            incident.resolveBy(ZOE).with(new Resolution("brake adjusted on site")).at(at("2026-11-08T09:00:00Z"));

            // when
            var events = incident.resolveFleetIssueOf(new BikeId("B-17")).at(at("2026-11-08T10:00:00Z"));

            // then
            assertThat(events.stream()).isEmpty();
            assertThat(incident.resolution()).contains(new Resolution("brake adjusted on site"));
        }

        // @formatter:off
        @DisplayName(
            """
             (derived, plan technician-returns-fixed-bike-to-circulation S4) given Anna's billing dispute I-10 naming B-17,
             when Fleet Maintenance reports B-17 fixed for I-10,
             then it is refused: only a fleet issue about the fixed bike is resolved because the bike is fixed
            """
        )
        // @formatter:on
        @Test
        void onlyFleetIssueOfTheFixedBikeIsResolvedWhenBikeIsFixed() {
            // given
            var billingDispute = report("I-10", ANNA, Category.WRONGFUL_CHARGE, "charged for a ride on B-17 I never took", "B-17", "2026-11-08T09:30:00Z").incident();
            var otherBikesIssue = annasFleetIssueI1AboutB17();

            // when
            var notAFleetIssue = catchThrowable(() -> billingDispute.resolveFleetIssueOf(new BikeId("B-17")).at(at("2026-11-08T10:00:00Z")));
            var otherBike = catchThrowable(() -> otherBikesIssue.resolveFleetIssueOf(new BikeId("B-22")).at(at("2026-11-08T10:00:00Z")));

            // then
            assertThat(notAFleetIssue).isInstanceOf(NotAFleetIssueOfBike.class);
            assertThat(otherBike).isInstanceOf(NotAFleetIssueOfBike.class);
            assertThat(billingDispute.status()).isEqualTo(IncidentStatus.OPEN);
        }

        // @formatter:off
        @DisplayName(
            """
             (derived) given fleet issue I-2 was resolved with "bike fixed" before anyone picked it up,
             when support agent Zoe picks it up,
             then the pick-up is refused: a resolved incident is closed
            """
        )
        // @formatter:on
        @Test
        void resolvedIncidentCannotBePickedUp() {
            // given
            var incident = report("I-2", BEN, Category.CANT_UNLOCK, "the lock doesn't open", "B-17", "2026-11-08T09:30:00Z").incident();
            incident.resolveFleetIssueOf(new BikeId("B-17")).at(at("2026-11-08T10:00:00Z"));

            // when
            var exception = catchThrowable(() -> incident.pickUpBy(ZOE).at(at("2026-11-08T10:30:00Z")));

            // then
            assertThat(exception).isInstanceOf(IncidentAlreadyResolved.class);
        }
    }
}
