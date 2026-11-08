package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeEvent.BikeReportedBroken;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeEvent.IncidentLinked;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeEvent.WithdrawalRequested;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeFixture.*;

/// Fault reports: specs `fleet-operations-takes-broken-bike-out-of-circulation` and `technician-reports-fault`.
@DisplayName("Fault report examples")
class FaultReportTest {

    // @formatter:off
    @DisplayName(
        """
         fleet-operations E1.1 given B-17 isn't broken,
         when Rentals reports B-17 withdrawn with reason lock failed at 10:40,
         then B-17 is broken with fault source lock failed (Bike Reported Broken) and no withdrawal is requested
        """
    )
    // @formatter:on
    @Test
    void bikeWithdrawnForFailedLockBecomesBroken() {
        // given
        var bike = Bike.unknown(B17);

        // when
        var events = bike.reportFault(rentalsWithdrew(FaultSource.LOCK_FAILED)).at(at("10:40"));

        // then
        assertThat(eventsOf(events, BikeReportedBroken.class)).containsExactly(
            new BikeReportedBroken(B17, FaultSource.LOCK_FAILED, Optional.empty(), Optional.empty(), Optional.empty(), at("10:40")));
        assertThat(eventsOf(events, WithdrawalRequested.class)).isEmpty();
        assertThat(bike.condition()).isEqualTo(BikeCondition.BROKEN);
        assertThat(bike.faultSource()).contains(FaultSource.LOCK_FAILED);
    }

    // @formatter:off
    @DisplayName(
        """
         fleet-operations E1.2 given B-22 isn't broken,
         when Rentals reports B-22 withdrawn with reason not returned at 20:00,
         then B-22 is broken with fault source not returned (Bike Reported Broken) (DE-21)
        """
    )
    // @formatter:on
    @Test
    void bikeNotReturnedBecomesBroken() {
        // given
        var bike = Bike.unknown(B22);

        // when
        var events = bike.reportFault(rentalsWithdrew(FaultSource.NOT_RETURNED)).at(at("20:00"));

        // then
        assertThat(eventsOf(events, BikeReportedBroken.class)).singleElement()
                                                              .extracting(BikeReportedBroken::source).isEqualTo(FaultSource.NOT_RETURNED);
        assertThat(bike.faultSource()).contains(FaultSource.NOT_RETURNED);
        assertThat(bike.reportedAt()).contains(at("20:00"));
    }

    // @formatter:off
    @DisplayName(
        """
         fleet-operations E2.1 given B-17 isn't broken,
         when Customer Support reports fleet issue I-1 for B-17 at 10:00 ("rear brake doesn't work"),
         then B-17 is broken with fault source fleet issue, linked to I-1 (Bike Reported Broken), and its withdrawal is requested
        """
    )
    // @formatter:on
    @Test
    void fleetIssueMakesBikeBrokenAndRequestsWithdrawal() {
        // given
        var bike = Bike.unknown(B17);

        // when
        var events = bike.reportFault(fleetIssue(I1, "rear brake doesn't work")).at(at("10:00"));

        // then
        assertThat(eventsOf(events, BikeReportedBroken.class)).containsExactly(
            new BikeReportedBroken(B17, FaultSource.FLEET_ISSUE, Optional.of(new FaultDescription("rear brake doesn't work")),
                                   Optional.of(I1), Optional.empty(), at("10:00")));
        assertThat(eventsOf(events, WithdrawalRequested.class)).containsExactly(new WithdrawalRequested(B17, at("10:00")));
        assertThat(bike.incidents()).containsExactly(I1);
    }

    // @formatter:off
    @DisplayName(
        """
         fleet-operations E3.1 given B-17 has been broken since 10:40 with fault source lock failed,
         when Customer Support reports fleet issue I-2 for B-17 at 11:00,
         then I-2 is linked to B-17 and nothing else changes: no second Bike Reported Broken, no withdrawal request
        """
    )
    // @formatter:on
    @Test
    void fleetIssueOnBrokenBikeOnlyLinksIncident() {
        // given
        var bike = brokenBike(B17, FaultSource.LOCK_FAILED, "10:40");

        // when
        var events = bike.reportFault(fleetIssue(I2, "brake squeals")).at(at("11:00"));

        // then
        assertThat(events.stream().toList()).containsExactly(new IncidentLinked(B17, I2, at("11:00")));
        assertThat(bike.incidents()).containsExactly(I2);
        assertThat(bike.faultSource()).contains(FaultSource.LOCK_FAILED);
        assertThat(bike.reportedAt()).contains(at("10:40"));
    }

    // @formatter:off
    @DisplayName(
        """
         technician-reports-fault E1.1 given B-22 isn't broken,
         when technician Tom reports a fault on B-22 at 09:00 ("loose handlebar"),
         then B-22 is broken with fault source technician (Bike Reported Broken) and its withdrawal is requested
        """
    )
    // @formatter:on
    @Test
    void technicianFaultReportMakesBikeBrokenAndRequestsWithdrawal() {
        // given
        var bike = Bike.unknown(B22);

        // when
        var events = bike.reportFault(technicianReport(TOM, "loose handlebar")).at(at("09:00"));

        // then
        assertThat(events.stream().toList()).containsExactly(
            new BikeReportedBroken(B22, FaultSource.TECHNICIAN, Optional.of(new FaultDescription("loose handlebar")),
                                   Optional.empty(), Optional.of(TOM), at("09:00")),
            new WithdrawalRequested(B22, at("09:00")));
        assertThat(bike.faultDescription()).contains(new FaultDescription("loose handlebar"));
    }

    // @formatter:off
    @DisplayName(
        """
         technician-reports-fault E3.1 given B-17 has been broken since 08:00 with fault source lock failed,
         when Tom reports a fault on B-17 at 09:00 ("bent mudguard"),
         then nothing is recorded and B-17 stays broken with fault source lock failed
        """
    )
    // @formatter:on
    @Test
    void technicianFaultReportOnBrokenBikeChangesNothing() {
        // given
        var bike = brokenBike(B17, FaultSource.LOCK_FAILED, "08:00");

        // when
        var events = bike.reportFault(technicianReport(TOM, "bent mudguard")).at(at("09:00"));

        // then
        assertThat(events.stream().toList()).isEmpty();
        assertThat(bike.faultSource()).contains(FaultSource.LOCK_FAILED);
        assertThat(bike.faultDescription()).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         derived: given B-17 was fixed and is back in circulation,
         when Rentals reports B-17 withdrawn with reason fault signalled at 15:00,
         then B-17 is broken again, with no incident links left from the earlier fault
        """
    )
    // @formatter:on
    @Test
    void fixedBikeCanBreakAgain() {
        // given
        var bike = bikeUnderMaintenance(B17, TOM, "09:20");
        bike.reportFault(fleetIssue(I1, "flat tyre")).at(at("09:30"));
        bike.returnToCirculation(new CirculationReturn(TOM, Place.station("Central"), at("11:00")))
            .through(new FakeCirculationReturns());

        // when
        var events = bike.reportFault(rentalsWithdrew(FaultSource.FAULT_SIGNALLED)).at(at("15:00"));

        // then
        assertThat(eventsOf(events, BikeReportedBroken.class)).hasSize(1);
        assertThat(bike.condition()).isEqualTo(BikeCondition.BROKEN);
        assertThat(bike.incidents()).isEqualTo(List.of());
    }
}
