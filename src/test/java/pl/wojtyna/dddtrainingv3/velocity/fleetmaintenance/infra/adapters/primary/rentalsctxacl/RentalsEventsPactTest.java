package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary.rentalsctxacl;

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
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReleaseTechnicianReservation;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReleaseTechnicianReservationUseCase;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportBikeBroken;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportBikeBrokenUseCase;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.StartMaintenance;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.StartMaintenanceUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeUnlocked;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.ReservationExpired;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.UnlockFailed;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.EVENT_JSON;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.FLEET_MAINTENANCE;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.INSTANT;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.RENTALS_EVENTS;

/// Consumer side of Rentals' integration events that Fleet Maintenance listens to (CML
/// `Rentals [U,OHS,PL]->[D,ACL] FleetMaintenance`): a bike withdrawn because of a fault becomes a broken bike; a
/// technician's unlock starts maintenance; a technician's reservation that expired or whose unlock failed is released.
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = RENTALS_EVENTS, providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Rentals → Fleet Maintenance (BikeWithdrawn, BikeUnlocked, ReservationExpired, UnlockFailed)")
class RentalsEventsPactTest {

    private static final String BIKE = "bike-17";
    private static final String TECHNICIAN = "tech-1";
    private static final String AT = "2026-11-08T10:00:00Z";
    private static final String FAULT_REASONS = "LOCK_FAILED|FAULT_REPORTED_AT_RETURN|FAULT_SIGNALLED|NOT_RETURNED";

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact bikeWithdrawnForAFault(MessagePactBuilder builder) {
        return builder.expectsToReceive("a bike was withdrawn because of a fault")
                      .withContent(new PactDslJsonBody().stringType("bikeId", BIKE)
                                                        .stringMatcher("reason", FAULT_REASONS, "FAULT_SIGNALLED")
                                                        .stringMatcher("withdrawnAt", INSTANT, AT)
                                                        .stringType("faultDescription", "E42"))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact bikeUnlockedForATechnician(MessagePactBuilder builder) {
        return builder.expectsToReceive("a withdrawn bike was unlocked for a technician")
                      .withContent(technicianHold().stringMatcher("unlockedAt", INSTANT, AT))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact technicianReservationExpired(MessagePactBuilder builder) {
        return builder.expectsToReceive("a technician's reservation expired")
                      .withContent(technicianHold().stringMatcher("expiredAt", INSTANT, AT))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact technicianUnlockFailed(MessagePactBuilder builder) {
        return builder.expectsToReceive("a technician's unlock failed")
                      .withContent(technicianHold().stringMatcher("failedAt", INSTANT, AT))
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "bikeWithdrawnForAFault")
    void bikeWithdrawnForAFaultIsReportedBroken(V4Interaction.AsynchronousMessage message) throws Exception {
        var reportBikeBroken = mock(ReportBikeBrokenUseCase.class);

        new BikeWithdrawnListener(reportBikeBroken).on(EVENT_JSON.readValue(message.contentsAsBytes(), BikeWithdrawn.class));

        verify(reportBikeBroken).handle(new ReportBikeBroken(BIKE, "FAULT_SIGNALLED", "E42", null, Instant.parse(AT)));
    }

    @Test
    @PactTestFor(pactMethod = "bikeUnlockedForATechnician")
    void technicianUnlockStartsMaintenance(V4Interaction.AsynchronousMessage message) throws Exception {
        var startMaintenance = mock(StartMaintenanceUseCase.class);

        new BikeUnlockedListener(startMaintenance).on(EVENT_JSON.readValue(message.contentsAsBytes(), BikeUnlocked.class));

        verify(startMaintenance).handle(new StartMaintenance(BIKE, TECHNICIAN, Instant.parse(AT)));
    }

    @Test
    @PactTestFor(pactMethod = "technicianReservationExpired")
    void expiredTechnicianReservationIsReleased(V4Interaction.AsynchronousMessage message) throws Exception {
        var release = mock(ReleaseTechnicianReservationUseCase.class);

        new ReservationExpiredListener(release).on(EVENT_JSON.readValue(message.contentsAsBytes(), ReservationExpired.class));

        verify(release).handle(new ReleaseTechnicianReservation(BIKE, TECHNICIAN, Instant.parse(AT)));
    }

    @Test
    @PactTestFor(pactMethod = "technicianUnlockFailed")
    void failedTechnicianUnlockReleasesTheReservation(V4Interaction.AsynchronousMessage message) throws Exception {
        var release = mock(ReleaseTechnicianReservationUseCase.class);

        new UnlockFailedListener(release).on(EVENT_JSON.readValue(message.contentsAsBytes(), UnlockFailed.class));

        verify(release).handle(new ReleaseTechnicianReservation(BIKE, TECHNICIAN, Instant.parse(AT)));
    }

    /// Fleet Maintenance acts only on technicians' holds, so `holderType` must say `TECHNICIAN`.
    private static PactDslJsonBody technicianHold() {
        return new PactDslJsonBody().stringType("bikeId", BIKE).stringValue("holderType", "TECHNICIAN").stringType("holderId", TECHNICIAN);
    }
}
