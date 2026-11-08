package pl.wojtyna.dddtrainingv3.velocity.journeys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor.ResponseCode;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.OLD_TOWN_SQUARE_DROP_ZONE;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.expect;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.unique;

/// From a new person to money on VeloCity's account: Accounts → Payments (card), Rentals (bike, ride),
/// Rentals → Settlement (`RentalSessionEnded`), Settlement → Pricing (price) and → Payments (charge).
@VelocitySpringTest
class RideToMoneyJourneyTest extends JourneyTestBase {

    @Autowired
    SimulatedPaymentProcessor paymentProcessor;

    @Test
    @DisplayName("""
        A person onboards in the app with a card, reserves a new bike at Central standing next to it, rides it for \
        30 minutes and ends the ride inside the Old Town Square drop zone: the ride is settled at PLN 5.00 \
        (hour 1 PLN 3.00 + drop zone fee PLN 2.00) and the payment processor captured PLN 5.00 from the card""")
    void rideIsPricedAndCharged() {
        clock.setTo("2027-03-10T09:00:00Z");
        var cardToken = "tok_visa_" + unique("ride-to-money");

        // the person onboards in the rider app
        var riderId = expect(apps.register(unique("anna") + "@example.com"), 201).get("riderId");
        expect(apps.confirmEmail(riderId), 200);
        expect(apps.verifyPhone(riderId, "+48 600 100 200"), 200);
        expect(apps.providePersonalData(riderId, "Anna Nowak", "1995-04-12", "Poland"), 200);
        assertThat(expect(apps.addCard(riderId, cardToken), 200).get("verified")).isEqualTo("true");
        expect(apps.signUserAgreement(riderId), 200);
        var account = expect(apps.account(riderId), 200);
        assertThat(account.get("activated")).isEqualTo("true");
        assertThat(account.get("onboardingStep")).isEqualTo("NONE");
        assertThat(expect(apps.rentalEligibility(riderId), 200).get("eligible")).isEqualTo("true");

        // a fleet manager adds a bike at Central
        var bikeId = unique("bike-r2m");
        expect(apps.addBikeToFleet(bikeId, CENTRAL.stationId()), 201);

        // the rider reserves it standing next to it, unlocks it and rides 30 minutes
        expect(apps.reserve(bikeId, riderId, CENTRAL.tenMetresNorth()), 201);
        var unlock = expect(apps.unlock(bikeId, riderId), 200);
        assertThat(unlock.get("outcome")).isEqualTo("UNLOCKED");
        var sessionId = unlock.get("sessionId");
        clock.advanceBy(Duration.ofMinutes(30));

        // ...and ends it inside the Old Town Square drop zone
        assertThat(expect(apps.endRideHere(bikeId, riderId, OLD_TOWN_SQUARE_DROP_ZONE), 200).get("endReason"))
            .isEqualTo("FREE_FLOATING_RETURN");

        // Settlement hears the ride ended, prices it in Pricing and charges it through Payments
        eventually(() -> {
            var ride = expect(apps.rideSettlement(sessionId), 200);
            assertThat(ride.get("status")).isEqualTo("SETTLED");
            assertThat(new BigDecimal(ride.get("fee"))).isEqualByComparingTo("5.00");
            assertThat(ride.get("riderId")).isEqualTo(riderId);
        });
        assertThat(paymentProcessor.capturesOf(cardToken))
            .singleElement()
            .satisfies(capture -> {
                assertThat(capture.amount()).isEqualByComparingTo("5.00");
                assertThat(capture.currency()).isEqualTo("PLN");
                assertThat(capture.responseCode()).isEqualTo(ResponseCode.APPROVED);
            });
        assertThat(expect(apps.balance(riderId), 200).list("unpaidRides")).isEmpty();
        assertThat(expect(apps.bike(bikeId), 200).get("availability")).isEqualTo("AVAILABLE");
    }
}
